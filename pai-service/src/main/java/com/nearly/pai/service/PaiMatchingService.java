package com.nearly.pai.service;

import com.nearly.pai.dto.MatchRequest;
import com.nearly.pai.dto.MatchResponse;
import com.nearly.pai.dto.QueueStatusResponse;
import com.nearly.pai.dto.RateMatchRequest;
import com.nearly.pai.model.MatchHistory;
import com.nearly.pai.model.MatchQueue;
import com.nearly.pai.model.MatchRating;
import com.nearly.pai.repository.MatchHistoryRepository;
import com.nearly.pai.repository.MatchQueueRepository;
import com.nearly.pai.repository.MatchRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * PAI (Peer AI) Matching Service.
 * Provides intelligent matching for random chat users based on preferences.
 * All data is persisted to MongoDB.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaiMatchingService {

    private final MatchQueueRepository queueRepository;
    private final MatchRatingRepository ratingRepository;
    private final MatchHistoryRepository historyRepository;

    private static final Duration QUEUE_TIMEOUT = Duration.ofMinutes(2);

    /**
     * Find a match for the given session.
     */
    public MatchResponse findMatch(MatchRequest request) {
        String sessionId = request.getSessionId();
        String chatMode = request.getChatMode() != null ? request.getChatMode() : "text";
        
        log.info("Finding match for session {} in {} mode", sessionId, chatMode);
        
        // Check if already in queue
        Optional<MatchQueue> existingEntry = queueRepository.findById(sessionId);
        if (existingEntry.isPresent()) {
            log.debug("Session {} already in queue", sessionId);
        }
        
        // Try to find a match from queue
        Optional<MatchQueue> candidateOpt = queueRepository
            .findFirstByChatModeAndSessionIdNotOrderByCreatedAtAsc(chatMode, sessionId);
        
        if (candidateOpt.isPresent()) {
            MatchQueue candidate = candidateOpt.get();
            
            // Calculate match score
            double matchScore = calculateMatchScore(request, candidate);
            
            if (matchScore >= 0.5) {
                // Remove matched user from queue
                queueRepository.deleteById(candidate.getSessionId());
                queueRepository.deleteById(sessionId);
                
                // Create room
                String roomId = "pai-room-" + UUID.randomUUID().toString();
                
                // Save match history
                MatchHistory history = MatchHistory.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId1(sessionId)
                    .sessionId2(candidate.getSessionId())
                    .roomId(roomId)
                    .chatMode(chatMode)
                    .matchScore(matchScore)
                    .createdAt(Instant.now())
                    .build();
                historyRepository.save(history);
                
                log.info("Matched sessions {} and {} with score {} in room {}", 
                    sessionId, candidate.getSessionId(), matchScore, roomId);
                
                return MatchResponse.builder()
                    .success(true)
                    .matchedSessionId(candidate.getSessionId())
                    .matchScore(matchScore)
                    .roomId(roomId)
                    .build();
            }
        }
        
        // No match found, add to queue
        String interestsJson = null;
        String language = null;
        Integer minAge = null;
        Integer maxAge = null;
        
        if (request.getPreferences() != null) {
            var prefs = request.getPreferences();
            if (prefs.getInterests() != null && !prefs.getInterests().isEmpty()) {
                interestsJson = String.join(",", prefs.getInterests());
            }
            language = prefs.getLanguage();
            if (prefs.getAgeRange() != null) {
                minAge = prefs.getAgeRange().getMin();
                maxAge = prefs.getAgeRange().getMax();
            }
        }
        
        MatchQueue queueEntry = MatchQueue.builder()
            .sessionId(sessionId)
            .chatMode(chatMode)
            .interests(interestsJson)
            .language(language)
            .minAge(minAge)
            .maxAge(maxAge)
            .createdAt(Instant.now())
            .lastActive(Instant.now())
            .build();
        
        queueRepository.save(queueEntry);
        log.info("Session {} added to {} queue", sessionId, chatMode);
        
        return MatchResponse.builder()
            .success(false)
            .message("Added to matching queue")
            .build();
    }

    /**
     * Leave the matching queue.
     */
    public void leaveQueue(String sessionId) {
        queueRepository.deleteById(sessionId);
        log.info("Session {} removed from queue", sessionId);
    }

    /**
     * Get queue status for a session.
     */
    public QueueStatusResponse getQueueStatus(String sessionId) {
        Optional<MatchQueue> entry = queueRepository.findById(sessionId);
        
        if (entry.isPresent()) {
            String chatMode = entry.get().getChatMode();
            List<MatchQueue> queue = queueRepository.findByChatModeOrderByCreatedAtAsc(chatMode);
            
            int position = 1;
            for (MatchQueue q : queue) {
                if (q.getSessionId().equals(sessionId)) {
                    break;
                }
                position++;
            }
            
            int estimatedWait = "video".equals(chatMode) ? position * 15 : position * 10;
            
            return QueueStatusResponse.builder()
                .inQueue(true)
                .position(position)
                .estimatedWaitTime(estimatedWait)
                .build();
        }
        
        return QueueStatusResponse.builder()
            .inQueue(false)
            .position(0)
            .estimatedWaitTime(0)
            .build();
    }

    /**
     * Rate a match for PAI learning.
     */
    public void rateMatch(RateMatchRequest request) {
        MatchRating rating = MatchRating.builder()
            .id(UUID.randomUUID().toString())
            .sessionId(request.getSessionId())
            .matchedSessionId(request.getMatchedSessionId())
            .rating(request.getRating())
            .feedback(request.getFeedback())
            .createdAt(Instant.now())
            .build();
        
        ratingRepository.save(rating);
        
        log.info("Match rating recorded: {} rated {} as {}/5", 
            request.getSessionId(), request.getMatchedSessionId(), request.getRating());
    }

    /**
     * Calculate match score between two users based on preferences.
     */
    private double calculateMatchScore(MatchRequest user1, MatchQueue user2) {
        double score = 0.5; // Base score
        
        var prefs1 = user1.getPreferences();
        if (prefs1 == null) {
            return score + Math.random() * 0.3;
        }
        
        // Language match bonus
        if (prefs1.getLanguage() != null && prefs1.getLanguage().equals(user2.getLanguage())) {
            score += 0.2;
        }
        
        // Interest overlap bonus
        if (prefs1.getInterests() != null && user2.getInterests() != null) {
            Set<String> interests1 = new HashSet<>(prefs1.getInterests());
            Set<String> interests2 = new HashSet<>(Arrays.asList(user2.getInterests().split(",")));
            
            long commonInterests = interests1.stream()
                .filter(interests2::contains)
                .count();
            score += Math.min(0.3, commonInterests * 0.1);
        }
        
        // Use historical ratings to improve matching (PAI learning)
        List<MatchRating> ratings = ratingRepository.findByMatchedSessionId(user2.getSessionId());
        if (!ratings.isEmpty()) {
            double avgRating = ratings.stream()
                .mapToInt(MatchRating::getRating)
                .average()
                .orElse(3.0);
            if (avgRating >= 4.0) {
                score += 0.1; // Bonus for well-rated users
            }
        }
        
        // Add some randomness for variety
        score += Math.random() * 0.1;
        
        return Math.min(1.0, score);
    }

    /**
     * Get total users in queues.
     */
    public long getTotalQueueSize() {
        return queueRepository.count();
    }

    /**
     * Cleanup stale queue entries.
     */
    @Scheduled(fixedRate = 30000)
    public void cleanupStaleEntries() {
        Instant cutoff = Instant.now().minus(QUEUE_TIMEOUT);
        queueRepository.deleteByCreatedAtBefore(cutoff);
        log.debug("Cleaned up stale queue entries");
    }
}
