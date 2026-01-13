package com.nearly.moments.controller;
import com.nearly.moments.model.*;
import com.nearly.moments.service.MomentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/moments") @RequiredArgsConstructor
public class MomentController {
    private final MomentService service;

    @GetMapping
    public List<Moment> getMoments(@RequestParam(required = false) String visibility, @RequestParam(required = false) Integer limit) {
        return service.getMoments(visibility, limit);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Moment> getMoment(@PathVariable String id) { return service.getMoment(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Moment createMoment(@RequestBody Moment moment) { return service.createMoment(moment); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMoment(@PathVariable String id) { service.deleteMoment(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/like")
    public Moment likeMoment(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) { 
        String userId = body != null ? body.get("userId") : null;
        return service.likeMoment(id, userId); 
    }
    @PostMapping("/{id}/view")
    public Moment viewMoment(@PathVariable String id) { return service.viewMoment(id); }
    @PostMapping("/{id}/send")
    public DirectMoment sendDirectMoment(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.sendDirectMoment(id, body.get("senderId"), body.get("recipientId"));
    }
    @GetMapping("/direct/{userId}")
    public List<DirectMoment> getDirectMoments(@PathVariable String userId) { return service.getDirectMoments(userId); }
    @PostMapping("/direct/{id}/view")
    public ResponseEntity<Void> markViewed(@PathVariable String id) { service.markDirectMomentViewed(id); return ResponseEntity.ok().build(); }
    @GetMapping("/streaks/{userId}")
    public List<MomentStreak> getStreaks(@PathVariable String userId) { return service.getStreaks(userId); }
    @GetMapping("/streaks/{userId1}/{userId2}")
    public ResponseEntity<MomentStreak> getStreak(@PathVariable String userId1, @PathVariable String userId2) {
        return service.getStreak(userId1, userId2).map(ResponseEntity::ok).orElse(ResponseEntity.ok(MomentStreak.builder().streakCount(0).build()));
    }

    // ============ COMMENTS ============
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable String id) {
        return ResponseEntity.ok(service.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.addComment(id, body));
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

