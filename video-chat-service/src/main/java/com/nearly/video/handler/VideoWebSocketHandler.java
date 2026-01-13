package com.nearly.video.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nearly.video.dto.IceServerConfig;
import com.nearly.video.dto.SignalingMessage;
import com.nearly.video.service.VideoMatchingService;
import com.nearly.video.service.VideoMatchingService.ChatMode;
import com.nearly.video.service.VideoMatchingService.VideoMatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket handler for WebRTC video chat signaling.
 * ONLY signaling data passes through - NO video/audio streams.
 * All communication is peer-to-peer after initial signaling.
 * 
 * Supports:
 * - Anonymous random matching (like Omegle)
 * - Authenticated user matching with user IDs
 * - Both video and text chat modes
 * - Priority queue: online users matched first
 */
@Component
@Slf4j
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final VideoMatchingService matchingService;
    private final ObjectMapper objectMapper;

    // Public STUN servers for WebRTC
    private static final List<IceServerConfig.IceServer> ICE_SERVERS = List.of(
        IceServerConfig.IceServer.builder().urls("stun:stun.l.google.com:19302").build(),
        IceServerConfig.IceServer.builder().urls("stun:stun1.l.google.com:19302").build(),
        IceServerConfig.IceServer.builder().urls("stun:stun2.l.google.com:19302").build(),
        IceServerConfig.IceServer.builder().urls("stun:stun3.l.google.com:19302").build()
    );

    public VideoWebSocketHandler(VideoMatchingService matchingService) {
        this.matchingService = matchingService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        Long userId = extractUserId(session);
        String username = extractUsername(session);
        
        // Register user as online
        matchingService.registerOnline(sessionId, userId, username, session);
        matchingService.storeSession(sessionId, session);
        
        log.info("Video WebSocket connected: {} (userId: {}, username: {})", sessionId, userId, username);

        // Send ICE server configuration
        sendIceServers(session);

        // Send welcome message with online count
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.SYSTEM)
            .content("Connected. Ready for random video/text chat.")
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForVideo") + (Integer) stats.get("lookingForText"))
            .timestamp(Instant.now())
            .build());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        SignalingMessage signalMessage;

        try {
            signalMessage = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse signaling message from {}: {}", sessionId, e.getMessage());
            sendError(session, "Invalid message format");
            return;
        }

        // Set session info
        signalMessage.setSessionId(sessionId);
        signalMessage.setTimestamp(Instant.now());
        
        // Extract user info if not already set
        if (signalMessage.getUserId() == null) {
            signalMessage.setUserId(extractUserId(session));
        }
        if (signalMessage.getUsername() == null) {
            signalMessage.setUsername(extractUsername(session));
        }

        log.debug("Received {} from {} (userId: {})", signalMessage.getType(), sessionId, signalMessage.getUserId());

        switch (signalMessage.getType()) {
            case JOIN, JOIN_QUEUE, JOIN_VIDEO -> handleJoinVideo(session, sessionId, signalMessage);
            case JOIN_TEXT -> handleJoinText(session, sessionId, signalMessage);
            case REGISTER -> handleRegister(session, sessionId, signalMessage);
            case OFFER -> handleOffer(sessionId, signalMessage);
            case ANSWER -> handleAnswer(sessionId, signalMessage);
            case ICE_CANDIDATE -> handleIceCandidate(sessionId, signalMessage);
            case SKIP, NEXT -> handleSkip(session, sessionId, signalMessage);
            case LEAVE, LEAVE_QUEUE -> handleLeave(session, sessionId);
            case DISCONNECT -> handleDisconnect(session, sessionId);
            case CONNECT_MATCHED -> handleConnectMatched(session, sessionId, signalMessage);
            case CHAT_MESSAGE -> handleChatMessage(sessionId, signalMessage);
            case MUTE_VIDEO, UNMUTE_VIDEO, MUTE_AUDIO, UNMUTE_AUDIO -> handleMediaState(sessionId, signalMessage);
            case HEARTBEAT -> handleHeartbeat(session, sessionId);
            default -> log.warn("Unknown signal type: {}", signalMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);
        
        // Notify partner if in a room
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, SignalingMessage.builder()
                        .type(SignalingMessage.SignalType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        // Clean up
        matchingService.unregisterOnline(sessionId);
        
        log.info("Video WebSocket closed: {} - {}", sessionId, status);
    }

    /**
     * Handle user registration with user info.
     */
    private void handleRegister(WebSocketSession session, String sessionId, SignalingMessage message) {
        Long userId = message.getUserId();
        String username = message.getUsername();
        
        matchingService.registerOnline(sessionId, userId, username, session);
        
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.SYSTEM)
            .content("Registered successfully. Ready to match.")
            .userId(userId)
            .username(username)
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForVideo") + (Integer) stats.get("lookingForText"))
            .timestamp(Instant.now())
            .build());
        
        log.info("User registered: {} (userId: {}, username: {})", sessionId, userId, username);
    }

    /**
     * Handle joining video chat queue.
     */
    private void handleJoinVideo(WebSocketSession session, String sessionId, SignalingMessage message) {
        Long userId = message.getUserId();
        String username = message.getUsername() != null ? message.getUsername() : "Stranger";
        
        Optional<VideoMatchResult> matchResult = matchingService.joinQueue(
            sessionId, userId, username, session, ChatMode.VIDEO
        );

        if (matchResult.isPresent()) {
            notifyMatch(session, sessionId, matchResult.get(), ChatMode.VIDEO);
        } else {
            // Added to queue, waiting for match
            Map<String, Object> stats = matchingService.getStats();
            sendMessage(session, SignalingMessage.builder()
                .type(SignalingMessage.SignalType.QUEUE_STATUS)
                .content("Looking for a stranger for video chat...")
                .chatMode("video")
                .onlineCount((Integer) stats.get("onlineUsers"))
                .lookingCount((Integer) stats.get("lookingForVideo"))
                .queuePosition(matchingService.getVideoQueueSize())
                .timestamp(Instant.now())
                .build());
        }
    }

    /**
     * Handle joining text chat queue.
     */
    private void handleJoinText(WebSocketSession session, String sessionId, SignalingMessage message) {
        Long userId = message.getUserId();
        String username = message.getUsername() != null ? message.getUsername() : "Stranger";
        
        Optional<VideoMatchResult> matchResult = matchingService.joinQueue(
            sessionId, userId, username, session, ChatMode.TEXT
        );

        if (matchResult.isPresent()) {
            notifyMatch(session, sessionId, matchResult.get(), ChatMode.TEXT);
        } else {
            Map<String, Object> stats = matchingService.getStats();
            sendMessage(session, SignalingMessage.builder()
                .type(SignalingMessage.SignalType.QUEUE_STATUS)
                .content("Looking for a stranger for text chat...")
                .chatMode("text")
                .onlineCount((Integer) stats.get("onlineUsers"))
                .lookingCount((Integer) stats.get("lookingForText"))
                .queuePosition(matchingService.getTextQueueSize())
                .timestamp(Instant.now())
                .build());
        }
    }

    /**
     * Notify both users about a successful match.
     */
    private void notifyMatch(WebSocketSession joiningSession, String joiningSessionId, 
                            VideoMatchResult result, ChatMode chatMode) {
        String roomId = result.roomId();
        
        // Notify the waiting user (initiator - should create offer)
        sendMessage(result.partnerSession(), SignalingMessage.builder()
            .type(SignalingMessage.SignalType.MATCHED)
            .content("Matched! You will create the offer.")
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .payload("initiator")
            .isInitiator(true)
            .matchedSessionId(joiningSessionId)
            .timestamp(Instant.now())
            .build());
        
        // Also send room created notification
        sendMessage(result.partnerSession(), SignalingMessage.builder()
            .type(SignalingMessage.SignalType.ROOM_CREATED)
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .partnerUserId(extractUserId(joiningSession))
            .partnerUsername(extractUsername(joiningSession))
            .isInitiator(true)
            .timestamp(Instant.now())
            .build());

        // Notify the joining user (receiver - waits for offer)
        sendMessage(joiningSession, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.MATCHED)
            .content("Matched! Waiting for video connection...")
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .payload("receiver")
            .isInitiator(false)
            .matchedSessionId(result.partnerSessionId())
            .partnerUserId(result.partnerUserId())
            .partnerUsername(result.partnerUsername())
            .timestamp(Instant.now())
            .build());
        
        // Also send room created notification
        sendMessage(joiningSession, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.ROOM_CREATED)
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .partnerUserId(result.partnerUserId())
            .partnerUsername(result.partnerUsername())
            .isInitiator(false)
            .timestamp(Instant.now())
            .build());

        log.info("Users matched in room {} ({}) - initiator: {}, receiver: {}", 
            roomId, chatMode, result.partnerSessionId(), joiningSessionId);
    }

    /**
     * Handle skip/next - find a new match.
     */
    private void handleSkip(WebSocketSession session, String sessionId, SignalingMessage message) {
        // End current room and notify partner
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, SignalingMessage.builder()
                        .type(SignalingMessage.SignalType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        // Notify user they're looking again
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.SYSTEM)
            .content("Looking for a new stranger...")
            .timestamp(Instant.now())
            .build());

        // Determine chat mode from message or default to video
        ChatMode chatMode = "text".equalsIgnoreCase(message.getChatMode()) ? ChatMode.TEXT : ChatMode.VIDEO;
        
        // Try to match again
        Long userId = message.getUserId() != null ? message.getUserId() : extractUserId(session);
        String username = message.getUsername() != null ? message.getUsername() : extractUsername(session);
        
        Optional<VideoMatchResult> matchResult = matchingService.joinQueue(
            sessionId, userId, username, session, chatMode
        );

        if (matchResult.isPresent()) {
            notifyMatch(session, sessionId, matchResult.get(), chatMode);
        } else {
            Map<String, Object> stats = matchingService.getStats();
            int lookingCount = chatMode == ChatMode.VIDEO 
                ? (Integer) stats.get("lookingForVideo") 
                : (Integer) stats.get("lookingForText");
            
            sendMessage(session, SignalingMessage.builder()
                .type(SignalingMessage.SignalType.QUEUE_STATUS)
                .content("Looking for a stranger...")
                .chatMode(chatMode.name().toLowerCase())
                .onlineCount((Integer) stats.get("onlineUsers"))
                .lookingCount(lookingCount)
                .timestamp(Instant.now())
                .build());
        }
    }

    /**
     * Handle leave - user wants to stop matching/chatting.
     */
    private void handleLeave(WebSocketSession session, String sessionId) {
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, SignalingMessage.builder()
                        .type(SignalingMessage.SignalType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        matchingService.leaveQueue(sessionId);

        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.SYSTEM)
            .content("You have left the chat.")
            .timestamp(Instant.now())
            .build());
    }

    /**
     * Handle explicit disconnect message.
     */
    private void handleDisconnect(WebSocketSession session, String sessionId) {
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, SignalingMessage.builder()
                        .type(SignalingMessage.SignalType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        matchingService.leaveQueue(sessionId);
        log.info("Session {} disconnected via explicit message", sessionId);
    }

    /**
     * Handle connecting to a previously matched session.
     */
    private void handleConnectMatched(WebSocketSession session, String sessionId, SignalingMessage message) {
        String matchedSessionId = message.getMatchedSessionId();
        if (matchedSessionId == null) {
            sendError(session, "Missing matchedSessionId for connect_matched");
            return;
        }

        Optional<WebSocketSession> matchedSessionOpt = matchingService.getSession(matchedSessionId);
        if (matchedSessionOpt.isEmpty() || !matchedSessionOpt.get().isOpen()) {
            sendError(session, "Matched session is not available");
            return;
        }
        
        WebSocketSession matchedSession = matchedSessionOpt.get();
        ChatMode chatMode = "text".equalsIgnoreCase(message.getChatMode()) ? ChatMode.TEXT : ChatMode.VIDEO;
        
        String roomId = matchingService.createRoomForMatched(sessionId, matchedSessionId, chatMode);
        
        // Notify matched session (initiator)
        sendMessage(matchedSession, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.MATCHED)
            .content("Matched! You will create the offer.")
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .payload("initiator")
            .isInitiator(true)
            .matchedSessionId(sessionId)
            .timestamp(Instant.now())
            .build());

        // Notify joining session (receiver)
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.MATCHED)
            .content("Matched! Waiting for connection...")
            .roomId(roomId)
            .chatMode(chatMode.name().toLowerCase())
            .payload("receiver")
            .isInitiator(false)
            .matchedSessionId(matchedSessionId)
            .timestamp(Instant.now())
            .build());

        log.info("Connected matched users {} and {} in room {} ({})", 
            sessionId, matchedSessionId, roomId, chatMode);
    }

    /**
     * Handle WebRTC offer.
     */
    private void handleOffer(String sessionId, SignalingMessage message) {
        forwardToPartner(sessionId, message);
    }

    /**
     * Handle WebRTC answer.
     */
    private void handleAnswer(String sessionId, SignalingMessage message) {
        forwardToPartner(sessionId, message);
    }

    /**
     * Handle ICE candidate.
     */
    private void handleIceCandidate(String sessionId, SignalingMessage message) {
        forwardToPartner(sessionId, message);
    }

    /**
     * Handle chat message during video call.
     */
    private void handleChatMessage(String sessionId, SignalingMessage message) {
        Optional<String> partnerSessionId = matchingService.getPartnerSessionId(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, SignalingMessage.builder()
                        .type(SignalingMessage.SignalType.CHAT_MESSAGE)
                        .content(message.getContent())
                        .sessionId(sessionId)
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });
    }

    /**
     * Handle media state changes (mute/unmute).
     */
    private void handleMediaState(String sessionId, SignalingMessage message) {
        forwardToPartner(sessionId, message);
    }

    /**
     * Handle heartbeat - keep user online.
     */
    private void handleHeartbeat(WebSocketSession session, String sessionId) {
        matchingService.updateHeartbeat(sessionId);
        
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.ONLINE_COUNT)
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForVideo") + (Integer) stats.get("lookingForText"))
            .timestamp(Instant.now())
            .build());
    }

    /**
     * Forward a message to the partner in the same room.
     */
    private void forwardToPartner(String sessionId, SignalingMessage message) {
        Optional<String> partnerSessionId = matchingService.getPartnerSessionId(sessionId);
        
        if (partnerSessionId.isEmpty()) {
            matchingService.getSession(sessionId).ifPresent(session -> {
                sendError(session, "You're not connected to a stranger yet.");
            });
            return;
        }

        matchingService.getSession(partnerSessionId.get()).ifPresent(partnerSession -> {
            if (partnerSession.isOpen()) {
                sendMessage(partnerSession, message);
            }
        });
    }

    /**
     * Send ICE servers configuration.
     */
    private void sendIceServers(WebSocketSession session) {
        try {
            IceServerConfig config = IceServerConfig.builder()
                .iceServers(ICE_SERVERS)
                .build();
            
            String payload = objectMapper.writeValueAsString(config);
            
            sendMessage(session, SignalingMessage.builder()
                .type(SignalingMessage.SignalType.SYSTEM)
                .payload(payload)
                .content("ICE servers configuration")
                .timestamp(Instant.now())
                .build());
        } catch (Exception e) {
            log.error("Failed to send ICE servers: {}", e.getMessage());
        }
    }

    /**
     * Send a message to a session.
     */
    private void sendMessage(WebSocketSession session, SignalingMessage message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("Failed to send message: {}", e.getMessage());
            }
        }
    }

    /**
     * Send an error message.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, SignalingMessage.builder()
            .type(SignalingMessage.SignalType.ERROR)
            .content(errorMessage)
            .timestamp(Instant.now())
            .build());
    }

    /**
     * Extract session ID from WebSocket session.
     */
    private String extractSessionId(WebSocketSession session) {
        String sessionId = session.getHandshakeHeaders().getFirst("X-Session-Id");
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "video-ws-" + session.getId();
        }
        return sessionId;
    }

    /**
     * Extract user ID from WebSocket session.
     */
    private Long extractUserId(WebSocketSession session) {
        String userIdStr = session.getHandshakeHeaders().getFirst("X-User-Id");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.debug("Invalid user ID in header: {}", userIdStr);
            }
        }
        return null;
    }

    /**
     * Extract username from WebSocket session.
     */
    private String extractUsername(WebSocketSession session) {
        String username = session.getHandshakeHeaders().getFirst("X-Username");
        return username != null && !username.isEmpty() ? username : "Stranger";
    }
}
