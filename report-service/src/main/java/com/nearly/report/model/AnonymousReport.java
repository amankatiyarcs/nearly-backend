package com.nearly.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Anonymous report document.
 * PRIVACY: No user identifiable information is stored.
 * Reports are auto-deleted after retention period.
 */
@Document(collection = "anonymous_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousReport {
    
    @Id
    private String id;
    
    private String reason;          // Report reason category
    private String details;         // Optional additional details
    private String chatMode;        // "text" or "video"
    private Instant reportedAt;     // Timestamp
    private ReportStatus status;    // Processing status
    private Instant processedAt;    // When moderation reviewed
    private Instant expiresAt;      // Auto-deletion timestamp
    
    // NOTE: No session IDs, room IDs, or any user identifiers are stored
    // This ensures complete anonymity for all parties
    
    public enum ReportStatus {
        PENDING,        // Awaiting review
        REVIEWED,       // Reviewed by moderation
        ACTION_TAKEN,   // Action was taken
        DISMISSED,      // Report was dismissed
        EXPIRED         // Auto-expired
    }
}

