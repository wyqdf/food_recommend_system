package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.CommentRequest;
import com.foodrecommend.letmecook.entity.Comment;
import com.foodrecommend.letmecook.entity.User;
import com.foodrecommend.letmecook.mapper.CommentMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import com.foodrecommend.letmecook.mapper.UserMapper;
import com.foodrecommend.letmecook.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final CommentMapper commentMapper;
    private final RecipeMapper recipeMapper;
    private final UserMapper userMapper;
    
    @Override
    public PageResult<Comment> getComments(Integer recipeId, int page, int pageSize, Integer userId) {
        int offset = (page - 1) * pageSize;
        List<Comment> comments = commentMapper.findByRecipeId(recipeId, offset, pageSize);
        long total = commentMapper.countByRecipeId(recipeId);
        
        if (userId != null) {
            comments.forEach(c -> {
                c.setIsLiked(false);
            });
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

        User user = userMapper.findById(userId);
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setAvatar(user.getAvatar());
        }
        
        return comment;
    }
    
    @Override
    public void likeComment(Integer userId, Integer commentId) {
        commentMapper.incrementLikes(commentId);
    }
}
