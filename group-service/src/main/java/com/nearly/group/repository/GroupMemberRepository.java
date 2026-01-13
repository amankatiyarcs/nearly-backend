package com.nearly.group.repository;
import com.nearly.group.model.GroupMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {
    List<GroupMember> findByGroupId(String groupId);
    List<GroupMember> findByUserId(String userId);
    boolean existsByGroupIdAndUserId(String groupId, String userId);
    void deleteByGroupIdAndUserId(String groupId, String userId);
}

