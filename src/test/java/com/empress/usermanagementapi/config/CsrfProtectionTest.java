package com.empress.usermanagementapi.config;

import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.PasswordResetService;
import com.empress.usermanagementapi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests to verify CSRF protection is properly configured.
 * 
 * This test class verifies that:
 * 1. Form-based endpoints (login, register, forgot-password, reset-password) require CSRF tokens
 * 2. REST API endpoints (/users/*, /auth/*) do not require CSRF tokens (stateless)
 * 3. CSRF tokens are properly validated when present
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== Tests for form-based endpoints requiring CSRF =====

    @Test
    void testLoginRequiresCsrfToken() throws Exception {
        // Login without CSRF token should fail
        mockMvc.perform(post("/login")
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLoginWithCsrfTokenSucceeds() throws Exception {
        // Login with CSRF token should not be forbidden due to CSRF
        // It will redirect to login?error due to invalid credentials
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("password", "testpass"))
                .andExpect(status().is3xxRedirection()); // Redirects to /login?error
    }

    @Test
    void testRegisterRequiresCsrfToken() throws Exception {
        // Register without CSRF token should fail
        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRegisterWithCsrfTokenSucceeds() throws Exception {
        // Register with CSRF token should not be forbidden due to CSRF
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "new@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection()); // Will succeed and redirect
    }

    @Test
    void testForgotPasswordRequiresCsrfToken() throws Exception {
        // Forgot password without CSRF token should fail
        mockMvc.perform(post("/forgot-password")
                        .param("username", "testuser")
                        .param("email", "test@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testForgotPasswordWithCsrfTokenSucceeds() throws Exception {
        // Forgot password with CSRF token should not be forbidden due to CSRF
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("username", "testuser")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk()); // Will return 200 (shows form again)
    }

    @Test
    void testResetPasswordRequiresCsrfToken() throws Exception {
        // Reset password without CSRF token should fail
        mockMvc.perform(post("/reset-password")
                        .param("token", "sometoken")
                        .param("password", "newpassword"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testResetPasswordWithCsrfTokenSucceeds() throws Exception {
        // Reset password with CSRF token should not be forbidden due to CSRF
        // It will redirect or show form based on token validity
        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", "sometoken")
                        .param("password", "newpassword"))
                .andExpect(status().is3xxRedirection()); // Will redirect or show form
    }

    // ===== Tests for REST API endpoints that should NOT require CSRF =====

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRestApiPostDoesNotRequireCsrf() throws Exception {
        // REST API endpoint should work without CSRF token
        // The key test is that we don't get 403 Forbidden due to CSRF
        String userJson = objectMapper.writeValueAsString(
                new TestUserRequest("testuser", "test@example.com", "password123", "USER")
        );

        // Create a mock user to avoid NullPointerException
        com.empress.usermanagementapi.entity.User mockUser = new com.empress.usermanagementapi.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        // Mock the repository to return false for duplicates and a saved user
        org.mockito.Mockito.when(userRepository.existsByUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);
        org.mockito.Mockito.when(userRepository.existsByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(mockUser);

        // The request should not return 403 (CSRF forbidden)
        // It should succeed or return other error codes, but not 403
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Request returned 403 Forbidden - CSRF protection should be disabled for /users/*");
                    }
                });
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRestApiPutDoesNotRequireCsrf() throws Exception {
        // REST API PUT endpoint should work without CSRF token
        String updateJson = objectMapper.writeValueAsString(
                new TestEmailUpdate("newemail@example.com")
        );

        // Create a mock user to avoid NullPointerException
        com.empress.usermanagementapi.entity.User mockUser = new com.empress.usermanagementapi.entity.User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("old@example.com");

        // Mock the repository to return the user and allow save
        org.mockito.Mockito.when(userRepository.findByUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(mockUser);
        org.mockito.Mockito.when(userRepository.existsByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(mockUser);

        // The request should not return 403 (CSRF forbidden)
        // It should succeed with 200 or return other error codes, but not 403
        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Request returned 403 Forbidden - CSRF protection should be disabled for /users/*");
                    }
                });
    }

    // Helper classes for JSON serialization
    private static class TestUserRequest {
        public String username;
        public String email;
        public String password;
        public String role;

        public TestUserRequest(String username, String email, String password, String role) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.role = role;
        }
    }

    private static class TestEmailUpdate {
        public String email;

        public TestEmailUpdate(String email) {
            this.email = email;
        }
    }
}
