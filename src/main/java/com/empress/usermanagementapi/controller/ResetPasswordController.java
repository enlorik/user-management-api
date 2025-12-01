package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ResetPasswordController {

    private final PasswordResetService passwordResetService;

    public ResetPasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam("token") String token, Model model) {
        String error = passwordResetService.validatePasswordResetToken(token);

        if (error != null) {
            // invalid / expired token
            model.addAttribute("message", error);
            // token stays null -> hidden field empty
        } else {
            // valid token -> form will submit it back
            model.addAttribute("token", token);
        }

        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleReset(@RequestParam("token") String token,
                              @RequestParam("password") String password,
                              Model model) {
        String error = passwordResetService.resetPassword(token, password);

        if (error != null) {
            // still invalid / expired
            model.addAttribute("message", error);
            model.addAttribute("token", token);
            return "reset-password";
        }

        // success -> back to login with banner
        return "redirect:/login?resetSuccess";
    }
}
