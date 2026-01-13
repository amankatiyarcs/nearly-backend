package com.nearly.activity.repository;

import com.nearly.activity.model.Discussion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionRepository extends MongoRepository<Discussion, String> {
    
    List<Discussion> findByUserId(String userId);
    
    List<Discussion> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Discussion> findByCategory(String category);
    
    List<Discussion> findByIsPinnedTrue();
}
