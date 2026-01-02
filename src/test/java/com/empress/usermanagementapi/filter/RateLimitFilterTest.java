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
     * Test that POST requests within the rate limit are allowed and passed to the filter chain.
     */
    @Test
    void testLoginWithinRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
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
     * Test that POST requests exceeding the rate limit are rejected with HTTP 429.
     */
    @Test
    void testLoginExceedsRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
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
        when(request.getMethod()).thenReturn("POST");
        
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
        
        // Execute: Make 10 POST requests to /login
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("POST");
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Execute: Make 30 GET requests to /verify-email
        when(request.getRequestURI()).thenReturn("/verify-email");
        when(request.getMethod()).thenReturn("GET");
        for (int i = 0; i < 30; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 40 requests should pass (10 for login + 30 for verify-email)
        verify(filterChain, times(40)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that /register endpoint has correct rate limit (20 per 10 minutes) for POST requests.
     */
    @Test
    void testRegisterRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getMethod()).thenReturn("POST");
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
     * Test that /verify-email endpoint has correct rate limit (30 per minute) for all methods.
     */
    @Test
    void testVerifyEmailRateLimit() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/verify-email");
        when(request.getMethod()).thenReturn("GET");
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
        when(request.getMethod()).thenReturn("POST");
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
        when(request.getMethod()).thenReturn("POST");
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
    
    /**
     * Test that /auth/login endpoint is also rate limited (alternative login endpoint).
     */
    @Test
    void testAuthLoginEndpointRateLimited() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.80");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Make 10 POST requests
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 10 requests should pass
        verify(filterChain, times(10)).doFilter(request, response);
        
        // Execute: 11th request should be rejected
        rateLimitFilter.doFilter(request, response, filterChain);
        verify(response, times(1)).setStatus(429);
    }
    
    /**
     * Test that GET requests to /login are NOT rate-limited.
     * This ensures viewing the login page doesn't consume rate limit tokens.
     */
    @Test
    void testLoginPageGetRequestsNotRateLimited() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("GET");
        
        // Execute: Make 100 GET requests to /login (well beyond the POST limit of 10)
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 100 requests should pass through without rate limiting
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that GET requests to /register are NOT rate-limited.
     * This ensures viewing the registration page doesn't consume rate limit tokens.
     */
    @Test
    void testRegisterPageGetRequestsNotRateLimited() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getMethod()).thenReturn("GET");
        
        // Execute: Make 100 GET requests to /register (well beyond the POST limit of 20)
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 100 requests should pass through without rate limiting
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that /logout endpoint is NEVER rate-limited.
     * This ensures users can always logout regardless of rate limits.
     */
    @Test
    void testLogoutEndpointNotRateLimited() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/logout");
        when(request.getMethod()).thenReturn("POST");
        
        // Execute: Make 100 POST requests to /logout
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 100 requests should pass through without rate limiting
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test the complete login-logout cycle to ensure it doesn't trigger rate limiting.
     * This simulates the bug scenario: user logs in once and logs out.
     */
    @Test
    void testLoginLogoutCycleDoesNotTriggerRateLimit() throws Exception {
        // Setup
        when(request.getRemoteAddr()).thenReturn("192.168.1.93");
        
        // Step 1: User performs 5 login cycles (login POST + logout POST + view login page GET)
        for (int i = 0; i < 5; i++) {
            // POST to /login (should be rate-limited, consumes 1 token)
            when(request.getRequestURI()).thenReturn("/login");
            when(request.getMethod()).thenReturn("POST");
            rateLimitFilter.doFilter(request, response, filterChain);
            
            // POST to /logout (should NOT be rate-limited)
            when(request.getRequestURI()).thenReturn("/logout");
            when(request.getMethod()).thenReturn("POST");
            rateLimitFilter.doFilter(request, response, filterChain);
            
            // GET to /login after logout redirect (should NOT be rate-limited)
            when(request.getRequestURI()).thenReturn("/login");
            when(request.getMethod()).thenReturn("GET");
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 15 requests (5 login POST + 5 logout POST + 5 login GET) should pass
        // Only the 5 POST requests to /login should consume rate limit tokens
        verify(filterChain, times(15)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test that /login?logout redirect (after logout) doesn't consume rate limit tokens.
     */
    @Test
    void testLoginWithLogoutParameterNotRateLimited() throws Exception {
        // Setup - simulate the redirect after logout
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getMethod()).thenReturn("GET");
        
        // Execute: Make 100 GET requests to /login?logout
        for (int i = 0; i < 100; i++) {
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 100 requests should pass through without rate limiting
        verify(filterChain, times(100)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
    
    /**
     * Test mixed GET and POST requests to /login to ensure only POST is rate-limited.
     */
    @Test
    void testMixedGetAndPostRequestsToLogin() throws Exception {
        // Setup
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.95");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        
        // Execute: Alternate between GET and POST requests
        for (int i = 0; i < 10; i++) {
            // GET request (should not be rate-limited)
            when(request.getMethod()).thenReturn("GET");
            rateLimitFilter.doFilter(request, response, filterChain);
            
            // POST request (should be rate-limited)
            when(request.getMethod()).thenReturn("POST");
            rateLimitFilter.doFilter(request, response, filterChain);
        }
        
        // Verify: All 20 requests should pass (10 GET + 10 POST within limit)
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(429);
        
        // Execute: One more POST should be rejected (11th POST)
        when(request.getMethod()).thenReturn("POST");
        rateLimitFilter.doFilter(request, response, filterChain);
        
        // Verify: 21st request (11th POST) should be rejected
        verify(filterChain, times(20)).doFilter(request, response); // Still 20
        verify(response, times(1)).setStatus(429);
    }
}
