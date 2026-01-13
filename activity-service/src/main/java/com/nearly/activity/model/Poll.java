package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "polls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Poll {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String question;
    
    private String optionsJson; // JSON array of options
    
    @Builder.Default
    private Integer totalVotes = 0;
    
    private Instant expiresAt;
    
    private Boolean allowMultiple;
    
    @Indexed
    private Instant createdAt;
    
    private Instant updatedAt;
}
