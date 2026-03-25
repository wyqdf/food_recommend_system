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
            String time, String sort, String mode) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        String normalizedMode = normalizeMode(mode);
        Integer difficultyId = resolveDifficultyId(difficulty);
        if (StringUtils.hasText(difficulty) && difficultyId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        Integer timeCostId = resolveTimeCostId(time);
        if (StringUtils.hasText(time) && timeCostId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        if (!StringUtils.hasText(normalizedMode)) {
            int offset = (safePage - 1) * safePageSize;
            List<Recipe> recipes = recipeMapper.findByCondition(category, difficultyId, timeCostId, sort, offset, safePageSize, null);
            long total = recipeMapper.countByCondition(category, difficultyId, timeCostId, null);
            List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);
            return new PageResult<>(list, total, safePage, safePageSize);
        }

        int candidateSize = Math.max(safePage * safePageSize * 8, 96);
        List<Recipe> candidates = recipeMapper.findByCondition(category, difficultyId, timeCostId, sort, 0, candidateSize, null);
        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(candidates);
        String sceneCode = mapModeToSceneCode(normalizedMode);
        applySceneTagsAndReasons(list, "personal", sceneCode);
        list = rerankBySceneMode(list, normalizedMode, sceneCode);

        int from = Math.min((safePage - 1) * safePageSize, list.size());
        int to = Math.min(from + safePageSize, list.size());
        List<RecipeListDTO> pageList = list.subList(from, to);
        return new PageResult<>(pageList, list.size(), safePage, safePageSize);
    }

    @Override
    @Cacheable(value = "recipe_list", key = "#page + '_' + #pageSize + '_' + (#category ?: 'all') + '_' + (#difficulty ?: 'all') + '_' + (#time ?: 'all') + '_' + (#sort ?: 'new') + '_' + (#mode ?: 'all')", unless = "#result == null")
    public PageResult<RecipeListDTO> getRecipeListCached(int page, int pageSize, Integer category, String difficulty,
            String time, String sort, String mode) {
        return getRecipeList(page, pageSize, category, difficulty, time, sort, mode);
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
                log.warn("ES 闂傚倸鍊烽懗鍫曞箠閹捐瑙﹂悗锝庡墮閸ㄦ繈骞栧ǎ顒€濡肩痪鎯с偢閺屾洘绻濊箛鏇犳殸缂備礁澧庨崑銈咁嚕閸洖閱囨い鎰垫線閸戜粙鎮跺顓犵疄婵﹥妞介獮鎰償閿濆倹顫嶉梻浣稿悑濡炲潡宕归幍顔煎疾闂備礁鎼粙渚€宕㈡禒瀣€垮┑鐘崇閻?MySQL闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗婇崕鎴濃攽椤栨氨闂痺ord={}, reason={}", keyword, e.getMessage());
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
                log.warn("ES 闂備浇顕уù鐑藉磻閿濆纾规繝闈涚墢绾捐姤鎱ㄥ鈧·鍌炲极婵犲洦鐓曟繛鎴濆船閻忥繝鎮楀顓熺殤闁逞屽墮缁犲秹宕曢柆宥呯疇閻忕偛澧介々鎻掆攽閸屾碍鍟為柛瀣剁秮閺岀喖骞嶉搹顐ｇ彅闂佸憡姊婚弫鍝ユ閹烘挻缍囬柕濞у懐鏉介柣搴ゎ潐濞叉牠鎮ユ總绋跨畺婵炲棙鍨堕崑姗€鏌嶉妷銉ュ笭濠㈣娲熷?MySQL闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鐗婇崕鎴濃攽椤栨氨闂痺ord={}, reason={}", keyword, e.getMessage());
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
        mergeSuggestions(merged, recipeMapper.findTitleSuggestions(normalizedKeyword, safeLimit), "title", "Recipe", safeLimit);
        mergeSuggestions(merged, recipeMapper.findIngredientSuggestions(normalizedKeyword, safeLimit), "ingredient", "Ingredient", safeLimit);
        mergeSuggestions(merged, recipeMapper.findCategorySuggestions(normalizedKeyword, safeLimit), "category", "Category", safeLimit);
        mergeSuggestions(merged, recipeMapper.findAuthorSuggestions(normalizedKeyword, safeLimit), "author", "Author", safeLimit);
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
                recipes = recipeMapper.findByCondition(normalizedCategoryId, null, null, "hot", 0, safeLimit, null);
                reason = "Hot Recommendations";
                break;
            case "new":
                recipes = recipeMapper.findByCondition(normalizedCategoryId, null, null, null, 0, safeLimit, null);
                reason = "Latest Recipes";
                break;
            case "personal":
            default:
                recipes = collectPersonalCandidates(
                        safeLimit * (StringUtils.hasText(normalizedScene) || normalizedCategoryId != null ? 12 : 3),
                        normalizedCategoryId);
                reason = "Recommended for you";
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
                // Prefer step number from client, otherwise auto-increment.
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
                : recipeMapper.findByCondition(categoryId, null, null, "hot", 0, safeLimit, null);
        List<Recipe> hotCandidates = recipeMapper.findByCondition(null, null, null, "hot", 0, safeLimit / 2, null);
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
                reasons.add("Popular right now");
            } else if ("new".equals(recommendationType)) {
                reasons.add("Recently published");
            }
            if (sceneName != null && tags.contains(sceneName)) {
                reasons.add("Matches selected scene: " + sceneName);
            }
            if (reasons.isEmpty()) {
                reasons.add("Recommended by your preferences");
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
                reasons.add("Matches your selected scene");
            }

            List<String> matchedPreferredScenes = intersect(dto.getSceneTags(), preferredScenes);
            if (!matchedPreferredScenes.isEmpty()) {
                score += Math.min(2.2, matchedPreferredScenes.size() * 1.1);
                reasons.add("Matches your common scene: " + matchedPreferredScenes.get(0));
            }

            if (StringUtils.hasText(dto.getTaste()) && preferredTastes.contains(dto.getTaste())) {
                score += 1.5;
                reasons.add("Matches your taste preference: " + dto.getTaste());
            }

            if (matchesTimeBudget(dto.getTime(), timeBudget)) {
                score += 0.9;
                reasons.add("Time cost matches your preference");
            }

            List<String> matchedIngredients = intersect(dto.getIngredients(), new ArrayList<>(seedIngredients));
            if (!matchedIngredients.isEmpty()) {
                score += Math.min(2.4, matchedIngredients.size() * 0.8);
                reasons.add("Contains your frequent ingredient: " + matchedIngredients.get(0));
            }

            if (!intersect(dto.getCategories(), new ArrayList<>(seedCategories)).isEmpty()) {
                score += 0.8;
            }

            if (matchesDietGoal(dietGoal, dto)) {
                score += 1.2;
                reasons.add("Matches your diet goal: " + dietGoal);
            }

            score += Math.min(1.0, (dto.getLikeCount() == null ? 0 : dto.getLikeCount()) / 500.0);

            if (reasons.isEmpty()) {
                reasons.add("Recommended from your recent browsing and favorites");
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
                        reasons.add("Matches your selected scene");
                        dto.setReasons(reasons.stream().distinct().limit(2).collect(Collectors.toList()));
                    })
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(categoryName)) {
            selected = selected.stream()
                    .filter(dto -> dto.getCategories() != null && dto.getCategories().contains(categoryName))
                    .peek(dto -> {
                        List<String> reasons = dto.getReasons() == null ? new ArrayList<>() : new ArrayList<>(dto.getReasons());
                        reasons.add("Belongs to selected category: " + categoryName);
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
                ? recipeMapper.findByCondition(categoryId, null, null, "hot", 0, Math.max(safeLimit * 4, 24), null)
                : recipeMapper.findByCondition(null, null, null, "hot", 0, Math.max(safeLimit * 4, 24), null);
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
                reasons.add("Supplement with quality recipes in selected category");
            } else if (StringUtils.hasText(sceneCode)) {
                reasons.add("Supplement with popular recipes in selected scene");
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
            suffixes.add("scene: " + SceneTagResolver.sceneName(normalizedScene));
        }
        if (StringUtils.hasText(categoryName)) {
            suffixes.add("category: " + categoryName);
        }
        if (suffixes.isEmpty()) {
            return defaultReason;
        }
        return defaultReason + " (" + String.join(", ", suffixes) + ")";
    }

    private boolean matchesTimeBudget(String recipeTime, String timeBudget) {
        if (!StringUtils.hasText(recipeTime) || !StringUtils.hasText(timeBudget)) {
            return false;
        }
        if ("quick".equals(timeBudget)) {
            return recipeTime.contains("10") || recipeTime.contains("15") || recipeTime.contains("20")
                    || recipeTime.contains("30");
        }
        if ("medium".equals(timeBudget)) {
            return recipeTime.contains("30") || recipeTime.contains("40") || recipeTime.contains("45")
                    || recipeTime.contains("1h");
        }
        if ("long".equals(timeBudget)) {
            return recipeTime.contains("1h") || recipeTime.contains("2h") || recipeTime.contains("slow");
        }
        return false;
    }

    private boolean matchesDietGoal(String dietGoal, RecipeListDTO dto) {
        if (!StringUtils.hasText(dietGoal)) {
            return false;
        }
        if ("cut".equalsIgnoreCase(dietGoal) || "diet".equalsIgnoreCase(dietGoal)) {
            return dto.getSceneTags() != null && dto.getSceneTags().contains(SceneTagResolver.sceneName("diet"));
        }
        if ("bulk".equalsIgnoreCase(dietGoal) || "muscle".equalsIgnoreCase(dietGoal)) {
            return containsAny(dto.getIngredients(), "chicken", "beef", "egg", "shrimp");
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

    private boolean containsAny(String text, String... keys) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyList(List<String> values, String... keys) {
        return containsAny(values, keys);
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

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return null;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "family", "fitness", "quick", "party" -> normalized;
            default -> null;
        };
    }

    private String mapModeToSceneCode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return null;
        }
        return switch (mode) {
            case "family" -> "family";
            case "fitness" -> "diet";
            case "quick" -> "quick";
            case "party" -> "banquet";
            default -> null;
        };
    }

    private List<RecipeListDTO> rerankBySceneMode(List<RecipeListDTO> list, String mode, String sceneCode) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<RecipeListDTO> sorted = list.stream()
                .sorted(Comparator
                        .comparingDouble((RecipeListDTO item) -> sceneModeScore(item, mode, sceneCode)).reversed()
                        .thenComparing(item -> item.getLikeCount() == null ? 0 : item.getLikeCount(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if ("quick".equals(mode)) {
            List<RecipeListDTO> quickFirst = sorted.stream()
                    .filter(this::isQuickFriendlyByTime)
                    .collect(Collectors.toList());
            if (!quickFirst.isEmpty()) {
                return quickFirst;
            }
        }
        return sorted;
    }

    private double sceneModeScore(RecipeListDTO dto, String mode, String sceneCode) {
        double score = 0;
        String name = dto.getName() == null ? "" : dto.getName();
        String time = dto.getTime() == null ? "" : dto.getTime();
        String difficulty = dto.getDifficulty() == null ? "" : dto.getDifficulty();
        String taste = dto.getTaste() == null ? "" : dto.getTaste();
        List<String> categories = dto.getCategories() == null ? List.of() : dto.getCategories();
        List<String> ingredients = dto.getIngredients() == null ? List.of() : dto.getIngredients();

        if (StringUtils.hasText(sceneCode) && SceneTagResolver.matchesScene(dto, sceneCode)) {
            score += 10;
        }

        switch (mode) {
            case "family" -> {
                if (containsAny(name, "家常", "家庭", "全家", "儿童", "下饭", "营养")) score += 4;
                if (containsAnyList(categories, "家常菜", "家庭餐", "儿童餐", "下饭菜", "汤")) score += 4;
                if (containsAnyList(ingredients, "鸡蛋", "番茄", "土豆", "豆腐", "青菜")) score += 2;
            }
            case "fitness" -> {
                if (containsAny(name, "减脂", "健身", "轻食", "低卡", "高蛋白", "沙拉")) score += 5;
                if (containsAnyList(categories, "减脂", "健身", "轻食", "沙拉", "低脂")) score += 4;
                if (containsAnyList(ingredients, "鸡胸", "鸡胸肉", "虾", "虾仁", "鱼", "西兰花", "蛋白", "豆腐", "燕麦")) score += 4;
                if (containsAny(taste, "清淡", "原味")) score += 1;
            }
            case "quick" -> {
                if (containsAny(time, "10", "15", "20")) score += 5;
                if (containsAny(difficulty, "简单")) score += 2;
                if (containsAny(name, "快手", "速食", "一人食", "便当", "懒人")) score += 4;
                if (containsAnyList(categories, "快手菜", "一人食", "便当", "早餐")) score += 3;
                if (!isQuickFriendlyByTime(dto)) score -= 8;
            }
            case "party" -> {
                if (containsAny(name, "聚会", "宴客", "硬菜", "甜点", "招待", "烘焙", "派对")) score += 5;
                if (containsAnyList(categories, "宴客菜", "聚会", "甜点", "烘焙", "下午茶")) score += 4;
                if (containsAny(difficulty, "高级")) score += 2;
            }
            default -> {
            }
        }

        score += Math.min(2.0, (dto.getLikeCount() == null ? 0 : dto.getLikeCount()) / 300.0);
        return score;
    }

    private boolean isQuickFriendlyByTime(RecipeListDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTime())) {
            return false;
        }
        String time = dto.getTime().replaceAll("\\s+", "");
        if (time.contains("10") || time.contains("15") || time.contains("20")) {
            return true;
        }
        Integer minutes = parseMinutes(time);
        return minutes != null && minutes <= 20;
    }

    private Integer parseMinutes(String timeText) {
        if (!StringUtils.hasText(timeText)) {
            return null;
        }
        String text = timeText.trim();
        try {
            if (text.contains("小时")) {
                int hour = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                return hour * 60;
            }
            if (text.contains("分钟") || text.contains("分")) {
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
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

