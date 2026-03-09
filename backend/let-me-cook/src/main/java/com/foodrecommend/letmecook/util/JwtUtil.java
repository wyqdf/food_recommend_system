package com.foodrecommend.letmecook.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE_USER = "user";
    private static final String TOKEN_TYPE_ADMIN = "admin";
    
    @Value("${jwt.secret:letMeCookSecretKey2024FoodRecommendationSystem}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")
    private long expiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateToken(Integer userId) {
        return generateUserToken(userId);
    }

    public String generateUserToken(Integer userId) {
        return generateTokenInternal(userId, TOKEN_TYPE_USER, null);
    }

    public String generateAdminToken(Integer adminId, String role) {
        return generateTokenInternal(adminId, TOKEN_TYPE_ADMIN, role);
    }

    private String generateTokenInternal(Integer userId, String tokenType, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        if (role != null && !role.isEmpty()) {
            claims.put(CLAIM_ROLE, role);
        }

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(String.valueOf(userId))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Integer extractUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_USER_ID, Integer.class);
    }

    public String extractTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    public Integer extractAdminId(String token) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_ADMIN.equals(tokenType)) {
            return null;
        }
        return claims.get(CLAIM_USER_ID, Integer.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer validateTokenAndGetUserId(String token) {
        try {
            return extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer validateUserTokenAndGetUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_USER.equals(tokenType)) {
                return null;
            }
            return claims.get(CLAIM_USER_ID, Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer validateAdminTokenAndGetAdminId(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!TOKEN_TYPE_ADMIN.equals(tokenType)) {
                return null;
            }
            return claims.get(CLAIM_USER_ID, Integer.class);
        } catch (Exception e) {
            return null;
        }
    }
}
