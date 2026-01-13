package com.nearly.group.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "group_members") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupMember {
    @Id private String id;
    private String groupId;
    private String userId;
    private String role; // admin, moderator, member
    private Instant joinedAt;
}

