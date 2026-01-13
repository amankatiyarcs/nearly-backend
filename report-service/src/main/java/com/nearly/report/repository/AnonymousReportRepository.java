package com.nearly.report.repository;

import com.nearly.report.model.AnonymousReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnonymousReportRepository extends MongoRepository<AnonymousReport, String> {
    
    List<AnonymousReport> findByStatus(AnonymousReport.ReportStatus status);
    
    List<AnonymousReport> findByExpiresAtBefore(Instant timestamp);
    
    long countByReason(String reason);
    
    long countByChatMode(String chatMode);
    
    long countByStatus(AnonymousReport.ReportStatus status);
}

