package com.nearly.chat.repository;

import com.nearly.chat.model.OnlineUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OnlineUserRepository extends MongoRepository<OnlineUser, String> {
    
    List<OnlineUser> findByLastActiveAfter(Instant cutoff);
    
    void deleteByLastActiveBefore(Instant cutoff);
    
    long countByLastActiveAfter(Instant cutoff);
}
