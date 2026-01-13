package com.nearly.messaging.service;
import com.nearly.messaging.model.*;
import com.nearly.messaging.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class MessageService {
    private final MessageRepository msgRepo;
    private final ConversationRepository convRepo;
    private final KafkaTemplate<String, Object> kafka;

    public Message sendMessage(Message message) {
        message.setCreatedAt(Instant.now());
        message.setRead(false);
        Message saved = msgRepo.save(message);
        
        // Update conversation
        if (message.getGroupId() != null) {
            convRepo.findByGroupId(message.getGroupId()).ifPresent(c -> {
                c.setLastMessage(message.getContent());
                c.setLastSenderId(message.getSenderId());
                c.setLastMessageAt(Instant.now());
                convRepo.save(c);
            });
        }
        
        kafka.send("message-events", Map.of("type", "MESSAGE_SENT", "messageId", saved.getId(), "recipientId", message.getRecipientId() != null ? message.getRecipientId() : message.getGroupId()));
        return saved;
    }

    public List<Message> getGroupMessages(String groupId) {
        return msgRepo.findByGroupIdOrderByCreatedAtAsc(groupId);
    }

    public List<Message> getDirectMessages(String userId1, String userId2) {
        return msgRepo.findBySenderIdAndRecipientIdOrRecipientIdAndSenderIdOrderByCreatedAtAsc(userId1, userId2, userId1, userId2);
    }

    public List<Conversation> getConversations(String userId) {
        return convRepo.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
    }

    public void markAsRead(String recipientId) {
        msgRepo.findByRecipientIdAndIsReadFalse(recipientId).forEach(m -> {
            m.setRead(true);
            msgRepo.save(m);
        });
    }
}

