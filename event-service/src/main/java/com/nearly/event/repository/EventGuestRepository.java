package com.nearly.event.repository;

import com.nearly.event.model.EventGuest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EventGuestRepository extends MongoRepository<EventGuest, String> {
    List<EventGuest> findByEventId(String eventId);
    List<EventGuest> findByUserId(String userId);
    boolean existsByEventIdAndUserId(String eventId, String userId);
}

