package com.nearly.activity.repository;

import com.nearly.activity.model.PollVote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends MongoRepository<PollVote, String> {
    
    List<PollVote> findByPollId(String pollId);
    
    Optional<PollVote> findByPollIdAndUserId(String pollId, String userId);
    
    boolean existsByPollIdAndUserId(String pollId, String userId);
    
    long countByPollId(String pollId);
    
    long countByPollIdAndOptionId(String pollId, String optionId);
}
