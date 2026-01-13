package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {
    
    @Id
    private String id;
    
    @Indexed
    private String questionId;
    
    @Indexed
    private String userId;
    
    private String content;
    
    @Builder.Default
    private Integer upvotesCount = 0;
    
    @Builder.Default
    private Boolean isAccepted = false;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
