package com.empress.usermanagementapi.config;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for session-based authentication.
 * Verifies that JWT authentication has been removed and session-based auth works correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=localhost",
    "spring.mail.port=3025"
})
class SessionBasedAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testLoginWithValidCredentials_RedirectsToUserDashboard() throws Exception {
        // Create a verified test user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(true);
        userRepository.save(user);

        // Perform login using form login
        mockMvc.perform(formLogin("/login")
                        .user("username", "testuser")
                        .password("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));
    }

    @Test
    void testLoginWithAdminUser_RedirectsToAdminDashboard() throws Exception {
        // Create a verified admin user
        User admin = new User();
        admin.setUsername("adminuser");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Password123!"));
        admin.setRole(Role.ADMIN);
        admin.setVerified(true);
        userRepository.save(admin);

        // Perform login using form login
        mockMvc.perform(formLogin("/login")
                        .user("username", "adminuser")
                        .password("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void testLoginWithUnverifiedUser_Fails() throws Exception {
        // Create an unverified test user
        User user = new User();
        user.setUsername("unverified");
        user.setEmail("unverified@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setVerified(false);  // Not verified
        userRepository.save(user);

        // Attempt login - should fail due to unverified account
        mockMvc.perform(formLogin("/login")
                        .user("username", "unverified")
                        .password("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void testAccessProtectedEndpoint_WithoutLogin_RedirectsToLogin() throws Exception {
        // Access protected endpoint without authentication
        mockMvc.perform(get("/user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testPublicEndpoints_AreAccessibleWithoutAuth() throws Exception {
        // Test that public endpoints are accessible without authentication
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}
