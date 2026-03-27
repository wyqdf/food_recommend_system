package com.foodrecommend.letmecook.config;

import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import com.foodrecommend.letmecook.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserMapper userMapper;

    private UserAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new UserAuthInterceptor(jwtUtil, tokenBlacklistService, userMapper);
    }

    @Test
    void shouldAllowAnonymousCommentListRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/comments/recipe/339721");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
        assertNull(request.getAttribute("userId"));
        verify(jwtUtil, never()).validateUserTokenAndGetUserId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldAttachUserIdForCommentListWhenTokenValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/comments/recipe/339721");
        request.addHeader("Authorization", "Bearer valid-user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setId(12);
        user.setStatus(1);
        when(tokenBlacklistService.isBlacklisted("valid-user-token")).thenReturn(false);
        when(jwtUtil.validateUserTokenAndGetUserId("valid-user-token")).thenReturn(12);
        when(userMapper.findById(12)).thenReturn(user);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(12, request.getAttribute("userId"));
    }

    @Test
    void shouldRejectProtectedFavoriteRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/favorites");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertEquals(false, allowed);
        assertEquals(401, response.getStatus());
    }
}
