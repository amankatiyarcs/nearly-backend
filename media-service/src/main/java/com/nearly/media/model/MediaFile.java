package com.nearly.media.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "media_files")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MediaFile {
    @Id 
    private String id;
    private String userId;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private String s3Key;
    private String url;
    private String thumbnailUrl;
    private MediaType mediaType; // IMAGE, VIDEO, AUDIO, DOCUMENT
    private MediaContext context; // PROFILE, COVER, ACTIVITY, EVENT, GROUP, NEWS, MOMENT, MESSAGE, JOB, DEAL, PLACE, PAGE
    private String contextId; // ID of the related entity
    private int width;
    private int height;
    private int duration; // For video/audio in seconds
    private boolean isPublic;
    private Instant createdAt;
    private Instant expiresAt;
    
    public enum MediaType {
        IMAGE, VIDEO, AUDIO, DOCUMENT
    }
    
    public enum MediaContext {
        PROFILE, COVER, ACTIVITY, EVENT, GROUP, NEWS, MOMENT, MESSAGE, JOB, DEAL, PLACE, PAGE, OTHER
    }
}

