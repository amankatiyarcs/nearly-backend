package com.nearly.group.service;
import com.nearly.group.model.*;
import com.nearly.group.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepo;
    private final GroupMemberRepository memberRepo;
    private final KafkaTemplate<String, Object> kafka;

    public List<Group> getGroups(Integer limit) {
        if (limit != null) return groupRepo.findAll(PageRequest.of(0, limit)).getContent();
        return groupRepo.findAll();
    }
    public Optional<Group> getGroup(String id) { return groupRepo.findById(id); }
    public Group createGroup(Group group) {
        group.setCreatedAt(Instant.now());
        group.setMembersCount(1);
        Group saved = groupRepo.save(group);
        memberRepo.save(GroupMember.builder().groupId(saved.getId()).userId(group.getUserId()).role("admin").joinedAt(Instant.now()).build());
        kafka.send("group-events", Map.of("type", "GROUP_CREATED", "groupId", saved.getId()));
        return saved;
    }
    public Group updateGroup(String id, Group updates) {
        Group group = groupRepo.findById(id).orElseThrow();
        if (updates.getName() != null) group.setName(updates.getName());
        if (updates.getDescription() != null) group.setDescription(updates.getDescription());
        if (updates.getImageUrl() != null) group.setImageUrl(updates.getImageUrl());
        if (updates.getRules() != null) group.setRules(updates.getRules());
        return groupRepo.save(group);
    }
    public void deleteGroup(String id) { groupRepo.deleteById(id); memberRepo.findByGroupId(id).forEach(m -> memberRepo.delete(m)); }
    
    public List<GroupMember> getMembers(String groupId) { return memberRepo.findByGroupId(groupId); }
    public GroupMember joinGroup(String groupId, String userId) {
        if (memberRepo.existsByGroupIdAndUserId(groupId, userId)) throw new IllegalStateException("Already a member");
        GroupMember member = GroupMember.builder().groupId(groupId).userId(userId).role("member").joinedAt(Instant.now()).build();
        GroupMember saved = memberRepo.save(member);
        groupRepo.findById(groupId).ifPresent(g -> { 
            g.setMembersCount(g.getMembersCount() + 1); 
            groupRepo.save(g);
            
            // Send notification to group owner/admin
            if (g.getUserId() != null && !g.getUserId().equals(userId)) {
                kafka.send("group-events", Map.of(
                    "type", "GROUP_MEMBER_JOINED",
                    "groupId", groupId,
                    "groupName", g.getName() != null ? g.getName() : "",
                    "groupOwnerId", g.getUserId(),
                    "newMemberId", userId
                ));
            }
        });
        return saved;
    }
    public void leaveGroup(String groupId, String userId) {
        memberRepo.deleteByGroupIdAndUserId(groupId, userId);
        groupRepo.findById(groupId).ifPresent(g -> { g.setMembersCount(Math.max(0, g.getMembersCount() - 1)); groupRepo.save(g); });
    }
    public List<Group> getUserGroups(String userId) {
        return memberRepo.findByUserId(userId).stream()
            .map(m -> groupRepo.findById(m.getGroupId()).orElse(null))
            .filter(Objects::nonNull).toList();
    }
}

