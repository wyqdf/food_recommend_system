package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InteractionTrendSummary {
    private LocalDate statDate;
    private Integer likeCount = 0;
    private Integer favoriteCount = 0;
    private Integer viewCount = 0;
    private LocalDateTime lastUpdated;
}
