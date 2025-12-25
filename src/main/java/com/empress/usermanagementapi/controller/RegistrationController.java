package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.model.RegistrationRequest;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller for handling user registration requests.
 * 
 * This controller manages the registration flow, which includes:
 * - Validating user input using Bean Validation
 * - Checking for duplicate usernames and emails
 * - Creating new user accounts
 * - Generating and sending email verification tokens
 * 
 * The registration endpoint uses Spring's validation framework to ensure data quality
 * before processing. If validation fails, detailed error messages are returned to help
 * users correct their input.
 */
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

    /**
     * Handles user registration form submission with comprehensive validation.
     * 
     * This endpoint validates the registration request using Bean Validation annotations
     * and provides detailed error feedback to the user. The validation process:
     * 
     * 1. Checks input fields against constraints (required, format, length, etc.)
     * 2. If validation fails, exposes detailed error information to the view
     * 3. Checks for username/email uniqueness in the database
     * 4. Creates the user account and sends verification email if all checks pass
     * 
     * @param registrationRequest The form data with validation annotations
     * @param bindingResult Contains validation results and error details
     * @param model Model to pass data to the view (including error messages)
     * @return The view name to render (register page on error, redirect to login on success)
     */
    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("userForm") RegistrationRequest registrationRequest,
            BindingResult bindingResult,
            Model model
    ) {
        // Check for validation errors first and expose them to the view
        // The BindingResult contains detailed information about which fields failed
        // validation and what the specific error messages are
        if (bindingResult.hasErrors()) {
            // Add validation errors to the model so they can be displayed to the user
            // The errors are automatically available in the view via the binding result
            model.addAttribute("validationErrors", bindingResult.getFieldErrors());
            return "register";
        }

        // check username first
        if (userService.usernameExists(registrationRequest.getUsername())) {
            model.addAttribute("usernameError", "Username already in use");
            return "register";
        }
        // then check email
        if (userService.emailExists(registrationRequest.getEmail())) {
            model.addAttribute("emailError", "Email already in use");
            return "register";
        }

        // Create user from validated DTO
        User userForm = new User();
        userForm.setUsername(registrationRequest.getUsername());
        userForm.setEmail(registrationRequest.getEmail());
        userForm.setPassword(registrationRequest.getPassword());
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
