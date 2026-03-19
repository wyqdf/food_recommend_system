package com.foodrecommend.letmecook.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserReport7dDTO {
    private String periodStart;
    private String periodEnd;
    private Integer startedCookingCount;
    private Integer completedCookingCount;
    private Integer completionRate;
    private List<PreferenceItem> scenePreferences;
    private List<PreferenceItem> tastePreferences;
    private List<ActiveHourItem> activeHours;
    private String summary;
    private List<String> suggestions;

    @Data
    public static class PreferenceItem {
        private String code;
        private String name;
        private Integer count;
    }

    @Data
    public static class ActiveHourItem {
        private Integer hour;
        private String label;
        private Integer count;
    }
}
