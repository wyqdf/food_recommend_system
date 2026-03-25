package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.service.UserService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final AuthTokenHelper authTokenHelper;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            Integer userId = userService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", userId);
            data.put("username", request.getUsername());
            return Result.success(data, "注册成功");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
    
    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(@RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            UserProfileDTO profile = userService.getProfile(userId);
            return Result.success(profile);
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
    
    @PutMapping("/profile")
    public Result<Object> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpdateUserRequest request) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            userService.updateProfile(userId, request);
            return Result.success(null, "更新成功");
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }

    @GetMapping("/onboarding")
    public Result<OnboardingProfileDTO> getOnboardingProfile(
            @RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            OnboardingProfileDTO profile = userService.getOnboardingProfile(userId);
            return Result.success(profile);
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }

    @PutMapping("/onboarding")
    public Result<Object> updateOnboardingProfile(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpdateOnboardingRequest request) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            userService.updateOnboardingProfile(userId, request);
            return Result.success(null, "问卷保存成功");
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
}
