package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.CommentRequest;
import com.foodrecommend.letmecook.entity.Comment;
import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.CommentMapper;
import com.foodrecommend.letmecook.mapper.CommentLikeMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final RecipeMapper recipeMapper;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    
    @Override
    public PageResult<Comment> getComments(Integer recipeId, int page, int pageSize, Integer userId) {
        int offset = (page - 1) * pageSize;
        List<Comment> comments = commentMapper.findByRecipeId(recipeId, offset, pageSize);
        long total = commentMapper.countByRecipeId(recipeId);
        
        if (userId != null && comments != null && !comments.isEmpty()) {
            List<Integer> commentIds = comments.stream()
                    .map(Comment::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            Set<Integer> likedCommentIds = new HashSet<>(commentLikeMapper.findLikedCommentIds(userId, commentIds));
            comments.forEach(comment -> comment.setIsLiked(likedCommentIds.contains(comment.getId())));
        } else if (comments != null) {
            comments.forEach(comment -> comment.setIsLiked(false));
        }
        
        return new PageResult<>(comments, total, page, pageSize);
    }
    
    @Override
    @Transactional
    public Comment addComment(Integer userId, CommentRequest request) {
        Comment comment = new Comment();
        comment.setRecipeId(request.getRecipeId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setPublishTime(LocalDateTime.now());
        comment.setLikes(0);
        comment.setIsLiked(false);
        
        commentMapper.insert(comment);
        recipeMapper.incrementReplyCount(request.getRecipeId());
        evictRecipeDetail(request.getRecipeId());

        User user = userMapper.findById(userId);
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setAvatar(user.getAvatar());
        }
        
        return comment;
    }
    
    @Override
    @Transactional
    public void likeComment(Integer userId, Integer commentId) {
        try {
            int inserted = commentLikeMapper.insert(commentId, userId);
            if (inserted > 0) {
                commentMapper.incrementLikes(commentId);
            }
        } catch (DuplicateKeyException ignored) {
            // 点赞按幂等处理，重复点击不再重复加一。
        }
    }

    private void evictRecipeDetail(Integer recipeId) {
        if (recipeId == null) {
            return;
        }
        Cache cache = cacheManager.getCache("recipe_detail");
        if (cache != null) {
            cache.evict(recipeId);
        }
    }
}
