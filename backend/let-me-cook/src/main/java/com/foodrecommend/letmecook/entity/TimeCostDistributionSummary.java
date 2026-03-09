package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimeCostDistributionSummary {
    private LocalDate statDate;
    private Integer timecostId;
    private String timecostName;
    private Integer recipeCount = 0;
    private LocalDateTime lastUpdated;
}
