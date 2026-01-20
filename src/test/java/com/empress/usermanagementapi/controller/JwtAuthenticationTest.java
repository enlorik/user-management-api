package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.dto.AuthenticationRequest;
import com.empress.usermanagementapi.dto.AuthenticationResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for JWT authentication endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testLogin_ValidCredentials_ReturnsJwtToken() throws Exception {
        // Create a test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Prepare login request
        AuthenticationRequest request = new AuthenticationRequest("testuser", "Password123!");

        // Perform login
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AuthenticationResponse response = objectMapper.readValue(responseBody, AuthenticationResponse.class);

        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
    }

    @Test
    void testLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Create a test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Prepare login request with wrong password
        AuthenticationRequest request = new AuthenticationRequest("testuser", "WrongPassword!");

        // Perform login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testLogin_NonExistentUser_ReturnsUnauthorized() throws Exception {
        // Prepare login request for non-existent user
        AuthenticationRequest request = new AuthenticationRequest("nonexistent", "Password123!");

        // Perform login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testLogin_UnverifiedUser_ReturnsUnauthorized() throws Exception {
        // Create an unverified test user
        User user = new User();
        user.setUsername("unverified");
        user.setEmail("unverified@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(false);  // Not verified
        userRepository.save(user);

        // Prepare login request
        AuthenticationRequest request = new AuthenticationRequest("unverified", "Password123!");

        // Perform login - should fail due to unverified account
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpoint_WithValidToken_Succeeds() throws Exception {
        // Create a test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Login to get token
        AuthenticationRequest loginRequest = new AuthenticationRequest("testuser", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthenticationResponse authResponse = objectMapper.readValue(loginResponseBody, AuthenticationResponse.class);
        String token = authResponse.getToken();

        // Access protected endpoint with token
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testAccessProtectedEndpoint_WithoutToken_ReturnsUnauthorized() throws Exception {
        // Access protected endpoint without token
        mockMvc.perform(get("/users/me"))
                .andExpect(status().is3xxRedirection());  // Redirects to login for form-based
    }

    @Test
    void testAccessProtectedEndpoint_WithInvalidToken_ReturnsUnauthorized() throws Exception {
        // Access protected endpoint with invalid token
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().is3xxRedirection());  // Redirects to login
    }

    @Test
    void testAdminEndpoint_WithUserToken_ReturnsForbidden() throws Exception {
        // Create a regular user
        User user = new User();
        user.setUsername("regularuser");
        user.setEmail("regular@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Login to get token
        AuthenticationRequest loginRequest = new AuthenticationRequest("regularuser", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthenticationResponse authResponse = objectMapper.readValue(loginResponseBody, AuthenticationResponse.class);
        String token = authResponse.getToken();

        // Try to access admin endpoint with user token
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminEndpoint_WithAdminToken_Succeeds() throws Exception {
        // Create an admin user
        User admin = new User();
        admin.setUsername("adminuser");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Password123!"));
        admin.setRole(Role.ADMIN);
        admin.setVerified(true);
        userRepository.save(admin);

        // Login to get token
        AuthenticationRequest loginRequest = new AuthenticationRequest("adminuser", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthenticationResponse authResponse = objectMapper.readValue(loginResponseBody, AuthenticationResponse.class);
        String token = authResponse.getToken();

        // Access admin endpoint with admin token
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateMe_WithValidToken_Succeeds() throws Exception {
        // Create a test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Login to get token
        AuthenticationRequest loginRequest = new AuthenticationRequest("testuser", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthenticationResponse authResponse = objectMapper.readValue(loginResponseBody, AuthenticationResponse.class);
        String token = authResponse.getToken();

        // Update own email
        String updateRequest = "{\"email\": \"newemail@example.com\"}";
        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newemail@example.com"));
    }

    @Test
    void testLogin_EmptyUsername_ReturnsBadRequest() throws Exception {
        // Prepare login request with empty username
        AuthenticationRequest request = new AuthenticationRequest("", "Password123!");

        // Perform login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Prepare login request with empty password
        AuthenticationRequest request = new AuthenticationRequest("testuser", "");

        // Perform login
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
