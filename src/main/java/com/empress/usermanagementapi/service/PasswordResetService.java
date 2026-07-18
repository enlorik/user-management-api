package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.util.LoggingUtil;
import com.empress.usermanagementapi.util.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long TOKEN_EXPIRY_HOURS = 24; // change if you want longer/shorter

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenHasher tokenHasher;

    @Value("${app.base-url}")
    private String baseUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo,
                                UserRepository userRepo,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                TokenHasher tokenHasher) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenHasher = tokenHasher;
    }

    /**
     * Issues a password reset token for the given email.
     *
     * Only the SHA-256 hash of the token is persisted; the returned raw token
     * exists solely so the caller can place it in the reset link, and is never
     * stored or logged.
     *
     * @return the raw reset token to embed in the emailed link
     */
    public String createPasswordResetTokenForEmail(String email) {
        LoggingUtil.setActionType("PASSWORD_RESET_TOKEN_CREATE");
        log.info("Password reset token request - email: {}", LoggingUtil.maskEmail(email));
        
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Password reset failed - user not found for email: {}", 
                        LoggingUtil.maskEmail(email));
                LoggingUtil.clearActionType();
                return new IllegalArgumentException("No user with email: " + LoggingUtil.maskEmail(email));
            });

        LoggingUtil.setUserId(user.getId());
        log.info("Creating password reset token - userId: {}, username: {}", 
                user.getId(), user.getUsername());
        
        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        // Reuse existing row for this user if it exists, otherwise create a new one
        PasswordResetToken prt = tokenRepo.findByUser(user)
            .orElseGet(() -> {
                log.debug("Creating new password reset token - userId: {}", user.getId());
                PasswordResetToken t = new PasswordResetToken();
                t.setUser(user);
                return t;
            });

        prt.setTokenHash(tokenHasher.hash(rawToken));
        prt.setExpiryDate(expiry);
        prt.setUsed(false);

        tokenRepo.save(prt);
        log.info("Password reset token created successfully - userId: {}", user.getId());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
        return rawToken;
    }

    /**
     * Creates a password reset token and sends the reset email.
     *
     * Callers only need to validate who is allowed to request a reset; this service owns
     * the token creation and email-delivery workflow. Email delivery failures are allowed
     * to propagate so the controller can show the user that the reset email was not sent.
     */
    public void createTokenAndSendResetEmail(String email) {
        String rawToken = createPasswordResetTokenForEmail(email);
        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    public String validatePasswordResetToken(String token) {
        String cleanToken = token == null ? null : token.trim();

        if (cleanToken == null || cleanToken.isEmpty()) {
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        var opt = tokenRepo.findByTokenHash(tokenHasher.hash(cleanToken));

        if (opt.isEmpty()) {
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        PasswordResetToken prt = opt.get();

        if (prt.isUsed()) {
            return "This token has already been used. You must request a new password reset.";
        }

        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "The reset password link has expired. Please request a new one.";
        }

        // valid
        return null;
    }

    public String resetPassword(String token, String newPassword) {
        LoggingUtil.setActionType("PASSWORD_RESET");
        log.info("Password reset attempt - tokenLength: {}", token != null ? token.length() : 0);
        
        String cleanToken = token == null ? null : token.trim();

        if (cleanToken == null || cleanToken.isEmpty()) {
            log.warn("Password reset failed - invalid token format");
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        var opt = tokenRepo.findByTokenHash(tokenHasher.hash(cleanToken));

        if (opt.isEmpty()) {
            log.warn("Password reset failed - token not found");
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        PasswordResetToken prt = opt.get();
        User user = prt.getUser();
        LoggingUtil.setUserId(user.getId());

        if (prt.isUsed()) {
            log.warn("Password reset failed - token already used - userId: {}", user.getId());
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "This token has already been used. You must request a new password reset.";
        }

        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed - token expired - userId: {}", user.getId());
            LoggingUtil.clearActionType();
            LoggingUtil.clearUserId();
            return "The reset password link has expired. Please request a new one.";
        }

        // token is valid → change password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // mark token as used
        prt.setUsed(true);
        tokenRepo.save(prt);
        
        log.info("Password reset successful - userId: {}, username: {}", 
                user.getId(), user.getUsername());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();

        return null;
    }
}
