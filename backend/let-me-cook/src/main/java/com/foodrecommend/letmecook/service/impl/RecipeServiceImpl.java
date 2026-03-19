package com.foodrecommend.letmecook.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.config.SearchProperties;
import com.foodrecommend.letmecook.dto.*;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.RecipeListDTOAssembler;
import com.foodrecommend.letmecook.service.RecipeService;
import com.foodrecommend.letmecook.search.RecipeSearchService;
import com.foodrecommend.letmecook.search.RecipeSearchSyncEvent;
import com.foodrecommend.letmecook.util.SceneTagResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
    private final InteractionMapper interactionMapper;
    private final BehaviorEventMapper behaviorEventMapper;
    private final UserPreferenceProfileMapper userPreferenceProfileMapper;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;
    private final RecipeSearchService recipeSearchService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public PageResult<RecipeListDTO> getRecipeList(int page, int pageSize, Integer category, String difficulty,
            String time, String sort) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;
        Integer difficultyId = resolveDifficultyId(difficulty);
        if (StringUtils.hasText(difficulty) && difficultyId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        Integer timeCostId = resolveTimeCostId(time);
        if (StringUtils.hasText(time) && timeCostId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        List<Recipe> recipes = recipeMapper.findByCondition(category, difficultyId, timeCostId, sort, offset, safePageSize);
        long total = recipeMapper.countByCondition(category, difficultyId, timeCostId);

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
    public PageResult<RecipeListDTO> searchRecipes(String keyword, String sort, int page, int pageSize) {
        if (searchProperties.useElasticsearch()) {
            try {
                return recipeSearchService.searchRecipes(keyword, sort, page, pageSize);
            } catch (Exception e) {
                log.warn("ES 搜索失败，回退 MySQL。keyword={}, reason={}", keyword, e.getMessage());
            }
        }
        return searchRecipesByMySql(keyword, sort, page, pageSize);
    }

    private PageResult<RecipeListDTO> searchRecipesByMySql(String keyword, String sort, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        String normalizedSort = normalizeSearchSort(sort);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }
        int offset = (safePage - 1) * safePageSize;
        List<Recipe> recipes = recipeMapper.searchByKeyword(normalizedKeyword, normalizedSort, offset, safePageSize);
        long total = recipeMapper.countByKeyword(normalizedKeyword);

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);

        return new PageResult<>(list, total, safePage, safePageSize);
    }

    @Override
    public List<SearchSuggestionDTO> getSearchSuggestions(String keyword, int limit) {
        if (searchProperties.useElasticsearch()) {
            try {
                return recipeSearchService.getSearchSuggestions(keyword, limit);
            } catch (Exception e) {
                log.warn("ES 建议词失败，回退 MySQL。keyword={}, reason={}", keyword, e.getMessage());
            }
        }
        return getSearchSuggestionsByMySql(keyword, limit);
    }

    private List<SearchSuggestionDTO> getSearchSuggestionsByMySql(String keyword, int limit) {
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return List.of();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 10);
        LinkedHashMap<String, SearchSuggestionDTO> merged = new LinkedHashMap<>();
        mergeSuggestions(merged, recipeMapper.findTitleSuggestions(normalizedKeyword, safeLimit), "title", "菜谱", safeLimit);
        mergeSuggestions(merged, recipeMapper.findIngredientSuggestions(normalizedKeyword, safeLimit), "ingredient", "食材", safeLimit);
        mergeSuggestions(merged, recipeMapper.findCategorySuggestions(normalizedKeyword, safeLimit), "category", "分类", safeLimit);
        mergeSuggestions(merged, recipeMapper.findAuthorSuggestions(normalizedKeyword, safeLimit), "author", "作者", safeLimit);
        return merged.values().stream().limit(safeLimit).toList();
    }

    @Override
    @Cacheable(value = "categories", key = "'all'", unless = "#result == null")
    public List<Category> getCategories() {
        return categoryMapper.findAllWithCount();
    }

    @Override
    public RecommendResponse getRecommendations(int limit, Integer userId) {
        return getRecommendationsByType("personal", limit, userId, null, null);
    }

    @Override
    public RecommendResponse getRecommendationsByType(String type, int limit, Integer userId) {
        return getRecommendationsByType(type, limit, userId, null, null);
    }

    @Override
    public RecommendResponse getRecommendationsByType(String type, int limit, Integer userId, String sceneCode) {
        return getRecommendationsByType(type, limit, userId, sceneCode, null);
    }

    @Override
    @Cacheable(value = "recommendations", key = "#type + '_' + #limit + '_' + (#userId == null ? 'anon' : #userId) + '_' + (#sceneCode == null ? 'all' : #sceneCode) + '_' + (#categoryId == null ? 'all' : #categoryId)", unless = "#result == null")
    public RecommendResponse getRecommendationsByType(String type, int limit, Integer userId, String sceneCode, Integer categoryId) {
        List<Recipe> recipes;
        String reason;
        int safeLimit = Math.max(limit, 1);
        String normalizedType = (type == null || type.isBlank()) ? "personal" : type.toLowerCase();
        String normalizedScene = normalizeSceneCode(sceneCode);
        Integer normalizedCategoryId = normalizeCategoryId(categoryId);
        String selectedCategoryName = resolveCategoryName(normalizedCategoryId);

        switch (normalizedType) {
            case "hot":
                recipes = recipeMapper.findByCondition(normalizedCategoryId, null, null, "hot", 0, safeLimit);
                reason = "热门推荐";
                break;
            case "new":
                recipes = recipeMapper.findByCondition(normalizedCategoryId, null, null, null, 0, safeLimit);
                reason = "最新发布";
                break;
            case "personal":
            default:
                recipes = collectPersonalCandidates(
                        safeLimit * (StringUtils.hasText(normalizedScene) || normalizedCategoryId != null ? 12 : 3),
                        normalizedCategoryId);
                reason = "根据你的偏好推荐";
                break;
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);
        applySceneTagsAndReasons(list, normalizedType, normalizedScene);

        if ("personal".equals(normalizedType)) {
            int rerankLimit = (StringUtils.hasText(normalizedScene) || StringUtils.hasText(selectedCategoryName))
                    ? safeLimit * 12
                    : safeLimit;
            applyPersonalRerank(list, userId, normalizedScene, rerankLimit);
            list = applySelection(list, normalizedScene, selectedCategoryName, safeLimit);
        } else {
            list = applySelection(list, normalizedScene, selectedCategoryName, safeLimit);
        }
        list = supplementSelection(list, normalizedScene, normalizedCategoryId, selectedCategoryName, safeLimit);

        RecommendResponse response = new RecommendResponse();
        response.setList(list);
        response.setReason(buildResponseReason(normalizedScene, selectedCategoryName, reason));
        return response;
    }

    @Override
    @Cacheable(value = "similar_recipes", key = "#recipeId + '_' + #limit", unless = "#result == null")
    public List<RecipeListDTO> getSimilarRecipes(Integer recipeId, int limit) {
        List<RecipeIngredient> currentIngredients = recipeIngredientMapper.findByRecipeId(recipeId);
        List<Integer> mainIngredientIds = currentIngredients.stream()
                .filter(ing -> "main".equals(ing.getIngredientType()))
                .map(RecipeIngredient::getIngredientId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (mainIngredientIds.isEmpty()) {
            return List.of();
        }

        List<Recipe> similarRecipes = recipeMapper.findSimilarByIngredients(recipeId, mainIngredientIds, limit);
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

        publishSearchUpsert(recipeId);
        return recipeId;
    }

    private List<Recipe> collectPersonalCandidates(int candidateLimit, Integer categoryId) {
        int safeLimit = Math.max(candidateLimit, 24);
        List<Recipe> categoryCandidates = categoryId == null
                ? List.of()
                : recipeMapper.findByCondition(categoryId, null, null, "hot", 0, safeLimit);
        List<Recipe> hotCandidates = recipeMapper.findByCondition(null, null, null, "hot", 0, safeLimit / 2);
        List<Recipe> randomCandidates = recipeMapper.findRandom(safeLimit);

        Map<Integer, Recipe> merged = new LinkedHashMap<>();
        for (Recipe recipe : categoryCandidates) {
            merged.put(recipe.getId(), recipe);
        }
        for (Recipe recipe : hotCandidates) {
            merged.put(recipe.getId(), recipe);
        }
        for (Recipe recipe : randomCandidates) {
            merged.putIfAbsent(recipe.getId(), recipe);
        }
        return new ArrayList<>(merged.values());
    }

    private void applySceneTagsAndReasons(List<RecipeListDTO> list, String recommendationType, String sceneCode) {
        String sceneName = StringUtils.hasText(sceneCode) ? SceneTagResolver.sceneName(sceneCode) : null;
        for (RecipeListDTO dto : list) {
            List<String> tags = SceneTagResolver.resolveTagNames(dto);
            dto.setSceneTags(tags);

            List<String> reasons = new ArrayList<>();
            if ("hot".equals(recommendationType)) {
                reasons.add("当前热度较高");
            } else if ("new".equals(recommendationType)) {
                reasons.add("最近新发布");
            }
            if (sceneName != null && tags.contains(sceneName)) {
                reasons.add("匹配你选择的场景：" + sceneName);
            }
            if (reasons.isEmpty()) {
                reasons.add("基于你的偏好推荐");
            }
            dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
        }

        if (sceneName != null) {
            list.sort(Comparator.comparing((RecipeListDTO item) ->
                    item.getSceneTags() != null && item.getSceneTags().contains(sceneName)).reversed());
        }
    }

    private void applyPersonalRerank(List<RecipeListDTO> list, Integer userId, String sceneCode, int limit) {
        if (list == null || list.isEmpty()) {
            return;
        }

        String sceneName = StringUtils.hasText(sceneCode) ? SceneTagResolver.sceneName(sceneCode) : null;
        UserPreferenceProfile profile = userId == null ? null : userPreferenceProfileMapper.findByUserId(userId);

        List<String> preferredScenes = profile == null ? List.of() : parseJsonList(profile.getPreferredScenesJson());
        List<String> preferredTastes = profile == null ? List.of() : parseJsonList(profile.getPreferredTastesJson());
        String timeBudget = profile == null ? null : profile.getTimeBudget();
        String dietGoal = profile == null ? null : profile.getDietGoal();

        Set<String> seedIngredients = new HashSet<>();
        Set<String> seedCategories = new HashSet<>();
        if (userId != null) {
            Set<Integer> seedRecipeIds = new LinkedHashSet<>();

            List<Integer> favoriteIds = interactionMapper.findFavoriteRecipeIds(userId);
            for (int i = 0; i < favoriteIds.size() && i < 20; i++) {
                seedRecipeIds.add(favoriteIds.get(i));
            }
            List<Integer> behaviorIds = getRecentDistinctBehaviorRecipeIds(userId, 20);
            seedRecipeIds.addAll(behaviorIds);

            if (!seedRecipeIds.isEmpty()) {
                List<Integer> idList = new ArrayList<>(seedRecipeIds).stream().limit(30).toList();
                List<Recipe> seedRecipes = recipeMapper.findByIds(idList);
                List<RecipeListDTO> seedRecipeDtos = recipeListDTOAssembler.toListDTOBatch(seedRecipes);
                for (RecipeListDTO dto : seedRecipeDtos) {
                    if (dto.getIngredients() != null) {
                        seedIngredients.addAll(dto.getIngredients());
                    }
                    if (dto.getCategories() != null) {
                        seedCategories.addAll(dto.getCategories());
                    }
                }
            }
        }

        List<ScoredRecipe> scored = new ArrayList<>(list.size());
        for (RecipeListDTO dto : list) {
            double score = 0;
            List<String> reasons = new ArrayList<>(dto.getReasons() == null ? List.of() : dto.getReasons());

            if (sceneName != null && dto.getSceneTags() != null && dto.getSceneTags().contains(sceneName)) {
                score += 3.2;
                reasons.add("匹配你当前选择的场景");
            }

            List<String> matchedPreferredScenes = intersect(dto.getSceneTags(), preferredScenes);
            if (!matchedPreferredScenes.isEmpty()) {
                score += Math.min(2.2, matchedPreferredScenes.size() * 1.1);
                reasons.add("符合你常用场景：" + matchedPreferredScenes.get(0));
            }

            if (StringUtils.hasText(dto.getTaste()) && preferredTastes.contains(dto.getTaste())) {
                score += 1.5;
                reasons.add("符合你的口味偏好：" + dto.getTaste());
            }

            if (matchesTimeBudget(dto.getTime(), timeBudget)) {
                score += 0.9;
                reasons.add("耗时符合你的烹饪时间偏好");
            }

            List<String> matchedIngredients = intersect(dto.getIngredients(), new ArrayList<>(seedIngredients));
            if (!matchedIngredients.isEmpty()) {
                score += Math.min(2.4, matchedIngredients.size() * 0.8);
                reasons.add("含有你常做食材：" + matchedIngredients.get(0));
            }

            if (!intersect(dto.getCategories(), new ArrayList<>(seedCategories)).isEmpty()) {
                score += 0.8;
            }

            if (matchesDietGoal(dietGoal, dto)) {
                score += 1.2;
                reasons.add("符合你的饮食目标：" + dietGoal);
            }

            score += Math.min(1.0, (dto.getLikeCount() == null ? 0 : dto.getLikeCount()) / 500.0);

            if (reasons.isEmpty()) {
                reasons.add("根据你近期浏览与收藏习惯推荐");
            }

            dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
            scored.add(new ScoredRecipe(dto, score));
        }

        scored.sort(Comparator
                .comparingDouble(ScoredRecipe::score).reversed()
                .thenComparing(item -> item.recipe().getLikeCount() == null ? 0 : item.recipe().getLikeCount(),
                        Comparator.reverseOrder()));

        list.clear();
        list.addAll(scored.stream().map(ScoredRecipe::recipe).limit(Math.max(limit, 1)).collect(Collectors.toList()));
    }

    private List<RecipeListDTO> applySelection(List<RecipeListDTO> list, String sceneCode, String categoryName, int limit) {
        int safeLimit = Math.max(limit, 1);
        List<RecipeListDTO> selected = new ArrayList<>(list);

        if (StringUtils.hasText(sceneCode)) {
            selected = selected.stream()
                    .filter(dto -> SceneTagResolver.matchesScene(dto, sceneCode))
                    .peek(dto -> {
                        List<String> reasons = dto.getReasons() == null ? new ArrayList<>() : new ArrayList<>(dto.getReasons());
                        reasons.add("匹配你当前选择的场景");
                        dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
                    })
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(categoryName)) {
            selected = selected.stream()
                    .filter(dto -> dto.getCategories() != null && dto.getCategories().contains(categoryName))
                    .peek(dto -> {
                        List<String> reasons = dto.getReasons() == null ? new ArrayList<>() : new ArrayList<>(dto.getReasons());
                        reasons.add("属于你选择的分类：" + categoryName);
                        dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
                    })
                    .collect(Collectors.toList());
        }

        return selected.stream().limit(safeLimit).collect(Collectors.toList());
    }

    private List<RecipeListDTO> supplementSelection(List<RecipeListDTO> list,
            String sceneCode,
            Integer categoryId,
            String categoryName,
            int limit) {
        int safeLimit = Math.max(limit, 1);
        if (list.size() >= safeLimit || (categoryId == null && !StringUtils.hasText(sceneCode))) {
            return list.stream().limit(safeLimit).collect(Collectors.toList());
        }

        List<Recipe> fallbackRecipes = categoryId != null
                ? recipeMapper.findByCondition(categoryId, null, null, "hot", 0, Math.max(safeLimit * 4, 24))
                : recipeMapper.findByCondition(null, null, null, "hot", 0, Math.max(safeLimit * 4, 24));
        List<RecipeListDTO> fallbackList = recipeListDTOAssembler.toListDTOBatch(fallbackRecipes);
        applySceneTagsAndReasons(fallbackList, "hot", sceneCode);
        fallbackList = applySelection(fallbackList, sceneCode, categoryName, Math.max(safeLimit * 4, 24));

        LinkedHashMap<Integer, RecipeListDTO> merged = new LinkedHashMap<>();
        for (RecipeListDTO dto : list) {
            if (dto.getId() != null) {
                merged.put(dto.getId(), dto);
            }
        }
        for (RecipeListDTO dto : fallbackList) {
            if (dto.getId() == null || merged.containsKey(dto.getId())) {
                continue;
            }
            List<String> reasons = dto.getReasons() == null ? new ArrayList<>() : new ArrayList<>(dto.getReasons());
            if (StringUtils.hasText(categoryName)) {
                reasons.add("补充你选择分类下的优质菜谱");
            } else if (StringUtils.hasText(sceneCode)) {
                reasons.add("补充你选择场景下的热门菜谱");
            }
            dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
            merged.put(dto.getId(), dto);
            if (merged.size() >= safeLimit) {
                break;
            }
        }

        return merged.values().stream().limit(safeLimit).collect(Collectors.toList());
    }

    private String buildResponseReason(String normalizedScene, String categoryName, String defaultReason) {
        List<String> suffixes = new ArrayList<>();
        if (StringUtils.hasText(normalizedScene)) {
            suffixes.add("场景：" + SceneTagResolver.sceneName(normalizedScene));
        }
        if (StringUtils.hasText(categoryName)) {
            suffixes.add("分类：" + categoryName);
        }
        if (suffixes.isEmpty()) {
            return defaultReason;
        }
        return defaultReason + "（" + String.join("，", suffixes) + "）";
    }

    private boolean matchesTimeBudget(String recipeTime, String timeBudget) {
        if (!StringUtils.hasText(recipeTime) || !StringUtils.hasText(timeBudget)) {
            return false;
        }
        if ("quick".equals(timeBudget)) {
            return recipeTime.contains("10") || recipeTime.contains("15") || recipeTime.contains("20")
                    || recipeTime.contains("半小时") || recipeTime.contains("30分钟");
        }
        if ("medium".equals(timeBudget)) {
            return recipeTime.contains("30") || recipeTime.contains("40") || recipeTime.contains("45")
                    || recipeTime.contains("1小时");
        }
        if ("long".equals(timeBudget)) {
            return recipeTime.contains("1小时") || recipeTime.contains("2小时") || recipeTime.contains("慢炖");
        }
        return false;
    }

    private boolean matchesDietGoal(String dietGoal, RecipeListDTO dto) {
        if (!StringUtils.hasText(dietGoal)) {
            return false;
        }
        if ("减脂".equals(dietGoal)) {
            return dto.getSceneTags() != null && dto.getSceneTags().contains(SceneTagResolver.sceneName("diet"));
        }
        if ("增肌".equals(dietGoal)) {
            return containsAny(dto.getIngredients(), "鸡胸", "牛肉", "鸡蛋", "虾");
        }
        return false;
    }

    private boolean containsAny(List<String> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            for (String key : keys) {
                if (value.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> intersect(List<String> source, List<String> target) {
        if (source == null || source.isEmpty() || target == null || target.isEmpty()) {
            return List.of();
        }
        Set<String> targetSet = new HashSet<>(target);
        return source.stream().filter(targetSet::contains).distinct().collect(Collectors.toList());
    }

    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Integer resolveDifficultyId(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return null;
        }
        Difficulty entity = difficultyMapper.findByName(difficulty.trim());
        return entity == null ? null : entity.getId();
    }

    private Integer resolveTimeCostId(String time) {
        if (!StringUtils.hasText(time)) {
            return null;
        }
        TimeCost entity = timeCostMapper.findByName(time.trim());
        return entity == null ? null : entity.getId();
    }

    private List<Integer> getRecentDistinctBehaviorRecipeIds(Integer userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }

        int sampleSize = Math.max(limit * 6, 60);
        LinkedHashSet<Integer> distinctIds = new LinkedHashSet<>();
        for (Integer recipeId : behaviorEventMapper.findRecentRecipeIdsByUser(userId, sampleSize)) {
            if (recipeId == null || recipeId <= 0) {
                continue;
            }
            distinctIds.add(recipeId);
            if (distinctIds.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(distinctIds);
    }

    private String normalizeSceneCode(String sceneCode) {
        if (!StringUtils.hasText(sceneCode)) {
            return null;
        }
        String code = sceneCode.trim().toLowerCase();
        return code.isEmpty() ? null : code;
    }

    private Integer normalizeCategoryId(Integer categoryId) {
        return categoryId != null && categoryId > 0 ? categoryId : null;
    }

    private String resolveCategoryName(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.findById(categoryId);
        return category == null ? null : category.getName();
    }

    private String normalizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSearchSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "relevance";
        }
        String normalized = sort.trim().toLowerCase();
        if ("hot".equals(normalized) || "new".equals(normalized)) {
            return normalized;
        }
        return "relevance";
    }

    private void mergeSuggestions(Map<String, SearchSuggestionDTO> merged,
            List<String> values,
            String type,
            String typeLabel,
            int limit) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String normalizedValue = value.trim();
            merged.putIfAbsent(normalizedValue, new SearchSuggestionDTO(normalizedValue, type, typeLabel));
            if (merged.size() >= limit) {
                return;
            }
        }
    }

    private void publishSearchUpsert(Integer recipeId) {
        if (recipeId == null || recipeId <= 0) {
            return;
        }
        applicationEventPublisher.publishEvent(RecipeSearchSyncEvent.upsert(List.of(recipeId)));
    }

    private record ScoredRecipe(RecipeListDTO recipe, double score) {
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
