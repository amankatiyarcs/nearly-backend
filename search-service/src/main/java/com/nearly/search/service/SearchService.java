package com.nearly.search.service;
import com.nearly.search.model.SearchDocument;
import com.nearly.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class SearchService {
    private final SearchRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TRENDING_KEY = "trending:searches";

    public List<SearchDocument> search(String query, String type, String category, String location, Integer limit) {
        // Record search for trending
        redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, query, 1);
        redisTemplate.expire(TRENDING_KEY, Duration.ofHours(24));
        
        List<SearchDocument> results = repository.findByTitleContainingOrDescriptionContainingOrContentContaining(query, query, query);
        
        // Filter by type
        if (type != null) {
            results = results.stream().filter(d -> type.equals(d.getEntityType())).collect(Collectors.toList());
        }
        // Filter by category
        if (category != null) {
            results = results.stream().filter(d -> category.equals(d.getCategory())).collect(Collectors.toList());
        }
        // Filter by location
        if (location != null) {
            results = results.stream().filter(d -> d.getLocation() != null && d.getLocation().toLowerCase().contains(location.toLowerCase())).collect(Collectors.toList());
        }
        // Sort by popularity
        results.sort((a, b) -> Integer.compare(b.getPopularity(), a.getPopularity()));
        
        // Apply limit
        if (limit != null && results.size() > limit) {
            results = results.subList(0, limit);
        }
        
        return results;
    }

    public List<String> getTrendingSearches(int limit) {
        Set<Object> trending = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, limit - 1);
        return trending != null ? trending.stream().map(Object::toString).collect(Collectors.toList()) : List.of();
    }

    public List<SearchDocument> getByType(String type, int page, int size) {
        return repository.findByEntityType(type, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "popularity"))).getContent();
    }

    public void indexDocument(SearchDocument document) {
        document.setUpdatedAt(Instant.now());
        if (document.getCreatedAt() == null) document.setCreatedAt(Instant.now());
        repository.save(document);
        log.info("Indexed document: {} - {}", document.getEntityType(), document.getEntityId());
    }

    public void deleteDocument(String entityType, String entityId) {
        repository.deleteByEntityTypeAndEntityId(entityType, entityId);
        log.info("Deleted document: {} - {}", entityType, entityId);
    }

    public void reindexAll() {
        // In production, this would fetch from all services and reindex
        log.info("Reindexing all documents...");
    }
}

