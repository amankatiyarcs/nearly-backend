package com.nearly.search.repository;
import com.nearly.search.model.SearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SearchRepository extends ElasticsearchRepository<SearchDocument, String> {
    List<SearchDocument> findByTitleContainingOrDescriptionContainingOrContentContaining(String title, String description, String content);
    Page<SearchDocument> findByEntityType(String entityType, Pageable pageable);
    List<SearchDocument> findByCategory(String category);
    List<SearchDocument> findByLocation(String location);
    void deleteByEntityTypeAndEntityId(String entityType, String entityId);
}

