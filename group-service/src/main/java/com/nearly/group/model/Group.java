package com.nearly.group.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "groups") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Group {
    @Id private String id;
    private String userId;
    private String name;
    private String description;
    private String imageUrl;
    private String groupType; // public, private, secret
    private String category;
    private String rules;
    private int membersCount;
    private Instant createdAt;
}

