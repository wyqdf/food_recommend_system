package com.foodrecommend.letmecook.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGenerateToken() {
        String token = jwtUtil.generateToken(1);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testExtractUserId() {
        String token = jwtUtil.generateToken(123);
        Integer userId = jwtUtil.extractUserId(token);

        assertEquals(123, userId);
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateToken(1);
        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtil.validateToken(invalidToken);
        assertFalse(isValid);
    }
}
