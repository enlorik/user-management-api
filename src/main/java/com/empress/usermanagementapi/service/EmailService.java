package com.empress.usermanagementapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    // Comes from env var RESEND_API_KEY (Railway) -> property resend.api.key
    @Value("${resend.api.key}")
    private String resendApiKey;

    // Comes from env var RESEND_FROM (Railway) -> property resend.from
    @Value("${resend.from}")
    private String resendFrom;   // e.g. "User Management <onboarding@resend.dev>"

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

            System.out.println("[MAIL] Sending email via Resend to " + to);
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("[MAIL] Resend error: HTTP " + response.statusCode()
                        + " body=" + response.body());
                throw new RuntimeException("Resend API error: " + response.statusCode());
            }

            System.out.println("[MAIL] Email sent via Resend, response: " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Resend", e);
        }
    }
}
