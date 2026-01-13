package com.nearly.marketplace.service;
import com.nearly.marketplace.model.*;
import com.nearly.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class MarketplaceService {
    private final JobRepository jobRepo;
    private final DealRepository dealRepo;
    private final PlaceRepository placeRepo;
    private final PageRepository pageRepo;
    private final KafkaTemplate<String, Object> kafka;

    // Jobs
    public List<Job> getJobs(String category, Integer limit) {
        if (category != null) return jobRepo.findByCategory(category);
        if (limit != null) return jobRepo.findAll(PageRequest.of(0, limit)).getContent();
        return jobRepo.findAll();
    }
    public Optional<Job> getJob(String id) { return jobRepo.findById(id); }
    public Job createJob(Job job) { job.setCreatedAt(Instant.now()); return jobRepo.save(job); }
    public Job updateJob(String id, Job updates) {
        Job job = jobRepo.findById(id).orElseThrow();
        if (updates.getTitle() != null) job.setTitle(updates.getTitle());
        if (updates.getDescription() != null) job.setDescription(updates.getDescription());
        return jobRepo.save(job);
    }
    public void deleteJob(String id) { jobRepo.deleteById(id); }
    public List<Job> searchJobs(String q) { return jobRepo.findByTitleContainingIgnoreCaseOrCompanyContainingIgnoreCase(q, q); }

    // Deals
    public List<Deal> getDeals(String category, Integer limit) {
        if (category != null) return dealRepo.findByCategory(category);
        if (limit != null) return dealRepo.findAll(PageRequest.of(0, limit)).getContent();
        return dealRepo.findByValidUntilAfter(Instant.now());
    }
    public Optional<Deal> getDeal(String id) { return dealRepo.findById(id); }
    public Deal createDeal(Deal deal) { deal.setCreatedAt(Instant.now()); return dealRepo.save(deal); }
    public Deal updateDeal(String id, Deal updates) {
        Deal deal = dealRepo.findById(id).orElseThrow();
        if (updates.getShortDescription() != null) deal.setShortDescription(updates.getShortDescription());
        return dealRepo.save(deal);
    }
    public void deleteDeal(String id) { dealRepo.deleteById(id); }
    public Deal claimDeal(String id) {
        Deal deal = dealRepo.findById(id).orElseThrow();
        deal.setClaimsCount(deal.getClaimsCount() + 1);
        return dealRepo.save(deal);
    }

    // Places
    public List<Place> getPlaces(String category, String city, Integer limit) {
        if (category != null) return placeRepo.findByCategory(category);
        if (city != null) return placeRepo.findByCity(city);
        if (limit != null) return placeRepo.findAll(PageRequest.of(0, limit)).getContent();
        return placeRepo.findAll();
    }
    public Optional<Place> getPlace(String id) { return placeRepo.findById(id); }
    public Place createPlace(Place place) { place.setCreatedAt(Instant.now()); return placeRepo.save(place); }
    public Place updatePlace(String id, Place updates) {
        Place place = placeRepo.findById(id).orElseThrow();
        if (updates.getName() != null) place.setName(updates.getName());
        if (updates.getFullDescription() != null) place.setFullDescription(updates.getFullDescription());
        return placeRepo.save(place);
    }
    public void deletePlace(String id) { placeRepo.deleteById(id); }

    // Pages
    public List<Page> getPages(String category, Integer limit) {
        if (category != null) return pageRepo.findByCategory(category);
        if (limit != null) return pageRepo.findAll(PageRequest.of(0, limit)).getContent();
        return pageRepo.findAll();
    }
    public Optional<Page> getPage(String id) { return pageRepo.findById(id); }
    public Optional<Page> getPageByUsername(String username) { return pageRepo.findByUsername(username); }
    public Page createPage(Page page) { page.setCreatedAt(Instant.now()); return pageRepo.save(page); }
    public Page updatePage(String id, Page updates) {
        Page page = pageRepo.findById(id).orElseThrow();
        if (updates.getName() != null) page.setName(updates.getName());
        if (updates.getAbout() != null) page.setAbout(updates.getAbout());
        if (updates.getAvatarUrl() != null) page.setAvatarUrl(updates.getAvatarUrl());
        return pageRepo.save(page);
    }
    public void deletePage(String id) { pageRepo.deleteById(id); }
    public Page followPage(String id) {
        Page page = pageRepo.findById(id).orElseThrow();
        page.setFollowersCount(page.getFollowersCount() + 1);
        return pageRepo.save(page);
    }
}

