package com.nearly.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebRTC signaling message for video chat.
 * NO video/audio data passes through the server - only signaling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {
    
    public enum SignalType {
        JOIN,               // User joins video matching queue
        @JsonProperty("join_queue")
        JOIN_QUEUE,         // User joins the matching queue (alias for JOIN)
        @JsonProperty("join_video")
        JOIN_VIDEO,         // User joins video chat queue specifically
        @JsonProperty("join_text")
        JOIN_TEXT,          // User joins text chat queue specifically
        LEAVE,              // User leaves
        @JsonProperty("leave_queue")
        LEAVE_QUEUE,        // User leaves the queue (alias for LEAVE)
        MATCHED,            // Matched with a stranger
        @JsonProperty("connect_matched")
        CONNECT_MATCHED,    // Connect to matched partner
        @JsonProperty("match_info")
        MATCH_INFO,         // Send match/partner info
        OFFER,              // WebRTC SDP offer
        ANSWER,             // WebRTC SDP answer
        ICE_CANDIDATE,      // ICE candidate for connection
        SKIP,               // Skip to next stranger
        @JsonProperty("next")
        NEXT,               // Alias for SKIP - find next match
        @JsonProperty("disconnect")
        DISCONNECT,         // User disconnects
        DISCONNECTED,       // Partner disconnected
        SYSTEM,             // System message
        ERROR,              // Error message
        CHAT_MESSAGE,       // Text message during video call
        MUTE_VIDEO,         // Partner muted video
        UNMUTE_VIDEO,       // Partner unmuted video
        MUTE_AUDIO,         // Partner muted audio
        UNMUTE_AUDIO,       // Partner unmuted audio
        @JsonProperty("queue_status")
        QUEUE_STATUS,       // Queue position/status update
        @JsonProperty("online_count")
        ONLINE_COUNT,       // Online users count update
        @JsonProperty("heartbeat")
        HEARTBEAT,          // Keep-alive heartbeat
        @JsonProperty("register")
        REGISTER,           // Register user online
        @JsonProperty("room_created")
        ROOM_CREATED        // Room has been created for match
    }

    private SignalType type;
    private String sessionId;
    private String roomId;
    private String matchedSessionId;  // For connect_matched
    private String chatMode;          // "video" or "text"
    private String payload;           // SDP or ICE candidate data
    private String content;           // Chat message content
    private Instant timestamp;
    
    // User information for authenticated users
    private Long userId;              // User ID if authenticated
    private String username;          // Display name
    private Long partnerUserId;       // Partner's user ID
    private String partnerUsername;   // Partner's display name
    
    // Queue/matching status
    private Integer queuePosition;    // Position in queue
    private Integer onlineCount;      // Total online users
    private Integer lookingCount;     // Users currently looking for match
    private Boolean isInitiator;      // Whether this user should create offer
}
