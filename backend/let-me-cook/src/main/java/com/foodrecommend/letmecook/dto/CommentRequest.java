package com.foodrecommend.letmecook.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Integer recipeId;
    private String content;
}
