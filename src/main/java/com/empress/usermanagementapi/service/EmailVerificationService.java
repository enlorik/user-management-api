package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final long TOKEN_EXPIRY_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepo,
                                    UserRepository userRepo) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
    }

    // create or refresh a token for this user and return the token string
    public String createTokenForUser(User user) {
        LoggingUtil.setActionType("EMAIL_VERIFICATION_TOKEN_CREATE");
        LoggingUtil.setUserId(user.getId());
        log.info("Creating email verification token - userId: {}, username: {}", 
                user.getId(), user.getUsername());
        
        EmailVerificationToken existing =
                tokenRepo.findByUser(user).orElse(null);

        String newTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        if (existing != null) {
            log.debug("Refreshing existing email verification token - userId: {}", user.getId());
            existing.setToken(newTokenValue);
            existing.setExpiryDate(expiry);
            existing.setUsed(false);
            tokenRepo.save(existing);
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return newTokenValue;
        }

        log.debug("Creating new email verification token - userId: {}", user.getId());
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(newTokenValue);
        token.setExpiryDate(expiry);
        token.setUsed(false);

        tokenRepo.save(token);
        log.info("Email verification token created successfully - userId: {}", user.getId());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
        return newTokenValue;
    }

    /**
     * @return null on success, or an error message on failure
     */
    public String verifyToken(String tokenValue) {
        LoggingUtil.setActionType("EMAIL_VERIFICATION");
        log.info("Email verification attempt - tokenLength: {}", 
                tokenValue != null ? tokenValue.length() : 0);
        
        var opt = tokenRepo.findByToken(tokenValue);
        if (opt.isEmpty()) {
            log.warn("Email verification failed - invalid token");
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "The provided verification link is invalid. Please check the link or request a new one.";
        }

        EmailVerificationToken token = opt.get();
        User user = token.getUser();
        LoggingUtil.setUserId(user.getId());

        if (token.isUsed()) {
            log.warn("Email verification failed - token already used - userId: {}", user.getId());
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "This verification link has already been used. You cannot use it again.";
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Email verification failed - token expired - userId: {}", user.getId());
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "The verification link has expired. Please request a new verification link.";
        }

        user.setVerified(true);
        userRepo.save(user);

        token.setUsed(true);
        tokenRepo.save(token);
        
        log.info("Email verified successfully - userId: {}, username: {}", 
                user.getId(), user.getUsername());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();

        return null; // success
    }
}
