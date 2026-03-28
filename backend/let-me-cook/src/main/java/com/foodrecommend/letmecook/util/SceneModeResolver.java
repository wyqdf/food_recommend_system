package com.foodrecommend.letmecook.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class SceneModeResolver {

    private SceneModeResolver() {
    }

    public static String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return null;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "family", "fitness", "quick", "party" -> normalized;
            default -> null;
        };
    }

    public static String resolveSceneCode(String mode) {
        String normalizedMode = normalizeMode(mode);
        if (!StringUtils.hasText(normalizedMode)) {
            return null;
        }
        return switch (normalizedMode) {
            case "family" -> "family";
            case "fitness" -> "diet";
            case "quick" -> "quick";
            case "party" -> "banquet";
            default -> null;
        };
    }
}
