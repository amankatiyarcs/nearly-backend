package com.nearly.user.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "follow_requests")
@CompoundIndex(name = "requester_target_idx", def = "{'requesterId': 1, 'targetUserId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequest {
    
    @Id
    private String id;
    
    @Indexed
    private String requesterId;
    
    @Indexed
    private String targetUserId;
    
    @Builder.Default
    private String status = "pending"; // pending, accepted, rejected
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
