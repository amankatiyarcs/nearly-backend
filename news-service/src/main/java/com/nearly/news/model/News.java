package com.nearly.news.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "news") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class News {
    @Id private String id;
    private String userId;
    private String headline;
    private String description;
    private String imageUrl;
    private String location;
    private Instant eventDate;
    private String eventTime;
    private String category;
    private int trueVotes;
    private int fakeVotes;
    private int likesCount;
    private int commentsCount;
    private Instant publishedAt;
}

