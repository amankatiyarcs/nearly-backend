package com.nearly.moments.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "moment_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentComment {
    
    @Id
    private String id;
    
    @Indexed
    private String momentId;
    
    @Indexed
    private String userId;
    
    private String content;
    
    private String parentCommentId;
    
    @Builder.Default
    private Integer likesCount = 0;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
