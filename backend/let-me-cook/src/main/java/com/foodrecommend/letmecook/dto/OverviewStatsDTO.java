package com.foodrecommend.letmecook.dto;

import lombok.Data;

@Data
public class OverviewStatsDTO {
    private Integer totalUsers;
    private Integer totalRecipes;
    private Integer totalCategories;
    private Integer totalComments;
    private Integer todayViews;
    private Integer todayNewUsers;
    private Integer todayNewRecipes;
}
