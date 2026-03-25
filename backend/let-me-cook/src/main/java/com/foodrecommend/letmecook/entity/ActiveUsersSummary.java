package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ActiveUsersSummary {
    private LocalDate statDate;
    private Integer userId;
    private String username;
    private Integer recipeCount = 0;
    private Integer commentCount = 0;
    private Integer totalScore = 0;
    private Integer userRank;
    private LocalDateTime lastUpdated;
}
