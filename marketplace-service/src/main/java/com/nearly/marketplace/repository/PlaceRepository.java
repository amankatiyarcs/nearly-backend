package com.nearly.marketplace.repository;
import com.nearly.marketplace.model.Place;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PlaceRepository extends MongoRepository<Place, String> {
    List<Place> findByUserId(String userId);
    List<Place> findByCategory(String category);
    List<Place> findByCity(String city);
    List<Place> findByIsTrendingTrue();
    List<Place> findByNameContainingIgnoreCase(String name);
}

