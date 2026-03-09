package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.admin.AdvancedStatisticsDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdvancedStatisticsCache {

    private final AdvancedStatisticsService advancedStatisticsService;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile AdvancedStatisticsDTO advancedStatistics = new AdvancedStatisticsDTO();
    private volatile List<AdvancedStatisticsDTO.TechniqueStatItem> difficultyDistribution = List.of();
    private volatile List<AdvancedStatisticsDTO.TechniqueStatItem> timeCostDistribution = List.of();
    private volatile AdvancedStatisticsService.MonthlyTrendDTO monthlyTrend = new AdvancedStatisticsService.MonthlyTrendDTO();
    private volatile LocalDateTime lastRefreshTime;

    @PostConstruct
    public void initialize() {
        log.info("Initializing advanced statistics cache at startup...");
        refreshWithRecalculation("startup");
    }

    @Scheduled(fixedDelayString = "${statistics.advanced.cache-refresh-ms:3600000}")
    public void scheduledRefresh() {
        refreshWithRecalculation("scheduled");
    }

    public String refreshNow() {
        return refreshWithRecalculation("manual");
    }

    public AdvancedStatisticsDTO getAdvancedStatistics() {
        return advancedStatistics;
    }

    public List<AdvancedStatisticsDTO.TechniqueStatItem> getDifficultyDistribution() {
        return difficultyDistribution;
    }

    public List<AdvancedStatisticsDTO.TechniqueStatItem> getTimeCostDistribution() {
        return timeCostDistribution;
    }

    public AdvancedStatisticsService.MonthlyTrendDTO getMonthlyTrend() {
        return monthlyTrend;
    }

    public LocalDateTime getLastRefreshTime() {
        return lastRefreshTime;
    }

    private String refreshWithRecalculation(String trigger) {
        if (!refreshLock.tryLock()) {
            log.info("Skip advanced statistics {} refresh because a refresh is already running", trigger);
            return "高级统计刷新任务正在执行中";
        }

        try {
            String statisticsResult = advancedStatisticsService.refreshAllStatistics();
            reloadCache();
            log.info("Advanced statistics cache refreshed by {} at {}", trigger, lastRefreshTime);
            return statisticsResult;
        } catch (Exception e) {
            log.error("Failed to refresh advanced statistics cache by {}", trigger, e);
            return "刷新失败: " + e.getMessage();
        } finally {
            refreshLock.unlock();
        }
    }

    private void reloadCache() {
        advancedStatistics = advancedStatisticsService.getAdvancedStatistics();
        difficultyDistribution = List.copyOf(advancedStatisticsService.getDifficultyDistribution());
        timeCostDistribution = List.copyOf(advancedStatisticsService.getTimeCostDistribution());
        monthlyTrend = advancedStatisticsService.getMonthlyTrend();
        lastRefreshTime = LocalDateTime.now();
    }
}
