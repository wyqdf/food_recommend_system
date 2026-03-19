package com.foodrecommend.letmecook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrecommend.letmecook.dto.BehaviorEventBatchRequest;
import com.foodrecommend.letmecook.entity.BehaviorEvent;
import com.foodrecommend.letmecook.mapper.BehaviorEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BehaviorEventService {

    private final BehaviorEventMapper behaviorEventMapper;
    private final ObjectMapper objectMapper;

    public int saveBatch(Integer userId, BehaviorEventBatchRequest request) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return 0;
        }

        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : UUID.randomUUID().toString();

        List<BehaviorEvent> events = new ArrayList<>();
        for (BehaviorEventBatchRequest.EventItem item : request.getEvents()) {
            if (item == null || !StringUtils.hasText(item.getEventType())) {
                continue;
            }
            BehaviorEvent event = new BehaviorEvent();
            event.setUserId(userId);
            event.setSessionId(sessionId);
            event.setRecipeId(item.getRecipeId());
            event.setEventType(item.getEventType().trim());
            event.setSourcePage(item.getSourcePage());
            event.setSceneCode(item.getSceneCode());
            event.setStepNumber(item.getStepNumber());
            event.setDurationMs(item.getDurationMs());
            event.setExtraJson(toJsonSafe(item.getExtra()));
            events.add(event);
        }

        if (events.isEmpty()) {
            return 0;
        }
        return behaviorEventMapper.insertBatch(events);
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
