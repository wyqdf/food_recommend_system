package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.entity.Interaction;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.FavoriteService;
import com.foodrecommend.letmecook.service.RecipeListDTOAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void addFavorite(Integer userId, Integer recipeId) {
        Interaction interaction = new Interaction();
        interaction.setUserId(userId);
        interaction.setRecipeId(recipeId);
        interaction.setInteractionType("favorite");
        try {
            int inserted = interactionMapper.insert(interaction);
            if (inserted > 0) {
                recipeMapper.incrementFavoriteCount(recipeId);
            }
        } catch (DuplicateKeyException ignored) {
            // 收藏唯一约束会拦住并发重复收藏；重复请求按幂等成功处理即可。
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Integer userId, Integer recipeId) {
        int deleted = interactionMapper.deleteFavorite(userId, recipeId);
        if (deleted > 0) {
            recipeMapper.decrementFavoriteCountBy(recipeId, deleted);
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
        long total = interactionMapper.countPublicFavoriteRecipes(userId);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        int start = (safePage - 1) * safePageSize;
        if (start >= total) {
            return new PageResult<>(List.of(), total, safePage, safePageSize);
        }

        List<Integer> pageRecipeIds = interactionMapper.findPublicFavoriteRecipeIdsPage(userId, start, safePageSize);
        if (pageRecipeIds == null || pageRecipeIds.isEmpty()) {
            return new PageResult<>(List.of(), total, safePage, safePageSize);
        }
        List<Recipe> recipes = recipeMapper.findByIds(pageRecipeIds);
        if (recipes == null || recipes.isEmpty()) {
            return new PageResult<>(List.of(), total, safePage, safePageSize);
        }

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

        return new PageResult<>(list, total, safePage, safePageSize);
    }
}
