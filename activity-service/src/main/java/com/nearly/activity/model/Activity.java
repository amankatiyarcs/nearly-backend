package com.nearly.activity.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "activities")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Activity {
    @Id
    private String id;
    private String userId;
    private String title;
    private String organizerName;
    private String description;
    private String imageUrl;
    private String location;
    private Instant startDate;
    private Instant endDate;
    private Integer maxParticipants;
    private String visibility; // public, private, friends
    private String cost;
    private String category;
    private int likesCount;
    private int commentsCount;
    private int participantsCount;
    private List<String> participantIds;
    private Instant createdAt;
    private Instant updatedAt;
}

