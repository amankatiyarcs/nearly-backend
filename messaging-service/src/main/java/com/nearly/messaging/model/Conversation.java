package com.nearly.messaging.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "conversations") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Conversation {
    @Id private String id;
    private List<String> participantIds;
    private String groupId;
    private String lastMessage;
    private String lastSenderId;
    private int unreadCount;
    private Instant lastMessageAt;
    private Instant createdAt;
}

