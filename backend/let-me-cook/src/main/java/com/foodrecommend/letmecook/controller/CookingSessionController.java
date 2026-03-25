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
            @RequestHeader("Authorization") String authorization,
            @RequestBody CookingSessionStartRequest request) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            CookingSessionDTO dto = cookingSessionService.startOrResumeSession(userId, request.getRecipeId());
            return Result.success(dto);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }

    @PutMapping("/{sessionId}/progress")
    public Result<CookingSessionDTO> updateProgress(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sessionId,
            @RequestBody(required = false) CookingSessionProgressRequest request) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            CookingSessionDTO dto = cookingSessionService.updateProgress(userId, sessionId, request);
            return Result.success(dto, "进度已保存");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }

    @PostMapping("/{sessionId}/finish")
    public Result<CookingSessionDTO> finishSession(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sessionId,
            @RequestBody(required = false) CookingSessionFinishRequest request) {
        try {
            Integer userId = authTokenHelper.requireUserId(authorization);
            CookingSessionDTO dto = cookingSessionService.finishSession(userId, sessionId, request);
            return Result.success(dto, "恭喜完成本次烹饪");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(401, "登录已过期，请重新登录");
        }
    }
}
