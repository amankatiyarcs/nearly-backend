package com.nearly.activity.repository;

import com.nearly.activity.model.Question;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {
    
    List<Question> findByUserId(String userId);
    
    List<Question> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Question> findByTagsContaining(String tag);
    
    List<Question> findByIsResolved(Boolean isResolved);
}
