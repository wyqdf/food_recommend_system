package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StatisticsOverview {
    private LocalDate statDate;
    private Integer totalUsers;
    private Integer totalRecipes;
    private Integer totalCategories;
    private Integer totalComments;
    private Integer todayViews;
    private Integer todayNewUsers;
    private Integer todayNewRecipes;
    private LocalDateTime lastUpdated;
}
