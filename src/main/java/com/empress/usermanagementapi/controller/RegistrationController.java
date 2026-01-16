package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.model.RegistrationRequest;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.UserService;
import com.empress.usermanagementapi.util.LoggingUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

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
        LoggingUtil.setActionType("USER_REGISTRATION");
        log.info("Registration attempt - username: {}, email: {}", 
                registrationRequest.getUsername(), 
                LoggingUtil.maskEmail(registrationRequest.getEmail()));
        
        // Check for validation errors first and expose them to the view
        // The BindingResult contains detailed information about which fields failed
        // validation and what the specific error messages are
        if (bindingResult.hasErrors()) {
            log.warn("Registration validation failed - username: {}, errorCount: {}", 
                    registrationRequest.getUsername(), 
                    bindingResult.getErrorCount());
            // Add validation errors to the model so they can be displayed to the user
            // The errors are automatically available in the view via the binding result
            model.addAttribute("validationErrors", bindingResult.getFieldErrors());
            LoggingUtil.clearActionType();
            return "register";
        }

        // check username first
        if (userService.usernameExists(registrationRequest.getUsername())) {
            log.warn("Registration failed - username already exists: {}", 
                    registrationRequest.getUsername());
            model.addAttribute("usernameError", "Username already in use");
            LoggingUtil.clearActionType();
            return "register";
        }
        // then check email
        if (userService.emailExists(registrationRequest.getEmail())) {
            log.warn("Registration failed - email already exists: {}", 
                    LoggingUtil.maskEmail(registrationRequest.getEmail()));
            model.addAttribute("emailError", "Email already in use");
            LoggingUtil.clearActionType();
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
        
        if (created != null && created.getId() != null) {
            LoggingUtil.setUserId(created.getId());
            log.info("User registered successfully - userId: {}, username: {}", 
                    created.getId(), 
                    created.getUsername());

            // create verification token
            String token = emailVerificationService.createTokenForUser(created);
            log.debug("Email verification token created - userId: {}", created.getId());

            String verifyLink = baseUrl + "/verify-email?token=" + token;

            try {
                emailService.sendVerificationEmail(created.getEmail(), verifyLink);
                log.info("Verification email sent - userId: {}", created.getId());
            } catch (Exception e) {
                // for now just log; account is created even if email fails
                log.error("Failed to send verification email - userId: {}, error: {}", 
                        created.getId(), 
                        e.getMessage());
                System.err.println("Failed to send verification email: " + e.getMessage());
            }
        } else {
            log.warn("User creation returned null or invalid user");
        }
        
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();

        // show a message on login page like:
        // "Registration successful. Please check your email to verify your account."
        return "redirect:/login?verifyEmail";
    }
}
