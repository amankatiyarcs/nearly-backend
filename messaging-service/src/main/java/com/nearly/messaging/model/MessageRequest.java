package com.nearly.messaging.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "message_requests")
@CompoundIndex(name = "sender_recipient_idx", def = "{'senderId': 1, 'recipientId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    
    @Id
    private String id;
    
    @Indexed
    private String senderId;
    
    @Indexed
    private String recipientId;
    
    private String initialMessage;
    
    @Builder.Default
    private String status = "pending"; // pending, accepted, declined
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
