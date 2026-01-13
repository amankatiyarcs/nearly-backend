package com.nearly.activity.repository;

import com.nearly.activity.model.Poll;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends MongoRepository<Poll, String> {
    
    List<Poll> findByUserId(String userId);
    
    List<Poll> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
