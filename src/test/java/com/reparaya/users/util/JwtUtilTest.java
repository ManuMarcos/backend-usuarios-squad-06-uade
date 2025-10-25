package com.reparaya.users.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "my-test-secret-key-1234567890123456";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = buildUtilWithExpiration(ONE_HOUR_MS);
    }

    @Test
    void generateToken_embedsEmailAndRoleClaims() {
        String token = jwtUtil.generateToken("user@example.com", "ROLE_ADMIN");

        assertNotNull(token);
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void validateToken_returnsTrueForValidTokenAndEmail() {
        String token = jwtUtil.generateToken("valid@example.com", "ROLE_USER");

        assertTrue(jwtUtil.validateToken(token, "valid@example.com"));
    }

    @Test
    void validateToken_returnsFalseWhenEmailDoesNotMatch() {
        String token = jwtUtil.generateToken("original@example.com", "ROLE_USER");

        assertFalse(jwtUtil.validateToken(token, "other@example.com"));
    }

    @Test
    void validateToken_returnsFalseWhenTokenExpired() {
        JwtUtil expiredUtil = buildUtilWithExpiration(-1_000L); // forces expiration in the past
        String token = expiredUtil.generateToken("expired@example.com", "ROLE_USER");

        assertFalse(expiredUtil.validateToken(token, "expired@example.com"));
    }

    @Test
    void validateToken_returnsFalseForCorruptedToken() {
        String token = jwtUtil.generateToken("corrupt@example.com", "ROLE_USER");
        String corrupted = token.substring(0, token.length() - 2) + "aa"; // modify signature

        assertFalse(jwtUtil.validateToken(corrupted, "corrupt@example.com"));
    }

    private JwtUtil buildUtilWithExpiration(long expirationMillis) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", SECRET);
        ReflectionTestUtils.setField(util, "expiration", expirationMillis);
        return util;
    }
}
