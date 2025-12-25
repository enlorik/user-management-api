package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.CreateUserRequest;
import com.empress.usermanagementapi.dto.ValidationErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController validation error handling.
 * 
 * These tests verify that the GlobalExceptionHandler properly intercepts
 * validation errors from REST endpoints and returns structured error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_EmptyUsername_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("");
        request.setEmail("test@example.com");
        request.setPassword("securepass123");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertEquals("Validation failed for one or more fields", errorResponse.getMessage());
        assertTrue(errorResponse.getErrors().size() > 0);
        
        // Verify that username error is present
        boolean foundUsernameError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username"));
        assertTrue(foundUsernameError, "Should have validation error for username field");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_ShortUsername_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ab");
        request.setEmail("test@example.com");
        request.setPassword("securepass123");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().size() > 0);
        
        // Verify username error with size constraint
        boolean foundUsernameSizeError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username") && 
                                   error.getMessage().contains("between 3 and 50"));
        assertTrue(foundUsernameSizeError, "Should have size validation error for username");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_InvalidUsernamePattern_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("invalid-user!");
        request.setEmail("test@example.com");
        request.setPassword("securepass123");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().size() > 0);
        
        // Verify username error with pattern constraint
        boolean foundUsernamePatternError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username") && 
                                   error.getMessage().contains("only letters and numbers"));
        assertTrue(foundUsernamePatternError, "Should have pattern validation error for username");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_InvalidEmail_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("notanemail");
        request.setPassword("securepass123");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().size() > 0);
        
        // Verify that email error is present
        boolean foundEmailError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("email"));
        assertTrue(foundEmailError, "Should have validation error for email field");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_ShortPassword_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("test@example.com");
        request.setPassword("short");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().size() > 0);
        
        // Verify that password error is present
        boolean foundPasswordError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password"));
        assertTrue(foundPasswordError, "Should have validation error for password field");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_MultipleValidationErrors_ReturnsAllErrors() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("");
        request.setEmail("notanemail");
        request.setPassword("short");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getTimestamp());
        assertEquals("Validation failed for one or more fields", errorResponse.getMessage());
        
        // Should have multiple validation errors
        assertTrue(errorResponse.getErrors().size() >= 3, 
                "Should have at least 3 validation errors");
        
        // Verify structure of error response
        for (ValidationErrorResponse.FieldError error : errorResponse.getErrors()) {
            assertNotNull(error.getField(), "Field name should not be null");
            assertNotNull(error.getMessage(), "Error message should not be null");
        }
    }

    @Test
    void testCreateUser_ValidData_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // This test verifies that valid data passes validation but is blocked by security
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser123");
        request.setEmail("valid@example.com");
        request.setPassword("securepass123");

        // Without authentication, should get redirected to login (302)
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }
}
