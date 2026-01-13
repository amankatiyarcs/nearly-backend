package com.nearly.report.service;

import com.nearly.report.config.KafkaConfig;
import com.nearly.report.dto.ReportRequest;
import com.nearly.report.dto.ReportResponse;
import com.nearly.report.model.AnonymousReport;
import com.nearly.report.model.ReportLog;
import com.nearly.report.repository.AnonymousReportRepository;
import com.nearly.report.repository.ReportLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling anonymous reports.
 * PRIVACY: No user identifiable data is stored.
 * Reports are auto-deleted after retention period.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final AnonymousReportRepository reportRepository;
    private final ReportLogRepository reportLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${report.retention.days:30}")
    private int retentionDays;

    /**
     * Submit an anonymous report.
     * Session IDs and room IDs are NOT stored.
     */
    public ReportResponse submitReport(ReportRequest request) {
        String reportId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(retentionDays, ChronoUnit.DAYS);

        // Create anonymous report (NO user identifiers stored)
        AnonymousReport report = AnonymousReport.builder()
            .id(reportId)
            .reason(request.getReason())
            .details(request.getDetails())
            .chatMode(request.getChatMode())
            .reportedAt(now)
            .status(AnonymousReport.ReportStatus.PENDING)
            .expiresAt(expiresAt)
            .build();

        // Save to MongoDB
        reportRepository.save(report);

        // Index to Elasticsearch for analytics (aggregates only)
        indexReportLog(report);

        // Publish to Kafka for async processing
        kafkaTemplate.send(KafkaConfig.REPORTS_TOPIC, reportId, Map.of(
            "reportId", reportId,
            "reason", request.getReason(),
            "chatMode", request.getChatMode(),
            "timestamp", now.toString()
        ));

        log.info("Anonymous report submitted: {} - Reason: {}", reportId, request.getReason());

        return ReportResponse.builder()
            .reportId(reportId)
            .success(true)
            .message("Report submitted successfully. Thank you for helping keep our community safe.")
            .submittedAt(now)
            .build();
    }

    /**
     * Get report statistics (aggregate only - no individual data).
     */
    public Map<String, Object> getReportStats() {
        return Map.of(
            "totalReports", reportRepository.count(),
            "pendingReports", reportRepository.countByStatus(AnonymousReport.ReportStatus.PENDING),
            "reviewedReports", reportRepository.countByStatus(AnonymousReport.ReportStatus.REVIEWED),
            "textChatReports", reportRepository.countByChatMode("text"),
            "videoChatReports", reportRepository.countByChatMode("video")
        );
    }

    /**
     * Auto-delete expired reports for privacy.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void cleanupExpiredReports() {
        List<AnonymousReport> expiredReports = reportRepository.findByExpiresAtBefore(Instant.now());
        
        if (!expiredReports.isEmpty()) {
            reportRepository.deleteAll(expiredReports);
            log.info("Deleted {} expired reports for privacy compliance", expiredReports.size());
        }
    }

    private void indexReportLog(AnonymousReport report) {
        try {
            var zdt = report.getReportedAt().atZone(ZoneOffset.UTC);
            
            ReportLog log = ReportLog.builder()
                .id(report.getId())
                .reason(report.getReason())
                .chatMode(report.getChatMode())
                .status(report.getStatus().name())
                .timestamp(report.getReportedAt())
                .hourOfDay(String.valueOf(zdt.getHour()))
                .dayOfWeek(zdt.getDayOfWeek().name())
                .build();

            reportLogRepository.save(log);
        } catch (Exception e) {
            // Don't fail the report submission if Elasticsearch is down
            ReportService.log.warn("Failed to index report to Elasticsearch: {}", e.getMessage());
        }
    }
}

