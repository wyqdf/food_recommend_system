package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.RecipeListDTOAssembler;
import com.foodrecommend.letmecook.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;
    private final CookingStepMapper cookingStepMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;
    private final TasteMapper tasteMapper;
    private final TechniqueMapper techniqueMapper;
    private final TimeCostMapper timeCostMapper;
    private final DifficultyMapper difficultyMapper;
    private final RecipeListDTOAssembler recipeListDTOAssembler;

    @Override
    public PageResult<RecipeListDTO> getRecipeList(int page, int pageSize, Integer category, String difficulty,
            String time, String sort) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;
        List<Recipe> recipes = recipeMapper.findByCondition(category, difficulty, time, sort, offset, safePageSize);
        long total = recipeMapper.countByCondition(category, difficulty, time);

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);

        return new PageResult<>(list, total, safePage, safePageSize);
    }

    @Override
    @Cacheable(value = "recipe_list", key = "#page + '_' + #pageSize + '_' + (#category ?: 'all') + '_' + (#difficulty ?: 'all') + '_' + (#time ?: 'all') + '_' + (#sort ?: 'new')", unless = "#result == null")
    public PageResult<RecipeListDTO> getRecipeListCached(int page, int pageSize, Integer category, String difficulty,
            String time, String sort) {
        return getRecipeList(page, pageSize, category, difficulty, time, sort);
    }

    @Override
    public RecipeDetailDTO getRecipeDetail(Integer id) {
        Recipe recipe = recipeMapper.findPublicById(id);
        if (recipe == null) {
            return null;
        }

        RecipeDetailDTO dto = new RecipeDetailDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setImage(recipe.getImage());
        dto.setDifficulty(recipe.getDifficultyName());
        dto.setTime(recipe.getTimeCostName());
        dto.setLikeCount(recipe.getLikeCount());
        dto.setFavoriteCount(recipe.getRatingCount());
        dto.setReplyCount(recipe.getReplyCount());
        dto.setTasteName(recipe.getTasteName());
        dto.setTechniqueName(recipe.getTechniqueName());
        dto.setTimeCostName(recipe.getTimeCostName());
        dto.setDifficultyName(recipe.getDifficultyName());
        dto.setTips(recipe.getTips());
        dto.setAuthor(recipe.getAuthor());
        dto.setAuthorUid(recipe.getAuthorUid());

        List<String> categories = categoryMapper.findByRecipeId(id);
        dto.setCategories(categories);

        List<RecipeIngredient> ingredients = recipeIngredientMapper.findByRecipeId(id);
        List<RecipeDetailDTO.IngredientDTO> ingredientDTOs = ingredients.stream()
                .map(ing -> {
                    RecipeDetailDTO.IngredientDTO ingredientDTO = new RecipeDetailDTO.IngredientDTO();
                    ingredientDTO.setId(ing.getId());
                    ingredientDTO.setName(ing.getIngredientName());
                    ingredientDTO.setType(ing.getIngredientType());
                    ingredientDTO.setQuantity(ing.getQuantity());
                    return ingredientDTO;
                })
                .collect(Collectors.toList());
        dto.setIngredients(ingredientDTOs);

        dto.setDescription(recipe.getDescription());

        List<CookingStep> steps = cookingStepMapper.findByRecipeId(id);
        List<RecipeDetailDTO.StepDTO> stepDTOs = steps.stream()
                .map(s -> {
                    RecipeDetailDTO.StepDTO stepDTO = new RecipeDetailDTO.StepDTO();
                    stepDTO.setStep(s.getStepNumber());
                    stepDTO.setDescription(s.getDescription());
                    stepDTO.setImage(s.getImage());
                    return stepDTO;
                })
                .collect(Collectors.toList());
        dto.setSteps(stepDTOs);

        RecipeDetailDTO.NutritionDTO nutrition = new RecipeDetailDTO.NutritionDTO();
        nutrition.setCalories(0);
        nutrition.setProtein(0);
        nutrition.setFat(0);
        nutrition.setCarbs(0);
        dto.setNutrition(nutrition);

        return dto;
    }

    @Override
    public PageResult<RecipeListDTO> searchRecipes(String keyword, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;
        List<Recipe> recipes = recipeMapper.searchByKeyword(keyword, offset, safePageSize);
        long total = recipeMapper.countByKeyword(keyword);

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);

        return new PageResult<>(list, total, safePage, safePageSize);
    }

    @Override
    @Cacheable(value = "categories", key = "'all'", unless = "#result == null")
    public List<Category> getCategories() {
        return categoryMapper.findAllWithCount();
    }

    @Override
    public RecommendResponse getRecommendations(int limit, Integer userId) {
        return getRecommendationsByType("personal", limit, userId);
    }

    @Override
    @Cacheable(value = "recommendations", key = "#type + '_' + #limit + '_' + (#userId == null ? 'anon' : #userId)", unless = "#result == null")
    public RecommendResponse getRecommendationsByType(String type, int limit, Integer userId) {
        List<Recipe> recipes;
        String reason;
        String normalizedType = (type == null || type.isBlank()) ? "personal" : type.toLowerCase();

        switch (normalizedType) {
            case "hot":
                recipes = recipeMapper.findByCondition(null, null, null, "hot", 0, limit);
                reason = "热门推荐";
                break;
            case "new":
                recipes = recipeMapper.findByCondition(null, null, null, null, 0, limit);
                reason = "最新发布";
                break;
            case "personal":
            default:
                recipes = recipeMapper.findRandom(limit);
                reason = "猜你喜欢";
                break;
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);

        RecommendResponse response = new RecommendResponse();
        response.setList(list);
        response.setReason(reason);
        return response;
    }

    @Override
    @Cacheable(value = "similar_recipes", key = "#recipeId + '_' + #limit", unless = "#result == null")
    public List<RecipeListDTO> getSimilarRecipes(Integer recipeId, int limit) {
        List<RecipeIngredient> currentIngredients = recipeIngredientMapper.findByRecipeId(recipeId);
        List<String> mainIngredients = currentIngredients.stream()
                .filter(ing -> "main".equals(ing.getIngredientType()))
                .map(RecipeIngredient::getIngredientName)
                .distinct()
                .collect(Collectors.toList());

        if (mainIngredients.isEmpty()) {
            return List.of();
        }

        List<Recipe> similarRecipes = recipeMapper.findSimilarByIngredients(recipeId, mainIngredients, limit);
        return recipeListDTOAssembler.toListDTOBatch(similarRecipes);
    }

    @Override
    @Transactional
    public Integer createRecipe(CreateRecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setAuthor(request.getAuthor());
        recipe.setAuthorUid(request.getAuthorUid());
        recipe.setDescription(request.getDescription());
        recipe.setTips(request.getTips());
        recipe.setCookware(request.getCookware());
        recipe.setImage(request.getImage());
        
        recipe.setTasteId(getOrCreateAttributeId(
                request.getTasteId(),
                request.getTasteName(),
                tasteMapper::findByName,
                name -> {
                    Taste taste = new Taste();
                    taste.setName(name);
                    return taste;
                },
                tasteMapper::insert,
                Taste::getId));
        recipe.setTechniqueId(getOrCreateAttributeId(
                request.getTechniqueId(),
                request.getTechniqueName(),
                techniqueMapper::findByName,
                name -> {
                    Technique technique = new Technique();
                    technique.setName(name);
                    return technique;
                },
                techniqueMapper::insert,
                Technique::getId));
        recipe.setTimeCostId(getOrCreateAttributeId(
                request.getTimeCostId(),
                request.getTimeCostName(),
                timeCostMapper::findByName,
                name -> {
                    TimeCost timeCost = new TimeCost();
                    timeCost.setName(name);
                    return timeCost;
                },
                timeCostMapper::insert,
                TimeCost::getId));
        recipe.setDifficultyId(getOrCreateAttributeId(
                request.getDifficultyId(),
                request.getDifficultyName(),
                difficultyMapper::findByName,
                name -> {
                    Difficulty difficulty = new Difficulty();
                    difficulty.setName(name);
                    return difficulty;
                },
                difficultyMapper::insert,
                Difficulty::getId));
        
        recipe.setReplyCount(0);
        recipe.setLikeCount(0);
        recipe.setRatingCount(0);
        recipe.setFavoriteCount(0);

        recipeMapper.insert(recipe);
        Integer recipeId = recipe.getId();

        if (request.getIngredients() != null) {
            for (CreateRecipeRequest.IngredientItem item : request.getIngredients()) {
                Integer ingredientId = item.getIngredientId();

                if (ingredientId == null && item.getIngredientName() != null
                        && !item.getIngredientName().trim().isEmpty()) {
                    Ingredient existingIngredient = ingredientMapper.findByName(item.getIngredientName().trim());
                    if (existingIngredient != null) {
                        ingredientId = existingIngredient.getId();
                    } else {
                        Ingredient newIngredient = new Ingredient();
                        newIngredient.setName(item.getIngredientName().trim());
                        ingredientMapper.insert(newIngredient);
                        ingredientId = newIngredient.getId();
                    }
                }

                if (ingredientId != null) {
                    RecipeIngredient ingredient = new RecipeIngredient();
                    ingredient.setRecipeId(recipeId);
                    ingredient.setIngredientId(ingredientId);
                    ingredient.setIngredientType(item.getType());
                    ingredient.setQuantity(item.getQuantity());
                    recipeIngredientMapper.insert(ingredient);
                }
            }
        }

        if (request.getSteps() != null) {
            int stepNumber = 1;
            for (CreateRecipeRequest.StepItem item : request.getSteps()) {
                CookingStep step = new CookingStep();
                step.setRecipeId(recipeId);
                // 优先使用前端传来的 stepNumber，如果为 null 则自动生成
                step.setStepNumber(item.getStepNumber() != null ? item.getStepNumber() : stepNumber++);
                step.setDescription(item.getDescription());
                step.setImage(item.getImage());
                cookingStepMapper.insert(step);
            }
        }

        Set<Integer> allCategoryIds = new LinkedHashSet<>();

        if (request.getCategoryIds() != null) {
            allCategoryIds.addAll(request.getCategoryIds().stream()
                    .filter(categoryId -> categoryId != null && categoryId > 0)
                    .collect(Collectors.toList()));
        }

        if (request.getCategoryNames() != null) {
            for (String categoryName : request.getCategoryNames()) {
                if (categoryName != null && !categoryName.trim().isEmpty()) {
                    Category existingCategory = categoryMapper.findByName(categoryName.trim());
                    if (existingCategory != null) {
                        allCategoryIds.add(existingCategory.getId());
                    } else {
                        Category newCategory = new Category();
                        newCategory.setName(categoryName.trim());
                        categoryMapper.insert(newCategory);
                        allCategoryIds.add(newCategory.getId());
                    }
                }
            }
        }

        for (Integer categoryId : allCategoryIds) {
            if (categoryId != null && categoryId > 0) {
                categoryMapper.insertRecipeCategory(recipeId, categoryId);
            }
        }

        return recipeId;
    }

    private <T> Integer getOrCreateAttributeId(
            Integer id,
            String name,
            Function<String, T> findByName,
            Function<String, T> entityFactory,
            Consumer<T> insert,
            Function<T, Integer> idExtractor) {
        if (id != null) {
            return id;
        }
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String trimmedName = name.trim();

        T existing = findByName.apply(trimmedName);
        if (existing != null) {
            return idExtractor.apply(existing);
        }

        T newEntity = entityFactory.apply(trimmedName);
        insert.accept(newEntity);
        return idExtractor.apply(newEntity);
    }
}
