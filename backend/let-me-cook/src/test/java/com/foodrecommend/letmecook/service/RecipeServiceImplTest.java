package com.foodrecommend.letmecook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.config.SearchProperties;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.search.RecipeSearchService;
import com.foodrecommend.letmecook.service.impl.RecipeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeServiceImplTest {

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CookingStepMapper cookingStepMapper;

    @Mock
    private RecipeIngredientMapper recipeIngredientMapper;

    @Mock
    private IngredientMapper ingredientMapper;

    @Mock
    private TasteMapper tasteMapper;

    @Mock
    private TechniqueMapper techniqueMapper;

    @Mock
    private TimeCostMapper timeCostMapper;

    @Mock
    private DifficultyMapper difficultyMapper;

    @Mock
    private RecipeListDTOAssembler recipeListDTOAssembler;

    @Mock
    private InteractionMapper interactionMapper;

    @Mock
    private BehaviorEventMapper behaviorEventMapper;

    @Mock
    private UserPreferenceProfileMapper userPreferenceProfileMapper;

    @Mock
    private DailyRecommendationMapper dailyRecommendationMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SearchProperties searchProperties;

    @Mock
    private RecipeSearchService recipeSearchService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRecipeList_shouldUsePageLocalFetchWhenModePageExceedsCandidateCap() {
        List<Recipe> recipes = List.of(createRecipe(1, "家庭菜"), createRecipe(2, "番茄炒蛋"));
        List<RecipeListDTO> recipeDtos = List.of(createRecipeListDto(1, "家庭菜"), createRecipeListDto(2, "番茄炒蛋"));

        when(recipeMapper.countByCondition(null, null, null)).thenReturn(10_000L);
        when(recipeMapper.findByCondition(null, null, null, "new", 5_988, 12)).thenReturn(recipes);
        when(recipeListDTOAssembler.toListDTOBatch(recipes)).thenReturn(recipeDtos);

        PageResult<RecipeListDTO> result = recipeService.getRecipeList(500, 12, null, null, null, "new", "family");

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals(10_000L, result.getTotal());
        verify(recipeMapper, times(1)).findByCondition(null, null, null, "new", 5_988, 12);
        verify(recipeMapper, never()).findByCondition(isNull(), isNull(), isNull(), eq("new"), eq(0), anyInt());
    }

    @Test
    void getRecipeList_shouldKeepDatabaseTotalForSceneAwareFirstPages() {
        List<Recipe> recipes = List.of(createRecipe(1, "家庭菜"), createRecipe(2, "番茄炒蛋"));
        List<RecipeListDTO> recipeDtos = List.of(createRecipeListDto(1, "家庭菜"), createRecipeListDto(2, "番茄炒蛋"));

        when(recipeMapper.countByCondition(null, null, null)).thenReturn(10_000L);
        when(recipeMapper.findByCondition(null, null, null, "new", 0, 240)).thenReturn(recipes);
        when(recipeListDTOAssembler.toListDTOBatch(recipes)).thenReturn(recipeDtos);

        PageResult<RecipeListDTO> result = recipeService.getRecipeList(1, 12, null, null, null, "new", "family");

        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals(10_000L, result.getTotal());
        verify(recipeMapper, times(1)).findByCondition(null, null, null, "new", 0, 240);
    }

    @Test
    void findRandomRecipes_shouldFallbackWhenStatusIdIndexIsMissing() {
        List<Recipe> fallbackRecipes = List.of(
                createRecipe(10, "菜谱A"),
                createRecipe(11, "菜谱B"),
                createRecipe(12, "菜谱C"));

        when(recipeMapper.findMaxPublicRecipeId())
                .thenThrow(new RuntimeException("Key 'idx_recipes_status_id' doesn't exist in table 'r'"));
        when(recipeMapper.findMaxPublicRecipeIdFallback()).thenReturn(100);
        when(recipeMapper.findRandomFromSeedFallback(anyInt(), eq(3))).thenReturn(fallbackRecipes);

        @SuppressWarnings("unchecked")
        List<Recipe> first = (List<Recipe>) ReflectionTestUtils.invokeMethod(recipeService, "findRandomRecipes", 3);
        @SuppressWarnings("unchecked")
        List<Recipe> second = (List<Recipe>) ReflectionTestUtils.invokeMethod(recipeService, "findRandomRecipes", 3);

        assertNotNull(first);
        assertEquals(3, first.size());
        assertNotNull(second);
        assertEquals(Set.of(10, 11, 12), second.stream().map(Recipe::getId).collect(java.util.stream.Collectors.toSet()));
        verify(recipeMapper, times(1)).findMaxPublicRecipeId();
        verify(recipeMapper, times(2)).findMaxPublicRecipeIdFallback();
        verify(recipeMapper, times(2)).findRandomFromSeedFallback(anyInt(), eq(3));
    }

    private Recipe createRecipe(int id, String title) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setStatus(1);
        return recipe;
    }

    private RecipeListDTO createRecipeListDto(int id, String name) {
        RecipeListDTO dto = new RecipeListDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCategories(List.of("家常菜"));
        dto.setIngredients(List.of("鸡蛋", "番茄"));
        dto.setSceneTags(List.of("家庭餐"));
        return dto;
    }
}
