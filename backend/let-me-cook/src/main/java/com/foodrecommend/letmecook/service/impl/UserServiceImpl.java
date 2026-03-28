package com.foodrecommend.letmecook.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrecommend.letmecook.common.ResultCode;
import com.foodrecommend.letmecook.common.exception.BadRequestException;
import com.foodrecommend.letmecook.common.exception.NotFoundException;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.UserPreferenceProfile;
import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.UserPreferenceProfileMapper;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.UserService;
import com.foodrecommend.letmecook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserPreferenceProfileMapper userPreferenceProfileMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BadRequestException(ResultCode.LOGIN_ERROR.getMessage());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException(ResultCode.LOGIN_ERROR.getMessage());
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BadRequestException(ResultCode.USER_DISABLED.getMessage());
        }

        String token = jwtUtil.generateUserToken(user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setEmail(user.getEmail());
        response.setUser(userInfo);

        return response;
    }

    @Override
    public Integer register(RegisterRequest request) {
        User existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BadRequestException(ResultCode.USERNAME_EXISTS.getMessage());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public UserProfileDTO getProfile(Integer userId) {
        User user = requireUser(userId);
        UserPreferenceProfile profile = userPreferenceProfileMapper.findByUserId(userId);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setEmail(user.getEmail());
        dto.setCreateTime(user.getCreateTime());
        dto.setFavoritesCount(userMapper.countFavoritesByUserId(userId));
        dto.setCommentsCount(userMapper.countCommentsByUserId(userId));
        dto.setOnboardingCompleted(profile != null && profile.getOnboardingCompleted() != null
                && profile.getOnboardingCompleted() == 1);

        return dto;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Integer userId, UpdateUserRequest request) {
        User user = new User();
        user.setId(userId);
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setEmail(request.getEmail());

        userMapper.update(user);
    }

    @Override
    public OnboardingProfileDTO getOnboardingProfile(Integer userId) {
        UserPreferenceProfile profile = userPreferenceProfileMapper.findByUserId(userId);
        OnboardingProfileDTO dto = new OnboardingProfileDTO();
        if (profile == null) {
            dto.setCompleted(false);
            return dto;
        }

        dto.setCompleted(profile.getOnboardingCompleted() != null && profile.getOnboardingCompleted() == 1);
        dto.setDietGoal(profile.getDietGoal());
        dto.setCookingSkill(profile.getCookingSkill());
        dto.setTimeBudget(profile.getTimeBudget());
        dto.setPreferredTastes(parseJsonList(profile.getPreferredTastesJson()));
        dto.setTabooIngredients(parseJsonList(profile.getTabooIngredientsJson()));
        dto.setAvailableCookwares(parseJsonList(profile.getAvailableCookwaresJson()));
        dto.setPreferredScenes(parseJsonList(profile.getPreferredScenesJson()));
        return dto;
    }

    @Override
    public void updateOnboardingProfile(Integer userId, UpdateOnboardingRequest request) {
        UserPreferenceProfile profile = new UserPreferenceProfile();
        profile.setUserId(userId);
        profile.setDietGoal(trimOrNull(request.getDietGoal()));
        profile.setCookingSkill(trimOrNull(request.getCookingSkill()));
        profile.setTimeBudget(trimOrNull(request.getTimeBudget()));
        profile.setPreferredTastesJson(toJsonSafe(request.getPreferredTastes()));
        profile.setTabooIngredientsJson(toJsonSafe(request.getTabooIngredients()));
        profile.setAvailableCookwaresJson(toJsonSafe(request.getAvailableCookwares()));
        profile.setPreferredScenesJson(toJsonSafe(request.getPreferredScenes()));
        profile.setOnboardingCompleted(1);
        userPreferenceProfileMapper.upsert(profile);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
