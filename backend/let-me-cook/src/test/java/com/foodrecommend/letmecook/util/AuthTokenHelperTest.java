package com.foodrecommend.letmecook.util;

import com.foodrecommend.letmecook.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenHelperTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthTokenHelper authTokenHelper;

    @Test
    void requireUserIdShouldReturnUserIdWhenTokenValid() {
        when(jwtUtil.validateUserTokenAndGetUserId("valid-token")).thenReturn(7);

        Integer userId = authTokenHelper.requireUserId("Bearer valid-token");

        assertEquals(7, userId);
        verify(jwtUtil).validateUserTokenAndGetUserId("valid-token");
    }

    @Test
    void requireUserIdShouldThrowUnauthorizedWhenHeaderMissing() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authTokenHelper.requireUserId(null)
        );

        assertEquals("登录已过期，请重新登录", exception.getMessage());
    }

    @Test
    void optionalUserIdShouldReturnNullWhenHeaderMissingOrInvalid() {
        assertNull(authTokenHelper.optionalUserId(null));

        when(jwtUtil.validateUserTokenAndGetUserId("invalid-token")).thenReturn(null);

        assertNull(authTokenHelper.optionalUserId("Bearer invalid-token"));
        verify(jwtUtil).validateUserTokenAndGetUserId("invalid-token");
    }
}
