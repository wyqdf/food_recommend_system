package com.foodrecommend.letmecook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CookingSessionDTO {
    private Long sessionId;
    private Integer recipeId;
    private String recipeTitle;
    private String status;
    private Boolean resumed;
    private Integer currentStep;
    private Integer totalSteps;
    private Integer progressPercent;
    private Integer durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime lastActiveTime;
    private LocalDateTime finishedAt;
}
