package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String title;
    
    private String body;
    
    @Indexed
    private String tags; // comma-separated tags
    
    @Builder.Default
    private Integer upvotesCount = 0;
    
    @Builder.Default
    private Integer answersCount = 0;
    
    @Builder.Default
    private Integer viewsCount = 0;
    
    private Boolean isResolved;
    
    private String acceptedAnswerId;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
