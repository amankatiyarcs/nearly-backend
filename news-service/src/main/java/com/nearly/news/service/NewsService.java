package com.nearly.news.service;
import com.nearly.news.model.News;
import com.nearly.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class NewsService {
    private final NewsRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    public List<News> getNews(Integer limit) {
        if (limit != null) return repo.findAll(PageRequest.of(0, limit)).getContent();
        return repo.findAllByOrderByPublishedAtDesc();
    }
    public Optional<News> getNewsItem(String id) { return repo.findById(id); }
    public News createNews(News news) {
        news.setPublishedAt(Instant.now());
        News saved = repo.save(news);
        kafka.send("news-events", Map.of("type", "NEWS_CREATED", "newsId", saved.getId()));
        return saved;
    }
    public News updateNews(String id, News updates) {
        News news = repo.findById(id).orElseThrow();
        if (updates.getHeadline() != null) news.setHeadline(updates.getHeadline());
        if (updates.getDescription() != null) news.setDescription(updates.getDescription());
        if (updates.getImageUrl() != null) news.setImageUrl(updates.getImageUrl());
        if (updates.getLocation() != null) news.setLocation(updates.getLocation());
        return repo.save(news);
    }
    public void deleteNews(String id) { repo.deleteById(id); }
    public News voteNews(String id, String voteType, boolean increment) {
        News news = repo.findById(id).orElseThrow();
        if ("true".equals(voteType)) news.setTrueVotes(news.getTrueVotes() + (increment ? 1 : -1));
        else if ("fake".equals(voteType)) news.setFakeVotes(news.getFakeVotes() + (increment ? 1 : -1));
        return repo.save(news);
    }
    public News likeNews(String id, boolean increment) {
        News news = repo.findById(id).orElseThrow();
        news.setLikesCount(news.getLikesCount() + (increment ? 1 : -1));
        return repo.save(news);
    }
    public List<News> getByUser(String userId) { return repo.findByUserId(userId); }
}

