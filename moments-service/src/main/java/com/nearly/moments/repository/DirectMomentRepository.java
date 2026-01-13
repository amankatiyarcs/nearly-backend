package com.nearly.moments.repository;
import com.nearly.moments.model.DirectMoment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DirectMomentRepository extends MongoRepository<DirectMoment, String> {
    List<DirectMoment> findByRecipientIdAndIsViewedFalse(String recipientId);
    List<DirectMoment> findBySenderId(String senderId);
}

