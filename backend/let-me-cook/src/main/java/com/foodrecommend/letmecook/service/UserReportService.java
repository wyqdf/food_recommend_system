package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.dto.UserReport7dDTO;
import com.foodrecommend.letmecook.mapper.BehaviorEventMapper;
import com.foodrecommend.letmecook.mapper.CookingSessionMapper;
import com.foodrecommend.letmecook.util.MapValueUtils;
import com.foodrecommend.letmecook.util.SceneTagResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserReportService {

    private final CookingSessionMapper cookingSessionMapper;
    private final BehaviorEventMapper behaviorEventMapper;

    public UserReport7dDTO build7dReport(Integer userId) {
        int days = 7;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        int startedCount = cookingSessionMapper.countStartedInLastDays(userId, days);
        int completedCount = cookingSessionMapper.countCompletedInLastDays(userId, days);
        int completionRate = startedCount <= 0 ? 0 : Math.min(100, Math.round(completedCount * 100f / startedCount));

        List<UserReport7dDTO.PreferenceItem> scenePreferences = buildScenePreferences(
                behaviorEventMapper.countScenesInLastDays(userId, days, 5));
        List<UserReport7dDTO.PreferenceItem> tastePreferences = buildTastePreferences(
                behaviorEventMapper.countTastePreferenceInLastDays(userId, days, 5));
        List<UserReport7dDTO.ActiveHourItem> activeHours = buildActiveHours(
                behaviorEventMapper.countActiveHoursInLastDays(userId, days, 5));

        UserReport7dDTO dto = new UserReport7dDTO();
        dto.setPeriodStart(start.toString());
        dto.setPeriodEnd(end.toString());
        dto.setStartedCookingCount(startedCount);
        dto.setCompletedCookingCount(completedCount);
        dto.setCompletionRate(completionRate);
        dto.setScenePreferences(scenePreferences);
        dto.setTastePreferences(tastePreferences);
        dto.setActiveHours(activeHours);
        dto.setSummary(buildSummary(startedCount, completedCount, completionRate, scenePreferences, tastePreferences, activeHours));
        dto.setSuggestions(buildSuggestions(startedCount, completionRate, scenePreferences, activeHours));
        return dto;
    }

    private List<UserReport7dDTO.PreferenceItem> buildScenePreferences(List<Map<String, Object>> rows) {
        List<UserReport7dDTO.PreferenceItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            String code = MapValueUtils.getString(row, "scene_code", "sceneCode");
            int count = MapValueUtils.getIntOrDefault(row, 0, "event_count", "eventCount");
            if (code == null || code.isBlank() || count <= 0) {
                continue;
            }
            UserReport7dDTO.PreferenceItem item = new UserReport7dDTO.PreferenceItem();
            item.setCode(code);
            item.setName(SceneTagResolver.sceneName(code));
            item.setCount(count);
            list.add(item);
        }
        return list;
    }

    private List<UserReport7dDTO.PreferenceItem> buildTastePreferences(List<Map<String, Object>> rows) {
        List<UserReport7dDTO.PreferenceItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            String name = MapValueUtils.getString(row, "taste_name", "tasteName");
            int count = MapValueUtils.getIntOrDefault(row, 0, "event_count", "eventCount");
            if (name == null || name.isBlank() || count <= 0) {
                continue;
            }
            UserReport7dDTO.PreferenceItem item = new UserReport7dDTO.PreferenceItem();
            item.setCode(name);
            item.setName(name);
            item.setCount(count);
            list.add(item);
        }
        return list;
    }

    private List<UserReport7dDTO.ActiveHourItem> buildActiveHours(List<Map<String, Object>> rows) {
        List<UserReport7dDTO.ActiveHourItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            Integer hour = MapValueUtils.getInt(row, "hour_of_day", "hourOfDay");
            int count = MapValueUtils.getIntOrDefault(row, 0, "event_count", "eventCount");
            if (hour == null || hour < 0 || hour > 23 || count <= 0) {
                continue;
            }
            UserReport7dDTO.ActiveHourItem item = new UserReport7dDTO.ActiveHourItem();
            item.setHour(hour);
            item.setLabel(String.format("%02d:00", hour));
            item.setCount(count);
            list.add(item);
        }
        return list;
    }

    private String buildSummary(int startedCount,
                                int completedCount,
                                int completionRate,
                                List<UserReport7dDTO.PreferenceItem> scenePreferences,
                                List<UserReport7dDTO.PreferenceItem> tastePreferences,
                                List<UserReport7dDTO.ActiveHourItem> activeHours) {
        if (startedCount <= 0) {
            return "近 7 天你还没有开启烹饪模式，今天就从一道快手菜开始吧。";
        }

        String topScene = scenePreferences.isEmpty() ? "暂无明显场景偏好" : scenePreferences.get(0).getName();
        String topTaste = tastePreferences.isEmpty() ? "口味偏好数据不足" : tastePreferences.get(0).getName();
        String topHour = activeHours.isEmpty() ? "活跃时段待积累" : activeHours.get(0).getLabel();

        return "近 7 天你发起烹饪 " + startedCount + " 次，完成 " + completedCount + " 次，完成率 "
                + completionRate + "%；常见场景是「" + topScene + "」，偏好口味为「" + topTaste + "」，活跃时段集中在 " + topHour + "。";
    }

    private List<String> buildSuggestions(int startedCount,
                                          int completionRate,
                                          List<UserReport7dDTO.PreferenceItem> scenePreferences,
                                          List<UserReport7dDTO.ActiveHourItem> activeHours) {
        List<String> suggestions = new ArrayList<>();

        if (startedCount <= 0) {
            suggestions.add("先从“快手”场景选择 1 道菜，完成一次全流程可快速建立个性化推荐信号。");
            suggestions.add("做菜过程中尽量使用烹饪模式，系统才能给出更稳定的节奏建议。");
        } else {
            if (completionRate < 60) {
                suggestions.add("你的完成率偏低，建议优先选择 20 分钟以内、步骤少于 6 步的菜谱。");
            } else if (completionRate >= 85) {
                suggestions.add("你的完成率很高，可以尝试提升难度或挑战宴客菜，继续扩展口味边界。");
            }
        }

        if (!scenePreferences.isEmpty() && "quick".equals(scenePreferences.get(0).getCode())) {
            suggestions.add("你偏好快手场景，建议提前准备常用食材，进一步缩短做菜时间。");
        }

        if (!activeHours.isEmpty()) {
            suggestions.add("你常在 " + activeHours.get(0).getLabel() + " 前后活跃，可在该时段前预先生成购物与备菜清单。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("继续保持每周至少 3 次烹饪记录，系统会给出更精准的营养与口味建议。");
        }

        return suggestions.stream().distinct().limit(3).toList();
    }
}
