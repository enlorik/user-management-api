package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final long TOKEN_EXPIRY_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepo,
                                    UserRepository userRepo,
                                    EmailService emailService) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
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
     * Creates a verification token and sends the verification email.
     *
     * The account is already created at this point, so email delivery failures are logged
     * without rolling back registration. This preserves the existing registration behavior
     * while keeping email orchestration inside the service layer.
     */
    public void createTokenAndSendVerificationEmail(User user) {
        String token = createTokenForUser(user);
        log.debug("Email verification token created - userId: {}", user.getId());

        String verifyLink = baseUrl + "/verify-email?token=" + token;

        try {
            emailService.sendVerificationEmail(user.getEmail(), verifyLink);
            log.info("Verification email sent - userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email - userId: {}, error: {}",
                    user.getId(),
                    e.getMessage());
        }
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
