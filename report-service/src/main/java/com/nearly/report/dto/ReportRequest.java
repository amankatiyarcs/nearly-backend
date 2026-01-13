package com.nearly.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotBlank(message = "Report reason is required")
    private String reason;
    
    @Size(max = 500, message = "Details must be less than 500 characters")
    private String details;
    
    @NotBlank(message = "Chat mode is required")
    private String chatMode; // "text" or "video"
    
    // Anonymous session ID of reporter - NOT stored for privacy
    private String sessionId;
    
    // Room ID where incident occurred - NOT stored
    private String roomId;
}

