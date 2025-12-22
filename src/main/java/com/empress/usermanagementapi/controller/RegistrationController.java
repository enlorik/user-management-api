package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    public RegistrationController(UserService userService,
                                  EmailVerificationService emailVerificationService,
                                  EmailService emailService) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public String registerSubmit(
            @ModelAttribute("userForm") User userForm,
            Model model
    ) {
        // check username first
        if (userService.usernameExists(userForm.getUsername())) {
            model.addAttribute("usernameError", "Username already in use");
            return "register";
        }
        // then check email
        if (userService.emailExists(userForm.getEmail())) {
            model.addAttribute("emailError", "Email already in use");
            return "register";
        }

        userForm.setRole(Role.USER);
        // verified stays false by default in User
        User created = userService.create(userForm);

        // create verification token
        String token = emailVerificationService.createTokenForUser(created);

        // same base URL style as your reset-password links
        String baseUrl = "https://user-management-api-java.up.railway.app";
        String verifyLink = baseUrl + "/verify-email?token=" + token;

        try {
            emailService.sendVerificationEmail(created.getEmail(), verifyLink);
        } catch (Exception e) {
            // for now just log; account is created even if email fails
            System.err.println("Failed to send verification email: " + e.getMessage());
        }

        // show a message on login page like:
        // "Registration successful. Please check your email to verify your account."
        return "redirect:/login?verifyEmail";
    }
}
