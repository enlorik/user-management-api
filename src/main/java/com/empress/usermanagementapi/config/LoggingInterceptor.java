package com.empress.usermanagementapi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * HTTP request/response logging interceptor.
 * Logs incoming HTTP requests and outgoing responses for monitoring and debugging.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("Incoming HTTP request - method: {}, uri: {}, remoteAddr: {}", 
                request.getMethod(), 
                request.getRequestURI(),
                request.getRemoteAddr());
        
        // Log headers (excluding sensitive ones)
        if (log.isDebugEnabled()) {
            String headers = Collections.list(request.getHeaderNames())
                    .stream()
                    .filter(name -> !name.equalsIgnoreCase("authorization") 
                                 && !name.equalsIgnoreCase("cookie"))
                    .map(name -> name + ": " + request.getHeader(name))
                    .collect(Collectors.joining(", "));
            log.debug("Request headers - {}", headers);
        }
        
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        // No-op: we log in afterCompletion to get the final status
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        Object startTimeAttr = request.getAttribute("startTime");
        long duration;
        
        if (startTimeAttr instanceof Long) {
            long startTime = (Long) startTimeAttr;
            duration = System.currentTimeMillis() - startTime;
        } else {
            log.warn("startTime attribute missing or invalid for request: {} {}", 
                    request.getMethod(), request.getRequestURI());
            duration = 0; // Duration unknown, use 0 to indicate timing unavailable
        }
        
        if (ex != null) {
            log.error("HTTP request completed with exception - method: {}, uri: {}, status: {}, duration: {}ms, exception: {}", 
                    request.getMethod(), 
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    ex.getClass().getSimpleName());
        } else {
            log.info("HTTP request completed - method: {}, uri: {}, status: {}, duration: {}ms", 
                    request.getMethod(), 
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
        }
    }
}
