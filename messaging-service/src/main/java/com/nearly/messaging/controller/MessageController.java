package com.nearly.messaging.controller;

import com.nearly.messaging.model.*;
import com.nearly.messaging.repository.*;
import com.nearly.messaging.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    
    private final MessageService service;
    private final MessageReactionRepository reactionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final MessageRequestRepository messageRequestRepository;
    private final MessageSeenRepository messageSeenRepository;

    @PostMapping
    public Message sendMessage(@RequestBody Message message) {
        return service.sendMessage(message);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getMessages(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String groupId) {
        if (groupId != null) {
            return ResponseEntity.ok(service.getGroupMessages(groupId));
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/group/{groupId}")
    public List<Message> getGroupMessages(@PathVariable String groupId) {
        return service.getGroupMessages(groupId);
    }

    @GetMapping("/direct/{userId}")
    public List<Message> getDirectMessages(@PathVariable String userId, @RequestParam String withUserId) {
        return service.getDirectMessages(userId, withUserId);
    }

    @GetMapping("/conversations/{userId}")
    public List<Conversation> getConversations(@PathVariable String userId) {
        return service.getConversations(userId);
    }

    @PostMapping("/read/{recipientId}")
    public ResponseEntity<Void> markAsRead(@PathVariable String recipientId) {
        service.markAsRead(recipientId);
        return ResponseEntity.ok().build();
    }

    // ============ MESSAGE SEEN (Database-backed) ============

    @PostMapping("/mark-seen")
    @Transactional
    public ResponseEntity<Map<String, Object>> markMessagesSeen(@RequestBody Map<String, Object> body) {
        String senderId = (String) body.get("senderId");
        String recipientId = (String) body.get("recipientId");
        
        @SuppressWarnings("unchecked")
        List<String> messageIds = (List<String>) body.get("messageIds");
        
        if (messageIds != null) {
            for (String messageId : messageIds) {
                if (!messageSeenRepository.existsByMessageIdAndUserId(messageId, recipientId)) {
                    MessageSeen seen = MessageSeen.builder()
                        .messageId(messageId)
                        .userId(recipientId)
                        .seenAt(Instant.now())
                        .build();
                    messageSeenRepository.save(seen);
                }
            }
        }
        
        log.info("Marking messages as seen between {} and {}", senderId, recipientId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============ MESSAGE REACTIONS (Database-backed) ============

    @PostMapping("/{messageId}/react")
    @Transactional
    public ResponseEntity<Map<String, Object>> reactToMessage(
            @PathVariable String messageId,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String emoji = body.get("emoji");
        
        Optional<MessageReaction> existingReaction = reactionRepository.findByMessageIdAndUserId(messageId, userId);
        
        if (existingReaction.isPresent()) {
            // Update existing reaction
            MessageReaction reaction = existingReaction.get();
            reaction.setEmoji(emoji);
            reactionRepository.save(reaction);
        } else {
            // Create new reaction
            MessageReaction reaction = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .createdAt(Instant.now())
                .build();
            reactionRepository.save(reaction);
        }
        
        log.info("User {} reacted to message {} with {}", userId, messageId, emoji);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "messageId", messageId,
            "emoji", emoji
        ));
    }

    @DeleteMapping("/{messageId}/react")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeReaction(
            @PathVariable String messageId,
            @RequestParam String userId) {
        reactionRepository.deleteByMessageIdAndUserId(messageId, userId);
        log.info("User {} removed reaction from message {}", userId, messageId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<List<Map<String, Object>>> getReactions(@PathVariable String messageId) {
        List<MessageReaction> reactions = reactionRepository.findByMessageId(messageId);
        List<Map<String, Object>> result = reactions.stream()
            .map(r -> Map.of(
                "userId", (Object) r.getUserId(),
                "emoji", r.getEmoji(),
                "createdAt", r.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ============ POLL VOTING (Database-backed) ============

    @PostMapping("/{messageId}/poll/vote")
    @Transactional
    public ResponseEntity<Map<String, Object>> voteOnPoll(
            @PathVariable String messageId,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String optionId = body.get("optionId");
        
        Optional<PollVote> existingVote = pollVoteRepository.findByMessageIdAndUserId(messageId, userId);
        
        if (existingVote.isPresent()) {
            // Update existing vote
            PollVote vote = existingVote.get();
            vote.setOptionId(optionId);
            vote.setVotedAt(Instant.now());
            pollVoteRepository.save(vote);
        } else {
            // Create new vote
            PollVote vote = PollVote.builder()
                .messageId(messageId)
                .userId(userId)
                .optionId(optionId)
                .votedAt(Instant.now())
                .build();
            pollVoteRepository.save(vote);
        }
        
        log.info("User {} voted {} on poll {}", userId, optionId, messageId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "messageId", messageId,
            "optionId", optionId
        ));
    }

    @GetMapping("/{messageId}/poll/results")
    public ResponseEntity<Map<String, Object>> getPollResults(@PathVariable String messageId) {
        List<PollVote> votes = pollVoteRepository.findByMessageId(messageId);
        
        Map<String, Long> resultsByOption = votes.stream()
            .collect(Collectors.groupingBy(PollVote::getOptionId, Collectors.counting()));
        
        return ResponseEntity.ok(Map.of(
            "messageId", messageId,
            "totalVotes", votes.size(),
            "results", resultsByOption
        ));
    }

    // ============ GROUP MESSAGES MARK SEEN ============

    @PostMapping("/groups/{groupId}/messages/mark-seen")
    @Transactional
    public ResponseEntity<Map<String, Object>> markGroupMessagesSeen(
            @PathVariable String groupId,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        
        // Get all unread messages in group and mark them as seen
        List<Message> groupMessages = service.getGroupMessages(groupId);
        for (Message message : groupMessages) {
            if (!messageSeenRepository.existsByMessageIdAndUserId(message.getId(), userId)) {
                MessageSeen seen = MessageSeen.builder()
                    .messageId(message.getId())
                    .userId(userId)
                    .seenAt(Instant.now())
                    .build();
                messageSeenRepository.save(seen);
            }
        }
        
        log.info("User {} marked group {} messages as seen", userId, groupId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============ MESSAGE REQUESTS (Database-backed) ============

    @GetMapping("/requests/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getMessageRequests(@PathVariable String userId) {
        List<MessageRequest> requests = messageRequestRepository.findByRecipientIdAndStatus(userId, "pending");
        
        List<Map<String, Object>> result = requests.stream()
            .map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("senderId", r.getSenderId());
                map.put("recipientId", r.getRecipientId());
                map.put("initialMessage", r.getInitialMessage());
                map.put("status", r.getStatus());
                map.put("createdAt", r.getCreatedAt().toString());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/requests")
    @Transactional
    public ResponseEntity<Map<String, Object>> createMessageRequest(@RequestBody Map<String, String> body) {
        String senderId = body.get("senderId");
        String recipientId = body.get("recipientId");
        String initialMessage = body.get("message");
        
        if (messageRequestRepository.existsBySenderIdAndRecipientId(senderId, recipientId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Message request already sent"
            ));
        }
        
        MessageRequest request = MessageRequest.builder()
            .senderId(senderId)
            .recipientId(recipientId)
            .initialMessage(initialMessage)
            .status("pending")
            .createdAt(Instant.now())
            .build();
        
        messageRequestRepository.save(request);
        
        log.info("Message request created from {} to {}", senderId, recipientId);
        return ResponseEntity.ok(Map.of("success", true, "requestId", request.getId()));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Transactional
    public ResponseEntity<Map<String, Object>> acceptMessageRequest(@PathVariable String requestId) {
        MessageRequest request = messageRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Message request not found"));
        
        request.setStatus("accepted");
        request.setUpdatedAt(Instant.now());
        messageRequestRepository.save(request);
        
        log.info("Message request {} accepted", requestId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Request accepted"));
    }

    @PostMapping("/requests/{requestId}/decline")
    @Transactional
    public ResponseEntity<Map<String, Object>> declineMessageRequest(@PathVariable String requestId) {
        MessageRequest request = messageRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Message request not found"));
        
        request.setStatus("declined");
        request.setUpdatedAt(Instant.now());
        messageRequestRepository.save(request);
        
        log.info("Message request {} declined", requestId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Request declined"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
