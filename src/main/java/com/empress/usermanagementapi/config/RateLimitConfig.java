package com.empress.usermanagementapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for rate limiting different endpoints.
 * This class provides different rate limit configurations for various endpoints
 * to protect against abuse while maintaining usability.
 */
@Configuration
public class RateLimitConfig {

    private final Map<String, Map<String, Bucket>> endpointBuckets = new ConcurrentHashMap<>();

    /**
     * Get or create a bucket for a specific endpoint and IP address.
     * 
     * @param endpoint The endpoint path
     * @param ip The client IP address
     * @return A Bucket configured with the appropriate rate limit for this endpoint
     */
    public Bucket resolveBucket(String endpoint, String ip) {
        return endpointBuckets
            .computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(ip, k -> createBucket(endpoint));
    }

    /**
     * Create a new bucket with rate limits based on the endpoint.
     * 
     * Rate limits:
     * - /login, /auth/login: 5 requests per minute (strict - prevent brute force)
     * - /register: 10 requests per 15 minutes (moderate - prevent spam registrations)
     * - /verify-email: 20 requests per minute (lenient but still protected)
     * - default: 100 requests per minute (general protection)
     */
    private Bucket createBucket(String endpoint) {
        Bandwidth limit;
        
        if (endpoint.equals("/login") || endpoint.equals("/auth/login")) {
            // Strict: 5 requests per minute to prevent brute force attacks
            limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        } else if (endpoint.equals("/register")) {
            // Moderate: 10 requests per 15 minutes to prevent spam registrations
            limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(15)));
        } else if (endpoint.equals("/verify-email")) {
            // Lenient but protected: 20 requests per minute
            limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        } else {
            // Default: 100 requests per minute for general protection
            limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        }
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}
