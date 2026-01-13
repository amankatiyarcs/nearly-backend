package com.nearly.search.consumer;
import com.nearly.search.model.SearchDocument;
import com.nearly.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @Slf4j @RequiredArgsConstructor
public class SearchIndexConsumer {
    private final SearchService service;

    @KafkaListener(topics = "user-events", groupId = "search-service")
    public void handleUserEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        if ("USER_CREATED".equals(type) || "USER_UPDATED".equals(type)) {
            // Index user - would need to fetch full user data from user service
            log.info("Would index user: {}", event.get("userId"));
        } else if ("USER_DELETED".equals(type)) {
            service.deleteDocument("user", (String) event.get("userId"));
        }
    }

    @KafkaListener(topics = "activity-events", groupId = "search-service")
    public void handleActivityEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Would index activity from event: {}", type);
    }

    @KafkaListener(topics = "event-events", groupId = "search-service")
    public void handleEventEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Would index event from event: {}", type);
    }

    @KafkaListener(topics = "group-events", groupId = "search-service")
    public void handleGroupEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Would index group from event: {}", type);
    }

    @KafkaListener(topics = "news-events", groupId = "search-service")
    public void handleNewsEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Would index news from event: {}", type);
    }

    @KafkaListener(topics = "moment-events", groupId = "search-service")
    public void handleMomentEvents(Map<String, Object> event) {
        // Moments are ephemeral, usually not indexed
        log.debug("Moment event received: {}", event.get("type"));
    }
}

