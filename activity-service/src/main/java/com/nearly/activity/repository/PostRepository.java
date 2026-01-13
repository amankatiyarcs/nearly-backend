package com.nearly.activity.repository;

import com.nearly.activity.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    
    List<Post> findByUserId(String userId);
    
    List<Post> findByUserIdIn(List<String> userIds);
    
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Post> findByVisibility(String visibility);
}
