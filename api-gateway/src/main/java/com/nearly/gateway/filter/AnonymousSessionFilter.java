package com.nearly.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Filter to generate and manage anonymous sessions for random chat users.
 * No user data is stored - only temporary session IDs for matching purposes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnonymousSessionFilter implements GlobalFilter, Ordered {


    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String SESSION_PREFIX = "anon:session:";
    private static final Duration SESSION_DURATION = Duration.ofHours(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Only apply to chat and video endpoints
        if (!path.startsWith("/api/chat") && !path.startsWith("/api/video") && 
            !path.startsWith("/ws/chat") && !path.startsWith("/ws/video") &&
            !path.startsWith("/api/random-chat") && !path.startsWith("/api/video-chat")) {
            return chain.filter(exchange);
        }

        String existingSessionId = exchange.getRequest().getHeaders().getFirst("X-Session-Id");
        
        if (existingSessionId == null || existingSessionId.isEmpty()) {
            // Generate new anonymous session
            final String newSessionId = generateAnonymousSessionId();
            
            return redisTemplate.opsForValue()
                .set(SESSION_PREFIX + newSessionId, "active", SESSION_DURATION)
                .flatMap(success -> {
                    log.debug("Created anonymous session: {}", newSessionId);
                    
                    ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-Session-Id", newSessionId)
                        .build();
                    
                    ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(request)
                        .build();
                    
                    modifiedExchange.getResponse().getHeaders().add("X-Session-Id", newSessionId);
                    
                    return chain.filter(modifiedExchange);
                });
        } else {
            // Validate existing session
            final String sessionId = existingSessionId;
            return redisTemplate.hasKey(SESSION_PREFIX + sessionId)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        // Extend session
                        return redisTemplate.expire(SESSION_PREFIX + sessionId, SESSION_DURATION)
                            .flatMap(extended -> chain.filter(exchange));
                    } else {
                        // Session expired, create new one
                        final String newSessionId = generateAnonymousSessionId();
                        return redisTemplate.opsForValue()
                            .set(SESSION_PREFIX + newSessionId, "active", SESSION_DURATION)
                            .flatMap(success -> {
                                ServerHttpRequest request = exchange.getRequest().mutate()
                                    .header("X-Session-Id", newSessionId)
                                    .build();
                                
                                ServerWebExchange modifiedExchange = exchange.mutate()
                                    .request(request)
                                    .build();
                                
                                modifiedExchange.getResponse().getHeaders().add("X-Session-Id", newSessionId);
                                
                                return chain.filter(modifiedExchange);
                            });
                    }
                });
        }
    }

    private String generateAnonymousSessionId() {
        // Generate a random session ID that cannot be traced back to the user
        return "anon-" + UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
