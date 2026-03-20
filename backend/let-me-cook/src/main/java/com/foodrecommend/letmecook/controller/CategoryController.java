package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.entity.Category;
import com.foodrecommend.letmecook.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryController {
    
    private final RecipeService recipeService;
    
    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        List<Category> categories = recipeService.getCategories();
        return Result.success(categories);
    }

    @GetMapping("/categories/recommend")
    public Result<List<Category>> getRecommendCategories(@RequestParam(defaultValue = "10") int limit) {
        List<Category> categories = recipeService.getRecommendCategories(limit);
        return Result.success(categories);
    }
}
