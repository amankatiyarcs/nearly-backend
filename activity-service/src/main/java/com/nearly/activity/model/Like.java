package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "likes")
@CompoundIndex(name = "user_target_idx", def = "{'userId': 1, 'targetType': 1, 'targetId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String targetType; // post, poll, question, discussion, comment
    
    @Indexed
    private String targetId;
    
    private Instant createdAt;
}
