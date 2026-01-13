package com.nearly.pai.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "match_queues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchQueue {
    
    @Id
    private String sessionId;
    
    @Indexed
    private String chatMode; // "text" or "video"
    
    private String interests; // JSON array as string
    
    private String language;
    
    private Integer minAge;
    
    private Integer maxAge;
    
    @Indexed
    private Instant createdAt;
    
    private Instant lastActive;
}
