package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.mapper.InteractionMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceImplTest {

    @Mock
    private InteractionMapper interactionMapper;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private RecipeListDTOAssembler recipeListDTOAssembler;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache recipeDetailCache;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("recipe_detail")).thenReturn(recipeDetailCache);
    }

    @Test
    void addFavorite_shouldIncrementCountWhenInsertSucceeds() {
        when(interactionMapper.insert(any())).thenReturn(1);

        favoriteService.addFavorite(1, 100);

        verify(interactionMapper, times(1)).insert(any());
        verify(recipeMapper, times(1)).incrementFavoriteCount(100);
        verify(recipeDetailCache, times(1)).evict(100);
    }

    @Test
    void addFavorite_shouldIgnoreDuplicateKeyWithoutIncrementingCount() {
        when(interactionMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate favorite"));

        favoriteService.addFavorite(1, 100);

        verify(interactionMapper, times(1)).insert(any());
        verify(recipeMapper, never()).incrementFavoriteCount(100);
        verify(recipeDetailCache, never()).evict(100);
    }

    @Test
    void removeFavorite_shouldDecrementByDeletedRows() {
        when(interactionMapper.deleteFavorite(1, 100)).thenReturn(2);

        favoriteService.removeFavorite(1, 100);

        verify(interactionMapper, times(1)).deleteFavorite(1, 100);
        verify(recipeMapper, times(1)).decrementFavoriteCountBy(100, 2);
        verify(recipeDetailCache, times(1)).evict(100);
    }
}
