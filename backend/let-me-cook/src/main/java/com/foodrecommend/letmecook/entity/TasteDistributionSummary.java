package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TasteDistributionSummary {
    private LocalDate statDate;
    private Integer tasteId;
    private String tasteName;
    private Integer recipeCount = 0;
    private LocalDateTime lastUpdated;
}
