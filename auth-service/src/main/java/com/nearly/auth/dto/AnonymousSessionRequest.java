package com.nearly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousSessionRequest {
    private String deviceFingerprint; // Optional, for rate limiting
    private String chatMode; // "text" or "video"
}

