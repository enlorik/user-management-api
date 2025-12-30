package com.empress.usermanagementapi.dto;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.validation.ValidationPatterns;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for creating a new user via the REST API.
 * This class encapsulates user data with Bean Validation annotations to ensure
 * data integrity before processing.
 * 
 * When used with @Valid annotation in REST controllers, validation errors will be
 * automatically caught by the GlobalExceptionHandler and translated into structured
 * error responses.
 */
public class CreateUserRequest {

    @NotEmpty(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username must contain only letters and numbers")
    private String username;

    @NotEmpty(message = "Email is required")
    @Pattern(regexp = ValidationPatterns.EMAIL_PATTERN, 
             message = ValidationPatterns.EMAIL_MESSAGE)
    private String email;

    @NotEmpty(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    @Pattern(regexp = ValidationPatterns.PASSWORD_PATTERN,
             message = ValidationPatterns.PASSWORD_MESSAGE)
    private String password;

    private Role role;

    public CreateUserRequest() {
    }

    // Getters and setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
