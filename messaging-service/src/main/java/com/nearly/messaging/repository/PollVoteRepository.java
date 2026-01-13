package com.nearly.messaging.repository;

import com.nearly.messaging.model.PollVote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends MongoRepository<PollVote, String> {
    
    List<PollVote> findByMessageId(String messageId);
    
    Optional<PollVote> findByMessageIdAndUserId(String messageId, String userId);
    
    void deleteByMessageIdAndUserId(String messageId, String userId);
    
    long countByMessageIdAndOptionId(String messageId, String optionId);
}
