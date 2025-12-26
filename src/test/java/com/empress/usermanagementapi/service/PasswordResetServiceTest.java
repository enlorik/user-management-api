package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PasswordResetService error messages.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PasswordResetServiceTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("oldpassword");
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testValidatePasswordResetToken_NullToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.validatePasswordResetToken(null);
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testValidatePasswordResetToken_EmptyToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.validatePasswordResetToken("");
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testValidatePasswordResetToken_InvalidToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.validatePasswordResetToken("invalid-token");
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testValidatePasswordResetToken_UsedToken_ReturnsUserFriendlyMessage() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("used-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(true);
        tokenRepository.save(token);

        String error = passwordResetService.validatePasswordResetToken("used-token");
        
        assertNotNull(error);
        assertEquals("This token has already been used. You must request a new password reset.", error);
    }

    @Test
    void testValidatePasswordResetToken_ExpiredToken_ReturnsUserFriendlyMessage() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = passwordResetService.validatePasswordResetToken("expired-token");
        
        assertNotNull(error);
        assertEquals("The reset password link has expired. Please request a new one.", error);
    }

    @Test
    void testValidatePasswordResetToken_ValidToken_ReturnsNull() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = passwordResetService.validatePasswordResetToken("valid-token");
        
        assertNull(error);
    }

    @Test
    void testResetPassword_NullToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.resetPassword(null, "newpassword");
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testResetPassword_EmptyToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.resetPassword("", "newpassword");
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testResetPassword_InvalidToken_ReturnsUserFriendlyMessage() {
        String error = passwordResetService.resetPassword("invalid-token", "newpassword");
        
        assertNotNull(error);
        assertEquals("The reset password token is invalid. Ensure you copied the entire link.", error);
    }

    @Test
    void testResetPassword_UsedToken_ReturnsUserFriendlyMessage() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("used-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(true);
        tokenRepository.save(token);

        String error = passwordResetService.resetPassword("used-token", "newpassword");
        
        assertNotNull(error);
        assertEquals("This token has already been used. You must request a new password reset.", error);
    }

    @Test
    void testResetPassword_ExpiredToken_ReturnsUserFriendlyMessage() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = passwordResetService.resetPassword("expired-token", "newpassword");
        
        assertNotNull(error);
        assertEquals("The reset password link has expired. Please request a new one.", error);
    }

    @Test
    void testResetPassword_ValidToken_ReturnsNullAndUpdatesPassword() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = passwordResetService.resetPassword("valid-token", "newpassword");
        
        assertNull(error);
        
        // Verify password was updated
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newpassword", updatedUser.getPassword()));
        
        // Verify token is marked as used
        PasswordResetToken updatedToken = tokenRepository.findByToken("valid-token").orElseThrow();
        assertTrue(updatedToken.isUsed());
    }
}
