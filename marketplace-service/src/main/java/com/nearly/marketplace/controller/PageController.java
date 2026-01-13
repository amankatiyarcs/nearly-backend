package com.nearly.marketplace.controller;
import com.nearly.marketplace.model.Page;
import com.nearly.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/pages") @RequiredArgsConstructor
public class PageController {
    private final MarketplaceService service;

    @GetMapping
    public List<Page> getPages(@RequestParam(required = false) String category, @RequestParam(required = false) Integer limit) {
        return service.getPages(category, limit);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Page> getPage(@PathVariable String id) { return service.getPage(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @GetMapping("/username/{username}")
    public ResponseEntity<Page> getPageByUsername(@PathVariable String username) { return service.getPageByUsername(username).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Page createPage(@RequestBody Page page) { return service.createPage(page); }
    @PatchMapping("/{id}")
    public Page updatePage(@PathVariable String id, @RequestBody Page updates) { return service.updatePage(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePage(@PathVariable String id) { service.deletePage(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/follow")
    public Page followPage(@PathVariable String id) { return service.followPage(id); }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

