package com.nearly.messaging.repository;

import com.nearly.messaging.model.MessageReaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends MongoRepository<MessageReaction, String> {
    
    List<MessageReaction> findByMessageId(String messageId);
    
    Optional<MessageReaction> findByMessageIdAndUserId(String messageId, String userId);
    
    void deleteByMessageIdAndUserId(String messageId, String userId);
    
    long countByMessageIdAndEmoji(String messageId, String emoji);
}
