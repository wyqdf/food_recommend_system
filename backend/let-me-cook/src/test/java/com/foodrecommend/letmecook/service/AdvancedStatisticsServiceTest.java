package com.foodrecommend.letmecook.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AdvancedStatisticsServiceTest {

    @Autowired
    private AdvancedStatisticsService advancedStatisticsService;

    @Test
    void refreshAllStatistics_shouldSucceed() {
        String result = advancedStatisticsService.refreshAllStatistics();
        assertTrue(
                result != null && result.startsWith("统计数据刷新完成"),
                () -> "Expected refresh success message, but got: " + result
        );
    }
}
