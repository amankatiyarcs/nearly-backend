package com.nearly.event.controller;

import com.nearly.event.model.*;
import com.nearly.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/events") @RequiredArgsConstructor
public class EventController {
    private final EventService service;

    @GetMapping
    public List<Event> getEvents(@RequestParam(required = false) Integer limit) { return service.getEvents(limit); }
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable String id) { return service.getEvent(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Event createEvent(@RequestBody Event event) { return service.createEvent(event); }
    @PatchMapping("/{id}")
    public Event updateEvent(@PathVariable String id, @RequestBody Event updates) { return service.updateEvent(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) { service.deleteEvent(id); return ResponseEntity.noContent().build(); }
    
    @PostMapping("/{id}/like")
    public Event likeEvent(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.likeEvent(id, body.get("userId"));
    }
    
    @GetMapping("/{id}/guests")
    public List<EventGuest> getGuests(@PathVariable String id) { return service.getGuests(id); }
    @PostMapping("/{id}/join")
    public EventGuest joinEvent(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.joinEvent(id, body.get("userId"), body.getOrDefault("status", "attending"));
    }
    
    @GetMapping("/{id}/comments")
    public List<EventComment> getComments(@PathVariable String id) { return service.getComments(id); }
    @PostMapping("/{id}/comments")
    public EventComment addComment(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.addComment(id, body.get("userId"), body.get("content"));
    }
    
    @GetMapping("/user/{userId}")
    public List<Event> getByUser(@PathVariable String userId) { return service.getByUser(userId); }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

