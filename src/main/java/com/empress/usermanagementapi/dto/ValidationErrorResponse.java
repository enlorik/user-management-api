package com.empress.usermanagementapi.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized response structure for validation errors.
 * This class provides a consistent format for communicating validation failures to API clients.
 * 
 * It includes:
 * - A timestamp of when the error occurred
 * - An overall status message
 * - A list of specific field validation errors
 * 
 * Used by the global exception handler to translate validation exceptions into
 * user-friendly error responses.
 */
public class ValidationErrorResponse {
    
    private LocalDateTime timestamp;
    private String message;
    private List<FieldError> errors;
    
    public ValidationErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.errors = new ArrayList<>();
    }
    
    public ValidationErrorResponse(String message) {
        this();
        this.message = message;
    }
    
    /**
     * Adds a field-specific validation error to the response.
     * 
     * @param field The name of the field that failed validation
     * @param rejectedValue The value that was rejected
     * @param message The validation error message
     */
    public void addFieldError(String field, Object rejectedValue, String message) {
        errors.add(new FieldError(field, rejectedValue, message));
    }
    
    // Getters and setters
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<FieldError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }
    
    /**
     * Represents a single field validation error.
     * Contains the field name, the rejected value, and the error message.
     */
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
        
        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }
        
        // Getters and setters
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
        
        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
