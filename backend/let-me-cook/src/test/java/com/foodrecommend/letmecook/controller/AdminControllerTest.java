package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.BatchDeleteRequest;
import com.foodrecommend.letmecook.service.AdminLogService;
import com.foodrecommend.letmecook.service.AdminService;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AdminLogService adminLogService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void batchDeleteUsersShouldReturn400WhenNoIds() {
        Result<Map<String, Integer>> result = adminController.batchDeleteUsers(null);

        assertEquals(400, result.getCode());
        assertEquals("请选择要删除的用户", result.getMessage());
        verify(adminService, never()).batchDeleteUsers(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void batchDeleteUsersShouldFilterInvalidAndDuplicateIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setIds(new Integer[]{1, null, -1, 1, 2});

        Result<Map<String, Integer>> result = adminController.batchDeleteUsers(request);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().get("successCount"));
        assertEquals(0, result.getData().get("failCount"));

        ArgumentCaptor<Integer[]> idsCaptor = ArgumentCaptor.forClass(Integer[].class);
        verify(adminService).batchDeleteUsers(idsCaptor.capture());
        assertEquals(2, idsCaptor.getValue().length);
        assertEquals(1, idsCaptor.getValue()[0]);
        assertEquals(2, idsCaptor.getValue()[1]);
    }
}
