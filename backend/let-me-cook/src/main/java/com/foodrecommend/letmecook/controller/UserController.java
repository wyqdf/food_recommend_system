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
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }
    
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Integer userId = userService.register(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", userId);
        data.put("username", request.getUsername());
        return Result.success(data, "注册成功");
    }
    
    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        UserProfileDTO profile = userService.getProfile(userId);
        return Result.success(profile);
    }
    
    @PutMapping("/profile")
    public Result<Object> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateUserRequest request) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        userService.updateProfile(userId, request);
        return Result.success(null, "更新成功");
    }

    @GetMapping("/onboarding")
    public Result<OnboardingProfileDTO> getOnboardingProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        OnboardingProfileDTO profile = userService.getOnboardingProfile(userId);
        return Result.success(profile);
    }

    @PutMapping("/onboarding")
    public Result<Object> updateOnboardingProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateOnboardingRequest request) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        userService.updateOnboardingProfile(userId, request);
        return Result.success(null, "问卷保存成功");
    }
}
