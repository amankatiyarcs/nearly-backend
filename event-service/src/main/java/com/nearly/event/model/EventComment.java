package com.nearly.event.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "event_comments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EventComment {
    @Id private String id;
    private String eventId;
    private String userId;
    private String content;
    private Instant createdAt;
}

