package com.nearly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousSessionResponse {
    private String sessionId;
    private String token;
    private Instant expiresAt;
    private String chatMode;
    private boolean success;
    private String message;
}

