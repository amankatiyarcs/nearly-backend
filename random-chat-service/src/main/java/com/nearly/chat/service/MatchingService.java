package com.nearly.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for matching users for random text chat.
 * Uses in-memory structures for fast matching - no database entities for rooms/matches.
 * Priority: Online users who are actively looking get matched first.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final RedisTemplate<String, String> redisTemplate;

    // Priority queue for users looking for text chat (ordered by join time - FIFO)
    private final ConcurrentLinkedDeque<WaitingUser> waitingQueue = new ConcurrentLinkedDeque<>();
    
    // All online users currently connected to chat
    private final Map<String, OnlineUser> onlineUsers = new ConcurrentHashMap<>();
    
    // Active chat rooms: roomId -> RoomInfo
    private final Map<String, RoomInfo> activeRooms = new ConcurrentHashMap<>();
    
    // Session to room mapping: sessionId -> roomId
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    
    // User ID to session ID mapping for authenticated users
    private final Map<Long, String> userIdToSession = new ConcurrentHashMap<>();
    
    // WebSocket sessions storage
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private static final String REDIS_ROOM_PREFIX = "chat:room:";
    private static final String REDIS_ONLINE_PREFIX = "chat:online:";
    
    private final AtomicLong roomCounter = new AtomicLong(System.currentTimeMillis());

    /**
     * Represents a user waiting in the queue for a match.
     */
    public record WaitingUser(
        String sessionId,
        Long userId,           // null for anonymous users
        String username,       // display name
        WebSocketSession session,
        Instant joinedAt,
        boolean isOnline       // true = actively connected via WebSocket
    ) implements Comparable<WaitingUser> {
        @Override
        public int compareTo(WaitingUser other) {
            // Priority: online users first, then by join time (earlier = higher priority)
            if (this.isOnline != other.isOnline) {
                return this.isOnline ? -1 : 1;
            }
            return this.joinedAt.compareTo(other.joinedAt);
        }
    }

    /**
     * Represents an online user connected to the chat system.
     */
    public record OnlineUser(
        String sessionId,
        Long userId,
        String username,
        WebSocketSession session,
        Instant connectedAt,
        Instant lastActive,
        UserStatus status
    ) {}

    /**
     * Represents an active chat room.
     */
    public record RoomInfo(
        String roomId,
        String sessionId1,
        String sessionId2,
        Long userId1,
        Long userId2,
        String username1,
        String username2,
        Instant createdAt,
        boolean isActive
    ) {}

    /**
     * Result of a matching operation.
     */
    public record MatchResult(
        String roomId,
        WebSocketSession partnerSession,
        String partnerSessionId,
        Long partnerUserId,
        String partnerUsername
    ) {}

    public enum UserStatus {
        ONLINE,           // Connected but not looking
        LOOKING,          // In queue looking for match
        IN_CHAT,          // Currently in a chat room
        IDLE              // Connected but inactive
    }

    /**
     * Register a user as online for chat.
     */
    public void registerOnline(String sessionId, Long userId, String username, WebSocketSession session) {
        OnlineUser user = new OnlineUser(
            sessionId,
            userId,
            username != null ? username : "Stranger",
            session,
            Instant.now(),
            Instant.now(),
            UserStatus.ONLINE
        );
        
        onlineUsers.put(sessionId, user);
        sessions.put(sessionId, session);
        
        if (userId != null) {
            userIdToSession.put(userId, sessionId);
        }
        
        // Track in Redis for distributed awareness
        redisTemplate.opsForValue().set(
            REDIS_ONLINE_PREFIX + sessionId,
            userId != null ? userId.toString() : "anonymous",
            Duration.ofMinutes(30)
        );
        
        log.info("User {} (userId: {}) registered online for chat", sessionId, userId);
    }

    /**
     * Unregister a user from chat.
     */
    public void unregisterOnline(String sessionId) {
        OnlineUser user = onlineUsers.remove(sessionId);
        sessions.remove(sessionId);
        
        if (user != null && user.userId() != null) {
            userIdToSession.remove(user.userId());
        }
        
        // Remove from queue
        leaveQueue(sessionId);
        
        // End any active room
        endRoom(sessionId);
        
        redisTemplate.delete(REDIS_ONLINE_PREFIX + sessionId);
        
        log.info("User {} unregistered from chat", sessionId);
    }

    /**
     * Add a user to the matching queue. If a match is found, returns the match result.
     * Priority: Online users who joined earlier get matched first.
     */
    public Optional<MatchResult> joinQueue(String sessionId, WebSocketSession session) {
        return joinQueue(sessionId, null, null, session);
    }

    /**
     * Add an authenticated user to the matching queue with user info.
     */
    public Optional<MatchResult> joinQueue(String sessionId, Long userId, String username, 
                                           WebSocketSession session) {
        log.info("User {} (userId: {}) joining text chat queue", sessionId, userId);

        // Check if already in a room
        if (sessionToRoom.containsKey(sessionId)) {
            log.warn("User {} already in a room, leaving first", sessionId);
            endRoom(sessionId);
        }

        // Remove from any existing queue first
        leaveQueue(sessionId);
        
        // Update online user status to LOOKING
        updateUserStatus(sessionId, UserStatus.LOOKING);
        
        synchronized (waitingQueue) {
            // Try to find a match from the queue
            WaitingUser matchedUser = findBestMatch(sessionId, userId);
            
            if (matchedUser != null) {
                // Found a match! Create a room
                waitingQueue.remove(matchedUser);
                
                String roomId = createRoom(
                    matchedUser.sessionId(), sessionId,
                    matchedUser.userId(), userId,
                    matchedUser.username(), username != null ? username : "Stranger"
                );
                
                // Update statuses
                updateUserStatus(sessionId, UserStatus.IN_CHAT);
                updateUserStatus(matchedUser.sessionId(), UserStatus.IN_CHAT);
                
                log.info("Matched user {} with {} in room {}", sessionId, matchedUser.sessionId(), roomId);
                
                // Return match result - the waiting user gets notified separately
                return Optional.of(new MatchResult(
                    roomId,
                    matchedUser.session(),
                    matchedUser.sessionId(),
                    matchedUser.userId(),
                    matchedUser.username()
                ));
            } else {
                // No match found, add to queue
                WaitingUser waitingUser = new WaitingUser(
                    sessionId,
                    userId,
                    username != null ? username : "Stranger",
                    session,
                    Instant.now(),
                    true // is online
                );
                
                waitingQueue.addLast(waitingUser);
                log.info("User {} added to queue. Queue size: {}", sessionId, waitingQueue.size());
                
                return Optional.empty();
            }
        }
    }

    /**
     * Find the best match from the queue (prioritize online users, then by join time).
     */
    private WaitingUser findBestMatch(String sessionId, Long userId) {
        WaitingUser bestMatch = null;
        
        for (WaitingUser waiting : waitingQueue) {
            // Don't match with self
            if (waiting.sessionId().equals(sessionId)) {
                continue;
            }
            
            // Don't match same user (if authenticated)
            if (userId != null && userId.equals(waiting.userId())) {
                continue;
            }
            
            // Check if the waiting user's session is still open
            if (waiting.session() == null || !waiting.session().isOpen()) {
                continue;
            }
            
            // Prioritize online users
            if (bestMatch == null) {
                bestMatch = waiting;
            } else if (waiting.isOnline() && !bestMatch.isOnline()) {
                bestMatch = waiting;
            } else if (waiting.isOnline() == bestMatch.isOnline() && 
                       waiting.joinedAt().isBefore(bestMatch.joinedAt())) {
                bestMatch = waiting;
            }
        }
        
        return bestMatch;
    }

    /**
     * Remove a user from the waiting queue.
     */
    public void leaveQueue(String sessionId) {
        waitingQueue.removeIf(u -> u.sessionId().equals(sessionId));
        updateUserStatus(sessionId, UserStatus.ONLINE);
        log.info("User {} removed from matching queue", sessionId);
    }

    /**
     * Create a chat room between two users.
     */
    private String createRoom(String sessionId1, String sessionId2, 
                              Long userId1, Long userId2,
                              String username1, String username2) {
        String roomId = "chat-room-" + roomCounter.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        RoomInfo roomInfo = new RoomInfo(
            roomId,
            sessionId1, sessionId2,
            userId1, userId2,
            username1, username2,
            Instant.now(),
            true
        );
        
        activeRooms.put(roomId, roomInfo);
        sessionToRoom.put(sessionId1, roomId);
        sessionToRoom.put(sessionId2, roomId);
        
        // Store in Redis for distributed systems
        redisTemplate.opsForHash().put(REDIS_ROOM_PREFIX + roomId, "session1", sessionId1);
        redisTemplate.opsForHash().put(REDIS_ROOM_PREFIX + roomId, "session2", sessionId2);
        redisTemplate.expire(REDIS_ROOM_PREFIX + roomId, Duration.ofHours(2));
        
        log.info("Created chat room {} between {} and {}", roomId, sessionId1, sessionId2);
        
        return roomId;
    }

    /**
     * End a chat room and notify partner.
     */
    public Optional<String> endRoom(String sessionId) {
        String roomId = sessionToRoom.remove(sessionId);
        if (roomId == null) {
            return Optional.empty();
        }

        RoomInfo roomInfo = activeRooms.remove(roomId);
        if (roomInfo == null) {
            return Optional.empty();
        }

        String partnerSessionId = roomInfo.sessionId1().equals(sessionId) 
            ? roomInfo.sessionId2() 
            : roomInfo.sessionId1();
        
        sessionToRoom.remove(partnerSessionId);
        
        // Update statuses
        updateUserStatus(sessionId, UserStatus.ONLINE);
        updateUserStatus(partnerSessionId, UserStatus.ONLINE);
        
        // Clean up Redis
        redisTemplate.delete(REDIS_ROOM_PREFIX + roomId);

        log.info("Room {} ended", roomId);
        
        return Optional.of(partnerSessionId);
    }

    /**
     * Get the partner session ID in a room.
     */
    public Optional<String> getPartnerSessionId(String sessionId) {
        String roomId = sessionToRoom.get(sessionId);
        if (roomId == null) {
            return Optional.empty();
        }
        
        RoomInfo roomInfo = activeRooms.get(roomId);
        if (roomInfo == null) {
            return Optional.empty();
        }
        
        return Optional.of(
            roomInfo.sessionId1().equals(sessionId) ? roomInfo.sessionId2() : roomInfo.sessionId1()
        );
    }

    /**
     * Get partner user info.
     */
    public Optional<OnlineUser> getPartnerInfo(String sessionId) {
        return getPartnerSessionId(sessionId)
            .map(onlineUsers::get);
    }

    /**
     * Get room ID for a session.
     */
    public Optional<String> getRoomId(String sessionId) {
        return Optional.ofNullable(sessionToRoom.get(sessionId));
    }

    /**
     * Get room info.
     */
    public Optional<RoomInfo> getRoomInfo(String roomId) {
        return Optional.ofNullable(activeRooms.get(roomId));
    }

    /**
     * Check if a session is in a room.
     */
    public boolean isInRoom(String sessionId) {
        return sessionToRoom.containsKey(sessionId);
    }

    /**
     * Check if a session is in queue.
     */
    public boolean isInQueue(String sessionId) {
        return waitingQueue.stream().anyMatch(u -> u.sessionId().equals(sessionId));
    }

    /**
     * Check if a session is waiting.
     */
    public boolean isWaiting(String sessionId) {
        return isInQueue(sessionId);
    }

    /**
     * Get WebSocket session by session ID.
     */
    public Optional<WebSocketSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Store WebSocket session.
     */
    public void storeSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }

    /**
     * Update user heartbeat.
     */
    public void updateHeartbeat(String sessionId) {
        OnlineUser existing = onlineUsers.get(sessionId);
        if (existing != null) {
            OnlineUser updated = new OnlineUser(
                existing.sessionId(),
                existing.userId(),
                existing.username(),
                existing.session(),
                existing.connectedAt(),
                Instant.now(),
                existing.status()
            );
            onlineUsers.put(sessionId, updated);
            redisTemplate.expire(REDIS_ONLINE_PREFIX + sessionId, Duration.ofMinutes(30));
        }
    }

    /**
     * Update user status.
     */
    private void updateUserStatus(String sessionId, UserStatus status) {
        OnlineUser existing = onlineUsers.get(sessionId);
        if (existing != null) {
            OnlineUser updated = new OnlineUser(
                existing.sessionId(),
                existing.userId(),
                existing.username(),
                existing.session(),
                existing.connectedAt(),
                Instant.now(),
                status
            );
            onlineUsers.put(sessionId, updated);
        }
    }

    /**
     * Get total online user count.
     */
    public int getOnlineCount() {
        return onlineUsers.size();
    }

    /**
     * Get users currently looking for matches.
     */
    public int getLookingCount() {
        return waitingQueue.size();
    }

    /**
     * Get waiting queue size.
     */
    public int getWaitingCount() {
        return waitingQueue.size();
    }

    /**
     * Get active rooms count.
     */
    public int getActiveRoomsCount() {
        return activeRooms.size();
    }

    /**
     * Get users currently in chats.
     */
    public int getInChatCount() {
        return activeRooms.size() * 2;
    }

    /**
     * Get statistics.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "onlineUsers", getOnlineCount(),
            "lookingForMatch", getLookingCount(),
            "activeRooms", getActiveRoomsCount(),
            "usersInChat", getInChatCount()
        );
    }

    /**
     * Get online users list (for admin/debug).
     */
    public List<Map<String, Object>> getOnlineUsersList() {
        return onlineUsers.values().stream()
            .map(user -> Map.<String, Object>of(
                "sessionId", user.sessionId(),
                "userId", user.userId() != null ? user.userId() : "anonymous",
                "username", user.username(),
                "status", user.status().name(),
                "connectedAt", user.connectedAt().toString()
            ))
            .toList();
    }

    /**
     * Cleanup stale connections - runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupStaleConnections() {
        Instant staleThreshold = Instant.now().minus(Duration.ofMinutes(5));
        
        // Clean up stale users from queue
        waitingQueue.removeIf(user -> {
            if (user.session() == null || !user.session().isOpen()) {
                log.debug("Removing closed session {} from queue", user.sessionId());
                return true;
            }
            return false;
        });
        
        // Clean up inactive online users
        List<String> staleUsers = onlineUsers.entrySet().stream()
            .filter(entry -> entry.getValue().lastActive().isBefore(staleThreshold))
            .map(Map.Entry::getKey)
            .toList();
        
        staleUsers.forEach(this::unregisterOnline);
        
        // Clean up stale rooms
        List<String> staleRooms = activeRooms.entrySet().stream()
            .filter(entry -> {
                RoomInfo room = entry.getValue();
                WebSocketSession session1 = sessions.get(room.sessionId1());
                WebSocketSession session2 = sessions.get(room.sessionId2());
                return (session1 == null || !session1.isOpen()) && 
                       (session2 == null || !session2.isOpen());
            })
            .map(Map.Entry::getKey)
            .toList();
        
        staleRooms.forEach(roomId -> {
            RoomInfo room = activeRooms.remove(roomId);
            if (room != null) {
                sessionToRoom.remove(room.sessionId1());
                sessionToRoom.remove(room.sessionId2());
                redisTemplate.delete(REDIS_ROOM_PREFIX + roomId);
            }
        });
        
        log.debug("Cleanup: {} stale users removed, {} stale rooms removed. " +
                  "Online: {}, Queue: {}, ActiveRooms: {}",
            staleUsers.size(), staleRooms.size(),
            onlineUsers.size(), waitingQueue.size(), activeRooms.size());
    }
}
