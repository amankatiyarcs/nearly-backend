package com.nearly.video.repository;

import com.nearly.video.model.VideoOnlineUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface VideoOnlineUserRepository extends MongoRepository<VideoOnlineUser, String> {
    
    long countByLastActiveAfter(Instant cutoff);
    
    void deleteByLastActiveBefore(Instant cutoff);
}
