package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.model.RegistrationRequest;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.EmailVerificationService;
import com.empress.usermanagementapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private EmailService emailService;

    @Test
    void testRegisterWithValidInput() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser123")
                        .param("email", "valid@example.com")
                        .param("password", "securepassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verifyEmail"));
    }

    @Test
    void testRegisterWithEmptyUsername() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "")
                        .param("email", "valid@example.com")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "username"));
    }

    @Test
    void testRegisterWithInvalidUsernamePattern() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "invalid-user!")
                        .param("email", "valid@example.com")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "username"));
    }

    @Test
    void testRegisterWithEmptyEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("email", "")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "email"));
    }

    @Test
    void testRegisterWithInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("email", "invalidemail")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "email"));
    }

    @Test
    void testRegisterWithEmptyPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("email", "valid@example.com")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "password"));
    }

    @Test
    void testRegisterWithShortPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("email", "valid@example.com")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "password"));
    }

    @Test
    void testRegisterWithTooLongPassword() throws Exception {
        String longPassword = "a".repeat(256);
        mockMvc.perform(post("/register")
                        .param("username", "validuser")
                        .param("email", "valid@example.com")
                        .param("password", longPassword))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "password"));
    }

    /**
     * Test that validation errors are exposed to the model for the view.
     * This verifies that the controller properly adds validation error details
     * so they can be displayed to users.
     */
    @Test
    void testRegisterWithMultipleValidationErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "")
                        .param("email", "invalidemail")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userForm", "username"))
                .andExpect(model().attributeHasFieldErrors("userForm", "email"))
                .andExpect(model().attributeHasFieldErrors("userForm", "password"))
                .andExpect(model().attributeExists("validationErrors"));
    }
}
