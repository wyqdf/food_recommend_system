package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.entity.SystemLog;
import com.foodrecommend.letmecook.mapper.SystemLogMapper;
import com.foodrecommend.letmecook.service.impl.AdminLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceImplTest {

    @Mock
    private SystemLogMapper systemLogMapper;

    @InjectMocks
    private AdminLogServiceImpl adminLogService;

    @Test
    void getLogsShouldUseSafePagination() {
        when(systemLogMapper.findByCondition(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(systemLogMapper.countByCondition(any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);

        PageResult<SystemLog> result = adminLogService.getLogs(0, 999, null, "", "", "", "", "");

        assertNotNull(result);
        assertEquals(1, result.getPage());
        assertEquals(200, result.getPageSize());
        verify(systemLogMapper, times(1))
                .findByCondition(isNull(), eq(""), eq(""), eq(""), isNull(), isNull(), eq(0), eq(200));
    }

    @Test
    void batchDeleteShouldFilterInvalidAndDuplicateIds() {
        when(systemLogMapper.batchDeleteByIds(anyList())).thenReturn(2);

        int deleted = adminLogService.batchDeleteLogs(new Integer[]{1, null, -1, 1, 2});

        assertEquals(2, deleted);
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemLogMapper, times(1)).batchDeleteByIds(captor.capture());
        assertEquals(List.of(1, 2), captor.getValue());
    }

    @Test
    void recordLogShouldSkipWhenAdminOrOperationInvalid() {
        adminLogService.recordLog(null, "CREATE_USER", "用户管理", "x", null, "127.0.0.1", "ua");
        adminLogService.recordLog(1, "   ", "用户管理", "x", null, "127.0.0.1", "ua");
        verify(systemLogMapper, never()).insert(any());
    }

    @Test
    void recordLogShouldInsertValidLog() {
        adminLogService.recordLog(1, "CREATE_USER", "用户管理", "创建用户", 100, "127.0.0.1", "ua");
        verify(systemLogMapper, times(1)).insert(any(SystemLog.class));
    }
}
