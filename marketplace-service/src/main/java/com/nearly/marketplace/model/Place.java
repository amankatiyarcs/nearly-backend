package com.nearly.marketplace.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "places") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Place {
    @Id private String id;
    private String userId;
    private String name;
    private String category;
    private String shortDescription;
    private String fullDescription;
    private String address;
    private String city;
    private String openTime;
    private String closeTime;
    private String entryFee;
    private String bestTime;
    private String phone;
    private String website;
    private boolean isTrending;
    private List<String> images;
    private List<String> amenities;
    private List<String> tips;
    private List<Map<String, String>> nearbyAttractions;
    private double latitude;
    private double longitude;
    private double rating;
    private int reviewsCount;
    private Instant createdAt;
}

