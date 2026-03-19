package com.foodrecommend.letmecook.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BehaviorEventBatchRequest {
    private String sessionId;
    private List<EventItem> events;

    @Data
    public static class EventItem {
        private Integer recipeId;
        private String eventType;
        private String sourcePage;
        private String sceneCode;
        private Integer stepNumber;
        private Integer durationMs;
        private Map<String, Object> extra;
    }
}
