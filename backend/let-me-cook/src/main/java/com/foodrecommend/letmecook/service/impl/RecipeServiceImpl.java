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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private static final int CATEGORY_REALTIME_POOL_SIZE = 240;
    private static final long CATEGORY_REALTIME_POOL_TTL_MILLIS = 10 * 60 * 1000L;
    private static final int SCENE_CANDIDATE_POOL_MIN_SIZE = 200;
    private static final int SCENE_CANDIDATE_POOL_MAX_SIZE = 2400;
    // 热门页分类筛选优先返回风格差异更大的代表分类，避免午餐/晚餐/热菜这类高重叠项挤满入口。
    private static final List<List<String>> DIVERSE_RECOMMEND_CATEGORY_SLOTS = List.of(
            List.of("家常菜"),
            List.of("工作餐", "快手菜", "懒人食谱", "快餐"),
            List.of("早餐"),
            List.of("汤类", "汤羹", "羹类"),
            List.of("凉菜", "小菜"),
            List.of("烘焙", "烤箱菜"),
            List.of("甜品", "下午茶", "糕点", "蛋糕", "饼干", "面包", "糖水", "冰品"),
            List.of("川菜", "湘菜", "粤菜", "鲁菜", "东北菜", "北京菜"),
            List.of("西餐", "外国美食", "日本料理", "韩国料理"),
            List.of("宴客菜", "朋友聚餐", "中式宴请", "酒席")
    );
    private static final Set<String> GENERIC_RECOMMEND_CATEGORY_NAMES = Set.of(
            "热菜", "午餐", "晚餐", "主食",
            "老人", "儿童", "婴儿",
            "春季食谱", "夏季食谱", "秋季食谱", "冬季食谱",
            "常见菜式"
    );
    private static final Set<String> COMMON_INGREDIENT_STOPWORDS = Set.of(
            "盐", "食盐", "糖", "白糖", "冰糖", "红糖",
            "酱油", "生抽", "老抽", "料酒",
            "食用油", "植物油", "玉米油", "花生油", "菜籽油",
            "淀粉", "水淀粉",
            "鸡精", "味精",
            "胡椒粉", "白胡椒粉", "黑胡椒粉"
    );

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
    private final DailyRecommendationMapper dailyRecommendationMapper;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;
    private final RecipeSearchService recipeSearchService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<Integer, CategoryRealtimePool> categoryRealtimePoolCache = new ConcurrentHashMap<>();

    @Override
    public PageResult<RecipeListDTO> getRecipeList(int page, int pageSize, Integer category, String difficulty,
            String time, String sort, String mode) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        Integer difficultyId = resolveDifficultyId(difficulty);
        if (StringUtils.hasText(difficulty) && difficultyId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        Integer timeCostId = resolveTimeCostId(time);
        if (StringUtils.hasText(time) && timeCostId == null) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        String normalizedMode = normalizeMode(mode);
        if (!StringUtils.hasText(normalizedMode)) {
            int offset = (safePage - 1) * safePageSize;
            List<Recipe> recipes = recipeMapper.findByCondition(category, difficultyId, timeCostId, sort, offset,
                    safePageSize);
            long total = recipeMapper.countByCondition(category, difficultyId, timeCostId);
            List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);
            return new PageResult<>(list, total, safePage, safePageSize);
        }

        long total = recipeMapper.countByCondition(category, difficultyId, timeCostId);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        int fetchSize = (int) Math.min(Math.max(total, (long) safePage * safePageSize), 5000L);
        List<Recipe> candidates = recipeMapper.findByCondition(category, difficultyId, timeCostId, sort, 0, fetchSize);
        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(candidates);
        String sceneCode = mapModeToSceneCode(normalizedMode);
        applySceneTagsAndReasons(list, "catalog", sceneCode, false, false);
        applyCategoryContextReasons(list, resolveCategoryName(normalizeCategoryId(category)));
        list = rerankBySceneMode(list, normalizedMode, sceneCode, sort);

        int from = Math.min((safePage - 1) * safePageSize, list.size());
        int to = Math.min(from + safePageSize, list.size());
        List<RecipeListDTO> pageList = new ArrayList<>(list.subList(from, to));
        return new PageResult<>(pageList, list.size(), safePage, safePageSize);
    }

    @Override
    @Cacheable(value = "recipe_list", key = "#page + '_' + #pageSize + '_' + (#category ?: 'all') + '_' + (#difficulty ?: 'all') + '_' + (#time ?: 'all') + '_' + (#sort ?: 'new') + '_' + (#mode ?: 'all')", unless = "#result == null")
    public PageResult<RecipeListDTO> getRecipeListCached(int page, int pageSize, Integer category, String difficulty,
            String time, String sort, String mode) {
        return getRecipeList(page, pageSize, category, difficulty, time, sort, mode);
    }

    @Override
    @Cacheable(value = "recipe_detail", key = "#id", unless = "#result == null")
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
        if (recipeSearchService.shouldUseElasticsearch()) {
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
    @Cacheable(value = "search_suggestions",
            key = "(#keyword == null ? '' : #keyword.trim().toLowerCase()) + '_' + #limit",
            unless = "#result == null || #result.isEmpty()")
    public List<SearchSuggestionDTO> getSearchSuggestions(String keyword, int limit) {
        if (recipeSearchService.shouldUseElasticsearch()) {
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
    @Cacheable(value = "recommend_categories", key = "#limit", unless = "#result == null")
    public List<Category> getRecommendCategories(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        List<Category> candidates = categoryMapper.findAllWithCount().stream()
                .filter(category -> category != null
                        && category.getId() != null
                        && StringUtils.hasText(category.getName())
                        && safeCategoryRecipeCount(category) > 0)
                .sorted(Comparator
                        .comparingInt(RecipeServiceImpl::safeCategoryRecipeCount)
                        .reversed()
                        .thenComparing(Category::getId))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Category> selected = new ArrayList<>();
        Set<Integer> selectedIds = new LinkedHashSet<>();
        for (List<String> slot : DIVERSE_RECOMMEND_CATEGORY_SLOTS) {
            Category matched = findPreferredRecommendCategory(candidates, slot, selectedIds);
            if (matched == null) {
                continue;
            }
            selected.add(matched);
            selectedIds.add(matched.getId());
            if (selected.size() >= safeLimit) {
                return selected;
            }
        }

        for (Category candidate : candidates) {
            if (selected.size() >= safeLimit) {
                break;
            }
            if (selectedIds.contains(candidate.getId())) {
                continue;
            }
            if (GENERIC_RECOMMEND_CATEGORY_NAMES.contains(candidate.getName())) {
                continue;
            }
            selected.add(candidate);
            selectedIds.add(candidate.getId());
        }

        return selected;
    }

    private static int safeCategoryRecipeCount(Category category) {
        return category == null || category.getRecipeCount() == null ? 0 : category.getRecipeCount();
    }

    private Category findPreferredRecommendCategory(List<Category> candidates, List<String> preferredNames, Set<Integer> selectedIds) {
        for (String preferredName : preferredNames) {
            for (Category candidate : candidates) {
                if (selectedIds.contains(candidate.getId())) {
                    continue;
                }
                if (preferredName.equals(candidate.getName())) {
                    return candidate;
                }
            }
        }
        return null;
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
    @Cacheable(
            value = "recommendations",
            key = "#type + '_' + #limit + '_' + (#userId == null ? 'anon' : #userId) + '_' + (#sceneCode == null ? 'all' : #sceneCode) + '_' + (#categoryId == null ? 'all' : #categoryId)",
            condition = "#type != null && (#type.equalsIgnoreCase('hot') || #type.equalsIgnoreCase('new') || (#userId == null && #type.equalsIgnoreCase('personal')))",
            unless = "#result == null"
    )
    public RecommendResponse getRecommendationsByType(String type, int limit, Integer userId, String sceneCode, Integer categoryId) {
        List<Recipe> recipes;
        String reason;
        int safeLimit = Math.max(limit, 1);
        String normalizedType = (type == null || type.isBlank()) ? "personal" : type.toLowerCase();
        String normalizedScene = normalizeSceneCode(sceneCode);
        Integer normalizedCategoryId = normalizeCategoryId(categoryId);
        String selectedCategoryName = resolveCategoryName(normalizedCategoryId);

        if (userId != null) {
            if ("daily".equals(normalizedType)) {
                RecommendResponse offlineResponse = getOfflineRankedRecommendations(safeLimit, userId, normalizedType);
                if (offlineResponse != null) {
                    return offlineResponse;
                }
            } else if ("personal".equals(normalizedType)) {
                RecommendResponse blendedResponse = getBlendedPersonalRecommendations(
                        safeLimit,
                        userId,
                        normalizedScene,
                        normalizedCategoryId,
                        selectedCategoryName);
                if (blendedResponse != null) {
                    return blendedResponse;
                }
            }
        }

        if ("personal".equals(normalizedType) && normalizedCategoryId != null) {
            RecommendResponse response = new RecommendResponse();
            response.setList(buildRealtimePersonalRecommendationList(
                    safeLimit,
                    userId,
                    normalizedScene,
                    normalizedCategoryId,
                    selectedCategoryName));
            response.setReason(buildResponseReason(normalizedScene, selectedCategoryName, "综合推荐"));
            return response;
        }

        switch (normalizedType) {
            case "daily":
                recipes = collectPersonalCandidates(resolvePersonalCandidatePoolSize(safeLimit, false), normalizedCategoryId);
                reason = "今日推荐暂未生成，已切换为实时推荐";
                break;
            case "hot":
                recipes = recipeMapper.findByCondition(
                        normalizedCategoryId,
                        null,
                        null,
                        "hot",
                        0,
                        resolveSceneCandidatePoolSize(safeLimit, normalizedScene)
                );
                reason = "热门推荐";
                break;
            case "new":
                recipes = recipeMapper.findByCondition(
                        normalizedCategoryId,
                        null,
                        null,
                        null,
                        0,
                        resolveSceneCandidatePoolSize(safeLimit, normalizedScene)
                );
                reason = "最新发布";
                break;
            case "personal":
            default:
                recipes = collectPersonalCandidates(
                        resolvePersonalCandidatePoolSize(
                                safeLimit,
                                StringUtils.hasText(normalizedScene) || normalizedCategoryId != null
                        ),
                        normalizedCategoryId);
                reason = "综合推荐";
                break;
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(recipes);
        applySceneTagsAndReasons(list, normalizedType, normalizedScene, "personal".equals(normalizedType), false);

        if ("personal".equals(normalizedType)) {
            int rerankLimit = Math.max(list.size(), safeLimit);
            applyPersonalRerank(list, userId, normalizedScene, rerankLimit);
            list = applySelection(list, normalizedScene, selectedCategoryName, safeLimit, true);
            if (StringUtils.hasText(normalizedScene) || normalizedCategoryId != null) {
                list = supplementSelection(list,
                        normalizedScene,
                        normalizedCategoryId,
                        selectedCategoryName,
                        safeLimit,
                        true);
            }
        } else {
            boolean strictScene = StringUtils.hasText(normalizedScene);
            list = applySelection(list, normalizedScene, selectedCategoryName, safeLimit, strictScene);
            if (strictScene || normalizedCategoryId != null) {
                list = supplementSelection(list,
                        normalizedScene,
                        normalizedCategoryId,
                        selectedCategoryName,
                        safeLimit,
                        strictScene);
            }
        }

        RecommendResponse response = new RecommendResponse();
        response.setList(list);
        response.setReason(buildResponseReason(normalizedScene, selectedCategoryName, reason));
        return response;
    }

    private RecommendResponse getBlendedPersonalRecommendations(int limit,
            Integer userId,
            String normalizedScene,
            Integer normalizedCategoryId,
            String selectedCategoryName) {
        int candidateLimit = Math.max(limit * 8, 48);
        List<RecipeListDTO> offlineTop100 = getOfflineRankedRecommendationList(candidateLimit, userId);
        if (offlineTop100 == null || offlineTop100.isEmpty()) {
            return null;
        }
        List<RecipeListDTO> realtimeList = buildRealtimePersonalRecommendationCandidates(
                Math.max(limit * 12, 120),
                normalizedScene,
                normalizedCategoryId,
                selectedCategoryName);

        LinkedHashMap<Integer, RecipeListDTO> merged = new LinkedHashMap<>();
        appendRecommendations(merged, offlineTop100, offlineTop100.size());
        appendRecommendations(merged, realtimeList, realtimeList.size());

        List<RecipeListDTO> combined = new ArrayList<>(merged.values());
        if (combined.isEmpty()) {
            return null;
        }
        applySceneTagsAndReasons(combined, "personal", normalizedScene, true, true);
        applyPersonalRerank(combined, userId, normalizedScene, Math.max(combined.size(), limit));
        List<RecipeListDTO> selected = applySelection(combined, normalizedScene, selectedCategoryName, limit, true);
        if (StringUtils.hasText(normalizedScene) || normalizedCategoryId != null) {
            selected = supplementSelection(selected,
                    normalizedScene,
                    normalizedCategoryId,
                    selectedCategoryName,
                    limit,
                    true);
        }

        RecommendResponse response = new RecommendResponse();
        response.setList(selected);
        response.setReason(buildResponseReason(normalizedScene, selectedCategoryName, "综合推荐"));
        return response;
    }

    private List<RecipeListDTO> buildRealtimePersonalRecommendationList(int limit,
            Integer userId,
            String normalizedScene,
            Integer normalizedCategoryId,
            String selectedCategoryName) {
        int safeLimit = Math.max(limit, 1);
        List<RecipeListDTO> list = buildRealtimePersonalRecommendationCandidates(
                resolvePersonalCandidatePoolSize(
                        safeLimit,
                        StringUtils.hasText(normalizedScene) || normalizedCategoryId != null
                ),
                normalizedScene,
                normalizedCategoryId,
                selectedCategoryName
        );

        applyPersonalRerank(list, userId, normalizedScene, Math.max(list.size(), safeLimit));
        list = applySelection(list, normalizedScene, selectedCategoryName, safeLimit, true);
        if (StringUtils.hasText(normalizedScene) || normalizedCategoryId != null) {
            return supplementSelection(list,
                    normalizedScene,
                    normalizedCategoryId,
                    selectedCategoryName,
                    safeLimit,
                    true);
        }
        return list;
    }

    private List<RecipeListDTO> buildRealtimePersonalRecommendationCandidates(int candidateLimit,
            String normalizedScene,
            Integer normalizedCategoryId,
            String selectedCategoryName) {
        int safeLimit = Math.max(candidateLimit, 1);
        List<RecipeListDTO> list;
        if (normalizedCategoryId != null) {
            list = buildCategoryRealtimeCandidateList(safeLimit, normalizedCategoryId);
        } else {
            List<Recipe> recipes = collectPersonalCandidates(safeLimit, normalizedCategoryId);
            list = recipeListDTOAssembler.toListDTOBatch(recipes);
        }
        applySceneTagsAndReasons(list, "personal", normalizedScene, true, false);
        if (StringUtils.hasText(selectedCategoryName)) {
            applyCategoryContextReasons(list, selectedCategoryName);
        }
        return list;
    }

    private List<RecipeListDTO> buildCategoryRealtimeCandidateList(int limit, Integer categoryId) {
        int safeLimit = Math.max(limit, 1);
        List<Integer> candidateIds = getCategoryRealtimePoolIds(categoryId, Math.max(safeLimit, 48));
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<Integer> selectedIds = candidateIds.stream()
                .limit(safeLimit)
                .toList();
        List<Recipe> recipes = recipeMapper.findByIds(selectedIds);
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        Map<Integer, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity(), (left, right) -> left));
        List<Recipe> orderedRecipes = selectedIds.stream()
                .map(recipeMap::get)
                .filter(recipe -> recipe != null)
                .toList();
        return recipeListDTOAssembler.toListDTOBatch(orderedRecipes);
    }

    private List<RecipeListDTO> buildStrictCategoryRealtimeRecommendationList(int limit,
            Integer categoryId,
            String categoryName) {
        int safeLimit = Math.max(limit, 1);
        List<Integer> candidateIds = getCategoryRealtimePoolIds(categoryId, Math.max(safeLimit * 8, 48));
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        List<Integer> shuffledIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledIds);
        List<Integer> selectedIds = shuffledIds.stream()
                .limit(safeLimit)
                .toList();
        List<Recipe> recipes = recipeMapper.findByIds(selectedIds);
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        Map<Integer, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity(), (left, right) -> left));
        List<Recipe> orderedRecipes = selectedIds.stream()
                .map(recipeMap::get)
                .filter(recipe -> recipe != null)
                .toList();
        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(orderedRecipes);
        for (RecipeListDTO dto : list) {
            dto.setReasons(List.of("当前热度较高"));
        }
        return list;
    }

    private List<Integer> getCategoryRealtimePoolIds(Integer categoryId, int requestedSize) {
        int safeRequestedSize = Math.max(requestedSize, 1);
        long now = System.currentTimeMillis();
        CategoryRealtimePool cachedPool = categoryRealtimePoolCache.get(categoryId);
        if (cachedPool != null
                && cachedPool.expireAtMillis > now
                && cachedPool.recipeIds.size() >= Math.min(safeRequestedSize, CATEGORY_REALTIME_POOL_SIZE)) {
            return cachedPool.recipeIds;
        }

        int fetchSize = Math.max(safeRequestedSize, CATEGORY_REALTIME_POOL_SIZE);
        List<Integer> recipeIds = recipeMapper.findHotIdsByCategory(categoryId, fetchSize);
        List<Integer> safeRecipeIds = recipeIds == null ? List.of() : List.copyOf(recipeIds);
        categoryRealtimePoolCache.put(categoryId,
                new CategoryRealtimePool(safeRecipeIds, now + CATEGORY_REALTIME_POOL_TTL_MILLIS));
        return safeRecipeIds;
    }

    private RecommendResponse getOfflineRankedRecommendations(int limit, Integer userId, String requestType) {
        List<RecipeListDTO> list = getOfflineRankedRecommendationList(limit, userId);
        if (list == null || list.isEmpty()) {
            return null;
        }

        RecommendResponse response = new RecommendResponse();
        response.setList(list);
        response.setReason("daily".equals(requestType) ? "最近可用推荐" : "综合推荐");
        return response;
    }

    private List<RecipeListDTO> getOfflineRankedRecommendationList(int limit, Integer userId) {
        if (userId == null) {
            return List.of();
        }
        int responseLimit = Math.min(Math.max(limit, 1), 100);
        List<DailyRecipeRecommendation> recommendations = dailyRecommendationMapper.findLatestRankedByUser(
                userId,
                responseLimit
        );
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }
        List<Integer> recipeIds = recommendations.stream()
                .map(DailyRecipeRecommendation::getRecipeId)
                .toList();
        List<Recipe> recipes = recipeMapper.findByIds(recipeIds);
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        Map<Integer, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity(), (left, right) -> left));
        List<Recipe> orderedRecipes = new ArrayList<>();
        for (DailyRecipeRecommendation recommendation : recommendations) {
            Recipe recipe = recipeMap.get(recommendation.getRecipeId());
            if (recipe != null) {
                orderedRecipes.add(recipe);
            }
        }

        List<RecipeListDTO> list = recipeListDTOAssembler.toListDTOBatch(orderedRecipes);
        applyDailyReasons(list, recommendations);
        return list;
    }

    private void applyDailyReasons(List<RecipeListDTO> list, List<DailyRecipeRecommendation> recommendations) {
        if (list == null || list.isEmpty() || recommendations == null || recommendations.isEmpty()) {
            return;
        }
        Map<Integer, DailyRecipeRecommendation> recommendationMap = recommendations.stream()
                .collect(Collectors.toMap(DailyRecipeRecommendation::getRecipeId, Function.identity(), (left, right) -> left));
        for (RecipeListDTO dto : list) {
            DailyRecipeRecommendation recommendation = recommendationMap.get(dto.getId());
            if (recommendation == null || !StringUtils.hasText(recommendation.getReasonJson())) {
                dto.setReasons(List.of("综合推荐"));
                continue;
            }
            dto.setReasons(parseDailyReasons(recommendation.getReasonJson()));
        }
    }

    private List<String> parseDailyReasons(String reasonJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(reasonJson, new TypeReference<Map<String, Object>>() {
            });
            LinkedHashSet<String> reasons = new LinkedHashSet<>();
            Object mainReason = payload.get("main_reason");
            if (mainReason instanceof String value
                    && StringUtils.hasText(value)) {
                String normalizedReason = normalizeDisplayedReason(value);
                if (StringUtils.hasText(normalizedReason) && !isNoisyOfflineReason(normalizedReason)) {
                    reasons.add(normalizedReason);
                }
            }
            Object matchedTags = payload.get("matched_tags");
            if (matchedTags instanceof List<?> tags) {
                String joined = tags.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .filter(StringUtils::hasText)
                        .filter(this::isMeaningfulOfflineTag)
                        .limit(3)
                        .collect(Collectors.joining(" / "));
                if (StringUtils.hasText(joined)) {
                    reasons.add(normalizeDisplayedReason("关联标签：" + joined));
                }
            }
            if (reasons.isEmpty()) {
                reasons.add("综合推荐");
            }
            return normalizeDisplayedReasons(reasons);
        } catch (Exception e) {
            log.debug("解析日推理由失败：{}", e.getMessage());
            return List.of("综合推荐");
        }
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

    private void applySceneTagsAndReasons(List<RecipeListDTO> list,
            String recommendationType,
            String sceneCode,
            boolean allowDefaultReason,
            boolean preserveExistingReasons) {
        for (RecipeListDTO dto : list) {
            List<String> tags = SceneTagResolver.resolveTagNames(dto);
            dto.setSceneTags(tags);

            List<String> existingReasons = preserveExistingReasons && dto.getReasons() != null
                    ? new ArrayList<>(normalizeDisplayedReasons(dto.getReasons()))
                    : List.of();
            List<String> reasons = new ArrayList<>();
            if ("hot".equals(recommendationType)) {
                reasons.add("当前热度较高");
            } else if ("new".equals(recommendationType)) {
                reasons.add("最近新发布");
            }
            reasons.addAll(existingReasons);
            if (allowDefaultReason && reasons.isEmpty()) {
                reasons.add("综合推荐");
            }
            dto.setReasons(normalizeDisplayedReasons(reasons));
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
                        dto.getIngredients().stream()
                                .filter(this::isMeaningfulPreferenceIngredient)
                                .forEach(seedIngredients::add);
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
            String sceneContextReason = buildSceneContextReason(dto, sceneCode);
            LinkedHashSet<String> reasons = new LinkedHashSet<>(keepInformativeReasons(dto.getReasons()));

            if (sceneMatched(dto, sceneCode)) {
                score += 16;
            }

            List<String> matchedPreferredScenes = intersect(dto.getSceneTags(), preferredScenes).stream()
                    .filter(scene -> !scene.equals(sceneName))
                    .collect(Collectors.toList());
            if (!matchedPreferredScenes.isEmpty()) {
                score += Math.min(2.2, matchedPreferredScenes.size() * 1.1);
            }

            if (StringUtils.hasText(dto.getTaste()) && preferredTastes.contains(dto.getTaste())) {
                score += 1.5;
                reasons.add("偏" + dto.getTaste() + "口，更贴近你的偏好");
            }

            if (matchesTimeBudget(dto.getTime(), timeBudget)) {
                score += 0.9;
                String timeBudgetReason = buildTimeBudgetPreferenceReason(dto.getTime(), timeBudget);
                if (StringUtils.hasText(timeBudgetReason)) {
                    reasons.add(timeBudgetReason);
                }
            }

            List<String> matchedIngredients = intersect(dto.getIngredients(), new ArrayList<>(seedIngredients));
            if (!matchedIngredients.isEmpty()) {
                score += Math.min(2.4, matchedIngredients.size() * 0.8);
                reasons.add("你最近常做的" + matchedIngredients.get(0) + "也在这道菜里");
            }

            List<String> matchedCategories = intersect(dto.getCategories(), new ArrayList<>(seedCategories));
            if (!matchedCategories.isEmpty()) {
                score += 0.8;
                reasons.add("你最近更常看" + matchedCategories.get(0) + "这类菜");
            }

            if (matchesDietGoal(dietGoal, dto)) {
                score += 1.2;
                String dietGoalReason = buildDietGoalPreferenceReason(dietGoal, dto);
                if (StringUtils.hasText(dietGoalReason)) {
                    reasons.add(dietGoalReason);
                }
            }

            score += Math.min(1.0, (dto.getLikeCount() == null ? 0 : dto.getLikeCount()) / 500.0);

            if (reasons.isEmpty() && StringUtils.hasText(sceneContextReason)) {
                reasons.add(sceneContextReason);
            }
            if (reasons.isEmpty()) {
                reasons.add("综合推荐");
            }

            dto.setReasons(normalizeDisplayedReasons(reasons));
            scored.add(new ScoredRecipe(dto, score));
        }

        scored.sort(Comparator
                .comparing((ScoredRecipe item) -> sceneMatched(item.recipe(), sceneCode)).reversed()
                .thenComparing(ScoredRecipe::score, Comparator.reverseOrder())
                .thenComparing(item -> item.recipe().getLikeCount() == null ? 0 : item.recipe().getLikeCount(),
                        Comparator.reverseOrder()));

        list.clear();
        list.addAll(scored.stream().map(ScoredRecipe::recipe).limit(Math.max(limit, 1)).collect(Collectors.toList()));
    }

    private List<RecipeListDTO> applySelection(List<RecipeListDTO> list,
            String sceneCode,
            String categoryName,
            int limit,
            boolean strictScene) {
        int safeLimit = Math.max(limit, 1);
        List<RecipeListDTO> selected = new ArrayList<>(list);

        if (StringUtils.hasText(categoryName)) {
            selected = selected.stream()
                    .filter(dto -> dto.getCategories() != null && dto.getCategories().contains(categoryName))
                    .collect(Collectors.toList());
        }

        if (!StringUtils.hasText(sceneCode)) {
            return selected.stream().limit(safeLimit).collect(Collectors.toList());
        }

        List<RecipeListDTO> matched = new ArrayList<>();
        List<RecipeListDTO> remainder = new ArrayList<>();
        for (RecipeListDTO dto : selected) {
            if (sceneMatched(dto, sceneCode)) {
                matched.add(dto);
            } else {
                remainder.add(dto);
            }
        }

        if (strictScene || matched.size() >= safeLimit) {
            return matched.stream().limit(safeLimit).collect(Collectors.toList());
        }

        List<RecipeListDTO> ordered = new ArrayList<>(matched);
        for (RecipeListDTO dto : remainder) {
            if (ordered.size() >= safeLimit) {
                break;
            }
            ordered.add(dto);
        }
        return ordered;
    }

    private void applyCategoryContextReasons(List<RecipeListDTO> list, String categoryName) {
        if (list == null || list.isEmpty() || !StringUtils.hasText(categoryName)) {
            return;
        }
        for (RecipeListDTO dto : list) {
            if (dto.getCategories() == null || !dto.getCategories().contains(categoryName)) {
                continue;
            }
            List<String> reasons = dto.getReasons() == null ? new ArrayList<>() : new ArrayList<>(dto.getReasons());
            reasons.add("属于你选择的分类：" + categoryName);
            dto.setReasons(normalizeDisplayedReasons(reasons));
        }
    }

    private List<RecipeListDTO> supplementSelection(List<RecipeListDTO> list,
            String sceneCode,
            Integer categoryId,
            String categoryName,
            int limit,
            boolean strictScene) {
        int safeLimit = Math.max(limit, 1);
        if (list.size() >= safeLimit || (categoryId == null && !StringUtils.hasText(sceneCode))) {
            return list.stream().limit(safeLimit).collect(Collectors.toList());
        }

        List<RecipeListDTO> fallbackList = buildSupplementCandidates(sceneCode, categoryId, safeLimit);
        applySceneTagsAndReasons(fallbackList, "supplement", sceneCode, false, false);
        fallbackList = applySelection(fallbackList,
                sceneCode,
                categoryName,
                Math.max(safeLimit * 4, 24),
                strictScene);

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
            if (strictScene && StringUtils.hasText(sceneCode) && !sceneMatched(dto, sceneCode)) {
                continue;
            }
            LinkedHashSet<String> reasons = new LinkedHashSet<>(keepInformativeReasons(dto.getReasons()));
            String sceneContextReason = buildSceneContextReason(dto, sceneCode);
            if (StringUtils.hasText(sceneContextReason)) {
                reasons.add(sceneContextReason);
            }
            if (reasons.isEmpty()) {
                reasons.add("优质菜谱补充");
            }
            dto.setReasons(normalizeDisplayedReasons(reasons));
            merged.put(dto.getId(), dto);
            if (merged.size() >= safeLimit) {
                break;
            }
        }

        return merged.values().stream().limit(safeLimit).collect(Collectors.toList());
    }

    private List<RecipeListDTO> buildSupplementCandidates(String sceneCode, Integer categoryId, int limit) {
        int fetchSize = StringUtils.hasText(sceneCode)
                ? resolveSceneCandidatePoolSize(Math.max(limit * 2, 24), sceneCode)
                : Math.max(limit * 4, 24);

        Map<Integer, Recipe> merged = new LinkedHashMap<>();
        appendRecipes(merged, categoryId == null
                ? List.of()
                : recipeMapper.findByCondition(categoryId, null, null, "hot", 0, fetchSize));
        appendRecipes(merged, recipeMapper.findByCondition(null, null, null, "hot", 0, fetchSize));
        appendRecipes(merged, recipeMapper.findRandom(fetchSize));
        return recipeListDTOAssembler.toListDTOBatch(new ArrayList<>(merged.values()));
    }

    private void appendRecipes(Map<Integer, Recipe> target, List<Recipe> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Recipe recipe : source) {
            if (recipe == null || recipe.getId() == null) {
                continue;
            }
            target.putIfAbsent(recipe.getId(), recipe);
        }
    }

    private void appendRecommendations(Map<Integer, RecipeListDTO> target, List<RecipeListDTO> source, int quota) {
        appendRecommendations(target, source, quota, Set.of());
    }

    private void appendRecommendations(Map<Integer, RecipeListDTO> target,
            List<RecipeListDTO> source,
            int quota,
            Set<Integer> excludedIds) {
        if (quota <= 0 || source == null || source.isEmpty()) {
            return;
        }
        int added = 0;
        for (RecipeListDTO dto : source) {
            if (dto.getId() == null || target.containsKey(dto.getId()) || excludedIds.contains(dto.getId())) {
                continue;
            }
            target.put(dto.getId(), dto);
            added++;
            if (added >= quota) {
                break;
            }
        }
    }

    private void appendRecommendationsRandomly(Map<Integer, RecipeListDTO> target,
            List<RecipeListDTO> source,
            int quota) {
        appendRecommendationsRandomly(target, source, quota, Set.of());
    }

    private void appendRecommendationsRandomly(Map<Integer, RecipeListDTO> target,
            List<RecipeListDTO> source,
            int quota,
            Set<Integer> excludedIds) {
        if (quota <= 0 || source == null || source.isEmpty()) {
            return;
        }
        List<RecipeListDTO> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        appendRecommendations(target, shuffled, quota, excludedIds);
    }

    private String buildResponseReason(String normalizedScene, String categoryName, String defaultReason) {
        List<String> suffixes = new ArrayList<>();
        if (StringUtils.hasText(categoryName)) {
            suffixes.add("分类：" + categoryName);
        }
        if (suffixes.isEmpty()) {
            return defaultReason;
        }
        return defaultReason + "（" + String.join("，", suffixes) + "）";
    }

    private int resolveSceneCandidatePoolSize(int limit, String sceneCode) {
        if (!StringUtils.hasText(sceneCode)) {
            return Math.max(limit, 1);
        }
        long size = Math.max((long) limit * 20L, SCENE_CANDIDATE_POOL_MIN_SIZE);
        return (int) Math.min(size, SCENE_CANDIDATE_POOL_MAX_SIZE);
    }

    private int resolvePersonalCandidatePoolSize(int limit, boolean hasSceneOrCategory) {
        if (hasSceneOrCategory) {
            return resolveSceneCandidatePoolSize(limit, "scene");
        }
        return Math.max(limit * 6, 72);
    }

    private boolean sceneMatched(RecipeListDTO dto, String sceneCode) {
        if (dto == null || !StringUtils.hasText(sceneCode)) {
            return false;
        }
        if (dto.getSceneTags() != null && dto.getSceneTags().contains(SceneTagResolver.sceneName(sceneCode))) {
            return true;
        }
        return SceneTagResolver.matchesScene(dto, sceneCode);
    }

    private boolean isMeaningfulPreferenceIngredient(String ingredientName) {
        if (!StringUtils.hasText(ingredientName)) {
            return false;
        }
        String normalized = ingredientName.trim();
        return !COMMON_INGREDIENT_STOPWORDS.contains(normalized);
    }

    private boolean isMeaningfulOfflineTag(String tag) {
        if (!StringUtils.hasText(tag)) {
            return false;
        }
        String normalized = tag.trim();
        return !COMMON_INGREDIENT_STOPWORDS.contains(normalized);
    }

    private boolean isNoisyOfflineReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return true;
        }
        int start = reason.indexOf('“');
        int end = reason.indexOf('”');
        if (start < 0 || end <= start + 1) {
            return false;
        }
        String keyword = reason.substring(start + 1, end).trim();
        return !keyword.isEmpty() && COMMON_INGREDIENT_STOPWORDS.contains(keyword);
    }

    private List<String> keepInformativeReasons(Collection<String> reasons) {
        return normalizeDisplayedReasons(reasons).stream()
                .filter(this::isInformativeReason)
                .collect(Collectors.toList());
    }

    private boolean isInformativeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return false;
        }
        return !Set.of("综合推荐", "优质菜谱补充", "根据你近期浏览与收藏习惯推荐").contains(reason.trim());
    }

    private List<String> normalizeDisplayedReasons(Collection<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of();
        }
        return reasons.stream()
                .map(this::normalizeDisplayedReason)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(2)
                .collect(Collectors.toList());
    }

    private String normalizeDisplayedReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String normalized = reason.trim();
        if (isNoisyOfflineReason(normalized)) {
            return null;
        }
        if (normalized.startsWith("匹配你选择的场景：")) {
            return null;
        }
        if (normalized.startsWith("补充匹配当前场景")) {
            return "优质菜谱补充";
        }
        normalized = normalized.replace("最近可用离线推荐", "最近可用推荐");
        normalized = normalized.replace("离线模型推荐", "综合推荐");
        return normalized;
    }

    private boolean matchesTimeBudget(String recipeTime, String timeBudget) {
        if (!StringUtils.hasText(recipeTime) || !StringUtils.hasText(timeBudget)) {
            return false;
        }
        if ("quick".equals(timeBudget)) {
            return SceneTagResolver.isQuickTime(recipeTime);
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

    private String buildTimeBudgetPreferenceReason(String recipeTime, String timeBudget) {
        if (!StringUtils.hasText(recipeTime) || !StringUtils.hasText(timeBudget)) {
            return null;
        }
        if ("quick".equals(timeBudget)) {
            return recipeTime + "可完成";
        }
        if ("medium".equals(timeBudget)) {
            return recipeTime + "更符合你常用的下厨时长";
        }
        if ("long".equals(timeBudget)) {
            return recipeTime + "更适合你偏好的慢工烹饪";
        }
        return null;
    }

    private String buildDietGoalPreferenceReason(String dietGoal, RecipeListDTO dto) {
        if (!StringUtils.hasText(dietGoal)) {
            return null;
        }
        if ("减脂".equals(dietGoal)) {
            if (containsAny(dto.getIngredients(), "鸡胸", "鸡胸肉", "虾", "虾仁", "鳕鱼", "鱼片", "豆腐", "魔芋")) {
                return "高蛋白食材占比更高";
            }
            if (containsAny(dto.getTaste(), "清淡", "原味")) {
                return "口味更清爽，适合当前饮食需求";
            }
            return "更贴近你当前的减脂饮食目标";
        }
        if ("增肌".equals(dietGoal)) {
            if (containsAny(dto.getIngredients(), "鸡胸", "牛肉", "鸡蛋", "虾", "鱼片")) {
                return "蛋白质来源更集中";
            }
            return "更适合补充优质蛋白";
        }
        return "更贴近你的饮食目标";
    }

    private String buildSceneContextReason(RecipeListDTO dto, String sceneCode) {
        if (dto == null || !StringUtils.hasText(sceneCode) || !sceneMatched(dto, sceneCode)) {
            return null;
        }
        String name = dto.getName() == null ? "" : dto.getName();
        String time = dto.getTime() == null ? "" : dto.getTime();
        String difficulty = dto.getDifficulty() == null ? "" : dto.getDifficulty();
        String taste = dto.getTaste() == null ? "" : dto.getTaste();
        List<String> categories = dto.getCategories() == null ? List.of() : dto.getCategories();
        List<String> ingredients = dto.getIngredients() == null ? List.of() : dto.getIngredients();
        return switch (sceneCode) {
            case "quick" -> buildQuickSceneReason(dto, name, time, difficulty, categories);
            case "family" -> {
                if (containsAny(categories, "早餐", "主食", "面食")) {
                    yield "更适合家庭日常备餐";
                }
                if (containsAny(categories, "汤", "汤羹", "羹类")) {
                    yield "汤羹上桌更适合家庭日常一餐";
                }
                if (containsAny(name, "下饭") || containsAny(categories, "下饭菜")) {
                    yield "口味更下饭，适合家常正餐";
                }
                if (containsAny(ingredients, "鸡蛋", "番茄", "土豆", "豆腐", "青菜")) {
                    yield "食材常见，适合家庭日常准备";
                }
                if (containsAny(name, "家常", "家庭", "营养") || containsAny(categories, "家常菜", "家庭餐")) {
                    yield "家常做法更适合日常餐桌";
                }
                yield "更适合多人日常用餐";
            }
            case "diet" -> {
                if (containsAny(ingredients, "鸡胸", "鸡胸肉", "虾", "虾仁", "鱼", "蛋白")) {
                    yield "高蛋白食材占比更高";
                }
                if (containsAny(ingredients, "西兰花", "黄瓜", "番茄", "菠菜", "苦瓜", "玉米粒", "蘑菇")) {
                    yield "蔬菜占比更高，整体更轻负担";
                }
                if (containsAny(taste, "清淡", "原味")) {
                    yield "口味更清爽，适合当前饮食需求";
                }
                if (containsAny(name, "减脂", "健身", "轻食", "低卡", "高蛋白", "沙拉")
                        || containsAny(categories, "减脂", "健身", "轻食", "沙拉", "低脂")) {
                    yield "低负担搭配，更适合当前饮食目标";
                }
                yield "整体更偏轻负担做法";
            }
            case "banquet" -> {
                if (containsAny(name, "聚会", "宴客", "硬菜", "招待", "派对")
                        || containsAny(categories, "宴客菜", "聚会", "甜点", "烘焙", "下午茶")) {
                    yield "更适合招待或聚餐上桌";
                }
                if (containsAny(difficulty, "高级")) {
                    yield "完成度更高，适合聚会场景";
                }
                yield "摆盘和完成度更适合聚会分享";
            }
            default -> null;
        };
    }

    private String buildQuickSceneReason(RecipeListDTO dto, String name, String time, String difficulty, List<String> categories) {
        if (dto != null && SceneTagResolver.isQuickTime(dto.getTime()) && StringUtils.hasText(time)) {
            return time + "可完成";
        }
        if (containsAny(name, "快手", "速食", "一人食", "便当", "懒人")) {
            return "准备步骤更轻，适合工作日快手做";
        }
        if (containsAny(categories, "快手菜", "一人食", "便当", "早餐")) {
            return "更适合工作日快手备餐";
        }
        if (containsAny(difficulty, "简单")) {
            return "步骤更简洁，适合赶时间时做";
        }
        return "更适合工作日快速准备";
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

    private List<RecipeListDTO> rerankBySceneMode(List<RecipeListDTO> list, String mode, String sceneCode, String sort) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Map<Integer, Integer> originalOrder = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            RecipeListDTO dto = list.get(i);
            if (dto != null && dto.getId() != null) {
                originalOrder.put(dto.getId(), i);
            }
        }

        Comparator<RecipeListDTO> sortComparator = buildCatalogSortComparator(sort, originalOrder);
        List<RecipeListDTO> sorted = list.stream()
                .sorted(Comparator
                        .comparing((RecipeListDTO item) -> sceneMatched(item, sceneCode)).reversed()
                        .thenComparing(sortComparator)
                        .thenComparing(item -> sceneModeScore(item, mode, sceneCode), Comparator.reverseOrder())
                        .thenComparingInt(item -> originalOrder.getOrDefault(item.getId(), Integer.MAX_VALUE)))
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

    private Comparator<RecipeListDTO> buildCatalogSortComparator(String sort, Map<Integer, Integer> originalOrder) {
        if ("collect".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparing((RecipeListDTO item) -> safeInt(item.getFavoriteCount()), Comparator.reverseOrder())
                    .thenComparing((RecipeListDTO item) -> safeInt(item.getLikeCount()), Comparator.reverseOrder())
                    .thenComparingInt(item -> originalOrder.getOrDefault(item.getId(), Integer.MAX_VALUE));
        }
        if ("hot".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparing((RecipeListDTO item) -> safeInt(item.getLikeCount()), Comparator.reverseOrder())
                    .thenComparing((RecipeListDTO item) -> safeInt(item.getFavoriteCount()), Comparator.reverseOrder())
                    .thenComparingInt(item -> originalOrder.getOrDefault(item.getId(), Integer.MAX_VALUE));
        }
        return Comparator.comparingInt(item -> originalOrder.getOrDefault(item.getId(), Integer.MAX_VALUE));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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
                if (containsAny(name, "家常", "家庭", "全家", "儿童", "下饭", "营养")) {
                    score += 4;
                }
                if (containsAny(categories, "家常菜", "家庭餐", "儿童餐", "下饭菜", "汤")) {
                    score += 4;
                }
                if (containsAny(ingredients, "鸡蛋", "番茄", "土豆", "豆腐", "青菜")) {
                    score += 2;
                }
            }
            case "fitness" -> {
                if (containsAny(name, "减脂", "健身", "轻食", "低卡", "高蛋白", "沙拉")) {
                    score += 5;
                }
                if (containsAny(categories, "减脂", "健身", "轻食", "沙拉", "低脂")) {
                    score += 4;
                }
                if (containsAny(ingredients, "鸡胸", "鸡胸肉", "虾", "虾仁", "鱼", "西兰花", "蛋白", "豆腐", "燕麦")) {
                    score += 4;
                }
                if (containsAny(taste, "清淡", "原味")) {
                    score += 1;
                }
            }
            case "quick" -> {
                if (SceneTagResolver.isQuickTime(time)) {
                    score += 5;
                }
                if (containsAny(difficulty, "简单")) {
                    score += 2;
                }
                if (containsAny(name, "快手", "速食", "一人食", "便当", "懒人")) {
                    score += 4;
                }
                if (containsAny(categories, "快手菜", "一人食", "便当", "早餐")) {
                    score += 3;
                }
                if (!isQuickFriendlyByTime(dto)) {
                    score -= 8;
                }
            }
            case "party" -> {
                if (containsAny(name, "聚会", "宴客", "硬菜", "甜点", "招待", "烘焙", "派对")) {
                    score += 5;
                }
                if (containsAny(categories, "宴客菜", "聚会", "甜点", "烘焙", "下午茶")) {
                    score += 4;
                }
                if (containsAny(difficulty, "高级")) {
                    score += 2;
                }
            }
            default -> {
            }
        }

        score += Math.min(2.0, (dto.getLikeCount() == null ? 0 : dto.getLikeCount()) / 300.0);
        return score;
    }

    private boolean isQuickFriendlyByTime(RecipeListDTO dto) {
        return dto != null && SceneTagResolver.isQuickTime(dto.getTime());
    }

    private Integer normalizeCategoryId(Integer categoryId) {
        return categoryId != null && categoryId > 0 ? categoryId : null;
    }

    private record CategoryRealtimePool(List<Integer> recipeIds, long expireAtMillis) {
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
