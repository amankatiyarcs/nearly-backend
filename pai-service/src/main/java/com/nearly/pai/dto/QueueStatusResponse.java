package com.nearly.pai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponse {
    private boolean inQueue;
    private Integer position;
    private Integer estimatedWaitTime; // seconds
}
