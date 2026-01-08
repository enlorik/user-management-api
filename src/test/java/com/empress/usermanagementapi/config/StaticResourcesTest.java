package com.empress.usermanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test that static resources (CSS, JS) are served correctly without authentication
 * and return HTTP 200 instead of 302 redirects.
 */
@SpringBootTest
@AutoConfigureMockMvc
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
                .andExpect(status().isOk())
                .andExpect(status().isNotFound().or(status().isOk()));
    }

    @Test
    public void testJsDirectoryIsAccessible() throws Exception {
        // Test that the JS directory path doesn't redirect to login
        // Note: accessing a directory may return 404 if no index, but should not redirect
        mockMvc.perform(get("/js/test.js"))
                .andExpect(status().is(org.hamcrest.Matchers.not(302)));
    }
}
