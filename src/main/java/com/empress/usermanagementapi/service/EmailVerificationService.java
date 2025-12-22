package com.empress.usermanagementapi.service;

import com.empress.usermanagementapi.entity.EmailVerificationToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.EmailVerificationTokenRepository;
import com.empress.usermanagementapi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final long TOKEN_EXPIRY_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepo,
                                    UserRepository userRepo) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
    }

    // create or refresh a token for this user and return the token string
    public String createTokenForUser(User user) {
        EmailVerificationToken existing =
                tokenRepo.findByUser(user).orElse(null);

        String newTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        if (existing != null) {
            existing.setToken(newTokenValue);
            existing.setExpiryDate(expiry);
            existing.setUsed(false);
            tokenRepo.save(existing);
            return newTokenValue;
        }

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(newTokenValue);
        token.setExpiryDate(expiry);
        token.setUsed(false);

        tokenRepo.save(token);
        return newTokenValue;
    }

    /**
     * @return null on success, or an error message on failure
     */
    public String verifyToken(String tokenValue) {
        var opt = tokenRepo.findByToken(tokenValue);
        if (opt.isEmpty()) {
            return "Invalid verification link.";
        }

        EmailVerificationToken token = opt.get();

        if (token.isUsed()) {
            return "This verification link has already been used.";
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "This verification link has expired.";
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepo.save(user);

        token.setUsed(true);
        tokenRepo.save(token);

        return null; // success
    }
}
