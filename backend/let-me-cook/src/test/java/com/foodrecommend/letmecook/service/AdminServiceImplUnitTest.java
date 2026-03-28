package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.exception.BadRequestException;
import com.foodrecommend.letmecook.dto.admin.StatisticsDTO;
import com.foodrecommend.letmecook.dto.admin.UserDTO;
import com.foodrecommend.letmecook.entity.CategoryDistributionSummary;
import com.foodrecommend.letmecook.entity.DifficultyDistributionSummary;
import com.foodrecommend.letmecook.entity.TopRecipesHourly;
import com.foodrecommend.letmecook.entity.UserTrendDaily;
import com.foodrecommend.letmecook.mapper.AdminMapper;
import com.foodrecommend.letmecook.mapper.AdminUserMapper;
import com.foodrecommend.letmecook.mapper.StatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsSummaryMapper;
import com.foodrecommend.letmecook.service.impl.AdminServiceImpl;
import com.foodrecommend.letmecook.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplUnitTest {

    @Mock
    private AdminMapper adminMapper;
    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private StatisticsMapper statisticsMapper;
    @Mock
    private StatisticsSummaryMapper statisticsSummaryMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getUsersShouldUseSafePagination() {
        when(adminUserMapper.countUsers(any(), any())).thenReturn(1L);
        when(adminUserMapper.findUserIds(any(), any(), anyInt(), anyInt())).thenReturn(List.of(1));

        UserDTO user = new UserDTO();
        user.setId(1);
        when(adminUserMapper.findUsersByIds(List.of(1))).thenReturn(List.of(user));
        when(adminUserMapper.countFavoritesByUserIds(List.of(1))).thenReturn(List.of(Map.of("userId", 1, "total", 2)));
        when(adminUserMapper.countCommentsByUserIds(List.of(1))).thenReturn(List.of(Map.of("userId", 1, "total", 3)));

        PageResult<UserDTO> pageResult = adminService.getUsers(0, 999, null, null);

        assertEquals(1, pageResult.getPage());
        assertEquals(200, pageResult.getPageSize());
        assertEquals(1, pageResult.getList().size());
        assertEquals(2, pageResult.getList().get(0).getFavoritesCount());
        assertEquals(3, pageResult.getList().get(0).getCommentsCount());
        verify(adminUserMapper).findUserIds(isNull(), isNull(), eq(0), eq(200));
    }

    @Test
    void getUserStatisticsShouldRespectRangeAndAggregateWeekly() {
        UserTrendDaily firstWeek = new UserTrendDaily();
        firstWeek.setStatDate(LocalDate.of(2026, 3, 3));
        firstWeek.setNewUsersCount(2);
        UserTrendDaily secondWeek = new UserTrendDaily();
        secondWeek.setStatDate(LocalDate.of(2026, 3, 10));
        secondWeek.setNewUsersCount(5);
        UserTrendDaily outOfRange = new UserTrendDaily();
        outOfRange.setStatDate(LocalDate.of(2026, 3, 20));
        outOfRange.setNewUsersCount(9);

        when(statisticsSummaryMapper.getUserTrendByDateRange(LocalDate.of(2026, 3, 3)))
                .thenReturn(List.of(firstWeek, secondWeek, outOfRange));

        StatisticsDTO dto = adminService.getUserStatistics("weekly", "2026-03-03", "2026-03-16");

        assertEquals(3, dto.getTrend().size());
        assertEquals("2026-03-02", dto.getTrend().get(0).getDate());
        assertEquals(2, dto.getTrend().get(0).getCount());
        assertEquals("2026-03-09", dto.getTrend().get(1).getDate());
        assertEquals(5, dto.getTrend().get(1).getCount());
        assertEquals("2026-03-16", dto.getTrend().get(2).getDate());
        assertEquals(0, dto.getTrend().get(2).getCount());
    }

    @Test
    void getRecipeStatisticsShouldUseSnapshotBeforeExplicitEndTime() {
        CategoryDistributionSummary category = new CategoryDistributionSummary();
        category.setCategoryName("家常菜");
        category.setRecipeCount(8);
        DifficultyDistributionSummary difficulty = new DifficultyDistributionSummary();
        difficulty.setDifficultyName("简单");
        difficulty.setRecipeCount(6);
        TopRecipesHourly topRecipe = new TopRecipesHourly();
        topRecipe.setRecipeId(7);
        topRecipe.setRecipeTitle("番茄炒蛋");
        topRecipe.setViewCount(20);
        topRecipe.setLikeCount(10);

        when(statisticsSummaryMapper.getRecipeTrendByDateRange(LocalDate.of(2026, 3, 9))).thenReturn(List.of());
        when(statisticsMapper.getRecipeTrendByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(statisticsSummaryMapper.getCategoryDistributionByDate(LocalDate.of(2026, 3, 15))).thenReturn(List.of(category));
        when(statisticsSummaryMapper.getDifficultyDistributionByDate(LocalDate.of(2026, 3, 15))).thenReturn(List.of(difficulty));
        when(statisticsSummaryMapper.getTopRecipesBefore(LocalDate.of(2026, 3, 16).atStartOfDay().minusNanos(1), 10))
                .thenReturn(List.of(topRecipe));

        StatisticsDTO dto = adminService.getRecipeStatistics("daily", "2026-03-09", "2026-03-15T12:30:00");

        assertEquals(7, dto.getTrend().size());
        assertEquals(1, dto.getCategoryDistribution().size());
        assertEquals("家常菜", dto.getCategoryDistribution().get(0).getName());
        assertEquals(1, dto.getDifficultyDistribution().size());
        assertEquals("简单", dto.getDifficultyDistribution().get(0).getName());
        assertEquals(1, dto.getTopRecipes().size());
        assertEquals(7, dto.getTopRecipes().get(0).getId());
        verify(statisticsSummaryMapper).getTopRecipesBefore(
                eq(LocalDate.of(2026, 3, 16).atStartOfDay().minusNanos(1)),
                eq(10));
    }

    @Test
    void getCommentStatisticsShouldRejectInvalidDate() {
        assertThrows(BadRequestException.class,
                () -> adminService.getCommentStatistics("daily", "not-a-date", null));
    }
}
