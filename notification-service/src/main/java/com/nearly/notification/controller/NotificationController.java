package com.nearly.notification.controller;
import com.nearly.notification.model.Notification;
import com.nearly.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @PostMapping
    public Notification createNotification(@RequestBody Notification notification) { return service.createNotification(notification); }
    
    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable String userId, @RequestParam(required = false) String type) {
        return service.getNotifications(userId, type);
    }
    
    @GetMapping("/{userId}/unread")
    public List<Notification> getUnreadNotifications(@PathVariable String userId) { return service.getUnreadNotifications(userId); }
    
    @GetMapping("/{userId}/unread/count")
    public Map<String, Long> getUnreadCount(@PathVariable String userId) { return Map.of("count", service.getUnreadCount(userId)); }
    
    @PatchMapping("/{id}/read")
    public Notification markAsRead(@PathVariable String id) { return service.markAsRead(id); }
    
    @PostMapping("/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String userId) { service.markAllAsRead(userId); return ResponseEntity.ok().build(); }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) { service.deleteNotification(id); return ResponseEntity.noContent().build(); }
    
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

