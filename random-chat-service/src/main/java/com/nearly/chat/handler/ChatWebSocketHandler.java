package com.nearly.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nearly.chat.dto.ChatMessage;
import com.nearly.chat.service.MatchingService;
import com.nearly.chat.service.MatchingService.MatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket handler for anonymous random text chat.
 * NO message content is stored - all messages are ephemeral.
 * 
 * Supports:
 * - Anonymous random matching (like Omegle)
 * - Authenticated user matching with user IDs
 * - Priority queue: online users matched first
 */
@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(MatchingService matchingService) {
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
        
        log.info("Chat WebSocket connected: {} (userId: {}, username: {})", sessionId, userId, username);

        // Send welcome message with online count
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.SYSTEM)
            .content("Connected. Ready for random text chat.")
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForMatch"))
            .timestamp(Instant.now())
            .build());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        ChatMessage chatMessage;

        try {
            chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse message from {}: {}", sessionId, e.getMessage());
            sendError(session, "Invalid message format");
            return;
        }

        // Set session info
        chatMessage.setSessionId(sessionId);
        chatMessage.setTimestamp(Instant.now());
        
        // Extract user info if not already set
        if (chatMessage.getUserId() == null) {
            chatMessage.setUserId(extractUserId(session));
        }
        if (chatMessage.getUsername() == null) {
            chatMessage.setUsername(extractUsername(session));
        }

        log.debug("Received {} from {} (userId: {})", chatMessage.getType(), sessionId, chatMessage.getUserId());

        switch (chatMessage.getType()) {
            case JOIN, JOIN_QUEUE -> handleJoin(session, sessionId, chatMessage);
            case REGISTER -> handleRegister(session, sessionId, chatMessage);
            case MESSAGE, CHAT_MESSAGE -> handleMessage(sessionId, chatMessage);
            case TYPING -> handleTyping(sessionId, true);
            case STOP_TYPING -> handleTyping(sessionId, false);
            case SKIP, NEXT -> handleSkip(session, sessionId, chatMessage);
            case LEAVE, LEAVE_QUEUE -> handleLeave(session, sessionId);
            case DISCONNECT -> handleDisconnect(session, sessionId);
            case HEARTBEAT -> handleHeartbeat(session, sessionId);
            default -> log.warn("Unknown message type: {}", chatMessage.getType());
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
                    sendMessage(partnerSession, ChatMessage.builder()
                        .type(ChatMessage.MessageType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        // Clean up
        matchingService.unregisterOnline(sessionId);
        
        log.info("Chat WebSocket closed: {} - {}", sessionId, status);
    }

    /**
     * Handle user registration with user info.
     */
    private void handleRegister(WebSocketSession session, String sessionId, ChatMessage message) {
        Long userId = message.getUserId();
        String username = message.getUsername();
        
        matchingService.registerOnline(sessionId, userId, username, session);
        
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.SYSTEM)
            .content("Registered successfully. Ready to match.")
            .userId(userId)
            .username(username)
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForMatch"))
            .timestamp(Instant.now())
            .build());
        
        log.info("User registered: {} (userId: {}, username: {})", sessionId, userId, username);
    }

    /**
     * Handle joining the matching queue.
     */
    private void handleJoin(WebSocketSession session, String sessionId, ChatMessage message) {
        Long userId = message.getUserId();
        String username = message.getUsername() != null ? message.getUsername() : "Stranger";
        
        Optional<MatchResult> matchResult = matchingService.joinQueue(sessionId, userId, username, session);

        if (matchResult.isPresent()) {
            notifyMatch(session, sessionId, matchResult.get());
        } else {
            // Added to queue, waiting for match
            Map<String, Object> stats = matchingService.getStats();
            sendMessage(session, ChatMessage.builder()
                .type(ChatMessage.MessageType.QUEUE_STATUS)
                .content("Looking for a stranger to chat with...")
                .onlineCount((Integer) stats.get("onlineUsers"))
                .lookingCount((Integer) stats.get("lookingForMatch"))
                .queuePosition(matchingService.getWaitingCount())
                .timestamp(Instant.now())
                .build());
        }
    }

    /**
     * Notify both users about a successful match.
     */
    private void notifyMatch(WebSocketSession joiningSession, String joiningSessionId, MatchResult result) {
        String roomId = result.roomId();
        
        // Notify the waiting user
        sendMessage(result.partnerSession(), ChatMessage.builder()
            .type(ChatMessage.MessageType.MATCHED)
            .content("You're now chatting with a stranger. Say hi!")
            .roomId(roomId)
            .partnerUsername(extractUsername(joiningSession))
            .timestamp(Instant.now())
            .build());
        
        // Also send room created notification to waiting user
        sendMessage(result.partnerSession(), ChatMessage.builder()
            .type(ChatMessage.MessageType.ROOM_CREATED)
            .roomId(roomId)
            .partnerUserId(extractUserId(joiningSession))
            .partnerUsername(extractUsername(joiningSession))
            .timestamp(Instant.now())
            .build());

        // Notify the joining user
        sendMessage(joiningSession, ChatMessage.builder()
            .type(ChatMessage.MessageType.MATCHED)
            .content("You're now chatting with a stranger. Say hi!")
            .roomId(roomId)
            .partnerUserId(result.partnerUserId())
            .partnerUsername(result.partnerUsername())
            .timestamp(Instant.now())
            .build());
        
        // Also send room created notification to joining user
        sendMessage(joiningSession, ChatMessage.builder()
            .type(ChatMessage.MessageType.ROOM_CREATED)
            .roomId(roomId)
            .partnerUserId(result.partnerUserId())
            .partnerUsername(result.partnerUsername())
            .timestamp(Instant.now())
            .build());

        log.info("Users matched in room {} - {} and {}", roomId, result.partnerSessionId(), joiningSessionId);
    }

    /**
     * Handle chat message.
     */
    private void handleMessage(String sessionId, ChatMessage message) {
        Optional<String> partnerSessionId = matchingService.getPartnerSessionId(sessionId);
        
        if (partnerSessionId.isEmpty()) {
            log.debug("No partner found for session {} - not in a room yet", sessionId);
            matchingService.getSession(sessionId).ifPresent(session -> {
                sendError(session, "You're not connected to a stranger yet.");
            });
            return;
        }

        log.debug("Forwarding message from {} to partner {}", sessionId, partnerSessionId.get());
        
        matchingService.getSession(partnerSessionId.get()).ifPresent(partnerSession -> {
            if (partnerSession.isOpen()) {
                // Forward message to partner (content is NOT stored anywhere)
                sendMessage(partnerSession, ChatMessage.builder()
                    .type(ChatMessage.MessageType.CHAT_MESSAGE)
                    .content(message.getContent())
                    .fromStranger(true)
                    .timestamp(Instant.now())
                    .build());
                log.debug("Message forwarded successfully to {}", partnerSessionId.get());
            } else {
                log.warn("Partner session {} is not open", partnerSessionId.get());
            }
        });
    }

    /**
     * Handle typing indicator.
     */
    private void handleTyping(String sessionId, boolean isTyping) {
        Optional<String> partnerSessionId = matchingService.getPartnerSessionId(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, ChatMessage.builder()
                        .type(isTyping ? ChatMessage.MessageType.TYPING : ChatMessage.MessageType.STOP_TYPING)
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });
    }

    /**
     * Handle skip/next - find a new match.
     */
    private void handleSkip(WebSocketSession session, String sessionId, ChatMessage message) {
        // End current room and notify partner
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, ChatMessage.builder()
                        .type(ChatMessage.MessageType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        // Notify user they're looking again
        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.SYSTEM)
            .content("Looking for a new stranger...")
            .timestamp(Instant.now())
            .build());

        // Try to match again
        Long userId = message.getUserId() != null ? message.getUserId() : extractUserId(session);
        String username = message.getUsername() != null ? message.getUsername() : extractUsername(session);
        
        Optional<MatchResult> matchResult = matchingService.joinQueue(sessionId, userId, username, session);

        if (matchResult.isPresent()) {
            notifyMatch(session, sessionId, matchResult.get());
        } else {
            Map<String, Object> stats = matchingService.getStats();
            sendMessage(session, ChatMessage.builder()
                .type(ChatMessage.MessageType.QUEUE_STATUS)
                .content("Looking for a stranger to chat with...")
                .onlineCount((Integer) stats.get("onlineUsers"))
                .lookingCount((Integer) stats.get("lookingForMatch"))
                .timestamp(Instant.now())
                .build());
        }
    }

    /**
     * Handle leave - user wants to stop chatting.
     */
    private void handleLeave(WebSocketSession session, String sessionId) {
        Optional<String> partnerSessionId = matchingService.endRoom(sessionId);
        
        partnerSessionId.ifPresent(partnerId -> {
            matchingService.getSession(partnerId).ifPresent(partnerSession -> {
                if (partnerSession.isOpen()) {
                    sendMessage(partnerSession, ChatMessage.builder()
                        .type(ChatMessage.MessageType.DISCONNECTED)
                        .content("Stranger has disconnected.")
                        .timestamp(Instant.now())
                        .build());
                }
            });
        });

        matchingService.leaveQueue(sessionId);

        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.SYSTEM)
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
                    sendMessage(partnerSession, ChatMessage.builder()
                        .type(ChatMessage.MessageType.DISCONNECTED)
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
     * Handle heartbeat - keep user online.
     */
    private void handleHeartbeat(WebSocketSession session, String sessionId) {
        matchingService.updateHeartbeat(sessionId);
        
        Map<String, Object> stats = matchingService.getStats();
        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.ONLINE_COUNT)
            .onlineCount((Integer) stats.get("onlineUsers"))
            .lookingCount((Integer) stats.get("lookingForMatch"))
            .timestamp(Instant.now())
            .build());
    }

    /**
     * Send a message to a session.
     */
    private void sendMessage(WebSocketSession session, ChatMessage message) {
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
        sendMessage(session, ChatMessage.builder()
            .type(ChatMessage.MessageType.ERROR)
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
            sessionId = "chat-ws-" + session.getId();
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
