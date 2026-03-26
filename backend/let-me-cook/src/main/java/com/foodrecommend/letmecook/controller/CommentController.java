package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.CommentRequest;
import com.foodrecommend.letmecook.entity.Comment;
import com.foodrecommend.letmecook.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    
    private final CommentService commentService;
    
    @GetMapping("/recipe/{recipeId}")
    public Result<Map<String, Object>> getComments(
            @PathVariable Integer recipeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest httpRequest) {
        
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        
        PageResult<Comment> result = commentService.getComments(recipeId, page, pageSize, userId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getList());
        data.put("total", result.getTotal());
        
        return Result.success(data);
    }
    
    @PostMapping
    public Result<Map<String, Object>> addComment(
            @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        try {
            Integer userId = (Integer) httpRequest.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "登录已过期，请重新登录");
            }
            Comment comment = commentService.addComment(userId, request);
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", comment.getId());
            data.put("recipeId", comment.getRecipeId());
            data.put("userId", comment.getUserId());
            data.put("content", comment.getContent());
            data.put("publishTime", comment.getPublishTime());
            data.put("likes", comment.getLikes());
            data.put("username", comment.getUsername());
            data.put("avatar", comment.getAvatar());
            data.put("isLiked", comment.getIsLiked());
            
            return Result.success(data, "评论成功");
        } catch (Exception e) {
            log.error("发表评论失败，recipeId={}, error={}", request.getRecipeId(), e.getMessage(), e);
            return Result.error(500, "评论提交失败，请稍后重试");
        }
    }
    
    @PostMapping("/{commentId}/like")
    public Result<Object> likeComment(
            @PathVariable Integer commentId,
            HttpServletRequest httpRequest) {
        try {
            Integer userId = (Integer) httpRequest.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "登录已过期，请重新登录");
            }
            commentService.likeComment(userId, commentId);
              return Result.success(null, "点赞成功");
          } catch (Exception e) {
              log.error("评论点赞失败，commentId={}, error={}", commentId, e.getMessage(), e);
              return Result.error(500, "评论点赞失败，请稍后重试");
          }
      }
}
