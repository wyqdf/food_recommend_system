package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.RecipeDTO;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.*;
import com.foodrecommend.letmecook.service.impl.AdminRecipeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private AdminRecipeServiceImpl adminRecipeService;

    @Test
    void getRecipesShouldUseSafePagination() {
        when(recipeMapper.findForAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(recipeMapper.countForAdmin(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);

        PageResult<RecipeDTO> result = adminRecipeService.getRecipes(
                0, 999, null, null, null, null, null, null, null, null, null);

        assertEquals(1, result.getPage());
        assertEquals(200, result.getPageSize());
        verify(recipeMapper).findForAdmin(
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
    void batchDeleteShouldReturnZeroForEmptyIds() {
        int[] result = adminRecipeService.batchDeleteRecipes(null);

        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
    }
}
