package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for EmailVerificationService error messages.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=localhost",
    "spring.mail.port=3025"
})
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testVerifyToken_InvalidToken_ReturnsUserFriendlyMessage() {
        String error = emailVerificationService.verifyToken("invalid-token");
        
        assertNotNull(error);
        assertEquals("The provided verification link is invalid. Please check the link or request a new one.", error);
    }

    @Test
    void testVerifyToken_UsedToken_ReturnsUserFriendlyMessage() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("used-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(true);
        tokenRepository.save(token);

        String error = emailVerificationService.verifyToken("used-token");
        
        assertNotNull(error);
        assertEquals("This verification link has already been used. You cannot use it again.", error);
    }

    @Test
    void testVerifyToken_ExpiredToken_ReturnsUserFriendlyMessage() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("expired-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = emailVerificationService.verifyToken("expired-token");
        
        assertNotNull(error);
        assertEquals("The verification link has expired. Please request a new verification link.", error);
    }

    @Test
    void testVerifyToken_ValidToken_ReturnsNull() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(false);
        tokenRepository.save(token);

        String error = emailVerificationService.verifyToken("valid-token");
        
        assertNull(error);
        
        // Verify user is marked as verified
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(updatedUser.isVerified());
        
        // Verify token is marked as used
        EmailVerificationToken updatedToken = tokenRepository.findByToken("valid-token").orElseThrow();
        assertTrue(updatedToken.isUsed());
    }
}
