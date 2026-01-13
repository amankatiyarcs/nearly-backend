package com.nearly.messaging.repository;
import com.nearly.messaging.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    List<Conversation> findByParticipantIdsContainingOrderByLastMessageAtDesc(String userId);
    Optional<Conversation> findByGroupId(String groupId);
}

