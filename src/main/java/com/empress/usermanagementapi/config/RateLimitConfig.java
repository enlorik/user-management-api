package com.empress.usermanagementapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for rate limiting different endpoints.
 * This class provides different rate limit configurations for various endpoints
 * to protect against abuse while maintaining usability.
 * 
 * Includes automatic cleanup of unused buckets to prevent memory leaks.
 */
@Configuration
@EnableScheduling
public class RateLimitConfig {

    private final Map<String, Map<String, BucketWrapper>> endpointBuckets = new ConcurrentHashMap<>();
    
    // Cleanup buckets not accessed for 1 hour
    private static final long BUCKET_EXPIRY_MILLIS = 60 * 60 * 1000;

    /**
     * Wrapper class to track last access time for cleanup purposes.
     */
    private static class BucketWrapper {
        final Bucket bucket;
        volatile long lastAccessTime;

        BucketWrapper(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * Get or create a bucket for a specific endpoint and IP address.
     * 
     * @param endpoint The endpoint path
     * @param ip The client IP address
     * @return A Bucket configured with the appropriate rate limit for this endpoint
     */
    public Bucket resolveBucket(String endpoint, String ip) {
        BucketWrapper wrapper = endpointBuckets
            .computeIfAbsent(endpoint, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(ip, k -> new BucketWrapper(createBucket(endpoint)));
        
        wrapper.updateAccessTime();
        return wrapper.bucket;
    }

    /**
     * Periodic cleanup of expired buckets to prevent memory leaks.
     * Runs every hour and removes buckets not accessed in the last hour.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // Run every hour
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        for (Map.Entry<String, Map<String, BucketWrapper>> endpointEntry : endpointBuckets.entrySet()) {
            Map<String, BucketWrapper> ipBuckets = endpointEntry.getValue();
            
            ipBuckets.entrySet().removeIf(entry -> {
                if (now - entry.getValue().lastAccessTime > BUCKET_EXPIRY_MILLIS) {
                    return true;
                }
                return false;
            });
            
            removedCount += ipBuckets.size();
        }
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
