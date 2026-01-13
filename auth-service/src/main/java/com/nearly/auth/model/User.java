package com.nearly.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
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
    
    private boolean isPrivate;
    private boolean isVerified;
    private boolean showActivityStatus;
    private boolean allowStorySharing;
    private String messagePrivacy;
    
    private boolean isActive;
    private boolean emailVerified;
    
    private Instant lastActiveAt;
    private Instant createdAt;
    private Instant updatedAt;
}

