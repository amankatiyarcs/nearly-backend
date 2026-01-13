package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "discussions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discussion {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String title;
    
    private String content;
    
    @Indexed
    private String category;
    
    @Builder.Default
    private Integer likesCount = 0;
    
    @Builder.Default
    private Integer commentsCount = 0;
    
    @Builder.Default
    private Integer viewsCount = 0;
    
    private Boolean isPinned;
    
    private Boolean isLocked;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
