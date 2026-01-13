package com.nearly.marketplace.repository;
import com.nearly.marketplace.model.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PageRepository extends MongoRepository<Page, String> {
    Optional<Page> findByUsername(String username);
    List<Page> findByUserId(String userId);
    List<Page> findByCategory(String category);
    List<Page> findByIsVerifiedTrue();
    List<Page> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);
}

