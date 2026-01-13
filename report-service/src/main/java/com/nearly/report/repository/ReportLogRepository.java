package com.nearly.report.repository;

import com.nearly.report.model.ReportLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReportLogRepository extends ElasticsearchRepository<ReportLog, String> {
    
    List<ReportLog> findByReason(String reason);
    
    List<ReportLog> findByChatMode(String chatMode);
    
    List<ReportLog> findByTimestampBetween(Instant start, Instant end);
    
    long countByReason(String reason);
    
    long countByChatMode(String chatMode);
}

