package com.nearly.user.repository;

import com.nearly.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);
    List<User> findByInterestsContaining(String interest);
}

