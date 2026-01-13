package com.nearly.activity.controller;

import com.nearly.activity.model.Activity;
import com.nearly.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/activities") @RequiredArgsConstructor
public class ActivityController {
    private final ActivityService service;

    @GetMapping
    public List<Activity> getActivities(@RequestParam(required = false) Integer limit) {
        return service.getActivities(limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivity(@PathVariable String id) {
        return service.getActivity(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Activity createActivity(@RequestBody Activity activity) {
        return service.createActivity(activity);
    }

    @PatchMapping("/{id}")
    public Activity updateActivity(@PathVariable String id, @RequestBody Activity updates) {
        return service.updateActivity(id, updates);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String id) {
        service.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public Activity likeActivity(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Boolean increment = body.get("increment") != null ? (Boolean) body.get("increment") : true;
        String userId = body.get("userId") != null ? (String) body.get("userId") : null;
        return service.likeActivity(id, increment, userId);
    }

    @PostMapping("/{id}/join")
    public Activity joinActivity(@PathVariable String id, @RequestBody Map<String, String> body) {
        return service.joinActivity(id, body.get("userId"));
    }

    @GetMapping("/user/{userId}")
    public List<Activity> getByUser(@PathVariable String userId) {
        return service.getByUser(userId);
    }

    @GetMapping("/category/{category}")
    public List<Activity> getByCategory(@PathVariable String category) {
        return service.getByCategory(category);
    }

    // ============ COMMENTS ============

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable String id) {
        return ResponseEntity.ok(service.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> comment = service.addComment(id, body);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

