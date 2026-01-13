package com.nearly.messaging.controller;

import com.nearly.messaging.model.Conversation;
import com.nearly.messaging.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<Conversation>> getConversations(
            @RequestParam(required = false) String userId) {
        if (userId != null) {
            return ResponseEntity.ok(messageService.getConversations(userId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String conversationId) {
        // Return a mock conversation for now
        return ResponseEntity.ok(Conversation.builder()
            .id(conversationId)
            .build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
