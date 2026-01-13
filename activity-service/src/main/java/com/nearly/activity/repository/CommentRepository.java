package com.nearly.activity.repository;

import com.nearly.activity.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
    
    List<Comment> findByParentCommentId(String parentCommentId);
    
    long countByTargetTypeAndTargetId(String targetType, String targetId);
    
    void deleteByTargetTypeAndTargetId(String targetType, String targetId);
}
