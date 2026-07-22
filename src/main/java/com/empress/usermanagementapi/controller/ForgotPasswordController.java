package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.service.AccountRecoveryService;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);
    private static final String RESET_REQUEST_MESSAGE =
            "If an account with those details exists, a reset link has been sent.";

    private final AccountRecoveryService accountRecoveryService;

    public ForgotPasswordController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
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

        // All account-dependent work happens asynchronously so the response
        // time does not reveal whether the account exists. A rejected
        // submission (executor saturated) must not change the public reply.
        try {
            accountRecoveryService.processResetRequest(trimmedUsername, trimmedEmail);
        } catch (Exception e) {
            log.error("Failed to submit account recovery request - username: {}, email: {}",
                    trimmedUsername,
                    LoggingUtil.maskEmail(trimmedEmail),
                    e);
        }

        model.addAttribute("success", RESET_REQUEST_MESSAGE);
        return "forgot-password";
    }
}
