package com.foodrecommend.letmecook.dto;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingProfileDTO {
    private Boolean completed;
    private String dietGoal;
    private String cookingSkill;
    private String timeBudget;
    private List<String> preferredTastes;
    private List<String> tabooIngredients;
    private List<String> availableCookwares;
    private List<String> preferredScenes;
}
