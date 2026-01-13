package com.nearly.media.dto;

import com.nearly.media.model.MediaFile.MediaContext;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UploadRequest {
    private String userId;
    private MediaContext context;
    private String contextId;
    private boolean isPublic;
}

