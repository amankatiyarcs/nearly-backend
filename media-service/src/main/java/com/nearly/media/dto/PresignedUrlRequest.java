package com.nearly.media.dto;

import com.nearly.media.model.MediaFile.MediaContext;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PresignedUrlRequest {
    private String userId;
    private String fileName;
    private String contentType;
    private MediaContext context;
    private String contextId;
    private boolean isPublic;
}

