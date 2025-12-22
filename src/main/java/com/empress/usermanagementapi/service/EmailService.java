package com.empress.usermanagementapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // use whatever is configured as spring.mail.username
    @Value("${spring.mail.username}")
    private String defaultFrom;

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);

        // set a valid "from" address (important for Gmail)
        if (defaultFrom != null && !defaultFrom.isBlank()) {
            msg.setFrom(defaultFrom);
        }

        msg.setSubject("Password Reset Request");
        msg.setText(
            "You requested to reset your password.\n\n" +
            "Click the link below to set a new password:\n" +
            resetLink + "\n\n" +
            "If you didn't request this, you can ignore this email."
        );

        System.out.println("[MAIL] Sending password reset to " + to);
        mailSender.send(msg);
        System.out.println("[MAIL] Password reset email sent (or queued) successfully");
    }
}
