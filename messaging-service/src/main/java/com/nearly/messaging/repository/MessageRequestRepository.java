package com.nearly.messaging.repository;

import com.nearly.messaging.model.MessageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRequestRepository extends MongoRepository<MessageRequest, String> {
    
    List<MessageRequest> findByRecipientIdAndStatus(String recipientId, String status);
    
    Optional<MessageRequest> findBySenderIdAndRecipientId(String senderId, String recipientId);
    
    void deleteBySenderIdAndRecipientId(String senderId, String recipientId);
    
    boolean existsBySenderIdAndRecipientId(String senderId, String recipientId);
}
