package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.UserService;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private final UserService userService;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public ForgotPasswordController(UserService userService,
                                    EmailService emailService,
                                    PasswordResetService passwordResetService) {
        this.userService = userService;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForm(Model model) {
        // Clear messages on a fresh GET
        model.addAttribute("error", null);
        model.addAttribute("success", null);
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForm(@RequestParam String username,
                             @RequestParam String email,
                             Model model) {

        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();

        if (trimmedUsername.isEmpty() || trimmedEmail.isEmpty()) {
            model.addAttribute("error", "Username and email are required.");
            return "forgot-password";
        }

        // Check username + email combo
        Optional<User> opt = userService.findByUsernameAndEmail(trimmedUsername, trimmedEmail);

        if (opt.isEmpty()) {
            // This is the part you were missing: explicit feedback
            model.addAttribute("error", "No account found with that username and email.");
            return "forgot-password";
        }

        // User exists, create token + send email
        PasswordResetToken tokenEntity =
                passwordResetService.createPasswordResetTokenForEmail(trimmedEmail);
        String token = tokenEntity.getToken();

        String resetLink = baseUrl + "/reset-password?token=" + token;

        try {
            emailService.sendPasswordResetEmail(trimmedEmail, resetLink);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            model.addAttribute("error", "Failed to send password reset email. Please try again later.");
            return "forgot-password";
        }

        model.addAttribute("success", "Password reset link has been sent to your email.");
        return "forgot-password";
    }
}
