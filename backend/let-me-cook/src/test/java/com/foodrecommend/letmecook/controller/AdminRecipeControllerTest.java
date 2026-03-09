package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.BatchDeleteRequest;
import com.foodrecommend.letmecook.service.AdminRecipeService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecipeControllerTest {

    @Mock
    private AdminRecipeService adminRecipeService;

    @InjectMocks
    private AdminRecipeController adminRecipeController;

    @Test
    void batchDeleteShouldReturn400WhenRequestInvalid() {
        Result<Map<String, Object>> result = adminRecipeController.batchDelete(null);

        assertEquals(400, result.getCode());
        assertEquals("请选择要删除的食谱", result.getMessage());
        verify(adminRecipeService, never()).batchDeleteRecipes(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void batchDeleteShouldFilterInvalidAndDuplicateIds() {
        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setIds(new Integer[]{1, null, -3, 1, 2});
        when(adminRecipeService.batchDeleteRecipes(org.mockito.ArgumentMatchers.any())).thenReturn(new int[]{2, 0});

        Result<Map<String, Object>> result = adminRecipeController.batchDelete(request);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().get("successCount"));
        assertEquals(0, result.getData().get("failCount"));

        ArgumentCaptor<Integer[]> captor = ArgumentCaptor.forClass(Integer[].class);
        verify(adminRecipeService).batchDeleteRecipes(captor.capture());
        assertEquals(2, captor.getValue().length);
        assertEquals(1, captor.getValue()[0]);
        assertEquals(2, captor.getValue()[1]);
    }
}
