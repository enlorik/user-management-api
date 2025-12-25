package com.empress.usermanagementapi.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class to verify Swagger OpenAPI schema correctness.
 * Specifically tests that the 'id' field is marked as READ_ONLY
 * and excluded from POST request bodies.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SwaggerSchemaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUserSchemaExcludesIdFromPostRequest() throws Exception {
        // Get the OpenAPI spec
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(content);

        // Navigate to the POST /users endpoint
        JsonNode postUsersEndpoint = apiDocs
                .path("paths")
                .path("/users")
                .path("post");

        assertNotNull(postUsersEndpoint, "POST /users endpoint should exist in OpenAPI spec");

        // Get the request body schema reference
        JsonNode requestBodyContent = postUsersEndpoint
                .path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema");

        assertFalse(requestBodyContent.isMissingNode(), "Request body schema should exist for POST /users");

        // Check if it references a schema
        String schemaRef = requestBodyContent.path("$ref").asText();
        
        if (!schemaRef.isEmpty()) {
            // It's a reference, follow it
            String schemaName = schemaRef.substring(schemaRef.lastIndexOf('/') + 1);
            JsonNode userSchema = apiDocs.path("components").path("schemas").path(schemaName);
            
            assertFalse(userSchema.isMissingNode(), "User schema should exist in components");
            
            // Check that 'id' field is either not in required list or is marked as readOnly
            JsonNode properties = userSchema.path("properties");
            JsonNode idProperty = properties.path("id");
            
            if (!idProperty.isMissingNode()) {
                // If id exists in properties, it should be marked as readOnly
                boolean isReadOnly = idProperty.path("readOnly").asBoolean(false);
                assertTrue(isReadOnly, "The 'id' field should be marked as readOnly in the schema");
            }
            
            // Additionally, 'id' should not be in the required list for POST
            JsonNode required = userSchema.path("required");
            if (!required.isMissingNode() && required.isArray()) {
                for (JsonNode field : required) {
                    assertNotEquals("id", field.asText(), "The 'id' field should not be required in POST requests");
                }
            }
        }
    }

    @Test
    void testUserSchemaIncludesIdInGetResponse() throws Exception {
        // Get the OpenAPI spec
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode apiDocs = objectMapper.readTree(content);

        // Navigate to the GET /users endpoint
        JsonNode getUsersEndpoint = apiDocs
                .path("paths")
                .path("/users")
                .path("get");

        assertNotNull(getUsersEndpoint, "GET /users endpoint should exist in OpenAPI spec");

        // Get the response schema reference (try both */* and application/json)
        JsonNode responseContent200 = getUsersEndpoint
                .path("responses")
                .path("200")
                .path("content");
        
        JsonNode responseSchema = null;
        if (!responseContent200.path("*/*").isMissingNode()) {
            responseSchema = responseContent200.path("*/*").path("schema");
        } else if (!responseContent200.path("application/json").isMissingNode()) {
            responseSchema = responseContent200.path("application/json").path("schema");
        }

        assertNotNull(responseSchema, "Response schema should exist for GET /users");
        assertFalse(responseSchema.isMissingNode(), "Response schema should exist for GET /users");

        // The response is an array of users
        JsonNode itemsSchema = responseSchema.path("items");
        
        if (!itemsSchema.isMissingNode()) {
            String schemaRef = itemsSchema.path("$ref").asText();
            
            if (!schemaRef.isEmpty()) {
                // It's a reference, follow it
                String schemaName = schemaRef.substring(schemaRef.lastIndexOf('/') + 1);
                JsonNode userSchema = apiDocs.path("components").path("schemas").path(schemaName);
                
                assertFalse(userSchema.isMissingNode(), "User schema should exist in components");
                
                // Check that 'id' field exists in properties (it should be present in responses)
                JsonNode properties = userSchema.path("properties");
                JsonNode idProperty = properties.path("id");
                
                assertFalse(idProperty.isMissingNode(), "The 'id' field should exist in response schema");
            }
        }
    }
}
