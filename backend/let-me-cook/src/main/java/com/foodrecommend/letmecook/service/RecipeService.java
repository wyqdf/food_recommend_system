package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.Category;

import java.util.List;

public interface RecipeService {

        PageResult<RecipeListDTO> getRecipeList(int page, int pageSize, Integer category, String difficulty,
                        String time,
                        String sort);

        PageResult<RecipeListDTO> getRecipeListCached(int page, int pageSize, Integer category, String difficulty,
                        String time, String sort);

        RecipeDetailDTO getRecipeDetail(Integer id);

        PageResult<RecipeListDTO> searchRecipes(String keyword, int page, int pageSize);

        List<Category> getCategories();

        RecommendResponse getRecommendations(int limit, Integer userId);

        RecommendResponse getRecommendationsByType(String type, int limit, Integer userId);

        List<RecipeListDTO> getSimilarRecipes(Integer recipeId, int limit);

        Integer createRecipe(CreateRecipeRequest request);
}
