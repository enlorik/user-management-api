package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.PasswordResetTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final long TOKEN_EXPIRY_HOURS = 24; // change if you want longer/shorter

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo,
                                UserRepository userRepo,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordResetToken createPasswordResetTokenForEmail(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No user with email: " + email));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        // Reuse existing row for this user if it exists, otherwise create a new one
        PasswordResetToken prt = tokenRepo.findByUser(user)
            .orElseGet(() -> {
                PasswordResetToken t = new PasswordResetToken();
                t.setUser(user);
                return t;
            });

        prt.setToken(token);
        prt.setExpiryDate(expiry);
        prt.setUsed(false);

        return tokenRepo.save(prt);
    }

    public String validatePasswordResetToken(String token) {
        String cleanToken = token == null ? null : token.trim();

        if (cleanToken == null || cleanToken.isEmpty()) {
            return "Invalid or expired token";
        }

        var opt = tokenRepo.findByToken(cleanToken);

        if (opt.isEmpty()) {
            return "Token not found";
        }

        PasswordResetToken prt = opt.get();

        if (prt.isUsed()) {
            return "Token already used";
        }

        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Token expired";
        }

        // valid
        return null;
    }

    public String resetPassword(String token, String newPassword) {
        String cleanToken = token == null ? null : token.trim();

        if (cleanToken == null || cleanToken.isEmpty()) {
            return "Invalid or expired token";
        }

        var opt = tokenRepo.findByToken(cleanToken);

        if (opt.isEmpty()) {
            return "Token not found";
        }

        PasswordResetToken prt = opt.get();

        if (prt.isUsed()) {
            return "Token already used";
        }

        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Token expired";
        }

        // token is valid → change password
        User u = prt.getUser();
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(u);

        // mark token as used
        prt.setUsed(true);
        tokenRepo.save(prt);

        return null;
    }
}
