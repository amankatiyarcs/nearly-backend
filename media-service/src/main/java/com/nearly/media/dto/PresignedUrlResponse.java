package com.nearly.media.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;
    private String fileId;
    private String s3Key;
    private String publicUrl;
    private long expiresIn; // seconds
}

