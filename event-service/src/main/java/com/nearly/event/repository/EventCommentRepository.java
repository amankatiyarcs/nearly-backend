package com.nearly.event.repository;

import com.nearly.event.model.EventComment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EventCommentRepository extends MongoRepository<EventComment, String> {
    List<EventComment> findByEventIdOrderByCreatedAtDesc(String eventId);
}

