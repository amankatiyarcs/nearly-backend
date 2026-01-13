package com.nearly.pai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    private boolean success;
    private String matchedSessionId;
    private String matchedUserId;
    private Double matchScore;
    private String roomId;
    private String message;
}
