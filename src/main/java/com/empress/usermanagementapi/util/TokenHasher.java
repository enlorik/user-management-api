package com.empress.usermanagementapi.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes deterministic SHA-256 hashes for one-time tokens (email verification
 * and password reset).
 *
 * The raw token is sent to the user and only its hash is persisted, so a leaked
 * token table cannot be replayed directly. A deterministic hash (rather than a
 * salted scheme like BCrypt) is required because tokens are looked up by value;
 * SHA-256 is sufficient here since the input is high-entropy random data, not a
 * user-chosen password.
 */
@Component
public class TokenHasher {

    /**
     * @return the SHA-256 digest of the token, encoded as lowercase hexadecimal
     */
    public String hash(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("rawToken must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 MessageDigest is not available in this JVM", e);
        }
    }
}
