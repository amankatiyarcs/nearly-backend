package com.nearly.pai.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "match_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRating {
    
    @Id
    private String id;
    
    @Indexed
    private String sessionId;
    
    @Indexed
    private String matchedSessionId;
    
    private Integer rating; // 1-5
    
    private String feedback;
    
    private Instant createdAt;
}
