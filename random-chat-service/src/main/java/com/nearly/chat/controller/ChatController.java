package com.nearly.chat.controller;

import com.nearly.chat.model.OnlineUser;
import com.nearly.chat.repository.OnlineUserRepository;
import com.nearly.chat.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for random text chat service.
 * Provides endpoints for:
 * - Online status management
 * - Queue status checking
 * - Statistics and health checks
 */
@RestController
@RequestMapping("/random-chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final MatchingService matchingService;
    private final OnlineUserRepository onlineUserRepository;
    
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);

    /**
     * Get online users count - total users connected to chat.
     */
    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineCount() {
        int onlineCount = matchingService.getOnlineCount();
        int lookingCount = matchingService.getLookingCount();
        
        return ResponseEntity.ok(Map.of(
            "count", onlineCount,
            "online", onlineCount,
            "looking", lookingCount,
            "inChat", matchingService.getInChatCount()
        ));
    }

    /**
     * Get online users count (alternate endpoint for backward compatibility).
     */
    @GetMapping("/online-count")
    public ResponseEntity<Map<String, Object>> getOnlineCountAlt() {
        return getOnlineCount();
    }

    /**
     * Register user as online for chat.
     * Should be called when user enters the random chat section.
     */
    @PostMapping("/online")
    public ResponseEntity<Map<String, Object>> registerOnline(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;
        String username = (String) body.get("username");
        
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "sessionId is required"
            ));
        }
        
        // Track in MongoDB for persistence
        OnlineUser user = onlineUserRepository.findById(sessionId)
            .orElse(OnlineUser.builder()
                .sessionId(sessionId)
                .createdAt(Instant.now())
                .build());
        
        user.setLastActive(Instant.now());
        user.setChatMode("text");
        onlineUserRepository.save(user);
        
        log.info("User {} (userId: {}) registered as online for chat", sessionId, userId);
        
        Map<String, Object> stats = matchingService.getStats();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", sessionId,
            "onlineCount", stats.get("onlineUsers"),
            "lookingForMatch", stats.get("lookingForMatch"),
            "activeRooms", stats.get("activeRooms")
        ));
    }

    /**
     * Unregister user (mark as offline).
     */
    @DeleteMapping("/online/{sessionId}")
    public ResponseEntity<Map<String, Object>> unregisterOnline(@PathVariable String sessionId) {
        onlineUserRepository.deleteById(sessionId);
        matchingService.unregisterOnline(sessionId);
        
        log.info("User {} unregistered from chat", sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Heartbeat to keep user online status active.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId != null) {
            // Update MongoDB
            onlineUserRepository.findById(sessionId).ifPresent(user -> {
                user.setLastActive(Instant.now());
                onlineUserRepository.save(user);
            });
            
            // Update in-memory
            matchingService.updateHeartbeat(sessionId);
            
            log.debug("Heartbeat received from {}", sessionId);
        }
        
        Map<String, Object> stats = matchingService.getStats();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "onlineCount", stats.get("onlineUsers"),
            "lookingForMatch", stats.get("lookingForMatch")
        ));
    }

    /**
     * Get queue status - how many users are looking for matches.
     */
    @GetMapping("/queue/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        Map<String, Object> stats = matchingService.getStats();
        return ResponseEntity.ok(Map.of(
            "queue", stats.get("lookingForMatch"),
            "totalLooking", matchingService.getLookingCount(),
            "onlineUsers", stats.get("onlineUsers"),
            "activeRooms", stats.get("activeRooms")
        ));
    }

    /**
     * Check if user is in queue.
     */
    @GetMapping("/queue/{sessionId}")
    public ResponseEntity<Map<String, Object>> checkQueueStatus(@PathVariable String sessionId) {
        boolean inQueue = matchingService.isInQueue(sessionId);
        boolean inRoom = matchingService.isInRoom(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "inQueue", inQueue,
            "inRoom", inRoom,
            "roomId", matchingService.getRoomId(sessionId).orElse(null),
            "status", inRoom ? "in_chat" : (inQueue ? "looking" : "idle")
        ));
    }

    /**
     * Leave the matching queue.
     */
    @DeleteMapping("/queue/{sessionId}")
    public ResponseEntity<Map<String, Object>> leaveQueue(@PathVariable String sessionId) {
        matchingService.leaveQueue(sessionId);
        log.info("User {} left the matching queue", sessionId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", sessionId
        ));
    }

    /**
     * Get chat statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = matchingService.getStats();
        
        // Add MongoDB count for extra online users
        long mongoOnline = onlineUserRepository.countByLastActiveAfter(Instant.now().minus(STALE_THRESHOLD));
        
        return ResponseEntity.ok(Map.of(
            "onlineUsers", stats.get("onlineUsers"),
            "mongoOnlineUsers", mongoOnline,
            "lookingForMatch", stats.get("lookingForMatch"),
            "activeRooms", stats.get("activeRooms"),
            "usersInChat", stats.get("usersInChat"),
            "status", "online",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Get detailed stats (admin endpoint).
     */
    @GetMapping("/stats/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedStats() {
        Map<String, Object> stats = matchingService.getStats();
        List<Map<String, Object>> onlineUsers = matchingService.getOnlineUsersList();
        
        return ResponseEntity.ok(Map.of(
            "stats", stats,
            "onlineUsers", onlineUsers,
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "random-chat-service",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Get room info by session ID.
     */
    @GetMapping("/room/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getRoomBySession(@PathVariable String sessionId) {
        var roomIdOpt = matchingService.getRoomId(sessionId);
        
        if (roomIdOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "inRoom", false,
                "sessionId", sessionId
            ));
        }
        
        String roomId = roomIdOpt.get();
        var roomInfoOpt = matchingService.getRoomInfo(roomId);
        
        if (roomInfoOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "inRoom", false,
                "sessionId", sessionId
            ));
        }
        
        var roomInfo = roomInfoOpt.get();
        
        return ResponseEntity.ok(Map.of(
            "inRoom", true,
            "roomId", roomId,
            "sessionId", sessionId,
            "createdAt", roomInfo.createdAt().toString()
        ));
    }

    /**
     * End a room by session ID.
     */
    @DeleteMapping("/room/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> endRoomBySession(@PathVariable String sessionId) {
        var partnerOpt = matchingService.endRoom(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "sessionId", sessionId,
            "partnerNotified", partnerOpt.isPresent()
        ));
    }

    /**
     * Clean up stale users who haven't sent heartbeat.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupStaleUsers() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        onlineUserRepository.deleteByLastActiveBefore(cutoff);
        log.debug("Cleaned up stale chat users from MongoDB");
    }
}
