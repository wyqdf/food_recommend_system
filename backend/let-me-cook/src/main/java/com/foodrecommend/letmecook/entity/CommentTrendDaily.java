package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CommentTrendDaily {
    private LocalDate statDate;
    private Integer newCommentsCount;
    private Integer totalComments;
    private LocalDateTime lastUpdated;
}
