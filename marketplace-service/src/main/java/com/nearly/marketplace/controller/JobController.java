package com.nearly.marketplace.controller;
import com.nearly.marketplace.model.Job;
import com.nearly.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/jobs") @RequiredArgsConstructor
public class JobController {
    private final MarketplaceService service;

    @GetMapping
    public List<Job> getJobs(@RequestParam(required = false) String category, @RequestParam(required = false) Integer limit) {
        return service.getJobs(category, limit);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable String id) { return service.getJob(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Job createJob(@RequestBody Job job) { return service.createJob(job); }
    @PatchMapping("/{id}")
    public Job updateJob(@PathVariable String id, @RequestBody Job updates) { return service.updateJob(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable String id) { service.deleteJob(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/search")
    public List<Job> searchJobs(@RequestParam String q) { return service.searchJobs(q); }
}

