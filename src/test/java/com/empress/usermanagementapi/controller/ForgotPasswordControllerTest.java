package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.service.AccountRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

    private static final String GENERIC_MESSAGE =
            "If an account with those details exists, a reset link has been sent.";

    @Mock
    private AccountRecoveryService accountRecoveryService;

    @Mock
    private Model model;

    private ForgotPasswordController controller;

    @BeforeEach
    void setUp() {
        controller = new ForgotPasswordController(accountRecoveryService);
    }

    @Test
    void handleForm_WhenUsernameBlank_ReturnsValidationError() {
        String view = controller.handleForm("   ", "user@example.com", model);

        assertEquals("forgot-password", view);
        verify(model).addAttribute("error", "Username and email are required.");
        verify(model, never()).addAttribute(eq("success"), any());
        verify(accountRecoveryService, never()).processResetRequest(anyString(), anyString());
    }

    @Test
    void handleForm_WhenEmailBlank_ReturnsValidationError() {
        String view = controller.handleForm("user", "   ", model);

        assertEquals("forgot-password", view);
        verify(model).addAttribute("error", "Username and email are required.");
        verify(model, never()).addAttribute(eq("success"), any());
        verify(accountRecoveryService, never()).processResetRequest(anyString(), anyString());
    }

    @Test
    void handleForm_WhenInputValid_DelegatesOnceAndReturnsGenericSuccess() {
        String view = controller.handleForm(" user ", " user@example.com ", model);

        assertEquals("forgot-password", view);
        verify(accountRecoveryService, times(1)).processResetRequest("user", "user@example.com");
        // The controller must not perform any account lookup itself; its only
        // collaborator interaction is the single asynchronous submission.
        verifyNoMoreInteractions(accountRecoveryService);
        verify(model).addAttribute("success", GENERIC_MESSAGE);
        verify(model, never()).addAttribute(eq("error"), any());
    }

    @Test
    void handleForm_WhenSubmissionRejected_StillReturnsGenericSuccess() {
        doThrow(new TaskRejectedException("executor saturated"))
                .when(accountRecoveryService)
                .processResetRequest("user", "user@example.com");

        String view = controller.handleForm("user", "user@example.com", model);

        assertEquals("forgot-password", view);
        verify(model).addAttribute("success", GENERIC_MESSAGE);
        verify(model, never()).addAttribute(eq("error"), any());
    }
}
