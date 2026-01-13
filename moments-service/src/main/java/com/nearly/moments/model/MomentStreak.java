package com.nearly.moments.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "moment_streaks") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MomentStreak {
    @Id private String id;
    private String userId1;
    private String userId2;
    private int streakCount;
    private Instant lastMomentAt;
    private Instant streakExpiresAt;
    private Instant createdAt;
}

