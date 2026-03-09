package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.ResponseDataBuilder;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.service.RecipeService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final AuthTokenHelper authTokenHelper;

    @GetMapping
    public Result<Map<String, Object>> getRecipeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String time,
            @RequestParam(defaultValue = "new") String sort) {

        PageResult<RecipeListDTO> result = recipeService.getRecipeListCached(page, pageSize, category, difficulty, time,
                sort);

        return Result.success(ResponseDataBuilder.page(result));
    }

    @GetMapping("/{id}")
    public Result<RecipeDetailDTO> getRecipeDetail(@PathVariable Integer id) {
        RecipeDetailDTO detail = recipeService.getRecipeDetail(id);
        if (detail == null) {
            return Result.error(404, "菜谱不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> searchRecipes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        PageResult<RecipeListDTO> result = recipeService.searchRecipes(keyword, page, pageSize);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getList());
        data.put("total", result.getTotal());

        return Result.success(data);
    }

    @GetMapping("/recommend")
    public Result<RecommendResponse> getRecommendations(
            @RequestParam(defaultValue = "personal") String type,
            @RequestParam(defaultValue = "16") int limit,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Integer userId = authTokenHelper.optionalUserId(authorization);
        RecommendResponse response = recipeService.getRecommendationsByType(type, limit, userId);
        return Result.success(response);
    }

    @GetMapping("/{id}/similar")
    public Result<List<RecipeListDTO>> getSimilarRecipes(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "5") int limit) {
        List<RecipeListDTO> similarRecipes = recipeService.getSimilarRecipes(id, limit);
        return Result.success(similarRecipes);
    }

    @PostMapping
    public Result<Map<String, Object>> createRecipe(@RequestBody CreateRecipeRequest request) {
        Integer recipeId = recipeService.createRecipe(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", recipeId);
        return Result.success(data);
    }
}
