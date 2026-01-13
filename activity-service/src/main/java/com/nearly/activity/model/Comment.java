package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "comments")
@CompoundIndex(name = "target_idx", def = "{'targetType': 1, 'targetId': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String targetType; // post, poll, question, discussion, activity
    
    @Indexed
    private String targetId;
    
    private String parentCommentId; // for nested replies
    
    private String content;
    
    @Builder.Default
    private Integer likesCount = 0;
    
    @Builder.Default
    private Integer repliesCount = 0;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
