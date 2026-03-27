package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.mapper.CommentLikeMapper;
import com.foodrecommend.letmecook.mapper.CommentMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

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

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
}
