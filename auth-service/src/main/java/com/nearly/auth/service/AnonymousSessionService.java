package com.nearly.auth.service;

import com.nearly.auth.dto.AnonymousSessionRequest;
import com.nearly.auth.dto.AnonymousSessionResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing anonymous sessions.
 * No user identifiable data is stored.
 * Sessions are temporary and auto-expire.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnonymousSessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret:your-256-bit-secret-key-for-jwt-token-signing}")
    private String jwtSecret;

    @Value("${security.anonymous.session-duration:3600000}")
    private long sessionDuration;

    private static final String SESSION_PREFIX = "anon:session:";
    private static final String RATE_LIMIT_PREFIX = "anon:ratelimit:";

    /**
     * Create a new anonymous session for random chat.
     * No user data is stored - only a temporary session ID.
     */
    public AnonymousSessionResponse createAnonymousSession(AnonymousSessionRequest request) {
        // Rate limit check (optional, based on device fingerprint hash)
        if (request.getDeviceFingerprint() != null) {
            String rateLimitKey = RATE_LIMIT_PREFIX + hashFingerprint(request.getDeviceFingerprint());
            Long count = redisTemplate.opsForValue().increment(rateLimitKey);
            if (count != null && count == 1) {
                redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
            }
            if (count != null && count > 10) {
                log.warn("Rate limit exceeded for anonymous session creation");
                return AnonymousSessionResponse.builder()
                    .success(false)
                    .message("Too many requests. Please try again later.")
                    .build();
            }
        }

        // Generate anonymous session
        String sessionId = "anon-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(sessionDuration);

        // Generate JWT token for the session
        String token = generateToken(sessionId, expiresAt);

        // Store session in Redis (no user data, just session state)
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "status", "active");
        redisTemplate.opsForHash().put(sessionKey, "mode", request.getChatMode());
        redisTemplate.opsForHash().put(sessionKey, "createdAt", Instant.now().toString());
        redisTemplate.expire(sessionKey, sessionDuration, TimeUnit.MILLISECONDS);

        log.info("Created anonymous session: {} for mode: {}", sessionId, request.getChatMode());

        return AnonymousSessionResponse.builder()
            .sessionId(sessionId)
            .token(token)
            .expiresAt(expiresAt)
            .chatMode(request.getChatMode())
            .success(true)
            .message("Anonymous session created successfully")
            .build();
    }

    /**
     * Validate an existing anonymous session.
     */
    public boolean validateSession(String sessionId) {
        if (sessionId == null || !sessionId.startsWith("anon-")) {
            return false;
        }
        String sessionKey = SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    /**
     * Invalidate/end an anonymous session.
     */
    public void invalidateSession(String sessionId) {
        if (sessionId != null) {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            log.info("Invalidated anonymous session: {}", sessionId);
        }
    }

    /**
     * Extend session duration.
     */
    public boolean extendSession(String sessionId) {
        if (!validateSession(sessionId)) {
            return false;
        }
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.expire(sessionKey, sessionDuration, TimeUnit.MILLISECONDS);
        return true;
    }

    private String generateToken(String sessionId, Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
            .subject(sessionId)
            .claim("type", "anonymous")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
    }

    /**
     * Hash device fingerprint for rate limiting.
     * Original fingerprint is never stored.
     */
    private String hashFingerprint(String fingerprint) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // Truncated hash
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }
}

