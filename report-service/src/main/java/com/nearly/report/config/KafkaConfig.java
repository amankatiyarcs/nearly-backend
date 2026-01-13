package com.nearly.report.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String REPORTS_TOPIC = "chat-reports";
    public static final String PROCESSED_REPORTS_TOPIC = "processed-reports";

    @Bean
    public NewTopic reportsTopic() {
        return TopicBuilder.name(REPORTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic processedReportsTopic() {
        return TopicBuilder.name(PROCESSED_REPORTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}

