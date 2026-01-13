package com.nearly.moments.repository;
import com.nearly.moments.model.Moment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface MomentRepository extends MongoRepository<Moment, String> {
    List<Moment> findByUserId(String userId);
    List<Moment> findByVisibilityAndExpiresAtAfter(String visibility, Instant now);
    List<Moment> findByExpiresAtBefore(Instant now);
    void deleteByExpiresAtBefore(Instant now);
}

