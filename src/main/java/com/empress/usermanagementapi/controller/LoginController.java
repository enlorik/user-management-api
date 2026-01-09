package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        LoggingUtil.setActionType("USER_LOGIN");
        log.info("Login attempt - username: {}", username);
        
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            if (auth.isAuthenticated()) {
                log.info("Login successful - username: {}", username);
                return "Login successful!";
            } else {
                log.warn("Login failed - authentication not confirmed - username: {}", username);
                return "Login failed.";
            }
        } catch (BadCredentialsException e) {
            log.warn("Login failed - bad credentials - username: {}", username);
            throw e;
        } catch (Exception e) {
            log.error("Login failed - unexpected error - username: {}, error: {}", 
                    username, e.getClass().getSimpleName());
            throw e;
        } finally {
            LoggingUtil.clearActionType();
        }
    }
}
