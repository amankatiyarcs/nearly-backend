package com.nearly.news.controller;
import com.nearly.news.model.News;
import com.nearly.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/news") @RequiredArgsConstructor
public class NewsController {
    private final NewsService service;

    @GetMapping
    public List<News> getNews(@RequestParam(required = false) Integer limit) { return service.getNews(limit); }
    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsItem(@PathVariable String id) { return service.getNewsItem(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public News createNews(@RequestBody News news) { return service.createNews(news); }
    @PatchMapping("/{id}")
    public News updateNews(@PathVariable String id, @RequestBody News updates) { return service.updateNews(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable String id) { service.deleteNews(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/vote")
    public News voteNews(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return service.voteNews(id, (String) body.get("voteType"), (Boolean) body.getOrDefault("increment", true));
    }
    @PostMapping("/{id}/like")
    public News likeNews(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return service.likeNews(id, body.getOrDefault("increment", true));
    }
    @GetMapping("/user/{userId}")
    public List<News> getByUser(@PathVariable String userId) { return service.getByUser(userId); }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

