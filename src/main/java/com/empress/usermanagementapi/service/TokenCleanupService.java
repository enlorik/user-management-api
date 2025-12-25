package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public TokenCleanupService(
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Scheduled task to clean up expired tokens.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired tokens");
        
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // Clean up expired email verification tokens
            long emailTokensDeleted = cleanupExpiredEmailVerificationTokens(now);
            logger.info("Deleted {} expired email verification tokens", emailTokensDeleted);
            
            // Clean up expired password reset tokens
            long passwordTokensDeleted = cleanupExpiredPasswordResetTokens(now);
            logger.info("Deleted {} expired password reset tokens", passwordTokensDeleted);
            
            logger.info("Token cleanup completed successfully. Total tokens deleted: {}", 
                    emailTokensDeleted + passwordTokensDeleted);
        } catch (Exception e) {
            logger.error("Error during token cleanup", e);
        }
    }

    /**
     * Deletes expired email verification tokens.
     * 
     * @param currentTime the current time to compare against expiry dates
     * @return the number of tokens deleted
     */
    public long cleanupExpiredEmailVerificationTokens(LocalDateTime currentTime) {
        return emailVerificationTokenRepository.deleteByExpiryDateBefore(currentTime);
    }

    /**
     * Deletes expired password reset tokens.
     * 
     * @param currentTime the current time to compare against expiry dates
     * @return the number of tokens deleted
     */
    public long cleanupExpiredPasswordResetTokens(LocalDateTime currentTime) {
        return passwordResetTokenRepository.deleteByExpiryDateBefore(currentTime);
    }
}
