package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QualityAnalysisSummary {
    private LocalDate statDate;
    private Integer highQualityRecipes = 0;
    private BigDecimal averageLikeCount = BigDecimal.ZERO;
    private BigDecimal averageCommentCount = BigDecimal.ZERO;
    private Integer zeroInteractionRecipes = 0;
    private Integer totalRecipes = 0;
    private LocalDateTime lastUpdated;
}
