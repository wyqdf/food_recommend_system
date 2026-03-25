package com.foodrecommend.letmecook.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BehaviorEvent {
    private Long id;
    private Integer userId;
    private String sessionId;
    private Integer recipeId;
    private String eventType;
    private String sourcePage;
    private String sceneCode;
    private Integer stepNumber;
    private Integer durationMs;
    private String extraJson;
    private LocalDateTime createTime;
}
