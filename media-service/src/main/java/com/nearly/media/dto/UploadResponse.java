package com.nearly.media.dto;

import com.nearly.media.model.MediaFile.MediaType;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UploadResponse {
    private String id;
    private String url;
    private String thumbnailUrl;
    private String fileName;
    private String contentType;
    private long fileSize;
    private MediaType mediaType;
    private boolean success;
    private String error;
}

