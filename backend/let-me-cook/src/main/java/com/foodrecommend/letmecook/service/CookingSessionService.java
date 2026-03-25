package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.CookingSessionDTO;
import com.foodrecommend.letmecook.dto.CookingSessionFinishRequest;
import com.foodrecommend.letmecook.dto.CookingSessionProgressRequest;
import com.foodrecommend.letmecook.entity.CookingSession;
import com.foodrecommend.letmecook.entity.Recipe;
import com.foodrecommend.letmecook.mapper.CookingSessionMapper;
import com.foodrecommend.letmecook.mapper.CookingStepMapper;
import com.foodrecommend.letmecook.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CookingSessionService {

    private final CookingSessionMapper cookingSessionMapper;
    private final RecipeMapper recipeMapper;
    private final CookingStepMapper cookingStepMapper;

    @Transactional
    public CookingSessionDTO startOrResumeSession(Integer userId, Integer recipeId) {
        if (recipeId == null || recipeId <= 0) {
            throw new RuntimeException("recipeId 无效");
        }

        CookingSession latest = cookingSessionMapper.findLatestInProgressByUserAndRecipe(userId, recipeId);
        if (latest != null) {
            return toDto(latest, true);
        }

        Recipe recipe = recipeMapper.findPublicById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("菜谱不存在");
        }

        int totalSteps = Math.max(cookingStepMapper.findByRecipeId(recipeId).size(), 1);
        CookingSession session = new CookingSession();
        session.setUserId(userId);
        session.setRecipeId(recipeId);
        session.setStatus("in_progress");
        session.setCurrentStep(1);
        session.setTotalSteps(totalSteps);
        session.setDurationMs(0);
        cookingSessionMapper.insert(session);

        CookingSession created = cookingSessionMapper.findByIdAndUser(session.getId(), userId);
        return toDto(created, false);
    }

    @Transactional
    public CookingSessionDTO updateProgress(Integer userId, Long sessionId, CookingSessionProgressRequest request) {
        CookingSession session = requireSession(userId, sessionId);
        int totalSteps = Math.max(session.getTotalSteps() == null ? 1 : session.getTotalSteps(), 1);
        int currentStep = session.getCurrentStep() == null ? 1 : session.getCurrentStep();
        Integer targetStep = request == null ? currentStep : request.getCurrentStep();
        if (targetStep == null) {
            targetStep = currentStep;
        }
        int safeStep = Math.min(Math.max(targetStep, 1), totalSteps);
        Integer durationMs = request == null ? null : safeDuration(request.getDurationMs());

        int updated = cookingSessionMapper.updateProgress(userId, sessionId, safeStep, durationMs);
        if (updated <= 0) {
            throw new RuntimeException("烹饪会话不存在或已结束");
        }
        return toDto(requireSession(userId, sessionId), true);
    }

    @Transactional
    public CookingSessionDTO finishSession(Integer userId, Long sessionId, CookingSessionFinishRequest request) {
        requireSession(userId, sessionId);
        Integer durationMs = request == null ? null : safeDuration(request.getDurationMs());
        int updated = cookingSessionMapper.finishSession(userId, sessionId, durationMs);
        if (updated <= 0) {
            throw new RuntimeException("烹饪会话不存在或已结束");
        }
        return toDto(requireSession(userId, sessionId), true);
    }

    private Integer safeDuration(Integer durationMs) {
        if (durationMs == null) {
            return null;
        }
        return Math.max(durationMs, 0);
    }

    private CookingSession requireSession(Integer userId, Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new RuntimeException("sessionId 无效");
        }
        CookingSession session = cookingSessionMapper.findByIdAndUser(sessionId, userId);
        if (session == null) {
            throw new RuntimeException("烹饪会话不存在");
        }
        return session;
    }

    private CookingSessionDTO toDto(CookingSession session, boolean resumed) {
        CookingSessionDTO dto = new CookingSessionDTO();
        dto.setSessionId(session.getId());
        dto.setRecipeId(session.getRecipeId());
        dto.setRecipeTitle(resolveRecipeTitle(session.getRecipeId()));
        dto.setStatus(session.getStatus());
        dto.setResumed(resumed);
        dto.setCurrentStep(session.getCurrentStep());
        dto.setTotalSteps(session.getTotalSteps());
        dto.setDurationMs(session.getDurationMs());
        dto.setStartedAt(session.getStartedAt());
        dto.setLastActiveTime(session.getLastActiveTime());
        dto.setFinishedAt(session.getFinishedAt());

        int total = Math.max(session.getTotalSteps() == null ? 0 : session.getTotalSteps(), 0);
        int current = Math.max(session.getCurrentStep() == null ? 0 : session.getCurrentStep(), 0);
        int percent = total <= 0 ? 0 : Math.min(100, Math.round(current * 100f / total));
        dto.setProgressPercent(percent);
        return dto;
    }

    private String resolveRecipeTitle(Integer recipeId) {
        if (recipeId == null) {
            return null;
        }
        Recipe recipe = recipeMapper.findPublicById(recipeId);
        return recipe == null ? null : recipe.getTitle();
    }
}
