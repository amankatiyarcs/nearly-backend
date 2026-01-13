package com.nearly.moments.repository;
import com.nearly.moments.model.MomentStreak;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MomentStreakRepository extends MongoRepository<MomentStreak, String> {
    Optional<MomentStreak> findByUserId1AndUserId2(String userId1, String userId2);
    List<MomentStreak> findByUserId1OrUserId2(String userId1, String userId2);
}

