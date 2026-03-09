package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TechniqueDistributionSummary {
    private LocalDate statDate;
    private Integer techniqueId;
    private String techniqueName;
    private Integer recipeCount = 0;
    private LocalDateTime lastUpdated;
}
