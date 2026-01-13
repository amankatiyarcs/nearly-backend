package com.nearly.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "error", "Authentication service is temporarily unavailable",
                "code", "AUTH_SERVICE_UNAVAILABLE",
                "retryAfter", 10
            )));
    }

    @GetMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chatFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "error", "Chat service is temporarily unavailable. Please try again shortly.",
                "code", "CHAT_SERVICE_UNAVAILABLE",
                "retryAfter", 15
            )));
    }

    @GetMapping("/video")
    public Mono<ResponseEntity<Map<String, Object>>> videoFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "error", "Video chat service is temporarily unavailable. Please try again shortly.",
                "code", "VIDEO_SERVICE_UNAVAILABLE",
                "retryAfter", 20
            )));
    }

    @GetMapping("/report")
    public Mono<ResponseEntity<Map<String, Object>>> reportFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "error", "Report service is temporarily unavailable. Your report was not submitted.",
                "code", "REPORT_SERVICE_UNAVAILABLE",
                "retryAfter", 10
            )));
    }

    @GetMapping("/service")
    public Mono<ResponseEntity<Map<String, Object>>> genericServiceFallback() {
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "success", false,
                "error", "Service is temporarily unavailable. Please try again later.",
                "code", "SERVICE_UNAVAILABLE",
                "retryAfter", 10
            )));
    }
}

