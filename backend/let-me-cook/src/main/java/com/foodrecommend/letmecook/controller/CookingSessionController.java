package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.CookingSessionDTO;
import com.foodrecommend.letmecook.dto.CookingSessionFinishRequest;
import com.foodrecommend.letmecook.dto.CookingSessionProgressRequest;
import com.foodrecommend.letmecook.dto.CookingSessionStartRequest;
import com.foodrecommend.letmecook.service.CookingSessionService;
import com.foodrecommend.letmecook.util.AuthTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/cooking-sessions")
@RequiredArgsConstructor
public class CookingSessionController {

    private final CookingSessionService cookingSessionService;
    private final AuthTokenHelper authTokenHelper;

    @PostMapping("/start")
    public Result<CookingSessionDTO> startSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CookingSessionStartRequest request) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        CookingSessionDTO dto = cookingSessionService.startOrResumeSession(userId, request.getRecipeId());
        return Result.success(dto);
    }

    @PutMapping("/{sessionId}/progress")
    public Result<CookingSessionDTO> updateProgress(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sessionId,
            @RequestBody(required = false) CookingSessionProgressRequest request) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        CookingSessionDTO dto = cookingSessionService.updateProgress(userId, sessionId, request);
        return Result.success(dto, "进度已保存");
    }

    @PostMapping("/{sessionId}/finish")
    public Result<CookingSessionDTO> finishSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sessionId,
            @RequestBody(required = false) CookingSessionFinishRequest request) {
        Integer userId = authTokenHelper.requireUserId(authorization);
        CookingSessionDTO dto = cookingSessionService.finishSession(userId, sessionId, request);
        return Result.success(dto, "恭喜完成本次烹饪");
    }
}
