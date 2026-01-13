package com.nearly.user.repository;

import com.nearly.user.model.FollowRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRequestRepository extends MongoRepository<FollowRequest, String> {
    
    List<FollowRequest> findByTargetUserIdAndStatus(String targetUserId, String status);
    
    Optional<FollowRequest> findByRequesterIdAndTargetUserId(String requesterId, String targetUserId);
    
    void deleteByRequesterIdAndTargetUserId(String requesterId, String targetUserId);
    
    boolean existsByRequesterIdAndTargetUserId(String requesterId, String targetUserId);
}
