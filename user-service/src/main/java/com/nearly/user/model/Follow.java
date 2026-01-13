package com.nearly.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "follows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "follow_unique", def = "{'followerId': 1, 'followingId': 1}", unique = true)
public class Follow {
    @Id
    private String id;
    private String followerId;
    private String followingId;
    private Instant createdAt;
}

