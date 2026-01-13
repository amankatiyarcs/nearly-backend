package com.nearly.moments.repository;

import com.nearly.moments.model.MomentComment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MomentCommentRepository extends MongoRepository<MomentComment, String> {
    
    List<MomentComment> findByMomentIdOrderByCreatedAtDesc(String momentId);
    
    List<MomentComment> findByParentCommentId(String parentCommentId);
    
    long countByMomentId(String momentId);
    
    void deleteByMomentId(String momentId);
}
