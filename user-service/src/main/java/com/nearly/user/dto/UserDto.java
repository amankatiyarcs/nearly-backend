package com.nearly.user.dto;

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
public class UserDto {
    private String id;
    private String username;
    private String email;
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
    private Instant lastActiveAt;
    private Instant createdAt;
}

