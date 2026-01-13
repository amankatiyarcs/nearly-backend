package com.nearly.pai.controller;

import com.nearly.pai.dto.MatchRequest;
import com.nearly.pai.dto.MatchResponse;
import com.nearly.pai.dto.QueueStatusResponse;
import com.nearly.pai.dto.RateMatchRequest;
import com.nearly.pai.service.PaiMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PAI Matching", description = "Peer AI - Intelligent User Matching APIs")
public class PaiController {

    private final PaiMatchingService matchingService;

    /**
     * Find a match for the session.
     */
    @Operation(summary = "Find Match", description = "Find a compatible user to chat with using PAI matching")
    @PostMapping("/match")
    public ResponseEntity<MatchResponse> findMatch(@RequestBody MatchRequest request) {
        log.info("Match request from session: {}", request.getSessionId());
        MatchResponse response = matchingService.findMatch(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Leave the matching queue.
     */
    @Operation(summary = "Leave Queue", description = "Remove session from matching queue")
    @DeleteMapping("/queue/{sessionId}")
    public ResponseEntity<Map<String, Object>> leaveQueue(@PathVariable String sessionId) {
        log.info("Leave queue request for session: {}", sessionId);
        matchingService.leaveQueue(sessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Get queue status for a session.
     */
    @Operation(summary = "Queue Status", description = "Get current queue position and estimated wait time")
    @GetMapping("/queue/{sessionId}/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable String sessionId) {
        QueueStatusResponse status = matchingService.getQueueStatus(sessionId);
        return ResponseEntity.ok(status);
    }

    /**
     * Rate a match (for PAI learning).
     */
    @Operation(summary = "Rate Match", description = "Rate a match to help improve PAI matching quality")
    @PostMapping("/match/rate")
    public ResponseEntity<Map<String, Object>> rateMatch(@RequestBody RateMatchRequest request) {
        log.info("Match rating from {}: {}/5", request.getSessionId(), request.getRating());
        matchingService.rateMatch(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Get PAI service statistics.
     */
    @Operation(summary = "Service Stats", description = "Get PAI service statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "queueSize", matchingService.getTotalQueueSize(),
            "status", "online"
        ));
    }

    /**
     * Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
