package com.nearly.activity.repository;

import com.nearly.activity.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {
    List<Activity> findByUserId(String userId);
    List<Activity> findByCategory(String category);
    List<Activity> findByStartDateAfter(Instant date);
    List<Activity> findByLocationContainingIgnoreCase(String location);
    List<Activity> findByVisibility(String visibility);
    List<Activity> findByParticipantIdsContaining(String userId);
}

