package com.nearly.video.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "video_online_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoOnlineUser {
    
    @Id
    private String sessionId;
    
    @Indexed
    private Instant lastActive;
    
    private Instant createdAt;
}
