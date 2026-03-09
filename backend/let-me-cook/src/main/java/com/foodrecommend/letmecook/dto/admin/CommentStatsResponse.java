package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class CommentStatsResponse {
    private List<TrendData> commentsTrend;
    private List<TopRecipe> topCommentedRecipes;
}
