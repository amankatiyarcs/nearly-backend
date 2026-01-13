package com.nearly.moments.service;

import com.nearly.moments.model.*;
import com.nearly.moments.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentService {
    private final MomentRepository momentRepo;
    private final DirectMomentRepository directRepo;
    private final MomentStreakRepository streakRepo;
    private final MomentCommentRepository commentRepo;
    private final KafkaTemplate<String, Object> kafka;

    public List<Moment> getMoments(String visibility, Integer limit) {
        if (visibility != null) {
            return momentRepo.findByVisibilityAndExpiresAtAfter(visibility, Instant.now());
        }
        if (limit != null) return momentRepo.findAll(PageRequest.of(0, limit)).getContent();
        return momentRepo.findAll();
    }
    
    public Optional<Moment> getMoment(String id) { return momentRepo.findById(id); }
    
    public Moment createMoment(Moment moment) {
        moment.setCreatedAt(Instant.now());
        moment.setExpiresAt(moment.getVisibility().equals("private") 
            ? Instant.now().plus(14, ChronoUnit.DAYS) 
            : Instant.now().plus(24, ChronoUnit.HOURS));
        Moment saved = momentRepo.save(moment);
        kafka.send("moment-events", Map.of("type", "MOMENT_CREATED", "momentId", saved.getId()));
        return saved;
    }
    
    @Transactional
    public void deleteMoment(String id) { 
        momentRepo.deleteById(id);
        commentRepo.deleteByMomentId(id);
    }
    
    public Moment likeMoment(String id, String userId) {
        Moment moment = momentRepo.findById(id).orElseThrow();
        moment.setLikesCount(moment.getLikesCount() + 1);
        Moment saved = momentRepo.save(moment);
        
        // Send notification to moment owner (only if it's not the same user)
        if (moment.getUserId() != null && !moment.getUserId().equals(userId)) {
            kafka.send("moment-events", Map.of(
                "type", "MOMENT_LIKED",
                "momentId", id,
                "momentOwnerId", moment.getUserId(),
                "likerId", userId != null ? userId : "anonymous"
            ));
        }
        
        return saved;
    }
    
    public Moment likeMoment(String id) {
        return likeMoment(id, null);
    }
    
    public Moment viewMoment(String id) {
        Moment moment = momentRepo.findById(id).orElseThrow();
        moment.setViewsCount(moment.getViewsCount() + 1);
        return momentRepo.save(moment);
    }
    
    public DirectMoment sendDirectMoment(String momentId, String senderId, String recipientId) {
        DirectMoment dm = DirectMoment.builder()
            .momentId(momentId).senderId(senderId).recipientId(recipientId)
            .isViewed(false).expiresAt(Instant.now().plus(14, ChronoUnit.DAYS)).createdAt(Instant.now())
            .build();
        updateStreak(senderId, recipientId);
        return directRepo.save(dm);
    }
    
    public List<DirectMoment> getDirectMoments(String userId) {
        return directRepo.findByRecipientIdAndIsViewedFalse(userId);
    }
    
    public void markDirectMomentViewed(String id) {
        directRepo.findById(id).ifPresent(dm -> { dm.setViewed(true); dm.setViewedAt(Instant.now()); directRepo.save(dm); });
    }
    
    public List<MomentStreak> getStreaks(String userId) {
        return streakRepo.findByUserId1OrUserId2(userId, userId);
    }
    
    public Optional<MomentStreak> getStreak(String userId1, String userId2) {
        return streakRepo.findByUserId1AndUserId2(userId1, userId2)
            .or(() -> streakRepo.findByUserId1AndUserId2(userId2, userId1));
    }
    
    private void updateStreak(String userId1, String userId2) {
        MomentStreak streak = getStreak(userId1, userId2).orElse(
            MomentStreak.builder().userId1(userId1).userId2(userId2).streakCount(0).createdAt(Instant.now()).build()
        );
        streak.setStreakCount(streak.getStreakCount() + 1);
        streak.setLastMomentAt(Instant.now());
        streak.setStreakExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        streakRepo.save(streak);
    }
    
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredMoments() {
        momentRepo.deleteByExpiresAtBefore(Instant.now());
    }

    // ============ COMMENTS (Database-backed) ============

    public List<Map<String, Object>> getComments(String momentId) {
        List<MomentComment> comments = commentRepo.findByMomentIdOrderByCreatedAtDesc(momentId);
        return comments.stream()
            .map(this::commentToMap)
            .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addComment(String momentId, Map<String, Object> commentData) {
        String commentUserId = (String) commentData.get("userId");
        MomentComment comment = MomentComment.builder()
            .momentId(momentId)
            .userId(commentUserId)
            .content((String) commentData.get("content"))
            .parentCommentId((String) commentData.get("parentCommentId"))
            .likesCount(0)
            .createdAt(Instant.now())
            .build();
        
        MomentComment saved = commentRepo.save(comment);
        
        // Update comments count and send notification
        momentRepo.findById(momentId).ifPresent(moment -> {
            moment.setCommentsCount(moment.getCommentsCount() + 1);
            momentRepo.save(moment);
            
            // Send notification to moment owner (only if it's not the same user)
            if (moment.getUserId() != null && !moment.getUserId().equals(commentUserId)) {
                kafka.send("moment-events", Map.of(
                    "type", "MOMENT_COMMENTED",
                    "momentId", momentId,
                    "momentOwnerId", moment.getUserId(),
                    "commentId", saved.getId(),
                    "commenterId", commentUserId != null ? commentUserId : "anonymous",
                    "commentContent", saved.getContent() != null ? saved.getContent().substring(0, Math.min(saved.getContent().length(), 100)) : ""
                ));
            }
        });
        
        log.info("Added comment {} to moment {}", saved.getId(), momentId);
        return commentToMap(saved);
    }

    private Map<String, Object> commentToMap(MomentComment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("momentId", comment.getMomentId());
        map.put("userId", comment.getUserId());
        map.put("content", comment.getContent());
        map.put("parentCommentId", comment.getParentCommentId());
        map.put("likesCount", comment.getLikesCount());
        map.put("createdAt", comment.getCreatedAt().toString());
        if (comment.getUpdatedAt() != null) {
            map.put("updatedAt", comment.getUpdatedAt().toString());
        }
        return map;
    }
}
