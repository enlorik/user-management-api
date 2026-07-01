package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.PasswordResetService;
import com.empress.usermanagementapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

    private static final String GENERIC_MESSAGE =
            "If an account with those details exists, a reset link has been sent.";

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private Model model;

    private ForgotPasswordController controller;

    @BeforeEach
    void setUp() {
        controller = new ForgotPasswordController(userService, passwordResetService);
    }

    @Test
    void handleForm_WhenNoAccountMatches_ReturnsGenericSuccessWithoutSendingEmail() {
        when(userService.findByUsernameAndEmail("missing", "missing@example.com"))
                .thenReturn(Optional.empty());

        String view = controller.handleForm(" missing ", " missing@example.com ", model);

        assertEquals("forgot-password", view);
        verify(model).addAttribute("success", GENERIC_MESSAGE);
        verify(model, never()).addAttribute(eq("error"), any());
        verify(passwordResetService, never()).createTokenAndSendResetEmail(anyString());
    }

    @Test
    void handleForm_WhenAccountMatches_SendsEmailAndReturnsGenericSuccess() {
        User user = new User();
        when(userService.findByUsernameAndEmail("user", "user@example.com"))
                .thenReturn(Optional.of(user));

        String view = controller.handleForm(" user ", " user@example.com ", model);

        assertEquals("forgot-password", view);
        verify(passwordResetService).createTokenAndSendResetEmail("user@example.com");
        verify(model).addAttribute("success", GENERIC_MESSAGE);
        verify(model, never()).addAttribute(eq("error"), any());
    }

    @Test
    void handleForm_WhenEmailDeliveryFails_StillReturnsGenericSuccess() {
        User user = new User();
        when(userService.findByUsernameAndEmail("user", "user@example.com"))
                .thenReturn(Optional.of(user));
        doThrow(new RuntimeException("email delivery failed"))
                .when(passwordResetService)
                .createTokenAndSendResetEmail("user@example.com");

        String view = controller.handleForm(" user ", " user@example.com ", model);

        assertEquals("forgot-password", view);
        verify(model).addAttribute("success", GENERIC_MESSAGE);
        verify(model, never()).addAttribute(eq("error"), any());
    }
}
