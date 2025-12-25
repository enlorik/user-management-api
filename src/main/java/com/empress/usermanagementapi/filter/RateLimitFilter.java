package com.empress.usermanagementapi.filter;

import com.empress.usermanagementapi.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that implements IP-based rate limiting for critical public endpoints.
 * 
 * This filter intercepts requests before they reach business logic and enforces
 * rate limits using the Bucket4j library with a token bucket algorithm.
 * 
 * Features:
 * - Extracts client IP from X-Forwarded-For header (for proxy/load balancer scenarios)
 * - Falls back to RemoteAddr if X-Forwarded-For is not available
 * - Returns HTTP 429 (Too Many Requests) with Retry-After header when limits are exceeded
 * - Logs rate limit events for analysis and debugging
 * - Handles exceptions gracefully to prevent filter failures from blocking requests
 */
@Component
@Order(1) // Execute early in the filter chain
public class RateLimitFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final RateLimitConfig rateLimitConfig;
    
    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        String path = request.getRequestURI();
        
        // Only apply rate limiting to specific endpoints
        if (!shouldRateLimit(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        String clientIp = null;
        Bucket bucket = null;
        ConsumptionProbe probe = null;
        
        try {
            clientIp = extractClientIp(request);
            bucket = resolveBucket(path, clientIp);
            
            if (bucket == null) {
                // Should not happen, but if it does, allow the request
                logger.warn("No bucket found for path: {} and IP: {}", path, clientIp);
                chain.doFilter(request, response);
                return;
            }
            
            probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                // Request is allowed
                logger.debug("Rate limit OK for IP {} on {}: {} tokens remaining", 
                    clientIp, path, probe.getRemainingTokens());
                chain.doFilter(request, response);
            } else {
                // Rate limit exceeded - try to send error response
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                logger.warn("Rate limit exceeded for IP {} on {}: retry after {} seconds", 
                    clientIp, path, waitForRefill);
                
                try {
                    response.setStatus(429); // HTTP 429 Too Many Requests
                    response.setHeader("Retry-After", String.valueOf(waitForRefill));
                    response.setContentType("application/json");
                    response.getWriter().write(String.format(
                        "{\"error\":\"Rate limit exceeded\",\"retryAfter\":%d}", waitForRefill));
                } catch (IOException e) {
                    // Even if we can't write the response, the rate limit is still enforced
                    logger.error("Failed to write rate limit error response for IP {} on path {}: {}", 
                        clientIp, path, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            // Log the exception - for critical errors, still try to allow the request
            logger.error("Error in rate limit filter for path {}: {}", path, e.getMessage(), e);
            chain.doFilter(request, response);
        }
    }
    
    /**
     * Determine if the given path should be rate limited.
     */
    private boolean shouldRateLimit(String path) {
        return path.equals("/login") || 
               path.startsWith("/auth/login") ||
               path.equals("/register") || 
               path.startsWith("/verify-email");
    }
    
    /**
     * Extract client IP address from the request.
     * Prioritizes X-Forwarded-For header (for proxy/load balancer scenarios),
     * falls back to RemoteAddr.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // We want the first one (the original client)
            String[] ips = xForwardedFor.split(",");
            String clientIp = ips[0].trim();
            logger.debug("Extracted IP from X-Forwarded-For: {}", clientIp);
            return clientIp;
        }
        
        // Fallback to RemoteAddr
        String remoteAddr = request.getRemoteAddr();
        logger.debug("Using RemoteAddr as client IP: {}", remoteAddr);
        return remoteAddr;
    }
    
    /**
     * Resolve the appropriate rate limit bucket for the given path and client IP.
     */
    private Bucket resolveBucket(String path, String clientIp) {
        if (path.equals("/login") || path.startsWith("/auth/login")) {
            return rateLimitConfig.resolveBucketForLogin(clientIp);
        } else if (path.equals("/register")) {
            return rateLimitConfig.resolveBucketForRegister(clientIp);
        } else if (path.startsWith("/verify-email")) {
            return rateLimitConfig.resolveBucketForVerifyEmail(clientIp);
        }
        return null;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RateLimitFilter initialized");
    }
    
    @Override
    public void destroy() {
        logger.info("RateLimitFilter destroyed");
    }
}
