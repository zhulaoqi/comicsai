package com.comicsai.common;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algo",
                86400000L
        );
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtUtil.generateToken(42L, "user@example.com");
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void parseToken_shouldContainEmailClaim() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("test@example.com", claims.get("email", String.class));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldReturnFalseForNull() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Create a JwtUtil with 0ms expiration
        JwtUtil expiredJwtUtil = new JwtUtil(
                "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algo",
                0L
        );
        String token = expiredJwtUtil.generateToken(1L, "test@example.com");
        assertFalse(expiredJwtUtil.validateToken(token));
    }
}
