package com.empress.usermanagementapi.config;

import com.empress.usermanagementapi.filter.JwtAuthenticationFilter;
import com.empress.usermanagementapi.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        logger.info("Configuring security filter chain with JWT support");
        
        http
            .csrf(csrf -> csrf
                // Disable CSRF for REST API endpoints (stateless with JWT)
                .ignoringRequestMatchers("/users/**", "/auth/**", "/api/**")
            )
            .sessionManagement(session -> session
                // Stateless session management for JWT
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> {
                logger.debug("Configuring authorization rules");
                authz
                    // static resources always public - must be first to avoid redirects
                    .requestMatchers("/css/**", "/js/**").permitAll()
                    // Swagger/OpenAPI endpoints
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    // Health check endpoint (for Docker HEALTHCHECK)
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    // auth + registration pages
                    .requestMatchers("/login", "/register").permitAll()
                    // JWT authentication endpoint
                    .requestMatchers("/auth/login").permitAll()
                    // forgot / reset password (all methods and subpaths)
                    .requestMatchers("/forgot-password", "/forgot-password/**",
                                     "/reset-password", "/reset-password/**").permitAll()
                    // email verification link from the email
                    .requestMatchers("/verify-email", "/verify-email/**").permitAll()
                    // dashboards
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                    // everything else requires auth
                    .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(this::loginSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void loginSuccessHandler(HttpServletRequest req,
                                     HttpServletResponse res,
                                     Authentication auth) throws java.io.IOException {
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        res.sendRedirect(isAdmin ? "/admin" : "/user");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> {
            var u = repo.findByUsername(username);
            if (u == null) {
                throw new UsernameNotFoundException("No such user: " + username);
            }

            // Block login if email is not verified
            boolean disabled = !u.isVerified();

            return org.springframework.security.core.userdetails.User
                .builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .disabled(disabled)
                .build();
        };
    }
}
