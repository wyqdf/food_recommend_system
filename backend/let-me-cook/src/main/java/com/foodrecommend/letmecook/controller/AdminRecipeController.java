package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.ResponseDataBuilder;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.*;
import com.foodrecommend.letmecook.service.AdminRecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/admin/recipes")
@RequiredArgsConstructor
public class AdminRecipeController {

    private final AdminRecipeService adminRecipeService;

    @GetMapping
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer tasteId,
            @RequestParam(required = false) Integer techniqueId,
            @RequestParam(required = false) Integer timeCostId,
            @RequestParam(required = false) Integer difficultyId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        PageResult<RecipeDTO> result = adminRecipeService.getRecipes(
                page, pageSize, keyword, categoryId, tasteId, techniqueId,
                timeCostId, difficultyId, status, startTime, endTime);

        return Result.success(ResponseDataBuilder.page(result));
    }

    @GetMapping("/{id}")
    public Result<RecipeDetailDTO> getById(@PathVariable Integer id) {
        RecipeDetailDTO recipe = adminRecipeService.getRecipeById(id);
        return Result.success(recipe);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody RecipeCreateRequest request) {
        RecipeDTO recipe = adminRecipeService.createRecipe(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", recipe.getId());
        data.put("title", recipe.getTitle());
        data.put("createTime", recipe.getCreateTime());
        return Result.success(data, "创建成功");
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody RecipeCreateRequest request) {
        adminRecipeService.updateRecipe(id, request);
        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        adminRecipeService.deleteRecipe(id);
        return Result.success(null, "删除成功");
    }

    @DeleteMapping("/batch")
    public Result<Map<String, Object>> batchDelete(@RequestBody BatchDeleteRequest request) {
        Integer[] ids = normalizeBatchIds(request == null ? null : request.getIds());
        if (ids.length == 0) {
            return Result.error(400, "请选择要删除的食谱");
        }
        int[] result = adminRecipeService.batchDeleteRecipes(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("successCount", result[0]);
        data.put("failCount", result[1]);
        return Result.success(data, "批量删除成功");
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Integer id, @RequestBody AuditRequest request) {
        adminRecipeService.auditRecipe(id, request);
        return Result.success(null, "审核成功");
    }

    private Integer[] normalizeBatchIds(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            return new Integer[0];
        }
        return Arrays.stream(ids)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toArray(Integer[]::new);
    }
}
