package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.ResponseDataBuilder;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.common.exception.UnauthorizedException;
import com.foodrecommend.letmecook.dto.admin.*;
import com.foodrecommend.letmecook.service.AdminLogService;
import com.foodrecommend.letmecook.service.AdminService;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import com.foodrecommend.letmecook.util.RequestClientInfoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Arrays;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    private final AdminLogService adminLogService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        String ip = RequestClientInfoUtil.extractClientIp(httpRequest);
        AdminLoginResponse response = adminService.login(request, ip);
        if (response != null && response.getAdmin() != null) {
            adminLogService.recordLog(
                    response.getAdmin().getId(),
                    "LOGIN",
                    "认证管理",
                    "管理员登录成功",
                    null,
                    ip,
                    httpRequest.getHeader("User-Agent"));
        }
        return Result.success(response, "登录成功");
    }
    
    @GetMapping("/profile")
    public Result<AdminProfileDTO> getProfile(HttpServletRequest request) {
        Integer adminId = getAdminIdFromRequest(request);
        AdminProfileDTO profile = adminService.getProfile(adminId);
        return Result.success(profile);
    }
    
    @PutMapping("/password")
    public Result<Void> updatePassword(
            HttpServletRequest httpRequest,
            @RequestBody ChangePasswordRequest request) {
        Integer adminId = getAdminIdFromRequest(httpRequest);
        adminService.updatePassword(adminId, request);
        return Result.success(null, "密码修改成功");
    }
    
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            tokenBlacklistService.addToBlacklist(token);
        }
        return Result.success(null, "退出成功");
    }
    
    @GetMapping("/users")
    public Result<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        PageResult<UserDTO> result = adminService.getUsers(page, pageSize, keyword, status);
        return Result.success(ResponseDataBuilder.page(result));
    }
    
    @GetMapping("/users/{id}")
    public Result<UserDTO> getUserById(@PathVariable Integer id) {
        UserDTO user = adminService.getUserById(id);
        return Result.success(user);
    }
    
    @PostMapping("/users")
    public Result<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        UserDTO user = adminService.createUser(request);
        return Result.success(user, "创建成功");
    }
    
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Integer id, @RequestBody UpdateUserRequestAdmin request) {
        adminService.updateUser(id, request);
        return Result.success(null, "更新成功");
    }
    
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Integer id) {
        adminService.deleteUser(id);
        return Result.success(null, "删除成功");
    }
    
    @DeleteMapping("/users/batch")
    public Result<Map<String, Integer>> batchDeleteUsers(@RequestBody BatchDeleteRequest request) {
        Integer[] validIds = normalizeBatchIds(request == null ? null : request.getIds());
        if (validIds.length == 0) {
            return Result.error(400, "请选择要删除的用户");
        }
        adminService.batchDeleteUsers(validIds);
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("successCount", validIds.length);
        data.put("failCount", 0);
        return Result.success(data, "批量删除成功");
    }
    
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Integer id, @RequestBody UpdateStatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return Result.success(null, "状态更新成功");
    }
    
    @PutMapping("/users/{id}/password")
    public Result<Void> resetUserPassword(@PathVariable Integer id, @RequestBody ResetPasswordRequest request) {
        adminService.resetUserPassword(id, request.getPassword());
        return Result.success(null, "密码重置成功");
    }
    
    private Integer[] normalizeBatchIds(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            return new Integer[0];
        }
        return Arrays.stream(ids)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toArray(Integer[]::new);
    }

    private Integer getAdminIdFromRequest(HttpServletRequest request) {
        Object adminId = request.getAttribute("adminId");
        if (adminId instanceof Integer) {
            return (Integer) adminId;
        }
        throw new UnauthorizedException("管理员登录已过期，请重新登录");
    }
}
