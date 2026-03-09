package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.UserDTO;
import com.foodrecommend.letmecook.mapper.AdminMapper;
import com.foodrecommend.letmecook.mapper.AdminUserMapper;
import com.foodrecommend.letmecook.mapper.StatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsSummaryMapper;
import com.foodrecommend.letmecook.service.impl.AdminServiceImpl;
import com.foodrecommend.letmecook.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplUnitTest {

    @Mock
    private AdminMapper adminMapper;
    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private StatisticsMapper statisticsMapper;
    @Mock
    private StatisticsSummaryMapper statisticsSummaryMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getUsersShouldUseSafePagination() {
        when(adminUserMapper.findUsers(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(adminUserMapper.countUsers(any(), any())).thenReturn(0L);

        PageResult<UserDTO> pageResult = adminService.getUsers(0, 999, null, null);

        assertEquals(1, pageResult.getPage());
        assertEquals(200, pageResult.getPageSize());
        verify(adminUserMapper).findUsers(isNull(), isNull(), eq(0), eq(200));
    }
}
