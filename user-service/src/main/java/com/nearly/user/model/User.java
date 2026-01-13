package com.nearly.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    private String name;
    private String bio;
    private String location;
    private List<String> interests;
    private String avatarUrl;
    private String coverUrl;
    
    private int followersCount;
    private int followingCount;
    private int postsCount;
    
    // Privacy settings
    private boolean isPrivate;
    private boolean showActivityStatus;
    private boolean allowStorySharing;
    private String messagePrivacy; // "everyone", "following", "followers", "noone"
    
    // Status
    private boolean isVerified;
    private boolean isActive;
    private Instant lastActiveAt;
    private Instant createdAt;
    private Instant updatedAt;
}

