package com.empress.usermanagementapi.filter;

import com.empress.usermanagementapi.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 * 
 * Tests cover:
 * - Successful requests within rate limits
 * - Rejected requests when rate limits are exceeded (HTTP 429)
 * - IP isolation (different IPs have separate buckets)
 * - Endpoint isolation (same IP, different endpoints have separate buckets)
 * - Error handling for filter exceptions
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    
    private RateLimitConfig rateLimitConfig;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws Exception {
        rateLimitConfig = new RateLimitConfig();
        rateLimitFilter = new RateLimitFilter(rateLimitConfig);
        
        responseWriter = new StringWriter();
    }
    
    /**
     * Test that requests within the rate limit are allowed and passed to the filter chain.
     */
    @Test
    void testLoginWithinRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        // Execute: Make 10 requests (the limit for /login is 10 per minute)
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 10 requests should pass through
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that requests exceeding the rate limit are rejected with HTTP 429.
     */
    @Test
    void testLoginExceedsRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Make 11 requests (exceeds the limit of 10 for /login)
        for (int i = 0; i < 11; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: First 10 should pass, 11th should be rejected
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, times(1)).setStatus(429);
        verify(response, times(1)).setHeader(eq("Retry-After"), anyString());
        
        // Verify response content
        String responseContent = responseWriter.toString();
        assertTrue(responseContent.contains("Rate limit exceeded"));
        assertTrue(responseContent.contains("retryAfter"));
    }
    
    /**
     * Test that different IPs have separate rate limit buckets.
     */
    @Test
    void testDifferentIpsHaveSeparateBuckets() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        
        // Execute: Make 10 requests from IP1
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Execute: Make 10 requests from IP2
        when(request.getRemoteAddr()).thenReturn("192.168.1.20");
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 20 requests should pass (10 from each IP)
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that the same IP has separate rate limit buckets for different endpoints.
     */
    @Test
    void testSameIpDifferentEndpointsHaveSeparateBuckets() throws Exception {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.30");
        
        // Execute: Make 10 requests to /login
        when(request.getRequestURI()).thenReturn("/login");
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Execute: Make 30 requests to /verify-email
        when(request.getRequestURI()).thenReturn("/verify-email");
        for (int i = 0; i < 30; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 40 requests should pass (10 for login + 30 for verify-email)
        verify(filterChain, times(40)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that /register endpoint has correct rate limit (20 per 10 minutes).
     */
    @Test
    void testRegisterRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.40");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Make 20 requests (the limit for /register is 20 per 10 minutes)
        for (int i = 0; i < 20; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 20 requests should pass through
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
        
        // Execute: Make 1 more request (exceeds the limit)
        rateLimitFilter.doFilter(request, response, filterChain);
        
        // Verify: 21st request should be rejected
        verify(filterChain, times(20)).doFilter(request, response); // Still 20
        verify(response, times(1)).setStatus(429);
    }
    
    /**
     * Test that /verify-email endpoint has correct rate limit (30 per minute).
     */
    @Test
    void testVerifyEmailRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/verify-email");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Make 30 requests (the limit for /verify-email is 30 per minute)
        for (int i = 0; i < 30; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 30 requests should pass through
        verify(filterChain, times(30)).doFilter(request, response);
        verify(response, never()).setStatus(429);
        
        // Execute: Make 1 more request (exceeds the limit)
        rateLimitFilter.doFilter(request, response, filterChain);
        
        // Verify: 31st request should be rejected
        verify(filterChain, times(30)).doFilter(request, response); // Still 30
        verify(response, times(1)).setStatus(429);
    }
    
    /**
     * Test that X-Forwarded-For header is used when available.
     */
    @Test
    void testXForwardedForHeaderExtraction() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Make 10 requests
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 10 requests should pass
        verify(filterChain, times(10)).doFilter(request, response);
        
        // Execute: 11th request should be rejected (using the IP from X-Forwarded-For)
        rateLimitFilter.doFilter(request, response, filterChain);
        verify(response, times(1)).setStatus(429);
    }
    
    /**
     * Test that requests to non-rate-limited endpoints pass through without rate limiting.
     */
    @Test
    void testNonRateLimitedEndpointsPassThrough() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/admin");
        
        // Execute: Make many requests to a non-rate-limited endpoint
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All requests should pass through
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that the filter handles IO exceptions when writing response but still enforces rate limit.
     */
    @Test
    void testFilterHandlesIOExceptionWhenWritingResponse() throws Exception {
        // Setup: Create a scenario where response.getWriter() throws an exception
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.70");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Make 10 requests to exhaust the limit
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Setup: Make response.getWriter() throw an IOException when rate limit is exceeded
        when(response.getWriter()).thenThrow(new java.io.IOException("Test IO exception"));
        
        // Execute: The 11th request should trigger rate limiting
        // Even though writing the response fails, the rate limit should still be enforced
        rateLimitFilter.doFilter(request, response, filterChain);
        
        // Verify: The 11th request should NOT pass through (rate limit enforced despite IO exception)
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, times(1)).setStatus(429);
    }
}
