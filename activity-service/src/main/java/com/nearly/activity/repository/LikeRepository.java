package com.nearly.activity.repository;

import com.nearly.activity.model.Like;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends MongoRepository<Like, String> {
    
    Optional<Like> findByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    
    boolean existsByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    
    void deleteByUserIdAndTargetTypeAndTargetId(String userId, String targetType, String targetId);
    
    long countByTargetTypeAndTargetId(String targetType, String targetId);
}
