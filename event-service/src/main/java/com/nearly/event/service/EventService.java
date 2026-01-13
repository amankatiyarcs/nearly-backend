package com.nearly.event.service;

import com.nearly.event.model.*;
import com.nearly.event.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepo;
    private final EventGuestRepository guestRepo;
    private final EventCommentRepository commentRepo;
    private final KafkaTemplate<String, Object> kafka;

    public List<Event> getEvents(Integer limit) {
        if (limit != null) return eventRepo.findAll(PageRequest.of(0, limit)).getContent();
        return eventRepo.findAll();
    }
    public Optional<Event> getEvent(String id) { return eventRepo.findById(id); }
    public Event createEvent(Event event) {
        event.setCreatedAt(Instant.now());
        Event saved = eventRepo.save(event);
        kafka.send("event-events", Map.of("type", "EVENT_CREATED", "eventId", saved.getId()));
        return saved;
    }
    public Event updateEvent(String id, Event updates) {
        Event event = eventRepo.findById(id).orElseThrow();
        if (updates.getTitle() != null) event.setTitle(updates.getTitle());
        if (updates.getDescription() != null) event.setDescription(updates.getDescription());
        if (updates.getLocation() != null) event.setLocation(updates.getLocation());
        if (updates.getStartDate() != null) event.setStartDate(updates.getStartDate());
        if (updates.getEndDate() != null) event.setEndDate(updates.getEndDate());
        return eventRepo.save(event);
    }
    public void deleteEvent(String id) { eventRepo.deleteById(id); }
    
    public List<EventGuest> getGuests(String eventId) { return guestRepo.findByEventId(eventId); }
    public EventGuest joinEvent(String eventId, String userId, String status) {
        if (guestRepo.existsByEventIdAndUserId(eventId, userId)) throw new IllegalStateException("Already joined");
        EventGuest guest = EventGuest.builder().eventId(eventId).userId(userId).status(status).createdAt(Instant.now()).build();
        eventRepo.findById(eventId).ifPresent(e -> { e.setAttendeesCount(e.getAttendeesCount() + 1); eventRepo.save(e); });
        return guestRepo.save(guest);
    }
    
    public List<EventComment> getComments(String eventId) { return commentRepo.findByEventIdOrderByCreatedAtDesc(eventId); }
    public EventComment addComment(String eventId, String userId, String content) {
        EventComment comment = EventComment.builder().eventId(eventId).userId(userId).content(content).createdAt(Instant.now()).build();
        EventComment saved = commentRepo.save(comment);
        eventRepo.findById(eventId).ifPresent(e -> { 
            e.setCommentsCount(e.getCommentsCount() + 1); 
            eventRepo.save(e);
            
            // Send notification to event owner (if different user)
            if (e.getUserId() != null && !e.getUserId().equals(userId)) {
                kafka.send("event-events", Map.of(
                    "type", "EVENT_COMMENTED",
                    "eventId", eventId,
                    "eventTitle", e.getTitle() != null ? e.getTitle() : "",
                    "eventOwnerId", e.getUserId(),
                    "commentId", saved.getId(),
                    "commenterId", userId != null ? userId : "anonymous",
                    "commentContent", content != null ? content.substring(0, Math.min(content.length(), 100)) : ""
                ));
            }
        });
        return saved;
    }
    
    public Event likeEvent(String id, String userId) {
        Event event = eventRepo.findById(id).orElseThrow();
        event.setLikesCount(event.getLikesCount() + 1);
        Event saved = eventRepo.save(event);
        
        // Send notification to event owner (if different user)
        if (event.getUserId() != null && !event.getUserId().equals(userId)) {
            kafka.send("event-events", Map.of(
                "type", "EVENT_LIKED",
                "eventId", id,
                "eventTitle", event.getTitle() != null ? event.getTitle() : "",
                "eventOwnerId", event.getUserId(),
                "likerId", userId != null ? userId : "anonymous"
            ));
        }
        return saved;
    }
    
    public List<Event> getByUser(String userId) { return eventRepo.findByUserId(userId); }
}

