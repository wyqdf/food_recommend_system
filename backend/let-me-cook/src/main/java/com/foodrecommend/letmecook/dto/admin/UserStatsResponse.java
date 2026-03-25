package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class UserStatsResponse {
    private List<TrendData> trend;
    private Integer totalNewUsers;
}
