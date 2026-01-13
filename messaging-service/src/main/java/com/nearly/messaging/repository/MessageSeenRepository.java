package com.nearly.messaging.repository;

import com.nearly.messaging.model.MessageSeen;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageSeenRepository extends MongoRepository<MessageSeen, String> {
    
    List<MessageSeen> findByMessageId(String messageId);
    
    Optional<MessageSeen> findByMessageIdAndUserId(String messageId, String userId);
    
    List<MessageSeen> findByMessageIdInAndUserId(List<String> messageIds, String userId);
    
    boolean existsByMessageIdAndUserId(String messageId, String userId);
}
