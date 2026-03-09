package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.ResultCode;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.UserService;
import com.foodrecommend.letmecook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException(ResultCode.LOGIN_ERROR.getMessage());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException(ResultCode.LOGIN_ERROR.getMessage());
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException(ResultCode.USER_DISABLED.getMessage());
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
            throw new RuntimeException(ResultCode.USERNAME_EXISTS.getMessage());
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
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setEmail(user.getEmail());
        dto.setCreateTime(user.getCreateTime());
        dto.setFavoritesCount(userMapper.countFavoritesByUserId(userId));
        dto.setCommentsCount(userMapper.countCommentsByUserId(userId));

        return dto;
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
}
