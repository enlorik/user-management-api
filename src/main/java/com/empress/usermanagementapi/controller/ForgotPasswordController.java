package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public ForgotPasswordController(UserRepository userRepo,
                                    EmailService emailService,
                                    PasswordResetService passwordResetService) {
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForm(Model model) {
        // Clear all messages on a fresh GET
        model.addAttribute("debugReceived", null);
        model.addAttribute("debugResetLink", null);
        model.addAttribute("message", null);
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForm(@RequestParam String username,
                             @RequestParam String email,
                             Model model) {

        // Show what was sent (for debugging)
        String received = "username=" + username + " email=" + email;
        model.addAttribute("debugReceived", received);

        Optional<User> opt = userRepo.findByUsernameAndEmail(username, email);

        // Generic banner, regardless of whether the user exists
        model.addAttribute("message",
                "If an account matches those details, you’ll receive an email shortly."
        );

        if (opt.isPresent()) {
            // Create and store token in DB with expiry
            PasswordResetToken tokenEntity =
                    passwordResetService.createPasswordResetTokenForEmail(email);
            String token = tokenEntity.getToken();

            // Adjust baseUrl to your deployed host as needed
            String baseUrl = "https://user-management-api-java.up.railway.app";
            String resetLink = baseUrl + "/reset-password?token=" + token;

            // Show the link on the page for debugging
            model.addAttribute("debugResetLink", resetLink);

            // Send the email. Wrap in try/catch so a failure does not abort the request.
            try {
                emailService.sendPasswordResetEmail(email, resetLink);
            } catch (Exception e) {
                // Log the exception; in real apps use a proper logger
                System.err.println("Failed to send password reset email: " + e.getMessage());
            }
        } else {
            model.addAttribute("debugResetLink", null);
        }

        return "forgot-password";
    }
}
