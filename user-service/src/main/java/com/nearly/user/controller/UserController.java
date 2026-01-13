package com.nearly.user.controller;

import com.nearly.user.dto.*;
import com.nearly.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ============ AUTH ENDPOINTS ============
    
    /**
     * Signup - Create new user account
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody CreateUserRequest request) {
        try {
            log.info("Signup request for username: {}", request.getUsername());
            UserDto user = userService.createUser(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "name", user.getName(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                )
            ));
        } catch (IllegalArgumentException e) {
            log.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Signup error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to create account"
            ));
        }
    }

    /**
     * Login - Authenticate user
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            log.info("Login attempt for: {}", request.getUsernameOrEmail());
            AuthResponse response = userService.authenticate(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "Invalid credentials"
            ));
        }
    }

    /**
     * Check username availability
     */
    @GetMapping("/auth/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            log.info("Checking username availability: {}", username);
            Map<String, Object> result = userService.checkUsernameAvailability(username);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Check username error", e);
            return ResponseEntity.ok(Map.of("available", true));
        }
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping("/current")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        // Try to get user ID from header first, then fall back to a default
        String currentUserId = userId != null ? userId : "current-user-id";

        return userService.getUserById(currentUserId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                // Check if we have a user ID from header (logged in user)
                if (userId != null) {
                    // This means the user signed up but their data isn't found
                    // Return a placeholder that indicates they need to complete setup
                    return ResponseEntity.ok(UserDto.builder()
                        .id(userId)
                        .username("user")
                        .name("New User")
                        .bio("Welcome to Nearly! Complete your profile setup.")
                        .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId)
                        .followersCount(0)
                        .followingCount(0)
                        .postsCount(0)
                        .build());
                } else {
                    // No user logged in, return default
                    return ResponseEntity.ok(UserDto.builder()
                        .id("current-user-id")
                        .username("current_user")
                        .name("Current User")
                        .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=current")
                        .build());
                }
            });
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                // Return a mock user for development/demo purposes
                // In production, this should return 404
                String displayName = username.replace('_', ' ');
                displayName = displayName.charAt(0) + displayName.substring(1);
                return ResponseEntity.ok(UserDto.builder()
                    .id("mock-" + username)
                    .username(username)
                    .name(displayName)
                    .bio("Profile of " + displayName + ". Database not set up yet.")
                    .avatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + username)
                    .followersCount(0)
                    .followingCount(0)
                    .postsCount(0)
                    .build());
            });
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(userService.searchUsers(q));
    }

    @PostMapping("/{followerId}/follow/{followingId}")
    public ResponseEntity<Map<String, Boolean>> followUser(
            @PathVariable String followerId, 
            @PathVariable String followingId) {
        userService.followUser(followerId, followingId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{followerId}/unfollow/{followingId}")
    public ResponseEntity<Map<String, Boolean>> unfollowUser(
            @PathVariable String followerId, 
            @PathVariable String followingId) {
        userService.unfollowUser(followerId, followingId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{followerId}/following/{followingId}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(
            @PathVariable String followerId, 
            @PathVariable String followingId) {
        return ResponseEntity.ok(Map.of("isFollowing", userService.isFollowing(followerId, followingId)));
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<List<UserDto>> getFollowers(@PathVariable String id) {
        return ResponseEntity.ok(userService.getFollowers(id));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<UserDto>> getFollowing(@PathVariable String id) {
        return ResponseEntity.ok(userService.getFollowing(id));
    }

    // ============ FOLLOW REQUESTS ============
    
    @GetMapping("/{userId}/follow-requests")
    public ResponseEntity<List<Map<String, Object>>> getFollowRequests(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getFollowRequests(userId));
    }

    @PostMapping("/{userId}/follow-requests/{requesterId}/accept")
    public ResponseEntity<Map<String, Object>> acceptFollowRequest(
            @PathVariable String userId,
            @PathVariable String requesterId) {
        userService.acceptFollowRequest(userId, requesterId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Follow request accepted"));
    }

    @PostMapping("/{userId}/follow-requests/{requesterId}/reject")
    public ResponseEntity<Map<String, Object>> rejectFollowRequest(
            @PathVariable String userId,
            @PathVariable String requesterId) {
        userService.rejectFollowRequest(userId, requesterId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Follow request rejected"));
    }

    // ============ SAVED POSTS ============
    
    @GetMapping("/{userId}/saved")
    public ResponseEntity<List<String>> getSavedPosts(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getSavedPosts(userId));
    }

    @PostMapping("/{userId}/saved/{postId}")
    public ResponseEntity<Map<String, Object>> savePost(
            @PathVariable String userId,
            @PathVariable String postId) {
        userService.savePost(userId, postId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Post saved"));
    }

    @DeleteMapping("/{userId}/saved/{postId}")
    public ResponseEntity<Map<String, Object>> unsavePost(
            @PathVariable String userId,
            @PathVariable String postId) {
        userService.unsavePost(userId, postId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Post unsaved"));
    }

    // ============ PASSWORD ============
    
    @PostMapping("/{id}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        userService.changePassword(id, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}

