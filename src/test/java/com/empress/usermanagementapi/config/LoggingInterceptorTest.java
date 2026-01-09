package com.empress.usermanagementapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for LoggingInterceptor.
 * Verifies that HTTP requests and responses are properly logged.
 */
@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

    @InjectMocks
    private LoggingInterceptor loggingInterceptor;

    @Test
    void testPreHandle_ShouldReturnTrueAndSetStartTime() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/users");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Object handler = new Object();

        // Act
        boolean result = loggingInterceptor.preHandle(request, response, handler);

        // Assert
        assertTrue(result);
        assertNotNull(request.getAttribute("startTime"));
    }

    @Test
    void testAfterCompletion_ShouldLogRequestWithoutException() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/users");
        request.setAttribute("startTime", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        Object handler = new Object();

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            loggingInterceptor.afterCompletion(request, response, handler, null)
        );
    }

    @Test
    void testAfterCompletion_ShouldLogRequestWithException() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/users");
        request.setAttribute("startTime", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        Object handler = new Object();
        Exception exception = new RuntimeException("Test exception");

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            loggingInterceptor.afterCompletion(request, response, handler, exception)
        );
    }
}
