package com.nearly.marketplace.controller;
import com.nearly.marketplace.model.Deal;
import com.nearly.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/deals") @RequiredArgsConstructor
public class DealController {
    private final MarketplaceService service;

    @GetMapping
    public List<Deal> getDeals(@RequestParam(required = false) String category, @RequestParam(required = false) Integer limit) {
        return service.getDeals(category, limit);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Deal> getDeal(@PathVariable String id) { return service.getDeal(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Deal createDeal(@RequestBody Deal deal) { return service.createDeal(deal); }
    @PatchMapping("/{id}")
    public Deal updateDeal(@PathVariable String id, @RequestBody Deal updates) { return service.updateDeal(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeal(@PathVariable String id) { service.deleteDeal(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/claim")
    public Deal claimDeal(@PathVariable String id) { return service.claimDeal(id); }
}

