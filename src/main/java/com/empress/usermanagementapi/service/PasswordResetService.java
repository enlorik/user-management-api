package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public PasswordResetService(PasswordResetTokenRepository tokenRepo,
                                UserRepository userRepo,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordResetToken createPasswordResetTokenForEmail(String email) {
        LoggingUtil.setActionType("PASSWORD_RESET_TOKEN_CREATE");
        log.info("Password reset token request - email: {}", LoggingUtil.maskEmail(email));
        
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Password reset failed - user not found for email: {}", 
                        LoggingUtil.maskEmail(email));
                LoggingUtil.clearActionType();
                return new IllegalArgumentException("No user with email: " + email);
            });

        LoggingUtil.setUserId(user.getId());
        log.info("Creating password reset token - userId: {}, username: {}", 
                user.getId(), user.getUsername());
        
        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        // Reuse existing row for this user if it exists, otherwise create a new one
        PasswordResetToken prt = tokenRepo.findByUser(user)
            .orElseGet(() -> {
                log.debug("Creating new password reset token - userId: {}", user.getId());
                PasswordResetToken t = new PasswordResetToken();
                t.setUser(user);
                return t;
            });

        prt.setToken(token);
        prt.setExpiryDate(expiry);
        prt.setUsed(false);

        PasswordResetToken saved = tokenRepo.save(prt);
        log.info("Password reset token created successfully - userId: {}", user.getId());
        LoggingUtil.clearActionType();
        LoggingUtil.clearUserId();
        return saved;
    }

    public String validatePasswordResetToken(String token) {
        String cleanToken = token == null ? null : token.trim();

        if (cleanToken == null || cleanToken.isEmpty()) {
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        var opt = tokenRepo.findByToken(cleanToken);

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
            return "The reset password token is invalid. Ensure you copied the entire link.";
        }

        var opt = tokenRepo.findByToken(cleanToken);

        if (opt.isEmpty()) {
            log.warn("Password reset failed - token not found");
            LoggingUtil.clearActionType();
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
