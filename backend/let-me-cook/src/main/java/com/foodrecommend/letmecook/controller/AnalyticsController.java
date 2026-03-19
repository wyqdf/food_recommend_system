package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.BehaviorEventBatchRequest;
import com.foodrecommend.letmecook.service.BehaviorEventService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final BehaviorEventService behaviorEventService;
    private final AuthTokenHelper authTokenHelper;

    @PostMapping("/events/batch")
    public Result<Map<String, Object>> saveEvents(
            @RequestBody BehaviorEventBatchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Integer userId = null;
        try {
            userId = authTokenHelper.optionalUserId(authorization);
        } catch (Exception ignored) {
            userId = null;
        }

        int accepted = behaviorEventService.saveBatch(userId, request);
        Map<String, Object> data = new HashMap<>();
        data.put("accepted", accepted);
        return Result.success(data);
    }
}
