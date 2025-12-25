package com.empress.usermanagementapi.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimitConfig.
 * 
 * Tests cover:
 * - Bucket creation and retrieval
 * - Rate limit enforcement for each endpoint
 * - Bucket isolation per IP address
 * - Bucket cleanup functionality
 */
class RateLimitConfigTest {
    
    private RateLimitConfig rateLimitConfig;
    
    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }
    
    /**
     * Test that login buckets enforce the correct rate limit (5 per minute).
     */
    @Test
    void testLoginBucketRateLimit() {
        String ip = "192.168.1.1";
        Bucket bucket = rateLimitConfig.resolveBucketForLogin(ip);
        
        // Should allow 5 requests
        for (int i = 0; i < 5; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }
        
        // 6th request should be rejected
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertFalse(probe.isConsumed(), "6th request should be rejected");
    }
    
    /**
     * Test that register buckets enforce the correct rate limit (10 per 15 minutes).
     */
    @Test
    void testRegisterBucketRateLimit() {
        String ip = "192.168.1.2";
        Bucket bucket = rateLimitConfig.resolveBucketForRegister(ip);
        
        // Should allow 10 requests
        for (int i = 0; i < 10; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }
        
        // 11th request should be rejected
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertFalse(probe.isConsumed(), "11th request should be rejected");
    }
    
    /**
     * Test that verify-email buckets enforce the correct rate limit (20 per minute).
     */
    @Test
    void testVerifyEmailBucketRateLimit() {
        String ip = "192.168.1.3";
        Bucket bucket = rateLimitConfig.resolveBucketForVerifyEmail(ip);
        
        // Should allow 20 requests
        for (int i = 0; i < 20; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }
        
        // 21st request should be rejected
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertFalse(probe.isConsumed(), "21st request should be rejected");
    }
    
    /**
     * Test that different IPs get different buckets for the same endpoint.
     */
    @Test
    void testDifferentIpsGetDifferentBuckets() {
        String ip1 = "192.168.1.10";
        String ip2 = "192.168.1.20";
        
        Bucket bucket1 = rateLimitConfig.resolveBucketForLogin(ip1);
        Bucket bucket2 = rateLimitConfig.resolveBucketForLogin(ip2);
        
        // Verify they are different bucket instances
        assertNotSame(bucket1, bucket2, "Different IPs should have different buckets");
        
        // Consume all tokens from bucket1
        for (int i = 0; i < 5; i++) {
            bucket1.tryConsumeAndReturnRemaining(1);
        }
        
        // bucket2 should still have tokens available
        ConsumptionProbe probe = bucket2.tryConsumeAndReturnRemaining(1);
        assertTrue(probe.isConsumed(), "bucket2 should still have tokens");
    }
    
    /**
     * Test that the same IP gets the same bucket on repeated calls.
     */
    @Test
    void testSameIpGetsSameBucket() {
        String ip = "192.168.1.30";
        
        Bucket bucket1 = rateLimitConfig.resolveBucketForLogin(ip);
        Bucket bucket2 = rateLimitConfig.resolveBucketForLogin(ip);
        
        // Verify they are the same bucket instance
        assertSame(bucket1, bucket2, "Same IP should get the same bucket");
    }
    
    /**
     * Test that the same IP gets different buckets for different endpoints.
     */
    @Test
    void testSameIpDifferentEndpointsGetDifferentBuckets() {
        String ip = "192.168.1.40";
        
        Bucket loginBucket = rateLimitConfig.resolveBucketForLogin(ip);
        Bucket registerBucket = rateLimitConfig.resolveBucketForRegister(ip);
        Bucket verifyEmailBucket = rateLimitConfig.resolveBucketForVerifyEmail(ip);
        
        // Verify they are different bucket instances
        assertNotSame(loginBucket, registerBucket, "Login and register buckets should be different");
        assertNotSame(loginBucket, verifyEmailBucket, "Login and verify-email buckets should be different");
        assertNotSame(registerBucket, verifyEmailBucket, "Register and verify-email buckets should be different");
    }
    
    /**
     * Test that bucket count increases as new IPs are seen.
     */
    @Test
    void testBucketCountIncreases() {
        assertEquals(0, rateLimitConfig.getLoginBucketCount(), "Initial login bucket count should be 0");
        
        rateLimitConfig.resolveBucketForLogin("192.168.1.50");
        assertEquals(1, rateLimitConfig.getLoginBucketCount(), "Login bucket count should be 1");
        
        rateLimitConfig.resolveBucketForLogin("192.168.1.51");
        assertEquals(2, rateLimitConfig.getLoginBucketCount(), "Login bucket count should be 2");
        
        // Same IP should not increase count
        rateLimitConfig.resolveBucketForLogin("192.168.1.50");
        assertEquals(2, rateLimitConfig.getLoginBucketCount(), "Login bucket count should still be 2");
    }
    
    /**
     * Test that bucket cleanup works correctly.
     * Note: This test uses reflection to manipulate last access times for testing.
     */
    @Test
    void testBucketCleanup() throws Exception {
        // Create some buckets
        rateLimitConfig.resolveBucketForLogin("192.168.1.60");
        rateLimitConfig.resolveBucketForLogin("192.168.1.61");
        rateLimitConfig.resolveBucketForRegister("192.168.1.62");
        
        assertEquals(2, rateLimitConfig.getLoginBucketCount());
        assertEquals(1, rateLimitConfig.getRegisterBucketCount());
        
        // Use reflection to manipulate last access times to simulate old buckets
        java.lang.reflect.Field loginLastAccessField = RateLimitConfig.class.getDeclaredField("loginLastAccess");
        loginLastAccessField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Long> loginLastAccess = (java.util.Map<String, Long>) loginLastAccessField.get(rateLimitConfig);
        
        // Set one IP's last access to more than 1 hour ago
        long oneHourAgo = System.currentTimeMillis() - (61 * 60 * 1000);
        loginLastAccess.put("192.168.1.60", oneHourAgo);
        
        // Run cleanup
        rateLimitConfig.cleanupExpiredBuckets();
        
        // Verify that the old bucket was removed
        assertEquals(1, rateLimitConfig.getLoginBucketCount(), "Old login bucket should be cleaned up");
        assertEquals(1, rateLimitConfig.getRegisterBucketCount(), "Register bucket should not be affected");
    }
    
    /**
     * Test that cleanup doesn't remove recently accessed buckets.
     */
    @Test
    void testCleanupDoesNotRemoveRecentBuckets() {
        // Create some buckets
        rateLimitConfig.resolveBucketForLogin("192.168.1.70");
        rateLimitConfig.resolveBucketForLogin("192.168.1.71");
        
        assertEquals(2, rateLimitConfig.getLoginBucketCount());
        
        // Run cleanup (all buckets are recent)
        rateLimitConfig.cleanupExpiredBuckets();
        
        // Verify that no buckets were removed
        assertEquals(2, rateLimitConfig.getLoginBucketCount(), "Recent buckets should not be cleaned up");
    }
}
