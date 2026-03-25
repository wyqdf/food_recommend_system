package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class TopRecipe {
    private Integer id;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
}
