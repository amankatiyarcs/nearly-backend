package com.nearly.search.controller;
import com.nearly.search.model.SearchDocument;
import com.nearly.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/search") @RequiredArgsConstructor
public class SearchController {
    private final SearchService service;

    @GetMapping
    public List<SearchDocument> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer limit) {
        return service.search(q, type, category, location, limit);
    }

    @GetMapping("/type/{type}")
    public List<SearchDocument> getByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getByType(type, page, size);
    }

    @GetMapping("/trending")
    public List<String> getTrendingSearches(@RequestParam(defaultValue = "10") int limit) {
        return service.getTrendingSearches(limit);
    }

    @PostMapping("/index")
    public ResponseEntity<SearchDocument> indexDocument(@RequestBody SearchDocument document) {
        service.indexDocument(document);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{entityType}/{entityId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String entityType, @PathVariable String entityId) {
        service.deleteDocument(entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindexAll() {
        service.reindexAll();
        return ResponseEntity.ok(Map.of("status", "Reindex started"));
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

