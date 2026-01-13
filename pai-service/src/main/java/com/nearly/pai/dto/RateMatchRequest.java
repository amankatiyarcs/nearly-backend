package com.nearly.pai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateMatchRequest {
    private String sessionId;
    private String matchedSessionId;
    private Integer rating; // 1-5
    private String feedback;
}
