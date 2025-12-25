package com.empress.usermanagementapi.exception;

import com.empress.usermanagementapi.dto.ValidationErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the GlobalExceptionHandler to verify that validation errors
 * are properly caught and translated into structured error responses.
 * 
 * These tests ensure that the exception handler correctly processes
 * MethodArgumentNotValidException and creates appropriate ValidationErrorResponse objects.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleValidationException_SingleFieldError() {
        // Create mock exception with a single field error
        MethodArgumentNotValidException mockException = createMockException(
                new FieldError("registrationRequest", "username", "", false, 
                        null, null, "Username is required")
        );

        // Handle the exception
        ResponseEntity<ValidationErrorResponse> response = 
                exceptionHandler.handleValidationException(mockException);

        // Verify the response
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Validation failed for one or more fields", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertEquals(1, body.getErrors().size());
        
        ValidationErrorResponse.FieldError error = body.getErrors().get(0);
        assertEquals("username", error.getField());
        assertEquals("Username is required", error.getMessage());
    }

    @Test
    void testHandleValidationException_MultipleFieldErrors() {
        // Create mock exception with multiple field errors
        MethodArgumentNotValidException mockException = createMockException(
                new FieldError("registrationRequest", "username", "", false, 
                        null, null, "Username is required"),
                new FieldError("registrationRequest", "email", "notanemail", false, 
                        null, null, "Email must be a valid email address"),
                new FieldError("registrationRequest", "password", "short", false, 
                        null, null, "Password must be between 6 and 255 characters")
        );

        // Handle the exception
        ResponseEntity<ValidationErrorResponse> response = 
                exceptionHandler.handleValidationException(mockException);

        // Verify the response
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.getErrors().size());
        
        // Verify all three errors are present
        List<String> fieldNames = body.getErrors().stream()
                .map(ValidationErrorResponse.FieldError::getField)
                .toList();
        assertTrue(fieldNames.contains("username"));
        assertTrue(fieldNames.contains("email"));
        assertTrue(fieldNames.contains("password"));
    }

    @Test
    void testHandleValidationException_ErrorWithRejectedValue() {
        // Create mock exception with a field error that has a rejected value
        MethodArgumentNotValidException mockException = createMockException(
                new FieldError("registrationRequest", "email", "invalid-email", false, 
                        null, null, "Email must be a valid email address")
        );

        // Handle the exception
        ResponseEntity<ValidationErrorResponse> response = 
                exceptionHandler.handleValidationException(mockException);

        // Verify the response includes the rejected value
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getErrors().size());
        
        ValidationErrorResponse.FieldError error = body.getErrors().get(0);
        assertEquals("email", error.getField());
        assertEquals("invalid-email", error.getRejectedValue());
        assertEquals("Email must be a valid email address", error.getMessage());
    }

    @Test
    void testHandleValidationException_ResponseStructure() {
        // Create mock exception
        MethodArgumentNotValidException mockException = createMockException(
                new FieldError("registrationRequest", "username", "", false, 
                        null, null, "Username is required")
        );

        // Handle the exception
        ResponseEntity<ValidationErrorResponse> response = 
                exceptionHandler.handleValidationException(mockException);

        // Verify response structure
        ValidationErrorResponse body = response.getBody();
        assertNotNull(body);
        
        // Check that all required fields are present
        assertNotNull(body.getTimestamp(), "Timestamp should be present");
        assertNotNull(body.getMessage(), "Message should be present");
        assertNotNull(body.getErrors(), "Errors list should be present");
        
        // Verify error structure
        for (ValidationErrorResponse.FieldError error : body.getErrors()) {
            assertNotNull(error.getField(), "Field name should not be null");
            assertNotNull(error.getMessage(), "Error message should not be null");
            // rejectedValue can be null for some validations
        }
    }

    /**
     * Helper method to create a mock MethodArgumentNotValidException with the given field errors.
     */
    private MethodArgumentNotValidException createMockException(FieldError... fieldErrors) {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldErrors));
        
        return exception;
    }
}
