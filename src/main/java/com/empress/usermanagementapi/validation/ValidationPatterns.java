package com.empress.usermanagementapi.validation;

/**
 * Centralized validation patterns for user input validation.
 * 
 * This class contains regex patterns used across the application for validating
 * user inputs such as email addresses and passwords. Centralizing these patterns
 * ensures consistency and makes them easier to maintain and update.
 */
public final class ValidationPatterns {

    private ValidationPatterns() {
        // Prevent instantiation
    }

    /**
     * Regex pattern for email validation.
     * Ensures the email has a valid format with:
     * - Local part (alphanumeric, dots, underscores, percent, plus, hyphen)
     * - @ symbol
     * - Domain name (alphanumeric, dots, hyphens)
     * - TLD (at least 2 alphabetic characters)
     */
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * Message for email validation failures.
     */
    public static final String EMAIL_MESSAGE = "Email must be a valid email address";

    /**
     * Regex pattern for password validation.
     * Enforces strong password requirements:
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one digit
     * - At least one special character
     * - Minimum 8 characters (enforced separately by @Size annotation)
     */
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>/~`]).{8,}$";

    /**
     * Message for password validation failures.
     */
    public static final String PASSWORD_MESSAGE = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";
}
