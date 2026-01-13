package com.nearly.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter to remove CORS headers from downstream services.
 * This prevents duplicate CORS headers when the API Gateway already handles CORS.
 */
@Component
public class RemoveDownstreamCorsFilter implements GlobalFilter, Ordered {

    private static final String[] CORS_HEADERS_TO_REMOVE = {
        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
        HttpHeaders.ACCESS_CONTROL_MAX_AGE
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            for (String header : CORS_HEADERS_TO_REMOVE) {
                // Keep only the first occurrence (from the gateway's CorsWebFilter)
                if (headers.containsKey(header)) {
                    var values = headers.get(header);
                    if (values != null && values.size() > 1) {
                        // Keep only the first value (from gateway)
                        String firstValue = values.get(0);
                        headers.remove(header);
                        headers.add(header, firstValue);
                    }
                }
            }
        }));
    }

    @Override
    public int getOrder() {
        // Run after the response is received from downstream but before it's sent to client
        // Lower values have higher priority, we want this to run late
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}

