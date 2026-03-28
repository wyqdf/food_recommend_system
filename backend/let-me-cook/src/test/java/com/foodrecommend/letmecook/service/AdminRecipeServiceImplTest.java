package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.RecipeDTO;
import com.foodrecommend.letmecook.dto.admin.RecipeCreateRequest;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.impl.AdminRecipeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecipeServiceImplTest {

    @Mock
    private RecipeMapper recipeMapper;
    @Mock
    private RecipeIngredientMapper recipeIngredientMapper;
    @Mock
    private CookingStepMapper cookingStepMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private TasteMapper tasteMapper;
    @Mock
    private TechniqueMapper techniqueMapper;
    @Mock
    private TimeCostMapper timeCostMapper;
    @Mock
    private DifficultyMapper difficultyMapper;
    @Mock
    private IngredientMapper ingredientMapper;
    @Mock
    private BehaviorEventMapper behaviorEventMapper;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdminRecipeServiceImpl adminRecipeService;

    @Test
    void getRecipesShouldUseSafePagination() {
        when(recipeMapper.countForAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);
        when(recipeMapper.findAdminRecipeIds(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(1));

        Recipe recipe = new Recipe();
        recipe.setId(1);
        recipe.setTitle("test");
        when(recipeMapper.findAdminByIds(List.of(1))).thenReturn(List.of(recipe));
        when(behaviorEventMapper.countRecipeViewsByRecipeIds(List.of(1))).thenReturn(List.of());

        PageResult<RecipeDTO> result = adminRecipeService.getRecipes(
                0, 999, null, null, null, null, null, null, null, null, null);

        assertEquals(1, result.getPage());
        assertEquals(200, result.getPageSize());
        verify(recipeMapper).findAdminRecipeIds(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(200));
    }

    @Test
    void batchDeleteShouldIgnoreInvalidAndDuplicateIds() {
        Recipe recipe1 = new Recipe();
        recipe1.setId(1);
        Recipe recipe2 = new Recipe();
        recipe2.setId(2);
        when(recipeMapper.findById(1)).thenReturn(recipe1);
        when(recipeMapper.findById(2)).thenReturn(recipe2);

        int[] result = adminRecipeService.batchDeleteRecipes(new Integer[]{1, null, -2, 1, 2});

        assertEquals(2, result[0]);
        assertEquals(0, result[1]);
        verify(recipeMapper).findById(1);
        verify(recipeMapper).findById(2);
        verify(recipeMapper).deleteById(1);
        verify(recipeMapper).deleteById(2);
    }

    @Test
    void createRecipeShouldIgnoreNullCategoryIds() {
        RecipeCreateRequest request = new RecipeCreateRequest();
        request.setTitle("test");
        request.setAuthor("tester");
        request.setCategoryIds(java.util.Arrays.asList(1, null, 1, 2));

        when(recipeMapper.insert(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            recipe.setId(99);
            return 1;
        });

        adminRecipeService.createRecipe(request);

        verify(categoryMapper).insertRecipeCategory(99, 1);
        verify(categoryMapper).insertRecipeCategory(99, 2);
        verify(categoryMapper, never()).insertRecipeCategory(eq(99), isNull());
    }
}
