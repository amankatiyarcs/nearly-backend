package com.nearly.chat.repository;

import com.nearly.chat.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    
    Optional<ChatRoom> findBySessionId1OrSessionId2(String sessionId1, String sessionId2);
    
    List<ChatRoom> findByActiveTrue();
    
    long countByActiveTrue();
}
