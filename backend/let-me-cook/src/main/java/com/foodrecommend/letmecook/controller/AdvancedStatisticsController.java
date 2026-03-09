package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.AdvancedStatisticsDTO;
import com.foodrecommend.letmecook.service.AdvancedStatisticsCache;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvancedStatisticsController {

    private final AdvancedStatisticsCache advancedStatisticsCache;

    /**
     * 获取高级统计数据
     */
    @GetMapping("/advanced")
    public Result<AdvancedStatisticsDTO> getAdvancedStatistics() {
        return Result.success(advancedStatisticsCache.getAdvancedStatistics());
    }

    /**
     * 获取难度分布
     */
    @GetMapping("/difficulty-distribution")
    public Result<?> getDifficultyDistribution() {
        return Result.success(advancedStatisticsCache.getDifficultyDistribution());
    }

    /**
     * 获取耗时分布
     */
    @GetMapping("/timecost-distribution")
    public Result<?> getTimeCostDistribution() {
        return Result.success(advancedStatisticsCache.getTimeCostDistribution());
    }

    /**
     * 获取月度趋势
     */
    @GetMapping("/monthly-trend")
    public Result<?> getMonthlyTrend() {
        return Result.success(advancedStatisticsCache.getMonthlyTrend());
    }

    /**
     * 手动刷新所有统计数据
     */
    @PostMapping("/refresh")
    public Result<String> refreshStatistics() {
        return Result.success(advancedStatisticsCache.refreshNow());
    }
}
