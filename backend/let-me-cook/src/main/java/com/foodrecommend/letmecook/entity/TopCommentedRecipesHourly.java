package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TopCommentedRecipesHourly {
    private LocalDateTime statTime;
    private Integer recipeId;
    private String recipeTitle;
    private Integer commentCount;
    private Integer recipeRank;
    private LocalDateTime lastUpdated;
}
