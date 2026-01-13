package com.nearly.moments.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "moments") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Moment {
    @Id private String id;
    private String userId;
    private String mediaUrl;
    private String mediaType; // image, video
    private String caption;
    private String textOverlays;
    private String filter;
    private String visibility; // global, friends, private
    private int viewsCount;
    private int likesCount;
    private Instant expiresAt;
    private Instant createdAt;
    private Integer commentsCount=0;
}

