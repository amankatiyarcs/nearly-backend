package com.nearly.report.service;

import com.nearly.report.config.KafkaConfig;
import com.nearly.report.model.AnonymousReport;
import com.nearly.report.repository.AnonymousReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Kafka consumer for processing reports asynchronously.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportConsumer {

    private final AnonymousReportRepository reportRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConfig.REPORTS_TOPIC, groupId = "report-processor")
    public void processReport(Map<String, Object> reportData) {
        String reportId = (String) reportData.get("reportId");
        String reason = (String) reportData.get("reason");

        log.info("Processing report: {} - Reason: {}", reportId, reason);

        try {
            // Simulate auto-moderation logic
            boolean requiresManualReview = isHighPriorityReport(reason);

            Optional<AnonymousReport> reportOpt = reportRepository.findById(reportId);
            if (reportOpt.isPresent()) {
                AnonymousReport report = reportOpt.get();
                
                if (requiresManualReview) {
                    // High priority - flag for manual review
                    log.info("Report {} flagged for manual review", reportId);
                } else {
                    // Auto-mark as reviewed for low-risk reports
                    report.setStatus(AnonymousReport.ReportStatus.REVIEWED);
                    report.setProcessedAt(Instant.now());
                    reportRepository.save(report);
                }

                // Publish processing result
                kafkaTemplate.send(KafkaConfig.PROCESSED_REPORTS_TOPIC, reportId, Map.of(
                    "reportId", reportId,
                    "status", report.getStatus().name(),
                    "requiresManualReview", requiresManualReview,
                    "processedAt", Instant.now().toString()
                ));
            }

        } catch (Exception e) {
            log.error("Failed to process report {}: {}", reportId, e.getMessage());
        }
    }

    private boolean isHighPriorityReport(String reason) {
        // High priority reasons that need manual review
        return reason != null && (
            reason.equalsIgnoreCase("underage") ||
            reason.equalsIgnoreCase("violence") ||
            reason.equalsIgnoreCase("harassment")
        );
    }
}

