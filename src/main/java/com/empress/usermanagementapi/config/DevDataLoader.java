package com.empress.usermanagementapi.config;

import com.empress.usermanagementapi.entity.Role;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Development data loader that creates a default admin user for local testing.
 * Only active when the 'local' profile is active.
 */
@Configuration
@Profile("local")
public class DevDataLoader {

    @Bean
    public CommandLineRunner loadDevData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin user already exists
            if (userRepository.findByUsername("admin") == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@example.com");
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setRole(Role.ADMIN);
                admin.setVerified(true);
                userRepository.save(admin);
                System.out.println("✓ Created default admin user for local development");
            }
        };
    }
}
