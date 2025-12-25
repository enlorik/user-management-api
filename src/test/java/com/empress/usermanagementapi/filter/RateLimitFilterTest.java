package com.empress.usermanagementapi.filter;

import com.empress.usermanagementapi.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 * These tests verify that rate limiting is properly enforced on critical endpoints.
 */
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private RateLimitConfig rateLimitConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitConfig = new RateLimitConfig();
        rateLimitFilter = new RateLimitFilter(rateLimitConfig);
    }

    @Test
    void testLoginEndpoint_RequestsWithinLimit_ShouldSucceed() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // Act - Make 5 requests (within the limit of 5 per minute)
        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testLoginEndpoint_RequestsExceedingLimit_ShouldReturn429() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.101");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Make 6 requests (exceeding the limit of 5 per minute)
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response, atLeastOnce()).addHeader(eq("Retry-After"), anyString());
    }

    @Test
    void testRegisterEndpoint_RequestsWithinLimit_ShouldSucceed() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.102");

        // Act - Make 10 requests (within the limit of 10 per 15 minutes)
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testRegisterEndpoint_RequestsExceedingLimit_ShouldReturn429() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.103");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Make 11 requests (exceeding the limit of 10 per 15 minutes)
        for (int i = 0; i < 11; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testVerifyEmailEndpoint_RequestsWithinLimit_ShouldSucceed() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/verify-email");
        when(request.getRemoteAddr()).thenReturn("192.168.1.104");

        // Act - Make 20 requests (within the limit of 20 per minute)
        for (int i = 0; i < 20; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testVerifyEmailEndpoint_RequestsExceedingLimit_ShouldReturn429() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/verify-email");
        when(request.getRemoteAddr()).thenReturn("192.168.1.105");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Make 21 requests (exceeding the limit of 20 per minute)
        for (int i = 0; i < 21; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(20)).doFilter(request, response);
        verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testDifferentIPs_ShouldHaveSeparateLimits() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - IP1 makes 5 requests
        when(request.getRemoteAddr()).thenReturn("192.168.1.106");
        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Act - IP2 makes 5 requests (should also succeed)
        when(request.getRemoteAddr()).thenReturn("192.168.1.107");
        for (int i = 0; i < 5; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testNonRateLimitedEndpoint_ShouldNotApplyRateLimit() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/admin/users");
        when(request.getRemoteAddr()).thenReturn("192.168.1.108");

        // Act - Make many requests to a non-rate-limited endpoint
        for (int i = 0; i < 200; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert - All requests should pass through
        verify(filterChain, times(200)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testXForwardedForHeader_ShouldBeUsedForIP() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.109");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Make 6 requests (exceeding the limit)
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert - Rate limit should be based on X-Forwarded-For IP (10.0.0.1)
        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void testRateLimitHeaders_ShouldBeAddedToResponse() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.110");

        // Act - Make a request within the limit
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert - X-Rate-Limit-Remaining header should be added
        verify(response, atLeastOnce()).addHeader(eq("X-Rate-Limit-Remaining"), anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testRetryAfterHeader_ShouldBeAddedWhenLimitExceeded() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.111");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Exceed the rate limit
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert - Retry-After and X-Rate-Limit-Retry-After-Seconds headers should be added
        verify(response, atLeastOnce()).addHeader(eq("Retry-After"), anyString());
        verify(response, atLeastOnce()).addHeader(eq("X-Rate-Limit-Retry-After-Seconds"), anyString());
    }

    @Test
    void testErrorMessage_ShouldContainRetryInformation() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.112");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act - Exceed the rate limit
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Assert - Error message should contain information about retry
        String responseContent = stringWriter.toString();
        assertTrue(responseContent.contains("Too many requests"));
        assertTrue(responseContent.contains("Rate limit exceeded"));
    }
}
