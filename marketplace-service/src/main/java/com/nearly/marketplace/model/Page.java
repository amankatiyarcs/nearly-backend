package com.nearly.marketplace.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "pages") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Page {
    @Id private String id;
    private String userId;
    private String name;
    @Indexed(unique = true)
    private String username;
    private String category;
    private String shortDescription;
    private String about;
    private String email;
    private String phone;
    private String website;
    private String location;
    private String facebook;
    private String instagram;
    private String twitter;
    private String avatarUrl;
    private String coverUrl;
    private boolean isVerified;
    private int followersCount;
    private int postsCount;
    private Instant createdAt;
}

