package com.nearly.pai.repository;

import com.nearly.pai.model.MatchRating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRatingRepository extends MongoRepository<MatchRating, String> {
    
    List<MatchRating> findBySessionId(String sessionId);
    
    List<MatchRating> findByMatchedSessionId(String matchedSessionId);
}
