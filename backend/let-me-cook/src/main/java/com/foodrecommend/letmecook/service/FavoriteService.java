package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.RecipeListDTO;

public interface FavoriteService {
    
    void addFavorite(Integer userId, Integer recipeId);
    
    void removeFavorite(Integer userId, Integer recipeId);
    
    boolean checkFavorite(Integer userId, Integer recipeId);
    
    PageResult<RecipeListDTO> getFavorites(Integer userId, int page, int pageSize);
}
