package com.nearly.group.repository;
import com.nearly.group.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GroupRepository extends MongoRepository<Group, String> {
    List<Group> findByUserId(String userId);
    List<Group> findByCategory(String category);
    List<Group> findByNameContainingIgnoreCase(String name);
}

