package com.foodrecommend.letmecook.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPreferenceProfile {
    private Integer id;
    private Integer userId;
    private String dietGoal;
    private String cookingSkill;
    private String timeBudget;
    private String preferredTastesJson;
    private String tabooIngredientsJson;
    private String availableCookwaresJson;
    private String preferredScenesJson;
    private Integer onboardingCompleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
