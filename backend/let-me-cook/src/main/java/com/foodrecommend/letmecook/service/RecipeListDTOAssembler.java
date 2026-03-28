package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.entity.RecipeIngredient;
import com.foodrecommend.letmecook.mapper.CategoryMapper;
import com.foodrecommend.letmecook.mapper.RecipeIngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecipeListDTOAssembler {

    private final CategoryMapper categoryMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;

    public List<RecipeListDTO> toListDTOBatch(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        List<Integer> recipeIds = recipes.stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());

        Map<Integer, List<String>> categoryMap = buildCategoryMap(recipeIds);
        Map<Integer, List<String>> ingredientMap = buildIngredientMap(recipeIds);

        return recipes.stream().map(recipe -> {
            RecipeListDTO dto = new RecipeListDTO();
            dto.setId(recipe.getId());
            dto.setName(recipe.getTitle());
            dto.setAuthor(recipe.getAuthor());
            dto.setAuthorUid(recipe.getAuthorUid());
            dto.setImage(recipe.getImage());
            dto.setDifficulty(recipe.getDifficultyName());
            dto.setTime(recipe.getTimeCostName());
            dto.setTaste(recipe.getTasteName());
            dto.setLikeCount(recipe.getLikeCount());
            dto.setFavoriteCount(recipe.getFavoriteCount());
            dto.setReplyCount(recipe.getReplyCount());
            dto.setCategories(categoryMap.getOrDefault(recipe.getId(), List.of()));
            dto.setIngredients(ingredientMap.getOrDefault(recipe.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Integer, List<String>> buildCategoryMap(List<Integer> recipeIds) {
        Map<Integer, List<String>> categoryMap = new HashMap<>();
        List<CategoryMapper.CategoryRecipeDTO> categoryRows = categoryMapper.findByRecipeIds(recipeIds);
        for (CategoryMapper.CategoryRecipeDTO row : categoryRows) {
            categoryMap.computeIfAbsent(row.getRecipeId(), key -> new ArrayList<>()).add(row.getName());
        }
        return categoryMap;
    }

    private Map<Integer, List<String>> buildIngredientMap(List<Integer> recipeIds) {
        Map<Integer, List<String>> ingredientMap = new HashMap<>();
        List<RecipeIngredient> ingredientRows = recipeIngredientMapper.findByRecipeIds(recipeIds);
        for (RecipeIngredient row : ingredientRows) {
            ingredientMap.computeIfAbsent(row.getRecipeId(), key -> new ArrayList<>()).add(row.getIngredientName());
        }
        return ingredientMap;
    }
}
