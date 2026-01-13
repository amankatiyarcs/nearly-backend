package com.nearly.notification.service;
import com.nearly.notification.model.Notification;
import com.nearly.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification createNotification(Notification notification) {
        notification.setCreatedAt(Instant.now());
        notification.setRead(false);
        Notification saved = repo.save(notification);
        // Send real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(notification.getUserId(), "/queue/notifications", saved);
        log.info("Notification sent to user {}: {}", notification.getUserId(), notification.getTitle());
        return saved;
    }

    public List<Notification> getNotifications(String userId, String type) {
        if (type != null) return repo.findByUserIdAndType(userId, type);
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(String userId) {
        return repo.findByUserIdAndIsReadFalse(userId);
    }

    public long getUnreadCount(String userId) {
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    public Notification markAsRead(String id) {
        Notification notification = repo.findById(id).orElseThrow();
        notification.setRead(true);
        return repo.save(notification);
    }

    public void markAllAsRead(String userId) {
        repo.findByUserIdAndIsReadFalse(userId).forEach(n -> {
            n.setRead(true);
            repo.save(n);
        });
    }

    public void deleteNotification(String id) {
        repo.deleteById(id);
    }
}

