package com.nearly.marketplace.repository;
import com.nearly.marketplace.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByUserId(String userId);
    List<Job> findByCategory(String category);
    List<Job> findByLocationContainingIgnoreCase(String location);
    List<Job> findByType(String type);
    List<Job> findByIsHotTrue();
    List<Job> findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(String title, String company);
}

