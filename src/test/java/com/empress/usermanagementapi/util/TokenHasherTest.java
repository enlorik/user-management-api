package com.empress.usermanagementapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    private final TokenHasher tokenHasher = new TokenHasher();

    @Test
    void testHash_KnownSha256Vector() {
        // NIST test vector: SHA-256("abc")
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                tokenHasher.hash("abc"));
    }

    @Test
    void testHash_EmptyStringVector() {
        // SHA-256 of the empty string
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                tokenHasher.hash(""));
    }

    @Test
    void testHash_SameInputProducesSameHash() {
        String token = "9f2c4e61-1b2a-4c3d-8e5f-0a1b2c3d4e5f";

        assertEquals(tokenHasher.hash(token), tokenHasher.hash(token));
    }

    @Test
    void testHash_DifferentInputsProduceDifferentHashes() {
        assertNotEquals(tokenHasher.hash("token-one"), tokenHasher.hash("token-two"));
    }

    @Test
    void testHash_OutputIsLowercaseHex() {
        String hash = tokenHasher.hash("any-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void testHash_NullInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> tokenHasher.hash(null));
    }
}
