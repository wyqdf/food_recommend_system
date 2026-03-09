package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.AdvancedStatisticsDTO;
import com.foodrecommend.letmecook.service.AdvancedStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvancedStatisticsController {

    private final AdvancedStatisticsService advancedStatisticsService;

    /**
     * 获取高级统计数据
     */
    @GetMapping("/advanced")
    public Result<AdvancedStatisticsDTO> getAdvancedStatistics() {
        AdvancedStatisticsDTO dto = advancedStatisticsService.getAdvancedStatistics();
        return Result.success(dto);
    }

    /**
     * 获取难度分布
     */
    @GetMapping("/difficulty-distribution")
    public Result<?> getDifficultyDistribution() {
        return Result.success(advancedStatisticsService.getDifficultyDistribution());
    }

    /**
     * 获取耗时分布
     */
    @GetMapping("/timecost-distribution")
    public Result<?> getTimeCostDistribution() {
        return Result.success(advancedStatisticsService.getTimeCostDistribution());
    }

    /**
     * 获取月度趋势
     */
    @GetMapping("/monthly-trend")
    public Result<?> getMonthlyTrend() {
        return Result.success(advancedStatisticsService.getMonthlyTrend());
    }

    /**
     * 手动刷新所有统计数据
     */
    @PostMapping("/refresh")
    public Result<String> refreshStatistics() {
        String result = advancedStatisticsService.refreshAllStatistics();
        return Result.success(result);
    }
}
