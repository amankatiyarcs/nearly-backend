package com.nearly.marketplace.controller;
import com.nearly.marketplace.model.Place;
import com.nearly.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/places") @RequiredArgsConstructor
public class PlaceController {
    private final MarketplaceService service;

    @GetMapping
    public List<Place> getPlaces(@RequestParam(required = false) String category, @RequestParam(required = false) String city, @RequestParam(required = false) Integer limit) {
        return service.getPlaces(category, city, limit);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Place> getPlace(@PathVariable String id) { return service.getPlace(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Place createPlace(@RequestBody Place place) { return service.createPlace(place); }
    @PatchMapping("/{id}")
    public Place updatePlace(@PathVariable String id, @RequestBody Place updates) { return service.updatePlace(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable String id) { service.deletePlace(id); return ResponseEntity.noContent().build(); }
}

