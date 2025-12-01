package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
            model.addAttribute("message", error);
            // token stays null → form will still render but hidden field empty
        } else {
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
            model.addAttribute("message", error);
            model.addAttribute("token", token);
            return "reset-password";
        }

        return "redirect:/login?resetSuccess";
    }
}
