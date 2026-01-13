package com.nearly.pai.repository;

import com.nearly.pai.model.MatchHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchHistoryRepository extends MongoRepository<MatchHistory, String> {
    
    List<MatchHistory> findBySessionId1OrSessionId2(String sessionId1, String sessionId2);
    
    List<MatchHistory> findByRoomId(String roomId);
}
