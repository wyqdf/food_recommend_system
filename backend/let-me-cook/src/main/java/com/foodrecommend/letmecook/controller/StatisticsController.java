package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.StatisticsDTO;
import com.foodrecommend.letmecook.dto.admin.StatisticsOverviewDTO;
import com.foodrecommend.letmecook.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final AdminService adminService;
    
    @GetMapping("/overview")
    public Result<StatisticsOverviewDTO> getOverview() {
        StatisticsOverviewDTO overview = adminService.getOverview();
        return Result.success(overview);
    }
    
    @GetMapping("/users")
    public Result<StatisticsDTO> getUserStatistics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        StatisticsDTO statistics = adminService.getUserStatistics(type, startTime, endTime);
        return Result.success(statistics);
    }
    
    @GetMapping("/recipes")
    public Result<StatisticsDTO> getRecipeStatistics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        StatisticsDTO statistics = adminService.getRecipeStatistics(type, startTime, endTime);
        return Result.success(statistics);
    }
    
    @GetMapping("/comments")
    public Result<StatisticsDTO> getCommentStatistics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        StatisticsDTO statistics = adminService.getCommentStatistics(type, startTime, endTime);
        return Result.success(statistics);
    }
}
