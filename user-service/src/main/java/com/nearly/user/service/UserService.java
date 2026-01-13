package com.nearly.user.service;

import com.nearly.user.dto.*;
import com.nearly.user.model.Follow;
import com.nearly.user.model.FollowRequest;
import com.nearly.user.model.SavedPost;
import com.nearly.user.model.User;
import com.nearly.user.repository.FollowRepository;
import com.nearly.user.repository.FollowRequestRepository;
import com.nearly.user.repository.SavedPostRepository;
import com.nearly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final SavedPostRepository savedPostRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ============ AUTH METHODS ============

    /**
     * Authenticate user with username/email and password
     */
    public AuthResponse authenticate(AuthRequest request) {
        // Try to find by username first, then by email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found for {}", request.getUsernameOrEmail());
            return AuthResponse.builder()
                .success(false)
                .error("Invalid username or password")
                .build();
        }

        User user = userOpt.get();
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for {}", request.getUsernameOrEmail());
            return AuthResponse.builder()
                .success(false)
                .error("Invalid username or password")
                .build();
        }

        // Update last active
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        log.info("Login successful for user: {}", user.getUsername());
        
        return AuthResponse.builder()
            .success(true)
            .user(toDto(user))
            .build();
    }

    /**
     * Check if username is available and suggest alternatives if not
     */
    public Map<String, Object> checkUsernameAvailability(String username) {
        // Validate format
        if (username == null || !username.matches("^[a-z0-9_]{3,30}$")) {
            return Map.of(
                "available", false,
                "error", "Invalid username format"
            );
        }

        boolean exists = userRepository.existsByUsername(username);
        
        if (!exists) {
            return Map.of("available", true);
        }

        // Generate suggestions
        List<String> suggestions = generateUsernameSuggestions(username);
        
        return Map.of(
            "available", false,
            "suggestions", suggestions
        );
    }

    /**
     * Generate username suggestions when requested username is taken
     */
    private List<String> generateUsernameSuggestions(String username) {
        List<String> suggestions = new ArrayList<>();
        String baseName = username.replaceAll("[0-9_]+$", "");
        Random random = new Random();

        // Try numeric suffixes
        for (int i = 0; i < 10 && suggestions.size() < 3; i++) {
            String suggestion = baseName + random.nextInt(1000);
            if (!userRepository.existsByUsername(suggestion) && !suggestion.equals(username)) {
                suggestions.add(suggestion);
            }
        }

        // Try prefixes if needed
        String[] prefixes = {"the", "real", "im", "its", "hey"};
        for (String prefix : prefixes) {
            if (suggestions.size() >= 3) break;
            String suggestion = prefix + "_" + baseName;
            if (!userRepository.existsByUsername(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions.subList(0, Math.min(3, suggestions.size()));
    }

    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .bio(request.getBio())
            .location(request.getLocation())
            .interests(request.getInterests())
            .avatarUrl(request.getAvatarUrl())
            .isPrivate(false)
            .showActivityStatus(true)
            .allowStorySharing(true)
            .messagePrivacy("everyone")
            .isActive(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        User saved = userRepository.save(user);
        
        // Publish user created event
        kafkaTemplate.send("user-events", Map.of(
            "type", "USER_CREATED",
            "userId", saved.getId(),
            "timestamp", Instant.now().toString()
        ));

        return toDto(saved);
    }

    public Optional<UserDto> getUserById(String id) {
        return userRepository.findById(id).map(this::toDto);
    }

    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(this::toDto);
    }

    public UserDto updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getInterests() != null) user.setInterests(request.getInterests());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getCoverUrl() != null) user.setCoverUrl(request.getCoverUrl());
        if (request.getIsPrivate() != null) user.setPrivate(request.getIsPrivate());
        if (request.getShowActivityStatus() != null) user.setShowActivityStatus(request.getShowActivityStatus());
        if (request.getAllowStorySharing() != null) user.setAllowStorySharing(request.getAllowStorySharing());
        if (request.getMessagePrivacy() != null) user.setMessagePrivacy(request.getMessagePrivacy());
        
        user.setUpdatedAt(Instant.now());
        return toDto(userRepository.save(user));
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
        kafkaTemplate.send("user-events", Map.of(
            "type", "USER_DELETED",
            "userId", id,
            "timestamp", Instant.now().toString()
        ));
    }

    public List<UserDto> searchUsers(String query) {
        return userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(query, query)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalArgumentException("Already following this user");
        }

        Follow follow = Follow.builder()
            .followerId(followerId)
            .followingId(followingId)
            .createdAt(Instant.now())
            .build();
        followRepository.save(follow);

        // Update counts
        userRepository.findById(followerId).ifPresent(u -> {
            u.setFollowingCount(u.getFollowingCount() + 1);
            userRepository.save(u);
        });
        userRepository.findById(followingId).ifPresent(u -> {
            u.setFollowersCount(u.getFollowersCount() + 1);
            userRepository.save(u);
        });

        // Publish event
        kafkaTemplate.send("user-events", Map.of(
            "type", "USER_FOLLOWED",
            "followerId", followerId,
            "followingId", followingId,
            "timestamp", Instant.now().toString()
        ));
    }

    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);

        userRepository.findById(followerId).ifPresent(u -> {
            u.setFollowingCount(Math.max(0, u.getFollowingCount() - 1));
            userRepository.save(u);
        });
        userRepository.findById(followingId).ifPresent(u -> {
            u.setFollowersCount(Math.max(0, u.getFollowersCount() - 1));
            userRepository.save(u);
        });
    }

    public boolean isFollowing(String followerId, String followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public List<UserDto> getFollowers(String userId) {
        return followRepository.findByFollowingId(userId).stream()
            .map(f -> userRepository.findById(f.getFollowerId()).orElse(null))
            .filter(u -> u != null)
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<UserDto> getFollowing(String userId) {
        return followRepository.findByFollowerId(userId).stream()
            .map(f -> userRepository.findById(f.getFollowingId()).orElse(null))
            .filter(u -> u != null)
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // ============ FOLLOW REQUESTS (Database-backed) ============

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowRequests(String userId) {
        return followRequestRepository.findByTargetUserIdAndStatus(userId, "pending").stream()
            .map(request -> {
                Map<String, Object> map = new HashMap<>();
                map.put("requesterId", request.getRequesterId());
                map.put("targetUserId", request.getTargetUserId());
                map.put("status", request.getStatus());
                map.put("createdAt", request.getCreatedAt().toString());
                
                // Include requester info if available
                userRepository.findById(request.getRequesterId()).ifPresent(requester -> {
                    map.put("requesterUsername", requester.getUsername());
                    map.put("requesterName", requester.getName());
                    map.put("requesterAvatarUrl", requester.getAvatarUrl());
                });
                
                return map;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public void createFollowRequest(String requesterId, String targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot send follow request to yourself");
        }
        
        if (followRequestRepository.existsByRequesterIdAndTargetUserId(requesterId, targetUserId)) {
            throw new IllegalArgumentException("Follow request already sent");
        }
        
        if (followRepository.existsByFollowerIdAndFollowingId(requesterId, targetUserId)) {
            throw new IllegalArgumentException("Already following this user");
        }
        
        FollowRequest request = FollowRequest.builder()
            .requesterId(requesterId)
            .targetUserId(targetUserId)
            .status("pending")
            .createdAt(Instant.now())
            .build();
        
        followRequestRepository.save(request);
        
        log.info("Follow request created from {} to {}", requesterId, targetUserId);
        
        // Publish event for notification
        kafkaTemplate.send("user-events", Map.of(
            "type", "FOLLOW_REQUEST_CREATED",
            "requesterId", requesterId,
            "targetUserId", targetUserId,
            "timestamp", Instant.now().toString()
        ));
    }

    @Transactional
    public void acceptFollowRequest(String userId, String requesterId) {
        FollowRequest request = followRequestRepository
            .findByRequesterIdAndTargetUserId(requesterId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Follow request not found"));
        
        request.setStatus("accepted");
        request.setUpdatedAt(Instant.now());
        followRequestRepository.save(request);
        
        // Actually follow
        followUser(requesterId, userId);
        
        log.info("Follow request from {} to {} accepted", requesterId, userId);
        
        // Publish event
        kafkaTemplate.send("user-events", Map.of(
            "type", "FOLLOW_REQUEST_ACCEPTED",
            "requesterId", requesterId,
            "targetUserId", userId,
            "timestamp", Instant.now().toString()
        ));
    }

    @Transactional
    public void rejectFollowRequest(String userId, String requesterId) {
        FollowRequest request = followRequestRepository
            .findByRequesterIdAndTargetUserId(requesterId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Follow request not found"));
        
        request.setStatus("rejected");
        request.setUpdatedAt(Instant.now());
        followRequestRepository.save(request);
        
        log.info("Follow request from {} to {} rejected", requesterId, userId);
    }

    // ============ SAVED POSTS (Database-backed) ============

    @Transactional(readOnly = true)
    public List<String> getSavedPosts(String userId) {
        return savedPostRepository.findByUserId(userId).stream()
            .map(SavedPost::getPostId)
            .collect(Collectors.toList());
    }

    @Transactional
    public void savePost(String userId, String postId) {
        if (savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
            log.debug("Post {} already saved by user {}", postId, userId);
            return;
        }
        
        SavedPost savedPost = SavedPost.builder()
            .userId(userId)
            .postId(postId)
            .savedAt(Instant.now())
            .build();
        
        savedPostRepository.save(savedPost);
        log.info("User {} saved post {}", userId, postId);
    }

    @Transactional
    public void unsavePost(String userId, String postId) {
        savedPostRepository.deleteByUserIdAndPostId(userId, postId);
        log.info("User {} unsaved post {}", userId, postId);
    }

    @Transactional(readOnly = true)
    public boolean isPostSaved(String userId, String postId) {
        return savedPostRepository.existsByUserIdAndPostId(userId, postId);
    }

    // ============ PASSWORD ============

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        log.info("Password changed for user {}", userId);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .name(user.getName())
            .bio(user.getBio())
            .location(user.getLocation())
            .interests(user.getInterests())
            .avatarUrl(user.getAvatarUrl())
            .coverUrl(user.getCoverUrl())
            .followersCount(user.getFollowersCount())
            .followingCount(user.getFollowingCount())
            .postsCount(user.getPostsCount())
            .isPrivate(user.isPrivate())
            .isVerified(user.isVerified())
            .lastActiveAt(user.getLastActiveAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
