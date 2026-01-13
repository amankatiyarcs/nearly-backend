package com.nearly.gateway.filter;

import com.nearly.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global authentication filter for API Gateway.
 * Validates JWT tokens for protected routes and passes user info to downstream services.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * List of public paths that don't require authentication.
     * These patterns are matched against the request path.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            // Auth endpoints - login, signup, password reset
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-otp",
            "/api/auth/check-username",
            "/api/auth/refresh",
            "/api/auth/health",
            // Anonymous session endpoints for random chat
            "/api/auth/anonymous/session",
            // Health and actuator endpoints
            "/actuator",
            // Swagger/OpenAPI endpoints
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            // Fallback endpoints
            "/fallback"
    );

    /**
     * Paths that allow both authenticated and anonymous access.
     * Anonymous sessions are handled separately.
     */
    private static final List<String> ANONYMOUS_ALLOWED_PATHS = List.of(
            "/api/random-chat",
            "/api/video-chat",
            "/ws/chat",
            "/ws/video"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.debug("AuthenticationFilter processing: {} {}", method, path);

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Public path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Check for anonymous session on allowed paths
        if (isAnonymousAllowedPath(path)) {
            String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-Id");
            if (sessionId != null && sessionId.startsWith("anon-")) {
                log.debug("Anonymous session allowed for path: {}", path);
                return chain.filter(exchange);
            }
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Extract token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // Validate token
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid or expired token for path: {}", path);
            return onUnauthorized(exchange, "Invalid or expired token");
        }

        // Check if it's an access token (not refresh token)
        if (!jwtUtil.isAccessToken(token)) {
            log.warn("Refresh token used for API access on path: {}", path);
            return onUnauthorized(exchange, "Access token required");
        }

        // Extract user information from token
        String userId = jwtUtil.extractUserId(token);
        String username = jwtUtil.extractUsername(token);
        String email = jwtUtil.extractEmail(token);

        log.debug("Authenticated user: {} (ID: {}) accessing: {}", username, userId, path);

        // Add user info to request headers for downstream services
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username != null ? username : "")
                .header("X-User-Email", email != null ? email : "")
                .header("X-Auth-Token", token)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Check if the path is a public path that doesn't require authentication.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(publicPath -> 
                path.equals(publicPath) || path.startsWith(publicPath + "/") || path.startsWith(publicPath)
        );
    }

    /**
     * Check if the path allows anonymous access (with session ID).
     */
    private boolean isAnonymousAllowedPath(String path) {
        return ANONYMOUS_ALLOWED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Handle unauthorized requests.
     */
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"error\":\"Unauthorized\",\"message\":\"%s\",\"statusCode\":401}",
                message
        );

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // Run after RequestLoggingFilter (HIGHEST_PRECEDENCE) and before other filters
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
