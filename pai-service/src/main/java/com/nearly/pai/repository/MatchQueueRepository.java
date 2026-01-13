package com.nearly.pai.repository;

import com.nearly.pai.model.MatchQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchQueueRepository extends MongoRepository<MatchQueue, String> {
    
    List<MatchQueue> findByChatMode(String chatMode);
    
    List<MatchQueue> findByChatModeOrderByCreatedAtAsc(String chatMode);
    
    Optional<MatchQueue> findFirstByChatModeAndSessionIdNotOrderByCreatedAtAsc(String chatMode, String sessionId);
    
    long countByChatMode(String chatMode);
    
    void deleteByCreatedAtBefore(Instant cutoff);
}
