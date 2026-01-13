package com.nearly.notification.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "notifications") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id private String id;
    private String userId;
    private String type; // like, comment, follow, mention, event, group, message
    private String title;
    private String description;
    private String actionUrl;
    private String relatedId;
    private List<String> relatedUserIds;
    private String imageUrl;
    private boolean isRead;
    private Instant createdAt;
}

