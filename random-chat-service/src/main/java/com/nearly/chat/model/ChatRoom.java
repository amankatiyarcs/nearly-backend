package com.nearly.chat.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "chat_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    
    @Id
    private String roomId;
    
    @Indexed
    private String sessionId1;
    
    @Indexed
    private String sessionId2;
    
    private String chatMode;
    
    private boolean active;
    
    private Instant createdAt;
    
    private Instant endedAt;
}
