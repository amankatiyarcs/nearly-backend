package com.nearly.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object")
public class UserDto {
    
    @Schema(description = "User ID")
    private String id;
    
    @Schema(description = "Username")
    private String username;
    
    @Schema(description = "Email address")
    private String email;
    
    @Schema(description = "Full name")
    private String name;
    
    @Schema(description = "User bio")
    private String bio;
    
    @Schema(description = "Location")
    private String location;
    
    @Schema(description = "User interests")
    private List<String> interests;
    
    @Schema(description = "Avatar URL")
    private String avatarUrl;
    
    @Schema(description = "Cover image URL")
    private String coverUrl;
    
    @Schema(description = "Followers count")
    private int followersCount;
    
    @Schema(description = "Following count")
    private int followingCount;
    
    @Schema(description = "Posts count")
    private int postsCount;
    
    @Schema(description = "Whether profile is private")
    private boolean isPrivate;
    
    @Schema(description = "Whether user is verified")
    private boolean isVerified;
    
    @Schema(description = "Whether email is verified")
    private boolean emailVerified;
    
    @Schema(description = "Last active timestamp")
    private Instant lastActiveAt;
    
    @Schema(description = "Account creation timestamp")
    private Instant createdAt;
}

