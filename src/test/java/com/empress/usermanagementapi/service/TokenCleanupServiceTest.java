package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
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
 * Test class for TokenCleanupService.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TokenCleanupServiceTest {

    @Autowired
    private TokenCleanupService tokenCleanupService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing tokens
        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create and save a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testCleanupExpiredEmailVerificationTokens() {
        // Create a second user for the valid token
        User validUser = new User();
        validUser.setUsername("validuser");
        validUser.setEmail("valid@example.com");
        validUser.setPassword("password");
        validUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        validUser = userRepository.save(validUser);

        // Create expired token
        EmailVerificationToken expiredToken = new EmailVerificationToken();
        expiredToken.setToken("expired-token");
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        emailVerificationTokenRepository.save(expiredToken);

        // Create non-expired token
        EmailVerificationToken validToken = new EmailVerificationToken();
        validToken.setToken("valid-token");
        validToken.setUser(validUser);
        validToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        emailVerificationTokenRepository.save(validToken);

        // Verify both tokens exist
        assertEquals(2, emailVerificationTokenRepository.count());

        // Run cleanup
        int deletedCount = tokenCleanupService.cleanupExpiredEmailVerificationTokens(LocalDateTime.now());

        // Verify only expired token was deleted
        assertEquals(1, deletedCount);
        assertEquals(1, emailVerificationTokenRepository.count());
        assertTrue(emailVerificationTokenRepository.findByToken("valid-token").isPresent());
        assertFalse(emailVerificationTokenRepository.findByToken("expired-token").isPresent());
    }

    @Test
    void testCleanupExpiredPasswordResetTokens() {
        // Create a second user for the valid token
        User validUser = new User();
        validUser.setUsername("validuser2");
        validUser.setEmail("valid2@example.com");
        validUser.setPassword("password");
        validUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        validUser = userRepository.save(validUser);

        // Create expired token
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken("expired-reset-token");
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        passwordResetTokenRepository.save(expiredToken);

        // Create non-expired token
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setToken("valid-reset-token");
        validToken.setUser(validUser);
        validToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        passwordResetTokenRepository.save(validToken);

        // Verify both tokens exist
        assertEquals(2, passwordResetTokenRepository.count());

        // Run cleanup
        int deletedCount = tokenCleanupService.cleanupExpiredPasswordResetTokens(LocalDateTime.now());

        // Verify only expired token was deleted
        assertEquals(1, deletedCount);
        assertEquals(1, passwordResetTokenRepository.count());
        assertTrue(passwordResetTokenRepository.findByToken("valid-reset-token").isPresent());
        assertFalse(passwordResetTokenRepository.findByToken("expired-reset-token").isPresent());
    }

    @Test
    void testCleanupWhenNoExpiredTokens() {
        // Create additional users for the tokens
        User emailUser = new User();
        emailUser.setUsername("emailuser");
        emailUser.setEmail("email@example.com");
        emailUser.setPassword("password");
        emailUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        emailUser = userRepository.save(emailUser);

        User passwordUser = new User();
        passwordUser.setUsername("passworduser");
        passwordUser.setEmail("password@example.com");
        passwordUser.setPassword("password");
        passwordUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        passwordUser = userRepository.save(passwordUser);

        // Create only non-expired tokens
        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setToken("valid-email-token");
        emailToken.setUser(emailUser);
        emailToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        emailVerificationTokenRepository.save(emailToken);

        PasswordResetToken passwordToken = new PasswordResetToken();
        passwordToken.setToken("valid-password-token");
        passwordToken.setUser(passwordUser);
        passwordToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        passwordResetTokenRepository.save(passwordToken);

        // Run cleanup
        int emailDeleted = tokenCleanupService.cleanupExpiredEmailVerificationTokens(LocalDateTime.now());
        int passwordDeleted = tokenCleanupService.cleanupExpiredPasswordResetTokens(LocalDateTime.now());

        // Verify no tokens were deleted
        assertEquals(0, emailDeleted);
        assertEquals(0, passwordDeleted);
        assertEquals(1, emailVerificationTokenRepository.count());
        assertEquals(1, passwordResetTokenRepository.count());
    }

    @Test
    void testCleanupExpiredTokensIntegration() {
        // Create additional users for the valid tokens
        User validEmailUser = new User();
        validEmailUser.setUsername("validemail");
        validEmailUser.setEmail("validemail@example.com");
        validEmailUser.setPassword("password");
        validEmailUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        validEmailUser = userRepository.save(validEmailUser);

        User expiredEmailUser = new User();
        expiredEmailUser.setUsername("expiredemail");
        expiredEmailUser.setEmail("expiredemail@example.com");
        expiredEmailUser.setPassword("password");
        expiredEmailUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        expiredEmailUser = userRepository.save(expiredEmailUser);

        User validPasswordUser = new User();
        validPasswordUser.setUsername("validpassword");
        validPasswordUser.setEmail("validpassword@example.com");
        validPasswordUser.setPassword("password");
        validPasswordUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        validPasswordUser = userRepository.save(validPasswordUser);

        User expiredPasswordUser = new User();
        expiredPasswordUser.setUsername("expiredpassword");
        expiredPasswordUser.setEmail("expiredpassword@example.com");
        expiredPasswordUser.setPassword("password");
        expiredPasswordUser.setRole(com.empress.usermanagementapi.entity.Role.USER);
        expiredPasswordUser = userRepository.save(expiredPasswordUser);

        // Create a mix of expired and valid tokens
        EmailVerificationToken expiredEmail = new EmailVerificationToken();
        expiredEmail.setToken("expired-email");
        expiredEmail.setUser(expiredEmailUser);
        expiredEmail.setExpiryDate(LocalDateTime.now().minusHours(1));
        emailVerificationTokenRepository.save(expiredEmail);

        PasswordResetToken expiredPassword = new PasswordResetToken();
        expiredPassword.setToken("expired-password");
        expiredPassword.setUser(expiredPasswordUser);
        expiredPassword.setExpiryDate(LocalDateTime.now().minusHours(2));
        passwordResetTokenRepository.save(expiredPassword);

        EmailVerificationToken validEmail = new EmailVerificationToken();
        validEmail.setToken("valid-email");
        validEmail.setUser(validEmailUser);
        validEmail.setExpiryDate(LocalDateTime.now().plusHours(1));
        emailVerificationTokenRepository.save(validEmail);

        PasswordResetToken validPassword = new PasswordResetToken();
        validPassword.setToken("valid-password");
        validPassword.setUser(validPasswordUser);
        validPassword.setExpiryDate(LocalDateTime.now().plusHours(2));
        passwordResetTokenRepository.save(validPassword);

        // Run the full cleanup
        tokenCleanupService.cleanupExpiredTokens();

        // Verify only valid tokens remain
        assertEquals(1, emailVerificationTokenRepository.count());
        assertEquals(1, passwordResetTokenRepository.count());
        assertTrue(emailVerificationTokenRepository.findByToken("valid-email").isPresent());
        assertTrue(passwordResetTokenRepository.findByToken("valid-password").isPresent());
    }
}
