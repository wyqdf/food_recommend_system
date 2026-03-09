package com.foodrecommend.letmecook.config;

import com.foodrecommend.letmecook.entity.Admin;
import com.foodrecommend.letmecook.mapper.AdminMapper;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import com.foodrecommend.letmecook.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final AdminMapper adminMapper;

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
            Integer adminId = jwtUtil.validateAdminTokenAndGetAdminId(token);
            if (adminId == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"无效的管理员 Token\"}");
                return false;
            }
            
            Admin admin = adminMapper.findById(adminId);
            if (admin == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"管理员不存在\"}");
                return false;
            }
            
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                tokenBlacklistService.addToBlacklist(token);
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"管理员账号已被禁用\"}");
                return false;
            }
            
            request.setAttribute("adminId", adminId);
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 验证失败：" + e.getMessage() + "\"}");
            return false;
        }

        return true;
    }
}
