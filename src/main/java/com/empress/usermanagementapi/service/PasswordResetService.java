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
        LocalDateTime expiry = LocalDateTime.now().plusHours(24); // change here if you want longer

        // If the user already has a token row, reuse it instead of inserting a new one
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

        return tokenRepo.findByToken(cleanToken)
            .filter(prt -> !prt.isUsed())
            .filter(prt -> prt.getExpiryDate().isAfter(LocalDateTime.now()))
            .map(prt -> (String) null)  // null = valid
            .orElse("Invalid or expired token");
    }

    public String resetPassword(String token, String newPassword) {
        String cleanToken = token == null ? null : token.trim();

        return tokenRepo.findByToken(cleanToken)
            .filter(prt -> !prt.isUsed())
            .filter(prt -> prt.getExpiryDate().isAfter(LocalDateTime.now()))
            .map(prt -> {
                User u = prt.getUser();
                u.setPassword(passwordEncoder.encode(newPassword));
                userRepo.save(u);

                // mark token as used instead of deleting it
                prt.setUsed(true);
                tokenRepo.save(prt);

                return (String) null;
            })
            .orElse("Invalid or expired token");
    }
}
