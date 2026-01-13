package com.nearly.news.repository;
import com.nearly.news.model.News;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NewsRepository extends MongoRepository<News, String> {
    List<News> findByUserId(String userId);
    List<News> findByCategory(String category);
    List<News> findByLocationContainingIgnoreCase(String location);
    List<News> findAllByOrderByPublishedAtDesc();
}

