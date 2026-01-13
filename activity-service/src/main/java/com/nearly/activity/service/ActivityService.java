package com.nearly.activity.service;

import com.nearly.activity.model.Activity;
import com.nearly.activity.model.Comment;
import com.nearly.activity.repository.ActivityRepository;
import com.nearly.activity.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository repository;
    private final CommentRepository commentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public List<Activity> getActivities(Integer limit) {
        if (limit != null) {
            return repository.findAll(PageRequest.of(0, limit)).getContent();
        }
        return repository.findAll();
    }

    public Optional<Activity> getActivity(String id) {
        return repository.findById(id);
    }

    public Activity createActivity(Activity activity) {
        activity.setCreatedAt(Instant.now());
        activity.setUpdatedAt(Instant.now());
        activity.setParticipantIds(new ArrayList<>());
        Activity saved = repository.save(activity);
        kafkaTemplate.send("activity-events", Map.of("type", "ACTIVITY_CREATED", "activityId", saved.getId()));
        return saved;
    }

    public Activity updateActivity(String id, Activity updates) {
        Activity activity = repository.findById(id).orElseThrow();
        if (updates.getTitle() != null) activity.setTitle(updates.getTitle());
        if (updates.getDescription() != null) activity.setDescription(updates.getDescription());
        if (updates.getImageUrl() != null) activity.setImageUrl(updates.getImageUrl());
        if (updates.getLocation() != null) activity.setLocation(updates.getLocation());
        if (updates.getStartDate() != null) activity.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) activity.setEndDate(updates.getEndDate());
        if (updates.getCost() != null) activity.setCost(updates.getCost());
        if (updates.getCategory() != null) activity.setCategory(updates.getCategory());
        activity.setUpdatedAt(Instant.now());
        return repository.save(activity);
    }

    public void deleteActivity(String id) {
        repository.deleteById(id);
        commentRepository.deleteByTargetTypeAndTargetId("activity", id);
    }

    public Activity likeActivity(String id, boolean increment) {
        return likeActivity(id, increment, null);
    }
    
    public Activity likeActivity(String id, boolean increment, String userId) {
        Activity activity = repository.findById(id).orElseThrow();
        activity.setLikesCount(activity.getLikesCount() + (increment ? 1 : -1));
        Activity saved = repository.save(activity);
        
        // Send notification for likes (only when incrementing and to the owner)
        if (increment && activity.getUserId() != null && !activity.getUserId().equals(userId)) {
            kafkaTemplate.send("activity-events", Map.of(
                "type", "ACTIVITY_LIKED",
                "activityId", id,
                "activityTitle", activity.getTitle() != null ? activity.getTitle() : "",
                "activityOwnerId", activity.getUserId(),
                "likerId", userId != null ? userId : "anonymous"
            ));
        }
        
        return saved;
    }

    public Activity joinActivity(String id, String userId) {
        Activity activity = repository.findById(id).orElseThrow();
        if (activity.getParticipantIds() == null) activity.setParticipantIds(new ArrayList<>());
        if (!activity.getParticipantIds().contains(userId)) {
            activity.getParticipantIds().add(userId);
            activity.setParticipantsCount(activity.getParticipantsCount() + 1);
        }
        return repository.save(activity);
    }

    public List<Activity> getByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public List<Activity> getByCategory(String category) {
        return repository.findByCategory(category);
    }

    // ============ COMMENTS (Database-backed) ============

    public List<Map<String, Object>> getComments(String activityId) {
        List<Comment> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("activity", activityId);
        return comments.stream()
            .map(this::commentToMap)
            .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addComment(String activityId, Map<String, Object> commentData) {
        String commentUserId = (String) commentData.get("userId");
        Comment comment = Comment.builder()
            .userId(commentUserId)
            .targetType("activity")
            .targetId(activityId)
            .content((String) commentData.get("content"))
            .parentCommentId((String) commentData.get("parentCommentId"))
            .likesCount(0)
            .repliesCount(0)
            .createdAt(Instant.now())
            .build();
        
        Comment saved = commentRepository.save(comment);
        
        // Update comments count and send notification
        repository.findById(activityId).ifPresent(activity -> {
            activity.setCommentsCount(activity.getCommentsCount() + 1);
            repository.save(activity);
            
            // Send notification to activity owner (only if it's not the same user)
            if (activity.getUserId() != null && !activity.getUserId().equals(commentUserId)) {
                kafkaTemplate.send("activity-events", Map.of(
                    "type", "ACTIVITY_COMMENTED",
                    "activityId", activityId,
                    "activityTitle", activity.getTitle() != null ? activity.getTitle() : "",
                    "activityOwnerId", activity.getUserId(),
                    "commentId", saved.getId(),
                    "commenterId", commentUserId != null ? commentUserId : "anonymous",
                    "commentContent", saved.getContent() != null ? saved.getContent().substring(0, Math.min(saved.getContent().length(), 100)) : ""
                ));
            }
        });
        
        log.info("Added comment {} to activity {}", saved.getId(), activityId);
        return commentToMap(saved);
    }

    private Map<String, Object> commentToMap(Comment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("userId", comment.getUserId());
        map.put("activityId", comment.getTargetId());
        map.put("content", comment.getContent());
        map.put("parentCommentId", comment.getParentCommentId());
        map.put("likesCount", comment.getLikesCount());
        map.put("repliesCount", comment.getRepliesCount());
        map.put("createdAt", comment.getCreatedAt().toString());
        if (comment.getUpdatedAt() != null) {
            map.put("updatedAt", comment.getUpdatedAt().toString());
        }
        return map;
    }
}
