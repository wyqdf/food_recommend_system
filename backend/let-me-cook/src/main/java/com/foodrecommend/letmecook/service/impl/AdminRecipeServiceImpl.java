package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.*;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.AdminRecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdminRecipeServiceImpl implements AdminRecipeService {

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final CookingStepMapper cookingStepMapper;
    private final CategoryMapper categoryMapper;
    private final TasteMapper tasteMapper;
    private final TechniqueMapper techniqueMapper;
    private final TimeCostMapper timeCostMapper;
    private final DifficultyMapper difficultyMapper;
    private final IngredientMapper ingredientMapper;

    @Override
    public PageResult<RecipeDTO> getRecipes(int page, int pageSize, String keyword,
            Integer categoryId, Integer tasteId, Integer techniqueId,
            Integer timeCostId, Integer difficultyId, Integer status,
            String startTime, String endTime) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        int offset = (safePage - 1) * safePageSize;

        List<Recipe> recipes = recipeMapper.findForAdmin(keyword, categoryId, tasteId,
                techniqueId, timeCostId, difficultyId, status, startTime, endTime, offset, safePageSize);
        long total = recipeMapper.countForAdmin(keyword, categoryId, tasteId,
                techniqueId, timeCostId, difficultyId, status, startTime, endTime);
        
        List<RecipeDTO> list = recipes.stream().map(this::convertToDTO).collect(Collectors.toList());
        return new PageResult<>(list, total, safePage, safePageSize);
    }

    @Override
    public RecipeDetailDTO getRecipeById(Integer id) {
        Recipe recipe = recipeMapper.findById(id);
        if (recipe == null) {
            throw new RuntimeException("食谱不存在");
        }
        return convertToDetailDTO(recipe);
    }

    @Override
    @Transactional
    public RecipeDTO createRecipe(RecipeCreateRequest request) {
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setAuthor(request.getAuthor());
        recipe.setAuthorUid(request.getAuthorUid());
        recipe.setImage(request.getImage());
        recipe.setDescription(request.getDescription());
        recipe.setTips(request.getTips());
        recipe.setCookware(request.getCookware());
        recipe.setTasteId(request.getTasteId());
        recipe.setTechniqueId(request.getTechniqueId());
        recipe.setTimeCostId(request.getTimeCostId());
        recipe.setDifficultyId(request.getDifficultyId());
        recipe.setReplyCount(0);
        recipe.setLikeCount(0);
        recipe.setRatingCount(0);
        recipe.setCreateTime(LocalDateTime.now());
        recipe.setUpdateTime(LocalDateTime.now());
        
        recipeMapper.insert(recipe);
        
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            for (IngredientDTO ingredientDTO : request.getIngredients()) {
                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipeId(recipe.getId());
                ingredient.setIngredientId(ingredientDTO.getIngredientId());
                ingredient.setIngredientType(ingredientDTO.getType());
                ingredient.setQuantity(ingredientDTO.getQuantity());
                recipeIngredientMapper.insert(ingredient);
            }
        }
        
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            int stepNumber = 1;
            for (StepDTO stepDTO : request.getSteps()) {
                CookingStep step = new CookingStep();
                step.setRecipeId(recipe.getId());
                // 优先使用前端传来的 stepNumber，如果为 null 则自动生成
                step.setStepNumber(stepDTO.getStepNumber() != null ? stepDTO.getStepNumber() : stepNumber++);
                step.setDescription(stepDTO.getDescription());
                step.setImage(stepDTO.getImage());
                cookingStepMapper.insert(step);
            }
        }
        
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Integer categoryId : request.getCategoryIds()) {
                categoryMapper.insertRecipeCategory(recipe.getId(), categoryId);
            }
        }
        
        return convertToDTO(recipe);
    }

    @Override
    @Transactional
    public void updateRecipe(Integer id, RecipeCreateRequest request) {
        Recipe recipe = recipeMapper.findById(id);
        if (recipe == null) {
            throw new RuntimeException("食谱不存在");
        }
        
        recipe.setTitle(request.getTitle());
        recipe.setAuthor(request.getAuthor());
        recipe.setAuthorUid(request.getAuthorUid());
        recipe.setImage(request.getImage());
        recipe.setDescription(request.getDescription());
        recipe.setTips(request.getTips());
        recipe.setCookware(request.getCookware());
        recipe.setTasteId(request.getTasteId());
        recipe.setTechniqueId(request.getTechniqueId());
        recipe.setTimeCostId(request.getTimeCostId());
        recipe.setDifficultyId(request.getDifficultyId());
        recipe.setUpdateTime(LocalDateTime.now());
        
        recipeMapper.update(recipe);
        
        recipeIngredientMapper.deleteByRecipeId(id);
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            for (IngredientDTO ingredientDTO : request.getIngredients()) {
                RecipeIngredient ingredient = new RecipeIngredient();
                ingredient.setRecipeId(id);
                ingredient.setIngredientId(ingredientDTO.getIngredientId());
                ingredient.setIngredientType(ingredientDTO.getType());
                ingredient.setQuantity(ingredientDTO.getQuantity());
                recipeIngredientMapper.insert(ingredient);
            }
        }
        
        cookingStepMapper.deleteByRecipeId(id);
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            int stepNumber = 1;
            for (StepDTO stepDTO : request.getSteps()) {
                CookingStep step = new CookingStep();
                step.setRecipeId(id);
                step.setStepNumber(stepDTO.getStepNumber() != null ? stepDTO.getStepNumber() : stepNumber++);
                step.setDescription(stepDTO.getDescription());
                step.setImage(stepDTO.getImage());
                cookingStepMapper.insert(step);
            }
        }
        
        categoryMapper.deleteRecipeCategoriesByRecipeId(id);
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            for (Integer categoryId : request.getCategoryIds()) {
                categoryMapper.insertRecipeCategory(id, categoryId);
            }
        }
    }

    @Override
    @Transactional
    public void deleteRecipe(Integer id) {
        Recipe recipe = recipeMapper.findById(id);
        if (recipe == null) {
            throw new RuntimeException("食谱不存在");
        }
        
        recipeIngredientMapper.deleteByRecipeId(id);
        cookingStepMapper.deleteByRecipeId(id);
        categoryMapper.deleteRecipeCategoriesByRecipeId(id);
        recipeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int[] batchDeleteRecipes(Integer[] ids) {
        Integer[] validIds = normalizeBatchIds(ids);
        if (validIds.length == 0) {
            return new int[]{0, 0};
        }

        int successCount = 0;
        int failCount = 0;

        for (Integer id : validIds) {
            try {
                deleteRecipe(id);
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }
        
        return new int[]{successCount, failCount};
    }

    @Override
    public void auditRecipe(Integer id, AuditRequest request) {
        Recipe recipe = recipeMapper.findById(id);
        if (recipe == null) {
            throw new RuntimeException("食谱不存在");
        }
        
        recipeMapper.updateStatus(id, request.getStatus());
    }

    private RecipeDTO convertToDTO(Recipe recipe) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setAuthor(recipe.getAuthor());
        dto.setAuthorUid(recipe.getAuthorUid());
        dto.setImage(recipe.getImage());
        dto.setDifficultyId(recipe.getDifficultyId());
        dto.setTasteId(recipe.getTasteId());
        dto.setTechniqueId(recipe.getTechniqueId());
        dto.setTimeCostId(recipe.getTimeCostId());
        dto.setReplyCount(recipe.getReplyCount());
        dto.setLikeCount(recipe.getLikeCount());
        dto.setViewCount(recipe.getRatingCount());

        dto.setDifficulty(resolveName(recipe.getDifficultyName(), recipe.getDifficultyId(),
                idVal -> {
                    Difficulty difficulty = difficultyMapper.findById(idVal);
                    return difficulty == null ? null : difficulty.getName();
                }));
        dto.setTaste(resolveName(recipe.getTasteName(), recipe.getTasteId(),
                idVal -> {
                    Taste taste = tasteMapper.findById(idVal);
                    return taste == null ? null : taste.getName();
                }));
        dto.setTechnique(resolveName(recipe.getTechniqueName(), recipe.getTechniqueId(),
                idVal -> {
                    Technique technique = techniqueMapper.findById(idVal);
                    return technique == null ? null : technique.getName();
                }));
        dto.setTimeCost(resolveName(recipe.getTimeCostName(), recipe.getTimeCostId(),
                idVal -> {
                    TimeCost timeCost = timeCostMapper.findById(idVal);
                    return timeCost == null ? null : timeCost.getName();
                }));
        
        if (recipe.getCreateTime() != null) {
            dto.setCreateTime(recipe.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return dto;
    }

    private RecipeDetailDTO convertToDetailDTO(Recipe recipe) {
        RecipeDetailDTO dto = new RecipeDetailDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setAuthor(recipe.getAuthor());
        dto.setAuthorUid(recipe.getAuthorUid());
        dto.setImage(recipe.getImage());
        dto.setDescription(recipe.getDescription());
        dto.setTips(recipe.getTips());
        dto.setCookware(recipe.getCookware());
        dto.setReplyCount(recipe.getReplyCount());
        dto.setLikeCount(recipe.getLikeCount());
        dto.setViewCount(recipe.getRatingCount());
        
        dto.setTaste(buildAttributeInfo(recipe.getTasteId(), idVal -> {
            Taste taste = tasteMapper.findById(idVal);
            return taste == null ? null : taste.getName();
        }));
        dto.setTechnique(buildAttributeInfo(recipe.getTechniqueId(), idVal -> {
            Technique technique = techniqueMapper.findById(idVal);
            return technique == null ? null : technique.getName();
        }));
        dto.setTimeCost(buildAttributeInfo(recipe.getTimeCostId(), idVal -> {
            TimeCost timeCost = timeCostMapper.findById(idVal);
            return timeCost == null ? null : timeCost.getName();
        }));
        dto.setDifficulty(buildAttributeInfo(recipe.getDifficultyId(), idVal -> {
            Difficulty difficulty = difficultyMapper.findById(idVal);
            return difficulty == null ? null : difficulty.getName();
        }));
        
        List<String> categoryNames = categoryMapper.findByRecipeId(recipe.getId());
        List<RecipeDetailDTO.CategoryInfo> categories = new ArrayList<>();
        for (String name : categoryNames) {
            RecipeDetailDTO.CategoryInfo categoryInfo = new RecipeDetailDTO.CategoryInfo();
            categoryInfo.setName(name);
            categories.add(categoryInfo);
        }
        dto.setCategories(categories);
        
        List<RecipeIngredient> ingredients = recipeIngredientMapper.findByRecipeId(recipe.getId());
        List<RecipeDetailDTO.IngredientInfo> ingredientInfos = new ArrayList<>();
        for (RecipeIngredient ingredient : ingredients) {
            RecipeDetailDTO.IngredientInfo info = new RecipeDetailDTO.IngredientInfo();
            info.setId(ingredient.getId());
            info.setIngredientId(ingredient.getIngredientId());
            info.setName(ingredient.getIngredientName());
            info.setType(ingredient.getIngredientType());
            info.setQuantity(ingredient.getQuantity());
            ingredientInfos.add(info);
        }
        dto.setIngredients(ingredientInfos);
        
        List<CookingStep> steps = cookingStepMapper.findByRecipeId(recipe.getId());
        List<RecipeDetailDTO.StepInfo> stepInfos = new ArrayList<>();
        for (CookingStep step : steps) {
            RecipeDetailDTO.StepInfo info = new RecipeDetailDTO.StepInfo();
            info.setId(step.getId());
            info.setStepNumber(step.getStepNumber());
            info.setDescription(step.getDescription());
            info.setImage(step.getImage());
            stepInfos.add(info);
        }
        dto.setSteps(stepInfos);
        
        if (recipe.getCreateTime() != null) {
            dto.setCreateTime(recipe.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (recipe.getUpdateTime() != null) {
            dto.setUpdateTime(recipe.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return dto;
    }

    private String resolveName(String currentName, Integer id, Function<Integer, String> nameResolver) {
        if (currentName != null) {
            return currentName;
        }
        if (id == null) {
            return null;
        }
        return nameResolver.apply(id);
    }

    private RecipeDetailDTO.AttributeInfo buildAttributeInfo(Integer id, Function<Integer, String> nameResolver) {
        if (id == null) {
            return null;
        }
        String name = nameResolver.apply(id);
        if (name == null) {
            return null;
        }
        RecipeDetailDTO.AttributeInfo info = new RecipeDetailDTO.AttributeInfo();
        info.setId(id);
        info.setName(name);
        return info;
    }

    private Integer[] normalizeBatchIds(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            return new Integer[0];
        }
        return Arrays.stream(ids)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toArray(Integer[]::new);
    }
}
