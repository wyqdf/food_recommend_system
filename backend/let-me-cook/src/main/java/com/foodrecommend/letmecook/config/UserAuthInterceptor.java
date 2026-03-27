package com.foodrecommend.letmecook.config;

import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import com.foodrecommend.letmecook.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        boolean optionalAuth = isOptionalCommentReadRequest(request);
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return rejectOrAllowAnonymous(response, optionalAuth, "未授权，需要登录");
        }

        String token = authorization.substring(7);
        
        if (tokenBlacklistService.isBlacklisted(token)) {
            return rejectOrAllowAnonymous(response, optionalAuth, "Token 已失效，请重新登录");
        }
        
        try {
            Integer userId = jwtUtil.validateUserTokenAndGetUserId(token);
            if (userId == null) {
                return rejectOrAllowAnonymous(response, optionalAuth, "无效的用户 Token");
            }
            
            User user = userMapper.findById(userId);
            if (user == null) {
                return rejectOrAllowAnonymous(response, optionalAuth, "用户不存在");
            }
            
            if (user.getStatus() != null && user.getStatus() == 0) {
                tokenBlacklistService.addToBlacklist(token);
                return rejectOrAllowAnonymous(response, optionalAuth, "账号已被禁用");
            }
            
            request.setAttribute("userId", userId);
        } catch (Exception e) {
            return rejectOrAllowAnonymous(response, optionalAuth, "Token 验证失败：" + e.getMessage());
        }

        return true;
    }

    private boolean isOptionalCommentReadRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && requestUri != null && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }
        return path != null && path.startsWith("/api/comments/recipe/");
    }

    private boolean rejectOrAllowAnonymous(HttpServletResponse response, boolean optionalAuth, String message) throws IOException {
        if (optionalAuth) {
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
