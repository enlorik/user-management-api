package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private Model model;

    @InjectMocks
    private RegistrationController registrationController;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setUsername("testuser");
        validUser.setEmail("test@example.com");
        validUser.setPassword("Password123!");
    }

    @Test
    void registerSubmit_WithValidUser_ShouldSucceed() {
        // Arrange
        when(userService.usernameExists(anyString())).thenReturn(false);
        when(userService.emailExists(anyString())).thenReturn(false);
        
        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername(validUser.getUsername());
        createdUser.setEmail(validUser.getEmail());
        createdUser.setRole(Role.USER);
        
        when(userService.create(any(User.class))).thenReturn(createdUser);
        when(emailVerificationService.createTokenForUser(any(User.class)))
                .thenReturn("test-token-123");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        String result = registrationController.registerSubmit(validUser, model);

        // Assert
        assertEquals("redirect:/login?verifyEmail", result);
        verify(userService).usernameExists(validUser.getUsername());
        verify(userService).emailExists(validUser.getEmail());
        verify(userService).create(any(User.class));
        verify(emailVerificationService).createTokenForUser(any(User.class));
        verify(emailService).sendVerificationEmail(eq(validUser.getEmail()), anyString());
        verify(model, never()).addAttribute(eq("usernameError"), anyString());
        verify(model, never()).addAttribute(eq("emailError"), anyString());
    }

    @Test
    void registerSubmit_WithDuplicateUsername_ShouldReturnError() {
        // Arrange
        when(userService.usernameExists(validUser.getUsername())).thenReturn(true);

        // Act
        String result = registrationController.registerSubmit(validUser, model);

        // Assert
        assertEquals("register", result);
        verify(userService).usernameExists(validUser.getUsername());
        verify(model).addAttribute("usernameError", "Username already in use");
        verify(userService, never()).emailExists(anyString());
        verify(userService, never()).create(any(User.class));
        verify(emailVerificationService, never()).createTokenForUser(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerSubmit_WithDuplicateEmail_ShouldReturnError() {
        // Arrange
        when(userService.usernameExists(validUser.getUsername())).thenReturn(false);
        when(userService.emailExists(validUser.getEmail())).thenReturn(true);

        // Act
        String result = registrationController.registerSubmit(validUser, model);

        // Assert
        assertEquals("register", result);
        verify(userService).usernameExists(validUser.getUsername());
        verify(userService).emailExists(validUser.getEmail());
        verify(model).addAttribute("emailError", "Email already in use");
        verify(userService, never()).create(any(User.class));
        verify(emailVerificationService, never()).createTokenForUser(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerSubmit_WhenEmailServiceFails_ShouldStillCreateUser() {
        // Arrange
        when(userService.usernameExists(anyString())).thenReturn(false);
        when(userService.emailExists(anyString())).thenReturn(false);
        
        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername(validUser.getUsername());
        createdUser.setEmail(validUser.getEmail());
        createdUser.setRole(Role.USER);
        
        when(userService.create(any(User.class))).thenReturn(createdUser);
        when(emailVerificationService.createTokenForUser(any(User.class)))
                .thenReturn("test-token-123");
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        String result = registrationController.registerSubmit(validUser, model);

        // Assert
        assertEquals("redirect:/login?verifyEmail", result);
        verify(userService).create(any(User.class));
        verify(emailVerificationService).createTokenForUser(any(User.class));
        verify(emailService).sendVerificationEmail(eq(validUser.getEmail()), anyString());
    }

    @Test
    void registerSubmit_ShouldSetUserRoleToUSER() {
        // Arrange
        when(userService.usernameExists(anyString())).thenReturn(false);
        when(userService.emailExists(anyString())).thenReturn(false);
        
        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setRole(Role.USER);
        
        when(userService.create(any(User.class))).thenReturn(createdUser);
        when(emailVerificationService.createTokenForUser(any(User.class)))
                .thenReturn("test-token-123");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        registrationController.registerSubmit(validUser, model);

        // Assert
        assertEquals(Role.USER, validUser.getRole());
        verify(userService).create(argThat(user -> 
            user.getRole() == Role.USER
        ));
    }

    @Test
    void registerSubmit_ShouldGenerateVerificationToken() {
        // Arrange
        when(userService.usernameExists(anyString())).thenReturn(false);
        when(userService.emailExists(anyString())).thenReturn(false);
        
        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername(validUser.getUsername());
        createdUser.setEmail(validUser.getEmail());
        
        when(userService.create(any(User.class))).thenReturn(createdUser);
        when(emailVerificationService.createTokenForUser(createdUser))
                .thenReturn("generated-token-xyz");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        registrationController.registerSubmit(validUser, model);

        // Assert
        verify(emailVerificationService).createTokenForUser(createdUser);
        verify(emailService).sendVerificationEmail(
            eq(validUser.getEmail()), 
            contains("generated-token-xyz")
        );
    }
}
