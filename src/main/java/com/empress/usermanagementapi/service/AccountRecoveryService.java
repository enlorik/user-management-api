package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Processes account recovery (forgot-password) requests asynchronously.
 *
 * All account-dependent work happens here, off the HTTP request thread:
 * account lookup, token creation and email delivery. Outcomes are logged
 * internally and never exposed to the caller, so the public response cannot
 * reveal whether an account exists or whether the email was sent.
 */
@Service
public class AccountRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(AccountRecoveryService.class);

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AccountRecoveryService(UserService userService,
                                  PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @Async("accountRecoveryExecutor")
    public void processResetRequest(String username, String email) {
        Optional<User> opt = userService.findByUsernameAndEmail(username, email);

        if (opt.isEmpty()) {
            log.info("Password reset requested for non-matching account - username: {}, email: {}",
                    username,
                    LoggingUtil.maskEmail(email));
            return;
        }

        try {
            passwordResetService.createTokenAndSendResetEmail(email);
            log.info("Password reset email sent - username: {}, email: {}",
                    username,
                    LoggingUtil.maskEmail(email));
        } catch (Exception e) {
            log.error("Failed to send password reset email - username: {}, email: {}",
                    username,
                    LoggingUtil.maskEmail(email),
                    e);
        }
    }
}
