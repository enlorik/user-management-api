package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.UserService;
import com.empress.usermanagementapi.service.PasswordResetService;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);
    private static final String RESET_REQUEST_MESSAGE =
            "If an account with those details exists, a reset link has been sent.";

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(UserService userService,
                                    PasswordResetService passwordResetService) {
        this.userService = userService;
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

        Optional<User> opt = userService.findByUsernameAndEmail(trimmedUsername, trimmedEmail);

        if (opt.isEmpty()) {
            log.info("Password reset requested for non-matching account - username: {}, email: {}",
                    trimmedUsername,
                    LoggingUtil.maskEmail(trimmedEmail));
            model.addAttribute("success", RESET_REQUEST_MESSAGE);
            return "forgot-password";
        }

        try {
            passwordResetService.createTokenAndSendResetEmail(trimmedEmail);
            log.info("Password reset email sent - username: {}, email: {}",
                    trimmedUsername,
                    LoggingUtil.maskEmail(trimmedEmail));
        } catch (Exception e) {
            log.error("Failed to send password reset email - username: {}, email: {}",
                    trimmedUsername,
                    LoggingUtil.maskEmail(trimmedEmail),
                    e);
        }

        model.addAttribute("success", RESET_REQUEST_MESSAGE);
        return "forgot-password";
    }
}
