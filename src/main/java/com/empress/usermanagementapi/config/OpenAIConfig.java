package com.empress.usermanagementapi.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAI API integration.
 * Loads API key from environment variables for secure configuration.
 */
@Configuration
public class OpenAIConfig {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIConfig.class);
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    /**
     * Create OpenAI client bean.
     * Returns null if API key is not configured, allowing the application
     * to fall back to rule-based summarization.
     * 
     * @return OpenAI client or null if not configured
     */
    @Bean
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenAI API key not configured. Log summarization will use rule-based approach. " +
                    "Set OPENAI_API_KEY environment variable to enable AI-powered summarization.");
            return null;
        }
        
        log.info("Initializing OpenAI client for AI-powered log summarization");
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
