package com.empress.usermanagementapi.entity;

import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify database schema compatibility after refactoring token entities.
 * This ensures that:
 * 1. Token entities still persist correctly
 * 2. All fields are properly mapped to database columns
 * 3. Inheritance structure doesn't break existing functionality
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BaseTokenEntitySchemaTest {

    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testEmailVerificationTokenInheritance() {
        // Create and save a user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        // Create and save email verification token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("test-token-123");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(false);
        
        EmailVerificationToken saved = emailTokenRepository.save(token);

        // Verify all inherited fields are properly persisted
        assertNotNull(saved.getId());
        assertEquals("test-token-123", saved.getToken());
        assertEquals(user.getId(), saved.getUser().getId());
        assertNotNull(saved.getExpiryDate());
        assertFalse(saved.isUsed());

        // Verify the token can be retrieved
        EmailVerificationToken retrieved = emailTokenRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(saved.getToken(), retrieved.getToken());
    }

    @Test
    void testPasswordResetTokenInheritance() {
        // Create and save a user
        User user = new User();
        user.setUsername("resetuser");
        user.setEmail("reset@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        // Create and save password reset token using constructor
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);
        PasswordResetToken token = new PasswordResetToken("reset-token-456", user, expiry);
        
        PasswordResetToken saved = passwordTokenRepository.save(token);

        // Verify all inherited fields are properly persisted
        assertNotNull(saved.getId());
        assertEquals("reset-token-456", saved.getToken());
        assertEquals(user.getId(), saved.getUser().getId());
        assertNotNull(saved.getExpiryDate());
        assertFalse(saved.isUsed());

        // Verify the token can be retrieved
        PasswordResetToken retrieved = passwordTokenRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(saved.getToken(), retrieved.getToken());
    }

    @Test
    void testTokenUpdate() {
        // Create and save a user
        User user = new User();
        user.setUsername("updateuser");
        user.setEmail("update@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        // Create and save token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("original-token");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));
        token.setUsed(false);
        
        EmailVerificationToken saved = emailTokenRepository.save(token);
        Long tokenId = saved.getId();

        // Update the token
        saved.setToken("updated-token");
        saved.setUsed(true);
        emailTokenRepository.save(saved);

        // Verify update worked
        EmailVerificationToken updated = emailTokenRepository.findById(tokenId).orElse(null);
        assertNotNull(updated);
        assertEquals("updated-token", updated.getToken());
        assertTrue(updated.isUsed());
    }

    @Test
    void testMultipleTokensForDifferentUsers() {
        // Create two users
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password");
        user1.setRole(Role.USER);
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword("password");
        user2.setRole(Role.USER);
        user2 = userRepository.save(user2);

        // Create tokens for both users
        EmailVerificationToken token1 = new EmailVerificationToken();
        token1.setToken("token-user1");
        token1.setUser(user1);
        token1.setExpiryDate(LocalDateTime.now().plusDays(1));
        emailTokenRepository.save(token1);

        PasswordResetToken token2 = new PasswordResetToken("token-user2", user2, LocalDateTime.now().plusDays(1));
        passwordTokenRepository.save(token2);

        // Verify both tokens exist independently
        assertEquals(1, emailTokenRepository.count());
        assertEquals(1, passwordTokenRepository.count());
        
        EmailVerificationToken retrievedEmail = emailTokenRepository.findByToken("token-user1").orElse(null);
        assertNotNull(retrievedEmail);
        assertEquals(user1.getId(), retrievedEmail.getUser().getId());

        PasswordResetToken retrievedPassword = passwordTokenRepository.findByToken("token-user2").orElse(null);
        assertNotNull(retrievedPassword);
        assertEquals(user2.getId(), retrievedPassword.getUser().getId());
    }
}
