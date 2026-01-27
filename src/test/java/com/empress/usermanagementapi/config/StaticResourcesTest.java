package com.empress.usermanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test that static resources (CSS, JS) are served correctly without authentication
 * and return HTTP 200 instead of 302 redirects.
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
public class StaticResourcesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCssResourcesAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/css"));
    }

    @Test
    public void testCssResourcesDoNotRedirect() throws Exception {
        // Verify that accessing a CSS file does NOT result in a 302 redirect
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().is(not(302)));
    }

    @Test
    public void testJsDirectoryIsAccessible() throws Exception {
        // Test that the JS directory path doesn't redirect to login
        // Note: accessing a non-existent JS file may return 404, but should not redirect
        mockMvc.perform(get("/js/test.js"))
                .andExpect(status().is(not(302)));
    }
}
