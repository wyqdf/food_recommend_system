package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.entity.Interaction;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.FavoriteService;
import com.foodrecommend.letmecook.service.RecipeListDTOAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final InteractionMapper interactionMapper;
    private final RecipeMapper recipeMapper;
    private final RecipeListDTOAssembler recipeListDTOAssembler;

    @Override
    public void addFavorite(Integer userId, Integer recipeId) {
        if (interactionMapper.existsFavorite(userId, recipeId)) {
            return;
        }

        Interaction interaction = new Interaction();
        interaction.setUserId(userId);
        interaction.setRecipeId(recipeId);
        interaction.setInteractionType("favorite");
        interactionMapper.insert(interaction);

        recipeMapper.incrementFavoriteCount(recipeId);
    }

    @Override
    public void removeFavorite(Integer userId, Integer recipeId) {
        int deleted = interactionMapper.deleteFavorite(userId, recipeId);
        if (deleted > 0) {
            recipeMapper.decrementFavoriteCount(recipeId);
        }
    }

    @Override
    public boolean checkFavorite(Integer userId, Integer recipeId) {
        return interactionMapper.existsFavorite(userId, recipeId);
    }

    @Override
    public PageResult<RecipeListDTO> getFavorites(Integer userId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        List<Integer> recipeIds = interactionMapper.findFavoriteRecipeIds(userId);

        int start = (safePage - 1) * safePageSize;
        int end = Math.min(start + safePageSize, recipeIds.size());
        
        if (start >= recipeIds.size()) {
            return new PageResult<>(List.of(), recipeIds.size(), safePage, safePageSize);
        }

        List<Integer> pageRecipeIds = recipeIds.subList(start, end);
        List<Recipe> recipes = recipeMapper.findByIds(pageRecipeIds);

        // 按收藏顺序输出，避免 IN 查询返回顺序不稳定
        Map<Integer, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        List<Recipe> orderedRecipes = new ArrayList<>(pageRecipeIds.size());
        for (Integer recipeId : pageRecipeIds) {
            Recipe recipe = recipeMap.get(recipeId);
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(orderedRecipes);

        return new PageResult<>(list, recipeIds.size(), safePage, safePageSize);
    }
}
