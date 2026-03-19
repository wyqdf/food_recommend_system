package com.foodrecommend.letmecook.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CookingSession {
    private Long id;
    private Integer userId;
    private Integer recipeId;
    private String status;
    private Integer currentStep;
    private Integer totalSteps;
    private Integer durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime lastActiveTime;
    private LocalDateTime finishedAt;
}
