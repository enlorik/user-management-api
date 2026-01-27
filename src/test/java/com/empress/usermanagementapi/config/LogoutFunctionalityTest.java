package com.empress.usermanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests to verify logout functionality works correctly with CSRF protection.
 * 
 * This test class verifies that:
 * 1. POST requests to /logout with CSRF token succeed
 * 2. GET requests to /logout are not allowed (return 405 Method Not Allowed or redirect)
 * 3. POST requests without CSRF token fail with 403 Forbidden
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=localhost",
    "spring.mail.port=3025"
})
public class LogoutFunctionalityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogoutWithPostAndCsrfSucceeds() throws Exception {
        // POST to /logout with CSRF token should succeed and redirect
        mockMvc.perform(post("/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testLogoutWithPostAndCsrfSucceedsForAdmin() throws Exception {
        // POST to /logout with CSRF token should succeed for admin too
        mockMvc.perform(post("/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogoutWithPostButNoCsrfFails() throws Exception {
        // POST to /logout without CSRF token should fail with 403 Forbidden
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogoutWithGetIsNotAllowed() throws Exception {
        // GET to /logout should not be allowed (405 Method Not Allowed or 404)
        // Spring Security by default doesn't allow GET for logout
        mockMvc.perform(get("/logout"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept either 404 (not found) or 405 (method not allowed)
                    // Both indicate that GET is not the correct method
                    if (status != 404 && status != 405) {
                        throw new AssertionError(
                            "Expected 404 or 405 for GET /logout, but got: " + status
                        );
                    }
                });
    }
}
