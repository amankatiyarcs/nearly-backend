package com.nearly.moments.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "direct_moments") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DirectMoment {
    @Id private String id;
    private String momentId;
    private String senderId;
    private String recipientId;
    private boolean isViewed;
    private Instant viewedAt;
    private Instant expiresAt;
    private Instant createdAt;
}

