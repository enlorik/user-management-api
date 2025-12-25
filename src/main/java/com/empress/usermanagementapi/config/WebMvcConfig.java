package com.empress.usermanagementapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for view controllers and redirects.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect /swagger-ui/ to /swagger-ui/index.html for better usability
        registry.addRedirectViewController("/swagger-ui/", "/swagger-ui/index.html");
    }
}
