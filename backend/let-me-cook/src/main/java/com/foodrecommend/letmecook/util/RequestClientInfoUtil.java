package com.foodrecommend.letmecook.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestClientInfoUtil {

    private RequestClientInfoUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlankOrUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlankOrUnknown(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static boolean isBlankOrUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim());
    }
}
