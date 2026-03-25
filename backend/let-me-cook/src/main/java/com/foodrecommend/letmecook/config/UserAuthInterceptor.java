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

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权，需要登录\"}");
            return false;
        }

        String token = authorization.substring(7);
        
        if (tokenBlacklistService.isBlacklisted(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 已失效，请重新登录\"}");
            return false;
        }
        
        try {
            Integer userId = jwtUtil.validateUserTokenAndGetUserId(token);
            if (userId == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"无效的用户 Token\"}");
                return false;
            }
            
            User user = userMapper.findById(userId);
            if (user == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"用户不存在\"}");
                return false;
            }
            
            if (user.getStatus() != null && user.getStatus() == 0) {
                tokenBlacklistService.addToBlacklist(token);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"账号已被禁用\"}");
                return false;
            }
            
            request.setAttribute("userId", userId);
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 验证失败：" + e.getMessage() + "\"}");
            return false;
        }

        return true;
    }
}
