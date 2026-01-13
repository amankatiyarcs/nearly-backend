package com.nearly.marketplace.repository;
import com.nearly.marketplace.model.Deal;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface DealRepository extends MongoRepository<Deal, String> {
    List<Deal> findByUserId(String userId);
    List<Deal> findByCategory(String category);
    List<Deal> findByIsFeaturedTrue();
    List<Deal> findByValidUntilAfter(Instant now);
    List<Deal> findByLocationContainingIgnoreCase(String location);
}

