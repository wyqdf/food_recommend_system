package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.CommentRequest;
import com.foodrecommend.letmecook.entity.Comment;

public interface CommentService {
    
    PageResult<Comment> getComments(Integer recipeId, int page, int pageSize, Integer userId);
    
    Comment addComment(Integer userId, CommentRequest request);
    
    void likeComment(Integer userId, Integer commentId);
}
