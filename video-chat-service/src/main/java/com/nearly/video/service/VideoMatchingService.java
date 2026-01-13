package com.nearly.video.service;

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
 * Service for matching users for random video/text chat.
 * Uses in-memory structures for fast matching - no database entities for rooms/matches.
 * Priority: Online users who are actively looking get matched first.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoMatchingService {

    private final RedisTemplate<String, String> redisTemplate;

    // Priority queue for users looking for video chat (ordered by join time - FIFO)
    private final ConcurrentLinkedDeque<WaitingUser> videoChatQueue = new ConcurrentLinkedDeque<>();
    
    // Priority queue for users looking for text chat
    private final ConcurrentLinkedDeque<WaitingUser> textChatQueue = new ConcurrentLinkedDeque<>();
    
    // All online users currently connected to video chat feature
    private final Map<String, OnlineUser> onlineUsers = new ConcurrentHashMap<>();
    
    // Active video/chat rooms: roomId -> RoomInfo
    private final Map<String, RoomInfo> activeRooms = new ConcurrentHashMap<>();
    
    // User to room mapping: sessionId -> roomId
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    
    // User ID to session ID mapping for authenticated users
    private final Map<Long, String> userIdToSession = new ConcurrentHashMap<>();
    
    // WebSocket sessions storage
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private static final String REDIS_ROOM_PREFIX = "video:room:";
    private static final String REDIS_ONLINE_PREFIX = "video:online:";
    
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
        ChatMode chatMode,
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
     * Represents an online user connected to the video chat system.
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
     * Represents an active video/chat room.
     */
    public record RoomInfo(
        String roomId,
        String sessionId1,
        String sessionId2,
        Long userId1,
        Long userId2,
        String username1,
        String username2,
        ChatMode chatMode,
        Instant createdAt,
        boolean isActive
    ) {}

    /**
     * Result of a matching operation.
     */
    public record VideoMatchResult(
        String roomId,
        WebSocketSession partnerSession,
        String partnerSessionId,
        Long partnerUserId,
        String partnerUsername,
        boolean isInitiator,
        ChatMode chatMode
    ) {}

    public enum ChatMode {
        VIDEO, TEXT
    }

    public enum UserStatus {
        ONLINE,           // Connected but not looking
        LOOKING,          // In queue looking for match
        IN_CALL,          // Currently in a video/chat room
        IDLE              // Connected but inactive
    }

    /**
     * Register a user as online for video chat.
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
        
        log.info("User {} (userId: {}) registered online for video chat", sessionId, userId);
    }

    /**
     * Unregister a user from video chat.
     */
    public void unregisterOnline(String sessionId) {
        OnlineUser user = onlineUsers.remove(sessionId);
        sessions.remove(sessionId);
        
        if (user != null && user.userId() != null) {
            userIdToSession.remove(user.userId());
        }
        
        // Remove from queues
        leaveQueue(sessionId);
        
        // End any active room
        endRoom(sessionId);
        
        redisTemplate.delete(REDIS_ONLINE_PREFIX + sessionId);
        
        log.info("User {} unregistered from video chat", sessionId);
    }

    /**
     * Add a user to the matching queue. If a match is found, returns the match result.
     * Priority: Online users who joined earlier get matched first.
     */
    public Optional<VideoMatchResult> joinQueue(String sessionId, WebSocketSession session, ChatMode chatMode) {
        return joinQueue(sessionId, null, null, session, chatMode);
    }

    /**
     * Add an authenticated user to the matching queue with user info.
     */
    public Optional<VideoMatchResult> joinQueue(String sessionId, Long userId, String username, 
                                                 WebSocketSession session, ChatMode chatMode) {
        log.info("User {} (userId: {}) joining {} queue", sessionId, userId, chatMode);

        // Check if already in a room
        if (sessionToRoom.containsKey(sessionId)) {
            log.warn("User {} already in a room, leaving first", sessionId);
            endRoom(sessionId);
        }

        // Remove from any existing queue first
        leaveQueue(sessionId);
        
        // Update online user status to LOOKING
        updateUserStatus(sessionId, UserStatus.LOOKING);
        
        // Get the appropriate queue
        ConcurrentLinkedDeque<WaitingUser> queue = chatMode == ChatMode.VIDEO ? videoChatQueue : textChatQueue;
        
        synchronized (queue) {
            // Try to find a match from the queue
            WaitingUser matchedUser = findBestMatch(sessionId, userId, queue);
            
            if (matchedUser != null) {
                // Found a match! Create a room
                queue.remove(matchedUser);
                
                String roomId = createRoom(
                    matchedUser.sessionId(), sessionId,
                    matchedUser.userId(), userId,
                    matchedUser.username(), username != null ? username : "Stranger",
                    chatMode
                );
                
                // Update statuses
                updateUserStatus(sessionId, UserStatus.IN_CALL);
                updateUserStatus(matchedUser.sessionId(), UserStatus.IN_CALL);
                
                log.info("Matched user {} with {} in room {} ({})", sessionId, matchedUser.sessionId(), roomId, chatMode);
                
                // Return match result - the waiting user is the initiator (creates offer)
                return Optional.of(new VideoMatchResult(
                    roomId,
                    matchedUser.session(),
                    matchedUser.sessionId(),
                    matchedUser.userId(),
                    matchedUser.username(),
                    false, // The joining user is NOT the initiator
                    chatMode
                ));
            } else {
                // No match found, add to queue
                WaitingUser waitingUser = new WaitingUser(
                    sessionId,
                    userId,
                    username != null ? username : "Stranger",
                    session,
                    Instant.now(),
                    chatMode,
                    true // is online
                );
                
                queue.addLast(waitingUser);
                log.info("User {} added to {} queue. Queue size: {}", sessionId, chatMode, queue.size());
                
                return Optional.empty();
            }
        }
    }

    /**
     * Find the best match from the queue (prioritize online users, then by join time).
     */
    private WaitingUser findBestMatch(String sessionId, Long userId, ConcurrentLinkedDeque<WaitingUser> queue) {
        WaitingUser bestMatch = null;
        
        for (WaitingUser waiting : queue) {
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
        videoChatQueue.removeIf(u -> u.sessionId().equals(sessionId));
        textChatQueue.removeIf(u -> u.sessionId().equals(sessionId));
        updateUserStatus(sessionId, UserStatus.ONLINE);
        log.info("User {} removed from matching queues", sessionId);
    }

    /**
     * Create a video/chat room between two users.
     */
    private String createRoom(String sessionId1, String sessionId2, 
                              Long userId1, Long userId2,
                              String username1, String username2,
                              ChatMode chatMode) {
        String roomId = "room-" + roomCounter.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        RoomInfo roomInfo = new RoomInfo(
            roomId,
            sessionId1, sessionId2,
            userId1, userId2,
            username1, username2,
            chatMode,
            Instant.now(),
            true
        );
        
        activeRooms.put(roomId, roomInfo);
        sessionToRoom.put(sessionId1, roomId);
        sessionToRoom.put(sessionId2, roomId);
        
        // Store in Redis for distributed systems
        redisTemplate.opsForHash().put(REDIS_ROOM_PREFIX + roomId, "session1", sessionId1);
        redisTemplate.opsForHash().put(REDIS_ROOM_PREFIX + roomId, "session2", sessionId2);
        redisTemplate.opsForHash().put(REDIS_ROOM_PREFIX + roomId, "chatMode", chatMode.name());
        redisTemplate.expire(REDIS_ROOM_PREFIX + roomId, Duration.ofHours(2));
        
        log.info("Created {} room {} between {} and {}", chatMode, roomId, sessionId1, sessionId2);
        
        return roomId;
    }

    /**
     * Create a room between two known sessions (for connect_matched).
     */
    public String createRoomForMatched(String sessionId1, String sessionId2, ChatMode chatMode) {
        // Clean up any existing rooms/queues
        leaveQueue(sessionId1);
        leaveQueue(sessionId2);
        endRoom(sessionId1);
        endRoom(sessionId2);
        
        OnlineUser user1 = onlineUsers.get(sessionId1);
        OnlineUser user2 = onlineUsers.get(sessionId2);
        
        return createRoom(
            sessionId1, sessionId2,
            user1 != null ? user1.userId() : null,
            user2 != null ? user2.userId() : null,
            user1 != null ? user1.username() : "Stranger",
            user2 != null ? user2.username() : "Stranger",
            chatMode
        );
    }

    /**
     * End a video/chat room and notify partner.
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
     * Check if session is the initiator (should create offer).
     */
    public boolean isInitiator(String sessionId) {
        String roomId = sessionToRoom.get(sessionId);
        if (roomId == null) return false;
        
        RoomInfo roomInfo = activeRooms.get(roomId);
        if (roomInfo == null) return false;
        
        // First session is initiator
        return roomInfo.sessionId1().equals(sessionId);
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
        return videoChatQueue.stream().anyMatch(u -> u.sessionId().equals(sessionId)) ||
               textChatQueue.stream().anyMatch(u -> u.sessionId().equals(sessionId));
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
        return videoChatQueue.size() + textChatQueue.size();
    }

    /**
     * Get video chat queue size.
     */
    public int getVideoQueueSize() {
        return videoChatQueue.size();
    }

    /**
     * Get text chat queue size.
     */
    public int getTextQueueSize() {
        return textChatQueue.size();
    }

    /**
     * Get active rooms count.
     */
    public int getActiveRoomsCount() {
        return activeRooms.size();
    }

    /**
     * Get users currently in calls.
     */
    public int getInCallCount() {
        return activeRooms.size() * 2;
    }

    /**
     * Get statistics.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "onlineUsers", getOnlineCount(),
            "lookingForVideo", getVideoQueueSize(),
            "lookingForText", getTextQueueSize(),
            "activeRooms", getActiveRoomsCount(),
            "usersInCall", getInCallCount()
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
        
        // Clean up stale users from queues
        videoChatQueue.removeIf(user -> {
            if (user.session() == null || !user.session().isOpen()) {
                log.debug("Removing closed session {} from video queue", user.sessionId());
                return true;
            }
            return false;
        });
        
        textChatQueue.removeIf(user -> {
            if (user.session() == null || !user.session().isOpen()) {
                log.debug("Removing closed session {} from text queue", user.sessionId());
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
                  "Online: {}, VideoQueue: {}, TextQueue: {}, ActiveRooms: {}",
            staleUsers.size(), staleRooms.size(),
            onlineUsers.size(), videoChatQueue.size(), textChatQueue.size(), activeRooms.size());
    }

    // Legacy methods for backward compatibility
    
    public Optional<VideoMatchResult> joinQueue(String sessionId, WebSocketSession session) {
        return joinQueue(sessionId, null, null, session, ChatMode.VIDEO);
    }

    public int getWaitingCount() {
        return getLookingCount();
    }
}
