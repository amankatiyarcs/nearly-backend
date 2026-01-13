package com.nearly.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for authentication settings.
 * Allows customization of public paths via application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "auth")
@Data
public class AuthConfig {

    /**
     * Additional public paths that don't require authentication.
     * These are added to the default public paths.
     */
    private List<String> publicPaths = new ArrayList<>();

    /**
     * Paths that allow anonymous session access.
     */
    private List<String> anonymousPaths = new ArrayList<>();

    /**
     * Whether to enable authentication filter.
     */
    private boolean enabled = true;
}
