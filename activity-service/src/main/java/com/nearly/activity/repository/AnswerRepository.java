package com.nearly.activity.repository;

import com.nearly.activity.model.Answer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends MongoRepository<Answer, String> {
    
    List<Answer> findByQuestionIdOrderByCreatedAtDesc(String questionId);
    
    Optional<Answer> findByQuestionIdAndIsAcceptedTrue(String questionId);
    
    long countByQuestionId(String questionId);
}
