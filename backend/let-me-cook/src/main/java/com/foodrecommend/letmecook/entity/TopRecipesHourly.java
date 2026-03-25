package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TopRecipesHourly {
    private LocalDateTime statTime;
    private Integer recipeId;
    private String recipeTitle;
    private Integer likeCount;
    private Integer viewCount;
    private Integer commentCount;
    private Integer recipeRank;
    private LocalDateTime lastUpdated;
}
