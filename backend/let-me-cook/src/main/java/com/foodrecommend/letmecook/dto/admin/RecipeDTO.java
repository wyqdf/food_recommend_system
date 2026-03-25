package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class RecipeDTO {
    private Integer id;
    private String title;
    private String author;
    private String authorUid;
    private String image;
    private String difficulty;
    private Integer difficultyId;
    private String taste;
    private Integer tasteId;
    private String technique;
    private Integer techniqueId;
    private String timeCost;
    private Integer timeCostId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
    private String createTime;
}
