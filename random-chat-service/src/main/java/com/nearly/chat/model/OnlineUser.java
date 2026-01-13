package com.nearly.chat.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "online_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUser {
    
    @Id
    private String sessionId;
    
    @Indexed
    private Instant lastActive;
    
    private String chatMode; // "text" or "video"
    
    private Instant createdAt;
}
