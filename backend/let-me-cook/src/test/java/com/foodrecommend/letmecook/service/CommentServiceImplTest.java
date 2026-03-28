package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.mapper.CommentLikeMapper;
import com.foodrecommend.letmecook.mapper.CommentMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.dto.CommentRequest;
import com.foodrecommend.letmecook.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CommentLikeMapper commentLikeMapper;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache recipeDetailCache;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("recipe_detail")).thenReturn(recipeDetailCache);
    }

    @Test
    void likeComment_shouldIncrementLikesOnlyOnce() {
        when(commentLikeMapper.insert(8, 1)).thenReturn(1);

        commentService.likeComment(1, 8);

        verify(commentLikeMapper, times(1)).insert(8, 1);
        verify(commentMapper, times(1)).incrementLikes(8);
    }

    @Test
    void likeComment_shouldIgnoreDuplicateLikes() {
        when(commentLikeMapper.insert(8, 1)).thenThrow(new DuplicateKeyException("duplicate like"));

        commentService.likeComment(1, 8);

        verify(commentLikeMapper, times(1)).insert(8, 1);
        verify(commentMapper, never()).incrementLikes(anyInt());
    }

    @Test
    void addComment_shouldEvictRecipeDetailCacheAfterReplyCountChanges() {
        CommentRequest request = new CommentRequest();
        request.setRecipeId(12);
        request.setContent("好吃");

        commentService.addComment(3, request);

        verify(commentMapper, times(1)).insert(any());
        verify(recipeMapper, times(1)).incrementReplyCount(12);
        verify(recipeDetailCache, times(1)).evict(12);
    }
}
