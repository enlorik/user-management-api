package com.empress.usermanagementapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for endpoint-specific rate limiting.
 * 
 * This class manages rate limit configurations for critical public endpoints
 * and provides bucket management with automatic cleanup of expired entries.
 * 
 * Rate limits:
 * - /login: 5 requests per minute
 * - /register: 10 requests per 15 minutes
 * - /verify-email: 20 requests per minute
 */
@Component
@EnableScheduling
public class RateLimitConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);
    
    // Maps to store buckets per IP address per endpoint
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> verifyEmailBuckets = new ConcurrentHashMap<>();
    
    // Timestamps to track last access for cleanup
    private final Map<String, Long> loginLastAccess = new ConcurrentHashMap<>();
    private final Map<String, Long> registerLastAccess = new ConcurrentHashMap<>();
    private final Map<String, Long> verifyEmailLastAccess = new ConcurrentHashMap<>();
    
    // Cleanup threshold: remove buckets not accessed for 1 hour
    private static final long CLEANUP_THRESHOLD_MS = 60 * 60 * 1000;
    
    /**
     * Get or create a rate limit bucket for the /login endpoint.
     * Limit: 5 requests per minute
     */
    public Bucket resolveBucketForLogin(String ip) {
        loginLastAccess.put(ip, System.currentTimeMillis());
        return loginBuckets.computeIfAbsent(ip, key -> createBucketForLogin());
    }
    
    /**
     * Get or create a rate limit bucket for the /register endpoint.
     * Limit: 10 requests per 15 minutes
     */
    public Bucket resolveBucketForRegister(String ip) {
        registerLastAccess.put(ip, System.currentTimeMillis());
        return registerBuckets.computeIfAbsent(ip, key -> createBucketForRegister());
    }
    
    /**
     * Get or create a rate limit bucket for the /verify-email endpoint.
     * Limit: 20 requests per minute
     */
    public Bucket resolveBucketForVerifyEmail(String ip) {
        verifyEmailLastAccess.put(ip, System.currentTimeMillis());
        return verifyEmailBuckets.computeIfAbsent(ip, key -> createBucketForVerifyEmail());
    }
    
    /**
     * Create a bucket for /login endpoint.
     * Configuration: 5 tokens, refill 5 tokens per minute
     */
    private Bucket createBucketForLogin() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Create a bucket for /register endpoint.
     * Configuration: 10 tokens, refill 10 tokens per 15 minutes
     */
    private Bucket createBucketForRegister() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(15)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Create a bucket for /verify-email endpoint.
     * Configuration: 20 tokens, refill 20 tokens per minute
     */
    private Bucket createBucketForVerifyEmail() {
        Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    /**
     * Cleanup expired buckets every 30 minutes.
     * Removes buckets that haven't been accessed for more than 1 hour.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // Run every 30 minutes
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        int cleaned = 0;
        
        // Cleanup login buckets
        cleaned += cleanupMap(loginBuckets, loginLastAccess, now, "login");
        
        // Cleanup register buckets
        cleaned += cleanupMap(registerBuckets, registerLastAccess, now, "register");
        
        // Cleanup verify-email buckets
        cleaned += cleanupMap(verifyEmailBuckets, verifyEmailLastAccess, now, "verify-email");
        
        if (cleaned > 0) {
            logger.info("Cleaned up {} expired rate limit buckets", cleaned);
        }
    }
    
    /**
     * Helper method to cleanup expired entries from a bucket map.
     */
    private int cleanupMap(Map<String, Bucket> bucketMap, Map<String, Long> lastAccessMap, long now, String endpoint) {
        int cleaned = 0;
        
        // Collect IPs to remove first to avoid ConcurrentModificationException
        List<String> ipsToRemove = new ArrayList<>();
        for (Map.Entry<String, Long> entry : lastAccessMap.entrySet()) {
            if (now - entry.getValue() > CLEANUP_THRESHOLD_MS) {
                ipsToRemove.add(entry.getKey());
            }
        }
        
        // Remove collected IPs
        for (String ip : ipsToRemove) {
            bucketMap.remove(ip);
            lastAccessMap.remove(ip);
            cleaned++;
            logger.debug("Removed expired bucket for IP {} on endpoint {}", ip, endpoint);
        }
        
        return cleaned;
    }
    
    /**
     * Get current bucket counts for monitoring (useful for testing and debugging).
     */
    public int getLoginBucketCount() {
        return loginBuckets.size();
    }
    
    public int getRegisterBucketCount() {
        return registerBuckets.size();
    }
    
    public int getVerifyEmailBucketCount() {
        return verifyEmailBuckets.size();
    }
}
