package com.empress.usermanagementapi.exception;

import com.empress.usermanagementapi.dto.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Global exception handler for centralized error handling across the application.
 * 
 * This class uses Spring's @ControllerAdvice to intercept exceptions thrown by controllers
 * and translate them into standardized error responses. It specifically handles validation
 * errors to provide detailed, user-friendly feedback.
 * 
 * Key features:
 * - Handles MethodArgumentNotValidException for @Valid annotated request bodies
 * - Translates validation errors into structured JSON responses
 * - Provides consistent error format across all REST endpoints
 * - Includes field names, rejected values, and error messages
 * 
 * This ensures that API clients receive clear information about validation failures,
 * improving the developer experience and making debugging easier.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles validation errors when @Valid annotation fails on controller method parameters.
     * 
     * This method is triggered when Spring's validation framework detects constraint violations
     * on request objects annotated with @Valid (commonly used in REST controllers with
     * @RequestBody parameters).
     * 
     * The method extracts all field errors from the exception and constructs a detailed
     * ValidationErrorResponse containing:
     * - The field name that failed validation
     * - The rejected value that caused the failure
     * - The specific validation error message
     * 
     * @param ex The exception thrown when validation fails
     * @return ResponseEntity containing ValidationErrorResponse with HTTP 400 Bad Request status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation failed - field count: {}", ex.getBindingResult().getFieldErrorCount());
        
        ValidationErrorResponse response = new ValidationErrorResponse(
            "Validation failed for one or more fields"
        );
        
        // Extract all field errors from the binding result
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            log.debug("Validation error - field: {}, message: {}", 
                    fieldError.getField(), 
                    fieldError.getDefaultMessage());
            
            response.addFieldError(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
            );
        }
        
        log.error("Validation exception - errors: {}", response.getErrors().size());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
