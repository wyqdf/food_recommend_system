package com.foodrecommend.letmecook.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

public final class MapValueUtils {

    private MapValueUtils() {
    }

    public static Integer getInt(Map<String, Object> source, String... keys) {
        Object value = getValue(source, keys);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public static int getIntOrDefault(Map<String, Object> source, int defaultValue, String... keys) {
        Integer value = getInt(source, keys);
        return value != null ? value : defaultValue;
    }

    public static String getString(Map<String, Object> source, String... keys) {
        Object value = getValue(source, keys);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static LocalDate getLocalDate(Map<String, Object> source, String... keys) {
        Object value = getValue(source, keys);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof CharSequence text) {
            String dateText = text.toString().trim();
            if (dateText.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(dateText.length() > 10 ? dateText.substring(0, 10) : dateText);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object getValue(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
