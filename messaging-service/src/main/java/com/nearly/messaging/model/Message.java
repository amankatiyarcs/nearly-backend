package com.nearly.messaging.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "messages") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {
    @Id private String id;
    private String senderId;
    private String recipientId;
    private String groupId;
    private String content;
    private String imageUrl;
    private String messageType; // text, image, system
    private boolean isRead;
    private Instant createdAt;
}

