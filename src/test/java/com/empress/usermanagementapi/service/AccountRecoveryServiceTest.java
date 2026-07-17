package com.empress.usermanagementapi.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.util.LoggingUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryServiceTest {

    private static final String USERNAME = "user";
    private static final String EMAIL = "user@example.com";

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetService passwordResetService;

    private AccountRecoveryService service;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        service = new AccountRecoveryService(userService, passwordResetService);

        serviceLogger = (Logger) LoggerFactory.getLogger(AccountRecoveryService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    @Test
    void processResetRequest_WhenNoAccountMatches_DoesNotCallPasswordResetService() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.empty());

        service.processResetRequest(USERNAME, EMAIL);

        verify(passwordResetService, never()).createTokenAndSendResetEmail(anyString());
    }

    @Test
    void processResetRequest_WhenAccountMatches_CallsPasswordResetService() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.of(new User()));

        service.processResetRequest(USERNAME, EMAIL);

        verify(passwordResetService).createTokenAndSendResetEmail(EMAIL);
    }

    @Test
    void processResetRequest_WhenDeliveryFails_ExceptionDoesNotEscape() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("email delivery failed"))
                .when(passwordResetService)
                .createTokenAndSendResetEmail(EMAIL);

        assertDoesNotThrow(() -> service.processResetRequest(USERNAME, EMAIL));
    }

    @Test
    void processResetRequest_LogsMaskedEmailForNonMatchingAccount() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.empty());

        service.processResetRequest(USERNAME, EMAIL);

        assertFalse(logAppender.list.isEmpty(), "expected an internal log entry");
        for (ILoggingEvent event : logAppender.list) {
            assertFalse(event.getFormattedMessage().contains(EMAIL),
                    "raw email must not appear in logs: " + event.getFormattedMessage());
        }
        assertTrue(logAppender.list.stream()
                        .anyMatch(e -> e.getFormattedMessage().contains("u**r@e***m")),
                "masked email should appear in logs");
    }

    @Test
    void processResetRequest_LogsMaskedEmailWhenDeliveryFails() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.of(new User()));
        doThrow(new RuntimeException("email delivery failed"))
                .when(passwordResetService)
                .createTokenAndSendResetEmail(EMAIL);

        service.processResetRequest(USERNAME, EMAIL);

        assertFalse(logAppender.list.isEmpty(), "expected an internal log entry");
        for (ILoggingEvent event : logAppender.list) {
            assertFalse(event.getFormattedMessage().contains(EMAIL),
                    "raw email must not appear in logs: " + event.getFormattedMessage());
        }
    }

    @Test
    void processResetRequest_ClearsMdcWhenDeliveryFailsMidway() {
        when(userService.findByUsernameAndEmail(USERNAME, EMAIL))
                .thenReturn(Optional.of(new User()));
        doAnswer(invocation -> {
            // Simulate a failure after downstream code has populated MDC but
            // before it could clean up, as when the token save throws.
            LoggingUtil.setActionType("PASSWORD_RESET_TOKEN_CREATE");
            LoggingUtil.setUserId(42L);
            throw new RuntimeException("token save failed");
        }).when(passwordResetService).createTokenAndSendResetEmail(EMAIL);

        service.processResetRequest(USERNAME, EMAIL);

        Map<String, String> mdc = MDC.getCopyOfContextMap();
        assertTrue(mdc == null || mdc.isEmpty(),
                "MDC must be cleared so pooled executor threads do not leak user context, but was: " + mdc);
    }
}
