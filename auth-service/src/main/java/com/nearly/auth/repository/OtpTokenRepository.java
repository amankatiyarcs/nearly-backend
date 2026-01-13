package com.nearly.auth.repository;

import com.nearly.auth.model.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByEmailAndTypeAndVerifiedFalse(String email, OtpToken.OtpType type);
    void deleteByEmailAndType(String email, OtpToken.OtpType type);
}

