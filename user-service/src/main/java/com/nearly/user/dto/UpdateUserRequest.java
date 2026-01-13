package com.nearly.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String name;
    private String bio;
    private String location;
    private List<String> interests;
    private String avatarUrl;
    private String coverUrl;
    private Boolean isPrivate;
    private Boolean showActivityStatus;
    private Boolean allowStorySharing;
    private String messagePrivacy;
}

