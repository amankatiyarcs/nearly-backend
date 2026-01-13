package com.nearly.search.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.Instant;

@Document(indexName = "nearly_search")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchDocument {
    @Id private String id;
    
    @Field(type = FieldType.Keyword)
    private String entityType; // user, activity, event, group, news, job, deal, place, page
    
    @Field(type = FieldType.Keyword)
    private String entityId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private String location;
    
    @Field(type = FieldType.Keyword)
    private String userId;
    
    @Field(type = FieldType.Text)
    private String imageUrl;
    
    @Field(type = FieldType.Integer)
    private int popularity;
    
    @Field(type = FieldType.Date)
    private Instant createdAt;
    
    @Field(type = FieldType.Date)
    private Instant updatedAt;
}

