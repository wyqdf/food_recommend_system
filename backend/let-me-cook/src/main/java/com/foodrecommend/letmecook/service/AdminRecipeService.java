package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.*;

public interface AdminRecipeService {
    
    PageResult<RecipeDTO> getRecipes(int page, int pageSize, String keyword,
            Integer categoryId, Integer tasteId, Integer techniqueId,
            Integer timeCostId, Integer difficultyId, Integer status,
            String startTime, String endTime);
    
    RecipeDetailDTO getRecipeById(Integer id);
    
    RecipeDTO createRecipe(RecipeCreateRequest request);
    
    void updateRecipe(Integer id, RecipeCreateRequest request);
    
    void deleteRecipe(Integer id);
    
    int[] batchDeleteRecipes(Integer[] ids);
    
    void auditRecipe(Integer id, AuditRequest request);
}
