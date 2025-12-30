package com.empress.usermanagementapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for EmailService with comprehensive logging and retry logic tests.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private HttpClient httpClient;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        
        // Set required fields using reflection
        ReflectionTestUtils.setField(emailService, "resendApiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "resendFrom", "test@example.com");
        ReflectionTestUtils.setField(emailService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(emailService, "retryDelayMs", 100L);
        ReflectionTestUtils.setField(emailService, "httpClient", httpClient);
        ReflectionTestUtils.setField(emailService, "objectMapper", new ObjectMapper());
    }

    @Test
    void testSendVerificationEmail_Success() throws Exception {
        // Arrange
        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(successResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        // Assert
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendPasswordResetEmail_Success() throws Exception {
        // Arrange
        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(successResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail("user@example.com", "http://reset-link"));

        // Assert
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_RetryableError_503_Success_OnRetry() throws Exception {
        // Arrange - First call fails with 503, second succeeds
        HttpResponse<String> firstResponse = mock(HttpResponse.class);
        HttpResponse<String> secondResponse = mock(HttpResponse.class);
        
        when(firstResponse.statusCode()).thenReturn(503);
        when(firstResponse.body()).thenReturn("Service Unavailable");
        when(secondResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, secondResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        // Assert - should have retried
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_NonRetryableError_400_FailsImmediately() throws Exception {
        // Arrange - 400 Bad Request (non-retryable)
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(400);
        when(errorResponse.body()).thenReturn("Bad Request");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(errorResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        String message = exception.getMessage();
        assertTrue(message.contains("verification") || message.contains("user@example.com"));
        
        // Should only try once (no retries for 400)
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_RetryableError_ExhaustsRetries() throws Exception {
        // Arrange - Always returns 503
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(503);
        when(errorResponse.body()).thenReturn("Service Unavailable");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(errorResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendPasswordResetEmail("user@example.com", "http://reset-link"));

        assertTrue(exception.getMessage().contains("password-reset"));
        assertTrue(exception.getMessage().contains("after 3 attempts"));
        
        // Should try maxRetryAttempts times
        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_RateLimited_429_Retries() throws Exception {
        // Arrange - 429 rate limited, then success
        HttpResponse<String> firstResponse = mock(HttpResponse.class);
        HttpResponse<String> secondResponse = mock(HttpResponse.class);
        
        when(firstResponse.statusCode()).thenReturn(429);
        when(firstResponse.body()).thenReturn("Rate Limited");
        when(secondResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, secondResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        // Assert - should have retried
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_SocketTimeout_Retries() throws Exception {
        // Arrange - First call times out, second succeeds
        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new SocketTimeoutException("Connection timed out"))
                .thenReturn(successResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        // Assert - should have retried
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_ConnectionRefused_Retries() throws Exception {
        // Arrange - First call fails with connection refused, second succeeds
        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new ConnectException("Connection refused"))
                .thenReturn(successResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail("user@example.com", "http://reset-link"));

        // Assert - should have retried
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_NonRetriableException_FailsImmediately() throws Exception {
        // Arrange - IOException is not retryable (unless message indicates timeout/connection)
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Invalid JSON"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        assertTrue(exception.getMessage().contains("verification"));
        
        // Should only try once (no retries for non-retryable exceptions)
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_500_ServerError_Retries() throws Exception {
        // Arrange - 500 Internal Server Error twice, then success
        HttpResponse<String> firstResponse = mock(HttpResponse.class);
        HttpResponse<String> secondResponse = mock(HttpResponse.class);
        HttpResponse<String> thirdResponse = mock(HttpResponse.class);
        
        when(firstResponse.statusCode()).thenReturn(500);
        when(firstResponse.body()).thenReturn("Internal Server Error");
        when(secondResponse.statusCode()).thenReturn(500);
        when(secondResponse.body()).thenReturn("Internal Server Error");
        when(thirdResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, secondResponse, thirdResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        // Assert - should have retried twice before success
        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_504_GatewayTimeout_Retries() throws Exception {
        // Arrange - 504 Gateway Timeout, then success
        HttpResponse<String> firstResponse = mock(HttpResponse.class);
        HttpResponse<String> secondResponse = mock(HttpResponse.class);
        
        when(firstResponse.statusCode()).thenReturn(504);
        when(firstResponse.body()).thenReturn("Gateway Timeout");
        when(secondResponse.statusCode()).thenReturn(200);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(firstResponse, secondResponse);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendPasswordResetEmail("user@example.com", "http://reset-link"));

        // Assert - should have retried
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_401_Unauthorized_NoRetry() throws Exception {
        // Arrange - 401 Unauthorized (authentication issue, not retryable)
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(401);
        when(errorResponse.body()).thenReturn("Unauthorized");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(errorResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        String message = exception.getMessage();
        assertTrue(message.contains("verification") || message.contains("user@example.com"));
        
        // Should only try once
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSendEmail_ExponentialBackoff() throws Exception {
        // Arrange - Always returns 503 to test all retries
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(503);
        when(errorResponse.body()).thenReturn("Service Unavailable");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(errorResponse);

        long startTime = System.currentTimeMillis();

        // Act
        assertThrows(RuntimeException.class, () -> 
            emailService.sendVerificationEmail("user@example.com", "http://verify-link"));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert - Exponential backoff should be: 100ms + 200ms = 300ms minimum
        // (first retry waits retryDelayMs * 1, second waits retryDelayMs * 2)
        assertTrue(duration >= 300, "Should have waited at least 300ms due to exponential backoff");
        
        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
