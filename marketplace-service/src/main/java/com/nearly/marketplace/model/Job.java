package com.nearly.marketplace.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "jobs") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Job {
    @Id private String id;
    private String userId;
    private String title;
    private String company;
    private String companyLogo;
    private String companyDescription;
    private String companyWebsite;
    private String companyEmail;
    private String location;
    private String locationType; // On-site, Remote, Hybrid
    private Integer salaryMin;
    private Integer salaryMax;
    private String type; // Full-time, Part-time, Contract, etc.
    private String experience;
    private String category;
    private boolean isHot;
    private String description;
    private List<String> responsibilities;
    private List<String> requirements;
    private List<String> benefits;
    private List<String> skills;
    private int applicantsCount;
    private Instant createdAt;
    private Instant expiresAt;
}

