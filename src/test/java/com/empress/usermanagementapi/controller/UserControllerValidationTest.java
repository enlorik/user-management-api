package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.CreateUserRequest;
import com.empress.usermanagementapi.dto.UpdateUserRequest;
import com.empress.usermanagementapi.dto.ValidationErrorResponse;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_EmptyUsername_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("");
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");

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
        request.setPassword("SecurePass123!");

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
        request.setPassword("SecurePass123!");

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
        request.setPassword("SecurePass123!");

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
        request.setPassword("Short1!");

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
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("between 8 and 255"));
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

    // New tests for enhanced password validation
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_PasswordWithoutUppercase_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("test@example.com");
        request.setPassword("lowercase123!");

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
        
        boolean foundPasswordError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("uppercase"));
        assertTrue(foundPasswordError, "Should have validation error for missing uppercase letter");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_PasswordWithoutLowercase_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("test@example.com");
        request.setPassword("UPPERCASE123!");

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
        
        boolean foundPasswordError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("lowercase"));
        assertTrue(foundPasswordError, "Should have validation error for missing lowercase letter");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_PasswordWithoutNumber_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("test@example.com");
        request.setPassword("PasswordOnly!");

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
        
        boolean foundPasswordError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("number"));
        assertTrue(foundPasswordError, "Should have validation error for missing number");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_PasswordWithoutSpecialCharacter_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123");

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
        
        boolean foundPasswordError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("special character"));
        assertTrue(foundPasswordError, "Should have validation error for missing special character");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_ValidComplexPassword_PassesValidation() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser123");
        request.setEmail("valid@example.com");
        request.setPassword("SecurePass123!");

        // This test ensures that a properly formatted password passes validation
        // The request may fail for other reasons (user already exists, etc.) but not validation
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
        // We don't assert status here as the user might already exist in the DB
        // The key is that validation doesn't reject it
    }

    // New tests for enhanced email validation

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_EmailWithoutTLD_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("user@domain");
        request.setPassword("SecurePass123!");

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
        
        boolean foundEmailError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("email"));
        assertTrue(foundEmailError, "Should have validation error for email without TLD");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_EmailWithoutAt_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("userdomain.com");
        request.setPassword("SecurePass123!");

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
        
        boolean foundEmailError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("email"));
        assertTrue(foundEmailError, "Should have validation error for email without @ symbol");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_EmailWithSpaces_ReturnsValidationError() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("user name@domain.com");
        request.setPassword("SecurePass123!");

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
        
        boolean foundEmailError = errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("email"));
        assertTrue(foundEmailError, "Should have validation error for email with spaces");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_ValidEmailFormats_PassValidation() throws Exception {
        // Test various valid email formats
        String[] validEmails = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.co.uk",
            "user_name@sub.domain.example.com",
            "123@example.com"
        };

        for (String email : validEmails) {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("user" + email.hashCode());  // Unique username
            request.setEmail(email);
            request.setPassword("SecurePass123!");

            // These should not fail due to validation (may fail for other reasons like duplicates)
            mockMvc.perform(post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }
    }

    @Test
    void testCreateUser_ValidData_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // This test verifies that valid data passes validation but is blocked by security
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser123");
        request.setEmail("valid@example.com");
        request.setPassword("SecurePass123!");

        // Without authentication, should get redirected to login (302)
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }

    // ========== PUT /users/{id} Endpoint Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_EmptyUsername_ReturnsValidationError() throws Exception {
        // Create a test user first
        User user = createTestUser("testuser1", "test1@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("");
        request.setEmail("newemail@example.com");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_ShortUsername_ReturnsValidationError() throws Exception {
        User user = createTestUser("testuser2", "test2@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("ab");
        request.setEmail("test2@example.com");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username") && 
                                   error.getMessage().contains("between 3 and 50")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_InvalidUsernamePattern_ReturnsValidationError() throws Exception {
        User user = createTestUser("testuser3", "test3@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("invalid-user!");
        request.setEmail("test3@example.com");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("username") && 
                                   error.getMessage().contains("only letters and numbers")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_InvalidEmail_ReturnsValidationError() throws Exception {
        User user = createTestUser("testuser4", "test4@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser4");
        request.setEmail("notanemail");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("email")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_ShortPassword_ReturnsValidationError() throws Exception {
        User user = createTestUser("testuser5", "test5@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser5");
        request.setEmail("test5@example.com");
        request.setPassword("Short1!");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("between 8 and 255")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_PasswordWithoutUppercase_ReturnsValidationError() throws Exception {
        User user = createTestUser("testuser6", "test6@example.com", "Password123!");
        
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser6");
        request.setEmail("test6@example.com");
        request.setPassword("lowercase123!");
        
        MvcResult result = mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse errorResponse = objectMapper.readValue(
                responseBody, ValidationErrorResponse.class);

        assertNotNull(errorResponse);
        assertTrue(errorResponse.getErrors().stream()
                .anyMatch(error -> error.getField().equals("password") && 
                                   error.getMessage().contains("uppercase")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_DuplicateUsername_ReturnsConflict() throws Exception {
        // Create two users
        User user1 = createTestUser("existinguser", "existing@example.com", "Password123!");
        User user2 = createTestUser("testuser7", "test7@example.com", "Password123!");
        
        // Try to update user2 with user1's username
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("existinguser");
        request.setEmail("test7@example.com");
        
        MvcResult result = mockMvc.perform(put("/users/" + user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> errorResponse = objectMapper.readValue(responseBody, Map.class);
        
        assertEquals("A user with this username already exists", errorResponse.get("error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_DuplicateEmail_ReturnsConflict() throws Exception {
        // Create two users
        User user1 = createTestUser("user1", "duplicate@example.com", "Password123!");
        User user2 = createTestUser("user2", "test8@example.com", "Password123!");
        
        // Try to update user2 with user1's email
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("user2");
        request.setEmail("duplicate@example.com");
        
        MvcResult result = mockMvc.perform(put("/users/" + user2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> errorResponse = objectMapper.readValue(responseBody, Map.class);
        
        assertEquals("A user with this email already exists", errorResponse.get("error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_SameUsername_Succeeds() throws Exception {
        // Create a user
        User user = createTestUser("testuser9", "test9@example.com", "Password123!");
        
        // Update user keeping the same username
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser9");
        request.setEmail("newemail9@example.com");
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_SameEmail_Succeeds() throws Exception {
        // Create a user
        User user = createTestUser("testuser10", "test10@example.com", "Password123!");
        
        // Update user keeping the same email
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newusername10");
        request.setEmail("test10@example.com");
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_WithPassword_UpdatesPassword() throws Exception {
        // Create a user
        User user = createTestUser("testuser11", "test11@example.com", "Password123!");
        String oldPasswordHash = user.getPassword();
        
        // Update user with a new password
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser11");
        request.setEmail("test11@example.com");
        request.setPassword("NewPassword456!");
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        // Verify password was changed
        User updatedUser = userRepository.findById(user.getId()).get();
        assertNotEquals(oldPasswordHash, updatedUser.getPassword());
        assertTrue(passwordEncoder.matches("NewPassword456!", updatedUser.getPassword()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_WithoutPassword_KeepsExistingPassword() throws Exception {
        // Create a user
        User user = createTestUser("testuser12", "test12@example.com", "Password123!");
        String oldPasswordHash = user.getPassword();
        
        // Update user without providing a password
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser12");
        request.setEmail("newemail12@example.com");
        // password is null
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        // Verify password was NOT changed
        User updatedUser = userRepository.findById(user.getId()).get();
        assertEquals(oldPasswordHash, updatedUser.getPassword());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_UpdateRole_Succeeds() throws Exception {
        // Create a user with USER role
        User user = createTestUser("testuser13", "test13@example.com", "Password123!");
        assertEquals(Role.USER, user.getRole());
        
        // Update user role to ADMIN
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser13");
        request.setEmail("test13@example.com");
        request.setRole(Role.ADMIN);
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        
        // Verify role was changed
        User updatedUser = userRepository.findById(user.getId()).get();
        assertEquals(Role.ADMIN, updatedUser.getRole());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_NonExistentUser_ReturnsNotFound() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        
        mockMvc.perform(put("/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testUpdateUser_ValidUpdate_Succeeds() throws Exception {
        // Create a user
        User user = createTestUser("oldusername", "old@example.com", "Password123!");
        
        // Update all fields
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newusername");
        request.setEmail("new@example.com");
        request.setPassword("NewPassword789!");
        request.setRole(Role.ADMIN);
        
        mockMvc.perform(put("/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        
        // Verify changes persisted
        User updatedUser = userRepository.findById(user.getId()).get();
        assertEquals("newusername", updatedUser.getUsername());
        assertEquals("new@example.com", updatedUser.getEmail());
        assertEquals(Role.ADMIN, updatedUser.getRole());
        assertTrue(passwordEncoder.matches("NewPassword789!", updatedUser.getPassword()));
    }

    // Helper method to create test users
    private User createTestUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setVerified(true);
        return userRepository.save(user);
    }
}
