package com.nearly.group.controller;
import com.nearly.group.model.*;
import com.nearly.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/groups") @RequiredArgsConstructor
public class GroupController {
    private final GroupService service;

    @GetMapping
    public List<Group> getGroups(@RequestParam(required = false) Integer limit) { return service.getGroups(limit); }
    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroup(@PathVariable String id) { return service.getGroup(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
    @PostMapping
    public Group createGroup(@RequestBody Group group) { return service.createGroup(group); }
    @PatchMapping("/{id}")
    public Group updateGroup(@PathVariable String id, @RequestBody Group updates) { return service.updateGroup(id, updates); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) { service.deleteGroup(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/{id}/members")
    public List<GroupMember> getMembers(@PathVariable String id) { return service.getMembers(id); }
    @PostMapping("/{id}/join")
    public GroupMember joinGroup(@PathVariable String id, @RequestBody Map<String, String> body) { return service.joinGroup(id, body.get("userId")); }
    @DeleteMapping("/{id}/leave/{userId}")
    public ResponseEntity<Void> leaveGroup(@PathVariable String id, @PathVariable String userId) { service.leaveGroup(id, userId); return ResponseEntity.noContent().build(); }
    @GetMapping("/user/{userId}")
    public List<Group> getUserGroups(@PathVariable String userId) { return service.getUserGroups(userId); }
    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}

