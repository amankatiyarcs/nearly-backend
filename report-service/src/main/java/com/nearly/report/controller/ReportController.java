package com.nearly.report.controller;

import com.nearly.report.dto.ReportRequest;
import com.nearly.report.dto.ReportResponse;
import com.nearly.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Submit an anonymous report.
     * No user identifiable information is stored.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> submitReport(
            @Valid @RequestBody ReportRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        
        // Session ID is used only for rate limiting, NOT stored
        request.setSessionId(sessionId);
        
        log.info("Receiving anonymous report - Reason: {}", request.getReason());
        
        ReportResponse response = reportService.submitReport(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get aggregate report statistics (for admin dashboard).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(reportService.getReportStats());
    }

    /**
     * Health check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

