package com.nearly.pai.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "match_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchHistory {
    
    @Id
    private String id;
    
    @Indexed
    private String sessionId1;
    
    @Indexed
    private String sessionId2;
    
    @Indexed
    private String roomId;
    
    private String chatMode;
    
    private Double matchScore;
    
    private Instant createdAt;
    
    private Instant endedAt;
}
