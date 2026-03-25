package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class RecipeStatsResponse {
    private List<DistributionData> categoryDistribution;
    private List<DistributionData> difficultyDistribution;
    private List<TopRecipe> topRecipes;
}
