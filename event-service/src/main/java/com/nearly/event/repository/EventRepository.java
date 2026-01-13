package com.nearly.event.repository;

import com.nearly.event.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByUserId(String userId);
    List<Event> findByCategory(String category);
    List<Event> findByStartDateAfter(Instant date);
    List<Event> findByLocationContainingIgnoreCase(String location);
}

