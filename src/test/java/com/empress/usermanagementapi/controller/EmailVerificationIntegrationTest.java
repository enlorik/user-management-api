package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        // Mock email service
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Create a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        testUser.setRole(Role.USER);
        testUser.setVerified(false);
        testUser = userRepository.save(testUser);

        // Create a valid verification token
        validToken = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(testUser);
        token.setToken(validToken);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);
        tokenRepository.save(token);
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyUser() throws Exception {
        // Act
        mockMvc.perform(get("/verify-email")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-result"))
                .andExpect(model().attribute("success", "Your email has been verified. You can now log in."))
                .andExpect(model().attributeDoesNotExist("error"));

        // Assert - user should be verified
        User verifiedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(verifiedUser);
        assertTrue(verifiedUser.isVerified(), "User should be verified");

        // Assert - token should be marked as used
        EmailVerificationToken usedToken = tokenRepository.findByToken(validToken).orElse(null);
        assertNotNull(usedToken);
        assertTrue(usedToken.isUsed(), "Token should be marked as used");
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldReturnError() throws Exception {
        // Arrange
        String invalidToken = "invalid-token-xyz";

        // Act & Assert
        mockMvc.perform(get("/verify-email")
                .param("token", invalidToken))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-result"))
                .andExpect(model().attribute("error", "Invalid verification link."))
                .andExpect(model().attributeDoesNotExist("success"));

        // Verify user is still not verified
        User user = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(user);
        assertFalse(user.isVerified(), "User should remain unverified");
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldReturnError() throws Exception {
        // Arrange - create an expired token for testUser
        String expiredToken = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(testUser);
        token.setToken(expiredToken);
        token.setExpiryDate(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        token.setUsed(false);
        
        // Delete existing token first due to OneToOne constraint
        tokenRepository.findByUser(testUser).ifPresent(t -> tokenRepository.delete(t));
        tokenRepository.save(token);

        // Act & Assert
        mockMvc.perform(get("/verify-email")
                .param("token", expiredToken))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-result"))
                .andExpect(model().attribute("error", "This verification link has expired."))
                .andExpect(model().attributeDoesNotExist("success"));

        // Verify user is still not verified
        User user = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(user);
        assertFalse(user.isVerified(), "User should remain unverified");
    }

    @Test
    void verifyEmail_WithAlreadyUsedToken_ShouldReturnError() throws Exception {
        // Arrange - mark token as used
        EmailVerificationToken token = tokenRepository.findByToken(validToken).orElse(null);
        assertNotNull(token);
        token.setUsed(true);
        tokenRepository.save(token);

        // Act & Assert
        mockMvc.perform(get("/verify-email")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-result"))
                .andExpect(model().attribute("error", "This verification link has already been used."))
                .andExpect(model().attributeDoesNotExist("success"));
    }

    @Test
    void endToEndFlow_RegisterAndVerify_ShouldWork() throws Exception {
        // Arrange - clean setup for end-to-end test
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        String username = "e2euser";
        String email = "e2e@example.com";
        String password = "Password123!";

        // Step 1: Register user
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify user created but not verified
        User registeredUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(registeredUser);
        assertFalse(registeredUser.isVerified(), "User should not be verified initially");

        // Get the verification token
        EmailVerificationToken token = tokenRepository.findByUser(registeredUser).orElse(null);
        assertNotNull(token);
        String tokenValue = token.getToken();

        // Step 2: Verify email
        mockMvc.perform(get("/verify-email")
                .param("token", tokenValue))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email-result"))
                .andExpect(model().attribute("success", "Your email has been verified. You can now log in."));

        // Verify user is now verified
        User verifiedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(verifiedUser);
        assertTrue(verifiedUser.isVerified(), "User should be verified after verification");
    }

    @Test
    void verifyEmail_MultipleTimes_ShouldOnlyWorkOnce() throws Exception {
        // First verification - should succeed
        mockMvc.perform(get("/verify-email")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(model().attribute("success", "Your email has been verified. You can now log in."));

        // Second verification - should fail
        mockMvc.perform(get("/verify-email")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "This verification link has already been used."));

        // User should still be verified
        User user = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(user);
        assertTrue(user.isVerified());
    }
}
