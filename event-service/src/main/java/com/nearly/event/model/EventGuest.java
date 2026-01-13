package com.nearly.event.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "event_guests")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventGuest {
    @Id private String id;
    private String eventId;
    private String userId;
    private String status; // attending, interested, declined
    private Instant createdAt;
}

