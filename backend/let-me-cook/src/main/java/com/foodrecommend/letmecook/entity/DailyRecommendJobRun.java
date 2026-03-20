package com.foodrecommend.letmecook.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyRecommendJobRun {
    private Long id;
    private LocalDate jobDate;
    private String phase;
    private String modelVersion;
    private Integer affectedUsers;
    private Integer affectedRecipes;
    private String status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
