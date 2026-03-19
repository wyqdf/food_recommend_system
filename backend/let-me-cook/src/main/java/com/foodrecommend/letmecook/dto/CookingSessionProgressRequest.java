package com.foodrecommend.letmecook.dto;

import lombok.Data;

@Data
public class CookingSessionProgressRequest {
    private Integer currentStep;
    private Integer durationMs;
}
