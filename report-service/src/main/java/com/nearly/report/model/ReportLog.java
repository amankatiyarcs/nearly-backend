package com.nearly.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Elasticsearch document for report analytics.
 * Used for aggregate statistics only - no user data.
 */
@Document(indexName = "report-logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportLog {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String reason;
    
    @Field(type = FieldType.Keyword)
    private String chatMode;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Date)
    private Instant timestamp;
    
    @Field(type = FieldType.Keyword)
    private String hourOfDay;       // For time-based analytics
    
    @Field(type = FieldType.Keyword)
    private String dayOfWeek;       // For day-based analytics
}

