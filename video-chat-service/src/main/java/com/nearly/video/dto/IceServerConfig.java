package com.nearly.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceServerConfig {
    private List<IceServer> iceServers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IceServer {
        private String urls;
        private String username;
        private String credential;
    }
}

