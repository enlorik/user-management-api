package com.empress.usermanagementapi.filter;

import com.empress.usermanagementapi.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to enforce rate limiting on critical endpoints.
 * This filter checks request rates based on IP address and endpoint,
 * returning HTTP 429 when limits are exceeded.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply rate limiting to specific endpoints
        if (shouldRateLimit(path)) {
            String ip = getClientIp(request);
            Bucket bucket = rateLimitConfig.resolveBucket(path, ip);
            
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                // Request allowed - add rate limit headers
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                response.addHeader("Retry-After", String.valueOf(waitForRefill));
                response.setContentType("application/json");
                response.getWriter().write(
                    String.format("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again in %d seconds.\"}", waitForRefill)
                );
            }
        } else {
            // No rate limiting for this endpoint
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Determine if the given path should be rate limited.
     */
    private boolean shouldRateLimit(String path) {
        return path.equals("/login") ||
               path.equals("/auth/login") ||
               path.equals("/register") ||
               path.equals("/verify-email");
    }

    /**
     * Extract the client IP address from the request.
     * Checks X-Forwarded-For header first (for proxied requests),
     * then falls back to the remote address.
     * 
     * Note: In production environments behind a reverse proxy or load balancer,
     * consider validating the X-Forwarded-For header against trusted proxy IPs
     * or using a more robust IP extraction strategy to prevent header spoofing.
     * For now, this implementation trusts the X-Forwarded-For header which is
     * suitable for the Railway deployment environment where the header is set
     * by the platform's reverse proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
