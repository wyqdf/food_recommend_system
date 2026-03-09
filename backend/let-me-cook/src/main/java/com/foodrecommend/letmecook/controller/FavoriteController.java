package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.FavoriteRequest;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.service.FavoriteService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    private final AuthTokenHelper authTokenHelper;
    
    @GetMapping
    public Result<PageResult<RecipeListDTO>> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            PageResult<RecipeListDTO> result = favoriteService.getFavorites(userId, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
    
    @PostMapping
    public Result<Object> addFavorite(
            @RequestBody FavoriteRequest request,
            @RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            favoriteService.addFavorite(userId, request.getRecipeId());
            return Result.success(null, "收藏成功");
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
    
    @DeleteMapping("/{recipeId}")
    public Result<Object> removeFavorite(
            @PathVariable Integer recipeId,
            @RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            favoriteService.removeFavorite(userId, recipeId);
            return Result.success(null, "取消收藏成功");
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
    
    @GetMapping("/check/{recipeId}")
    public Result<Map<String, Boolean>> checkFavorite(
            @PathVariable Integer recipeId,
            @RequestHeader("Authorization") String authorization) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            boolean isFavorite = favoriteService.checkFavorite(userId, recipeId);
            Map<String, Boolean> data = new HashMap<>();
            data.put("isFavorite", isFavorite);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
}
