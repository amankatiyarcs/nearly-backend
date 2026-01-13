package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String content;
    
    private String imageUrl;
    
    private String videoUrl;
    
    @Builder.Default
    private Integer likesCount = 0;
    
    @Builder.Default
    private Integer commentsCount = 0;
    
    @Builder.Default
    private Integer sharesCount = 0;
    
    private String visibility; // public, friends, private
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
