package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.config.JwtUtil;
import com.empress.usermanagementapi.dto.AuthenticationRequest;
import com.empress.usermanagementapi.dto.AuthenticationResponse;
import com.empress.usermanagementapi.util.LoggingUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequest request) {
        LoggingUtil.setActionType("USER_LOGIN");
        log.info("Login attempt - username: {}", request.getUsername());
        
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            if (auth.isAuthenticated()) {
                // Load user details to generate token
                final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
                final String jwt = jwtUtil.generateToken(userDetails);
                
                // Extract role
                String role = userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                        .orElse("USER");
                
                log.info("Login successful - username: {}, role: {}", request.getUsername(), role);
                
                AuthenticationResponse response = new AuthenticationResponse(jwt, request.getUsername(), role);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed - authentication not confirmed - username: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Login failed"));
            }
        } catch (org.springframework.security.authentication.DisabledException e) {
            log.warn("Login failed - account disabled/unverified - username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account is not verified"));
        } catch (BadCredentialsException e) {
            log.warn("Login failed - bad credentials - username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            log.error("Login failed - unexpected error - username: {}, error: {}", 
                    request.getUsername(), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred during authentication"));
        } finally {
            LoggingUtil.clearActionType();
        }
    }
}
