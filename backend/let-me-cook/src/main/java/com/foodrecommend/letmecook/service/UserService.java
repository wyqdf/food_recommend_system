package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.*;

public interface UserService {
    
    LoginResponse login(LoginRequest request);
    
    Integer register(RegisterRequest request);
    
    UserProfileDTO getProfile(Integer userId);
    
    void updateProfile(Integer userId, UpdateUserRequest request);

    OnboardingProfileDTO getOnboardingProfile(Integer userId);

    void updateOnboardingProfile(Integer userId, UpdateOnboardingRequest request);
}
