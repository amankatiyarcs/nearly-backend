package com.nearly.messaging.repository;
import com.nearly.messaging.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByGroupIdOrderByCreatedAtAsc(String groupId);
    List<Message> findBySenderIdAndRecipientIdOrRecipientIdAndSenderIdOrderByCreatedAtAsc(String s1, String r1, String s2, String r2);
    List<Message> findByRecipientIdAndIsReadFalse(String recipientId);
}

