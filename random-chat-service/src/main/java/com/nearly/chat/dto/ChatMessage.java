package com.nearly.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Chat message DTO for random text chat.
 * NO message content is stored - all messages are ephemeral.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    public enum MessageType {
        JOIN,               // User joins matching queue
        JOIN_QUEUE,         // User joins the matching queue (alias for JOIN)
        LEAVE,              // User leaves
        LEAVE_QUEUE,        // User leaves the queue (alias for LEAVE)
        MATCHED,            // Matched with a stranger
        ROOM_CREATED,       // Room has been created for match
        MESSAGE,            // Text message
        CHAT_MESSAGE,       // Chat message (alias for MESSAGE)
        TYPING,             // User is typing
        STOP_TYPING,        // User stopped typing
        SKIP,               // Skip to next stranger
        NEXT,               // Alias for SKIP - find next match
        DISCONNECT,         // User disconnects
        DISCONNECTED,       // Partner disconnected
        SYSTEM,             // System message
        ERROR,              // Error message
        QUEUE_STATUS,       // Queue position/status update
        ONLINE_COUNT,       // Online users count update
        HEARTBEAT,          // Keep-alive heartbeat
        REGISTER            // Register user online
    }

    private MessageType type;
    private String content;
    private String sessionId;
    private String roomId;
    
    @JsonProperty("isFromStranger")
    private boolean fromStranger;
    
    private Instant timestamp;
    
    // User information for authenticated users
    private Long userId;              // User ID if authenticated
    private String username;          // Display name
    private Long partnerUserId;       // Partner's user ID
    private String partnerUsername;   // Partner's display name
    
    // Chat mode
    private String chatMode;          // "text" or "video"
    
    // Queue/matching status
    private Integer queuePosition;    // Position in queue
    private Integer onlineCount;      // Total online users
    private Integer lookingCount;     // Users currently looking for match
    private Integer activeRooms;      // Active chat rooms
    
    // Matched session ID (for connect_matched)
    private String matchedSessionId;
}
