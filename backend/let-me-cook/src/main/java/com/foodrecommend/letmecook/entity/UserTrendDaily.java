package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserTrendDaily {
    private LocalDate statDate;
    private Integer newUsersCount;
    private Integer totalUsers;
    private LocalDateTime lastUpdated;
}
