package com.empress.usermanagementapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // Comes from env var RESEND_API_KEY (Railway) -> property resend.api.key
    @Value("${resend.api.key}")
    private String resendApiKey;

    // Comes from env var RESEND_FROM (Railway) -> property resend.from
    @Value("${resend.from}")
    private String resendFrom;   // e.g. "User Management <onboarding@resend.dev>"

    // Configuration for retry logic
    @Value("${email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${email.retry.delay-ms:1000}")
    private long retryDelayMs;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Password Reset Request";
        String body =
                "You requested to reset your password.\n\n" +
                "Click the link below to set a new password:\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can ignore this email.";

        sendEmail(to, subject, body);
    }

    public void sendVerificationEmail(String to, String verifyLink) {
        String subject = "Verify your email";
        String body =
                "Welcome! Please verify your email address.\n\n" +
                "Click the link below to verify your account:\n" +
                verifyLink + "\n\n" +
                "If you didn't create an account, you can ignore this email.";

        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String textBody) {
        String emailType = determineEmailType(subject);
        logger.info("Attempting to send {} email to: {}", emailType, to);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("from", resendFrom);
                payload.put("to", new String[]{to});
                payload.put("subject", subject);
                payload.put("text", textBody);

                String json = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.resend.com/emails"))
                        .header("Authorization", "Bearer " + resendApiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();

                if (attempt > 1) {
                    logger.info("Retry attempt {} of {} for {} email to: {}", 
                            attempt, maxRetryAttempts, emailType, to);
                }

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("Successfully sent {} email to: {} (HTTP {})", 
                            emailType, to, response.statusCode());
                    return; // Success - exit method
                } else {
                    String errorMsg = String.format(
                            "Failed to send %s email to: %s - HTTP %d - Response: %s",
                            emailType, to, response.statusCode(), response.body());
                    
                    if (isRetriableError(response.statusCode())) {
                        logger.warn("{}. Retryable error detected.", errorMsg);
                        lastException = new RuntimeException(errorMsg);
                        
                        if (attempt < maxRetryAttempts) {
                            Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                            continue; // Retry
                        }
                    } else {
                        logger.error("{}. Non-retryable error - aborting.", errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while sending {} email to: {} on attempt {}", 
                        emailType, to, attempt, e);
                throw new RuntimeException(
                        String.format("Interrupted while sending %s email to: %s", emailType, to), e);
            } catch (Exception e) {
                lastException = e;
                String errorMsg = String.format(
                        "Exception while sending %s email to: %s on attempt %d: %s",
                        emailType, to, attempt, e.getMessage());
                
                if (attempt < maxRetryAttempts && isRetriableException(e)) {
                    logger.warn("{}. Will retry.", errorMsg, e);
                    try {
                        Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrupted during retry delay for {} email to: {}", 
                                emailType, to, ie);
                        throw new RuntimeException(
                                String.format("Failed to send %s email to: %s", emailType, to), e);
                    }
                } else {
                    logger.error("{}. Max retries reached or non-retryable error.", errorMsg, e);
                    throw new RuntimeException(
                            String.format("Failed to send %s email to: %s after %d attempts", 
                                    emailType, to, attempt), e);
                }
            }
        }

        // If we've exhausted all retries
        logger.error("Failed to send {} email to: {} after {} attempts", 
                emailType, to, maxRetryAttempts);
        throw new RuntimeException(
                String.format("Failed to send %s email to: %s after %d attempts", 
                        emailType, to, maxRetryAttempts), lastException);
    }

    /**
     * Determines the type of email based on subject for logging purposes.
     */
    private String determineEmailType(String subject) {
        if (subject.toLowerCase().contains("verification") || subject.toLowerCase().contains("verify")) {
            return "verification";
        } else if (subject.toLowerCase().contains("password") || subject.toLowerCase().contains("reset")) {
            return "password-reset";
        }
        return "notification";
    }

    /**
     * Checks if an HTTP status code indicates a retryable error.
     * Retryable errors are typically server errors (5xx) or rate limiting (429).
     */
    private boolean isRetriableError(int statusCode) {
        return statusCode == 429 || // Too Many Requests
               statusCode == 503 || // Service Unavailable
               statusCode == 504 || // Gateway Timeout
               (statusCode >= 500 && statusCode < 600); // Server errors
    }

    /**
     * Checks if an exception indicates a retryable condition.
     * Typically network-related exceptions like timeouts or connection errors.
     */
    private boolean isRetriableException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        // Check for common network-related errors
        return message.contains("timeout") ||
               message.contains("Connection refused") ||
               message.contains("Connection reset") ||
               message.contains("Network is unreachable") ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException;
    }
}
