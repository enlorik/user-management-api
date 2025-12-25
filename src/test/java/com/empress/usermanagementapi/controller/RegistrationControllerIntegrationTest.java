package com.empress.usermanagementapi.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        
        // Mock email service to prevent actual email sending
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerUser_WithValidData_ShouldCreateUserInDatabase() throws Exception {
        // Arrange
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "Password123!";

        // Act
        mockMvc.perform(post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verifyEmail"));

        // Assert - verify user was created in database
        User savedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(savedUser, "User should be saved in database");
        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertEquals(Role.USER, savedUser.getRole());
        assertFalse(savedUser.isVerified(), "User should not be verified initially");
        assertNotEquals(password, savedUser.getPassword(), "Password should be encoded");
        
        // Verify email was sent
        verify(emailService).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    void registerUser_WithDuplicateUsername_ShouldReturnError() throws Exception {
        // Arrange - create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("encoded-password");
        existingUser.setRole(Role.USER);
        userRepository.save(existingUser);

        // Act & Assert
        mockMvc.perform(post("/register")
                .param("username", "existinguser")
                .param("email", "newemail@example.com")
                .param("password", "Password123!")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("usernameError", "Username already in use"));

        // Verify no new user was created
        assertEquals(1, userRepository.count());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerUser_WithDuplicateEmail_ShouldReturnError() throws Exception {
        // Arrange - create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("encoded-password");
        existingUser.setRole(Role.USER);
        userRepository.save(existingUser);

        // Act & Assert
        mockMvc.perform(post("/register")
                .param("username", "newusername")
                .param("email", "existing@example.com")
                .param("password", "Password123!")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("emailError", "Email already in use"));

        // Verify no new user was created
        assertEquals(1, userRepository.count());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerUser_ShouldCreateVerificationToken() throws Exception {
        // Arrange
        String username = "tokenuser";
        String email = "tokenuser@example.com";
        String password = "Password123!";

        // Act
        mockMvc.perform(post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Assert - verify verification token was created
        User savedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(savedUser);
        
        var token = tokenRepository.findByUser(savedUser);
        assertTrue(token.isPresent(), "Verification token should be created");
        assertNotNull(token.get().getToken());
        assertNotNull(token.get().getExpiryDate());
        assertFalse(token.get().isUsed());
    }

    @Test
    void registerUser_WhenEmailServiceFails_ShouldStillCreateUser() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Email service down"))
                .when(emailService).sendVerificationEmail(anyString(), anyString());

        String username = "emailfailuser";
        String email = "emailfail@example.com";
        String password = "Password123!";

        // Act
        mockMvc.perform(post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verifyEmail"));

        // Assert - user should still be created even if email fails
        User savedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(savedUser, "User should be created even when email fails");
        assertEquals(username, savedUser.getUsername());
    }

    @Test
    void registerMultipleUsers_ShouldAllBeStoredInDatabase() throws Exception {
        // Act - register multiple users
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/register")
                    .param("username", "user" + i)
                    .param("email", "user" + i + "@example.com")
                    .param("password", "Password123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        // Assert
        assertEquals(3, userRepository.count(), "All three users should be in database");
        
        // Verify each user exists
        for (int i = 1; i <= 3; i++) {
            User user = userRepository.findByEmail("user" + i + "@example.com").orElse(null);
            assertNotNull(user);
            assertEquals("user" + i, user.getUsername());
            assertEquals(Role.USER, user.getRole());
        }
    }
}
