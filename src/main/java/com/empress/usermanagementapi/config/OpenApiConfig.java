package com.empress.usermanagementapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI userManagementOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development Server");

        Server productionServer = new Server();
        productionServer.setUrl(baseUrl);
        productionServer.setDescription("Production Server (Railway)");

        Contact contact = new Contact();
        contact.setName("User Management API Team");
        contact.setUrl("https://github.com/enlorik/user-management-api");

        Info info = new Info()
                .title("User Management API")
                .version("1.0")
                .description("API for user registration, authentication, email verification, and password management")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer));
    }
}
