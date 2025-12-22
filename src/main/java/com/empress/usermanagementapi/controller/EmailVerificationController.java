package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.service.EmailVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {

        String error = emailVerificationService.verifyToken(token);

        if (error != null) {
            model.addAttribute("error", error);
        } else {
            model.addAttribute("success", "Your email has been verified. You can now log in.");
        }

        return "verify-email-result";
    }
}
