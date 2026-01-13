package com.nearly.notification.consumer;
import com.nearly.notification.model.Notification;
import com.nearly.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @Slf4j @RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService service;

    @KafkaListener(topics = "user-events", groupId = "notification-service")
    public void handleUserEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received user event: {}", type);
        
        if ("USER_FOLLOWED".equals(type)) {
            service.createNotification(Notification.builder()
                .userId((String) event.get("followingId"))
                .type("follow")
                .title("New Follower")
                .description("Someone started following you")
                .relatedId((String) event.get("followerId"))
                .build());
        }
    }

    @KafkaListener(topics = "activity-events", groupId = "notification-service")
    public void handleActivityEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received activity event: {}", type);
        
        if ("ACTIVITY_LIKED".equals(type)) {
            String ownerId = (String) event.get("activityOwnerId");
            String likerId = (String) event.get("likerId");
            String activityTitle = (String) event.get("activityTitle");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("activity_like")
                    .title("New Like on Your Activity")
                    .description("Someone liked your activity" + (activityTitle != null ? ": " + activityTitle : ""))
                    .relatedId((String) event.get("activityId"))
                    .build());
            }
        } else if ("ACTIVITY_COMMENTED".equals(type)) {
            String ownerId = (String) event.get("activityOwnerId");
            String commenterId = (String) event.get("commenterId");
            String activityTitle = (String) event.get("activityTitle");
            String commentContent = (String) event.get("commentContent");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("activity_comment")
                    .title("New Comment on Your Activity")
                    .description("Someone commented" + (activityTitle != null ? " on \"" + activityTitle + "\"" : "") + 
                                (commentContent != null && !commentContent.isEmpty() ? ": " + commentContent : ""))
                    .relatedId((String) event.get("activityId"))
                    .build());
            }
        }
    }

    @KafkaListener(topics = "event-events", groupId = "notification-service")
    public void handleEventEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received event event: {}", type);
        
        if ("EVENT_LIKED".equals(type)) {
            String ownerId = (String) event.get("eventOwnerId");
            String eventTitle = (String) event.get("eventTitle");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("event_like")
                    .title("New Like on Your Event")
                    .description("Someone liked your event" + (eventTitle != null ? ": " + eventTitle : ""))
                    .relatedId((String) event.get("eventId"))
                    .build());
            }
        } else if ("EVENT_COMMENTED".equals(type)) {
            String ownerId = (String) event.get("eventOwnerId");
            String eventTitle = (String) event.get("eventTitle");
            String commentContent = (String) event.get("commentContent");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("event_comment")
                    .title("New Comment on Your Event")
                    .description("Someone commented" + (eventTitle != null ? " on \"" + eventTitle + "\"" : "") + 
                                (commentContent != null && !commentContent.isEmpty() ? ": " + commentContent : ""))
                    .relatedId((String) event.get("eventId"))
                    .build());
            }
        }
    }

    @KafkaListener(topics = "group-events", groupId = "notification-service")
    public void handleGroupEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received group event: {}", type);
        
        if ("GROUP_MEMBER_JOINED".equals(type)) {
            String ownerId = (String) event.get("groupOwnerId");
            String groupName = (String) event.get("groupName");
            String newMemberId = (String) event.get("newMemberId");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("group_member_joined")
                    .title("New Member Joined Your Group")
                    .description("Someone joined your group" + (groupName != null ? ": " + groupName : ""))
                    .relatedId((String) event.get("groupId"))
                    .build());
            }
        }
    }

    @KafkaListener(topics = "message-events", groupId = "notification-service")
    public void handleMessageEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        if ("MESSAGE_SENT".equals(type)) {
            String recipientId = (String) event.get("recipientId");
            if (recipientId != null && !recipientId.startsWith("group_")) {
                service.createNotification(Notification.builder()
                    .userId(recipientId)
                    .type("message")
                    .title("New Message")
                    .description("You have a new message")
                    .relatedId((String) event.get("messageId"))
                    .build());
            }
        }
    }

    @KafkaListener(topics = "moment-events", groupId = "notification-service")
    public void handleMomentEvents(Map<String, Object> event) {
        String type = (String) event.get("type");
        log.info("Received moment event: {}", type);
        
        if ("MOMENT_LIKED".equals(type)) {
            String ownerId = (String) event.get("momentOwnerId");
            String likerId = (String) event.get("likerId");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("moment_like")
                    .title("New Like on Your Moment")
                    .description("Someone liked your moment")
                    .relatedId((String) event.get("momentId"))
                    .build());
            }
        } else if ("MOMENT_COMMENTED".equals(type)) {
            String ownerId = (String) event.get("momentOwnerId");
            String commenterId = (String) event.get("commenterId");
            String commentContent = (String) event.get("commentContent");
            
            if (ownerId != null) {
                service.createNotification(Notification.builder()
                    .userId(ownerId)
                    .type("moment_comment")
                    .title("New Comment on Your Moment")
                    .description("Someone commented on your moment" + 
                                (commentContent != null && !commentContent.isEmpty() ? ": " + commentContent : ""))
                    .relatedId((String) event.get("momentId"))
                    .build());
            }
        }
    }
}

