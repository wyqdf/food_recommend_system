package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.UserReport7dDTO;
import com.foodrecommend.letmecook.service.UserReportService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/reports")
@RequiredArgsConstructor
public class UserReportController {

    private final AuthTokenHelper authTokenHelper;
    private final UserReportService userReportService;

    @GetMapping("/7d")
    public Result<UserReport7dDTO> get7dReport(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        UserReport7dDTO report = userReportService.build7dReport(userId);
        return Result.success(report);
    }
}
