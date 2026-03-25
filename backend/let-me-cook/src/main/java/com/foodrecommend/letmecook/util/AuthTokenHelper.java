package com.foodrecommend.letmecook.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenHelper {

    private final JwtUtil jwtUtil;

    public Integer requireUserId(String authorization) {
        String token = extractBearerToken(authorization);
        Integer userId = jwtUtil.validateUserTokenAndGetUserId(token);
        if (userId == null) {
            throw new RuntimeException("无效的用户token");
        }
        return userId;
    }

    public Integer optionalUserId(String authorization) {
        String token = extractBearerTokenIfPresent(authorization);
        if (token == null) {
            return null;
        }
        return jwtUtil.validateUserTokenAndGetUserId(token);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("无效的token");
        }
        return authorization.substring(7);
    }

    private String extractBearerTokenIfPresent(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
