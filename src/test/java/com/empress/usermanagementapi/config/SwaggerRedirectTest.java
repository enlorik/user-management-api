package com.empress.usermanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class to verify Swagger UI redirect functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SwaggerRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSwaggerUiRedirect() throws Exception {
        // Test that /swagger-ui/ redirects to /swagger-ui/index.html
        mockMvc.perform(get("/swagger-ui/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void testSwaggerUiIndexAccessible() throws Exception {
        // Test that /swagger-ui/index.html is accessible
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testApiDocsAccessible() throws Exception {
        // Test that /v3/api-docs is accessible
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
