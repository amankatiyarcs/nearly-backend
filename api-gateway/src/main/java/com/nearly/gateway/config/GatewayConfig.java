package com.nearly.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter rateLimiter) {
        return builder.routes()
            // Auth Service Routes
            .route("auth-service", r -> r
                .path("/api/auth/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("auth-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://auth-service"))
            
            // User Service Routes
            .route("user-service", r -> r
                .path("/api/users/**", "/api/follows/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("user-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://user-service"))
            
            // Activity Service Routes
            .route("activity-service", r -> r
                .path("/api/activities/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("activity-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://activity-service"))
            
            // Posts/Polls/Questions/Discussions Routes (via Activity Service)
            .route("activity-posts", r -> r
                .path("/api/posts/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("activity-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://activity-service"))
            
            .route("activity-polls", r -> r
                .path("/api/polls/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("activity-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://activity-service"))
            
            .route("activity-questions", r -> r
                .path("/api/questions/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("activity-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://activity-service"))
            
            .route("activity-discussions", r -> r
                .path("/api/discussions/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("activity-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://activity-service"))
            
            // Event Service Routes
            .route("event-service", r -> r
                .path("/api/events/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("event-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://event-service"))
            
            // Group Service Routes
            .route("group-service", r -> r
                .path("/api/groups/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("group-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://group-service"))
            
            // News Service Routes
            .route("news-service", r -> r
                .path("/api/news/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("news-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://news-service"))
            
            // Messaging Service Routes
            .route("messaging-service", r -> r
                .path("/api/messages/**", "/api/conversations/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("messaging-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://messaging-service"))
            
            // Moments Service Routes
            .route("moments-service", r -> r
                .path("/api/moments/**", "/api/streaks/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("moments-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://moments-service"))
            
            // Marketplace Service Routes - Jobs
            .route("marketplace-jobs", r -> r
                .path("/api/jobs/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("marketplace-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://marketplace-service"))
            
            // Marketplace Service Routes - Deals
            .route("marketplace-deals", r -> r
                .path("/api/deals/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("marketplace-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://marketplace-service"))
            
            // Marketplace Service Routes - Places
            .route("marketplace-places", r -> r
                .path("/api/places/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("marketplace-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://marketplace-service"))
            
            // Marketplace Service Routes - Pages
            .route("marketplace-pages", r -> r
                .path("/api/pages/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("marketplace-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://marketplace-service"))
            
            // Notification Service Routes
            .route("notification-service", r -> r
                .path("/api/notifications/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("notification-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://notification-service"))
            
            // Search Service Routes
            .route("search-service", r -> r
                .path("/api/search/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("search-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://search-service"))
            
            // Media Service Routes
            .route("media-service", r -> r
                .path("/api/media/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c.setName("media-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://media-service"))
            
            // Random Chat Service Routes
            .route("random-chat-service", r -> r
                .path("/api/random-chat/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(sessionKeyResolver()))
                    .circuitBreaker(c -> c.setName("chat-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://random-chat-service"))
            
            // Video Chat Service Routes
            .route("video-chat-service", r -> r
                .path("/api/video-chat/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(sessionKeyResolver()))
                    .circuitBreaker(c -> c.setName("video-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://video-chat-service"))
            
            // Report Service Routes
            .route("report-service", r -> r
                .path("/api/reports/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(reportRateLimiter()).setKeyResolver(sessionKeyResolver()))
                    .circuitBreaker(c -> c.setName("report-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://report-service"))
            
            // PAI (Peer AI) Matching Service Routes
            .route("pai-service", r -> r
                .path("/api/pai/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(c -> c.setRateLimiter(rateLimiter).setKeyResolver(sessionKeyResolver()))
                    .circuitBreaker(c -> c.setName("pai-cb").setFallbackUri("forward:/fallback/service")))
                .uri("lb://pai-service"))
            
            // WebSocket Routes
            .route("ws-chat", r -> r.path("/ws/chat/**").uri("lb:ws://random-chat-service"))
            .route("ws-video", r -> r.path("/ws/video/**").uri("lb:ws://video-chat-service"))
            .route("ws-messaging", r -> r.path("/ws/messaging/**").uri("lb:ws://messaging-service"))
            .route("ws-notifications", r -> r.path("/ws/notifications/**").uri("lb:ws://notification-service"))
            
            .build();
    }


    @Primary
    @Bean("redisRateLimiter")
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean("reportRateLimiter")
    public RedisRateLimiter reportRateLimiter() {
        return new RedisRateLimiter(1, 5, 1);
    }

    @Primary
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            return Mono.just(ip);
        };
    }

    @Bean
    public KeyResolver sessionKeyResolver() {
        return exchange -> {
            String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-Id");
            if (sessionId == null) {
                sessionId = exchange.getRequest().getRemoteAddress() != null 
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            }
            return Mono.just(sessionId);
        };
    }
}
