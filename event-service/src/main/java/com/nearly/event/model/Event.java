package com.nearly.event.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "events")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Event {
    @Id private String id;
    private String userId;
    private String title;
    private String description;
    private String imageUrl;
    private String location;
    private Instant startDate;
    private Instant endDate;
    private Integer maxAttendees;
    private String visibility;
    private String entryType;
    private Integer price;
    private String category;
    private int attendeesCount;
    private int likesCount;
    private int commentsCount;
    private Instant createdAt;
}

