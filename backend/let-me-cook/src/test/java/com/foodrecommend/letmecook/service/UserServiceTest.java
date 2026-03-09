package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.impl.UserServiceImpl;
import com.foodrecommend.letmecook.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoginSuccess() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        String password = new BCryptPasswordEncoder().encode("test123");
        user.setPassword(password);
        user.setNickname("测试用户");

        when(userMapper.findByUsername("testuser")).thenReturn(user);
        when(jwtUtil.generateUserToken(1)).thenReturn("test-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("test123");

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(userMapper, times(1)).findByUsername("testuser");
        verify(jwtUtil, times(1)).generateUserToken(1);
    }

    @Test
    void testLoginUserNotFound() {
        when(userMapper.findByUsername("notexist")).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("notexist");
        request.setPassword("test123");

        assertThrows(RuntimeException.class, () -> userService.login(request));
    }

    @Test
    void testLoginWrongPassword() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        String password = new BCryptPasswordEncoder().encode("correctpassword");
        user.setPassword(password);

        when(userMapper.findByUsername("testuser")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        assertThrows(RuntimeException.class, () -> userService.login(request));
    }

    @Test
    void testRegisterSuccess() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("test123");
        request.setEmail("new@example.com");

        User savedUser = new User();
        savedUser.setId(999);
        savedUser.setUsername("newuser");
        
        doAnswer(invocation -> {
            User arg = invocation.getArgument(0);
            arg.setId(999);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Integer userId = userService.register(request);

        assertEquals(999, userId);
        verify(userMapper, times(1)).findByUsername("newuser");
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void testRegisterUserExists() {
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        when(userMapper.findByUsername("existinguser")).thenReturn(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("test123");

        assertThrows(RuntimeException.class, () -> userService.register(request));
    }

    @Test
    void testGetProfile() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");

        when(userMapper.findById(1)).thenReturn(user);
        when(userMapper.countFavoritesByUserId(1)).thenReturn(5);
        when(userMapper.countCommentsByUserId(1)).thenReturn(3);

        UserProfileDTO profile = userService.getProfile(1);

        assertNotNull(profile);
        assertEquals("testuser", profile.getUsername());
        assertEquals(5, profile.getFavoritesCount());
        assertEquals(3, profile.getCommentsCount());
    }

    @Test
    void testUpdateProfile() {
        User user = new User();
        user.setId(1);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("新昵称");
        request.setEmail("new@example.com");

        userService.updateProfile(1, request);

        verify(userMapper, times(1)).update(any(User.class));
    }
}
