package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.ResponseDataBuilder;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.service.RecipeService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import com.foodrecommend.letmecook.util.SceneModeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(required = false) String mode) {

        PageResult<RecipeListDTO> result = recipeService.getRecipeListCached(page, pageSize, category, difficulty, time,
                sort, mode);

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
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {

        PageResult<RecipeListDTO> result = recipeService.searchRecipes(keyword, sort, page, pageSize);
        return Result.success(ResponseDataBuilder.page(result));
    }

    @GetMapping("/search/suggestions")
    public Result<List<SearchSuggestionDTO>> getSearchSuggestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "8") int limit) {
        return Result.success(recipeService.getSearchSuggestions(keyword, limit));
    }

    @GetMapping("/recommend")
    public Result<RecommendResponse> getRecommendations(
            @RequestParam(defaultValue = "personal") String type,
            @RequestParam(defaultValue = "16") int limit,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer categoryId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = authTokenHelper.optionalUserId(authorization);
        String resolvedScene = StringUtils.hasText(scene) ? scene : SceneModeResolver.resolveSceneCode(mode);
        RecommendResponse response = recipeService.getRecommendationsByType(type, limit, userId, resolvedScene,
                categoryId);
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
