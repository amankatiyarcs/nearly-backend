package com.nearly.marketplace.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "deals") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Deal {
    @Id private String id;
    private String userId;
    private String businessName;
    private String category;
    private String discountType;
    private String discountValue;
    private String discountCode;
    private String shortDescription;
    private String fullDescription;
    private Instant validFrom;
    private Instant validUntil;
    private String location;
    private String address;
    private String phone;
    private String website;
    private String openHours;
    private boolean isFeatured;
    private List<String> images;
    private List<String> termsConditions;
    private List<String> highlights;
    private int claimsCount;
    private double rating;
    private int reviewsCount;
    private Instant createdAt;
}

