package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class OverviewResponse {
    private Integer totalUsers;
    private Integer totalRecipes;
    private Integer totalCategories;
    private Integer totalComments;
    private Integer todayViews;
    private Integer todayNewUsers;
    private Integer todayNewRecipes;
}
