package com.foodrecommend.letmecook.util;

import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.dto.SceneTagDTO;
import com.foodrecommend.letmecook.entity.Recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SceneTagResolver {

    private static final LinkedHashMap<String, SceneTagDTO> SCENES = new LinkedHashMap<>();

    static {
        SCENES.put("quick", new SceneTagDTO("quick", "快手", "20 分钟内可完成"));
        SCENES.put("diet", new SceneTagDTO("diet", "减脂", "低卡清淡、蛋白优先"));
        SCENES.put("solo", new SceneTagDTO("solo", "一人食", "分量轻、流程简单"));
        SCENES.put("family", new SceneTagDTO("family", "家庭餐", "家常高频、适合多人"));
        SCENES.put("banquet", new SceneTagDTO("banquet", "宴客", "适合招待或聚会"));
        SCENES.put("supper", new SceneTagDTO("supper", "夜宵", "操作快、风味集中"));
        SCENES.put("baby", new SceneTagDTO("baby", "宝宝餐", "儿童/辅食相关"));
        SCENES.put("with_rice", new SceneTagDTO("with_rice", "下饭", "重口或酱香型下饭菜"));
    }

    private SceneTagResolver() {
    }

    public static List<SceneTagDTO> catalog() {
        return new ArrayList<>(SCENES.values());
    }

    public static String sceneName(String code) {
        SceneTagDTO dto = SCENES.get(code);
        return dto == null ? code : dto.getName();
    }

    public static List<String> resolveTagNames(Recipe recipe, List<String> categories) {
        return resolveCodes(recipe.getTitle(), recipe.getDifficultyName(), recipe.getTimeCostName(), categories)
                .stream()
                .map(SceneTagResolver::sceneName)
                .toList();
    }

    public static List<String> resolveTagNames(RecipeListDTO recipe) {
        return resolveCodes(
                recipe.getName(),
                recipe.getDifficulty(),
                recipe.getTime(),
                recipe.getCategories(),
                recipe.getIngredients(),
                recipe.getTaste())
                .stream()
                .map(SceneTagResolver::sceneName)
                .toList();
    }

    public static List<String> resolveCodes(String title, String difficulty, String time, List<String> categories) {
        return resolveCodes(title, difficulty, time, categories, null, null);
    }

    public static List<String> resolveCodes(String title,
            String difficulty,
            String time,
            List<String> categories,
            List<String> ingredients,
            String taste) {
        Set<String> codes = new LinkedHashSet<>();
        String normalizedTitle = title == null ? "" : title;
        List<String> normalizedCategories = categories == null ? List.of() : categories;
        List<String> normalizedIngredients = ingredients == null ? List.of() : ingredients;

        if (isQuickTime(time)
                || containsAny(normalizedTitle, "快手", "速食", "速做", "懒人")
                || containsAnyList(normalizedCategories, "快手菜", "便当")) {
            codes.add("quick");
        }
        if (isDietRecipe(normalizedTitle, normalizedCategories, normalizedIngredients, taste)) {
            codes.add("diet");
        }
        if (containsAny(normalizedTitle, "一人食", "单人", "便当", "工作日")
                || containsAnyList(normalizedCategories, "一人食", "便当", "快手菜")) {
            codes.add("solo");
        }
        if (containsAny(normalizedTitle, "家常", "家庭", "全家", "儿童")
                || containsAnyList(normalizedCategories, "家常菜", "家庭餐", "儿童餐", "下饭菜")) {
            codes.add("family");
        }
        if (containsAny(normalizedTitle, "宴客", "聚会", "硬菜", "招待", "派对")
                || containsAnyList(normalizedCategories, "宴客菜", "聚会", "甜点", "烘焙")) {
            codes.add("banquet");
        }
        if (containsAny(normalizedTitle, "夜宵", "宵夜")
                || containsAnyList(normalizedCategories, "夜宵")) {
            codes.add("supper");
        }
        if (containsAny(normalizedTitle, "宝宝", "辅食", "儿童")
                || containsAnyList(normalizedCategories, "宝宝餐", "辅食", "儿童餐")) {
            codes.add("baby");
        }
        if (containsAny(normalizedTitle, "下饭", "鱼香", "宫保", "红烧", "麻婆", "辣子")
                || containsAnyList(normalizedCategories, "下饭菜")) {
            codes.add("with_rice");
        }

        return new ArrayList<>(codes);
    }

    public static boolean matchesScene(RecipeListDTO recipe, String sceneCode) {
        if (recipe == null || sceneCode == null || sceneCode.isBlank()) {
            return false;
        }
        return resolveCodes(
                recipe.getName(),
                recipe.getDifficulty(),
                recipe.getTime(),
                recipe.getCategories(),
                recipe.getIngredients(),
                recipe.getTaste()).contains(sceneCode.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isQuickTime(String timeText) {
        Integer minutes = parseMinutes(timeText);
        return minutes != null && minutes <= 20;
    }

    private static boolean isDietRecipe(String title, List<String> categories, List<String> ingredients, String taste) {
        boolean explicitSignal = containsAny(title, "减脂", "轻食", "低卡", "健身餐", "沙拉", "高蛋白")
                || containsAnyList(categories, "减脂", "轻食", "健身", "沙拉", "瘦身");

        boolean leanProteinSignal = containsDietFriendlyProtein(ingredients);
        boolean vegetableSignal = containsAnyList(ingredients,
                "生菜", "西兰花", "黄瓜", "番茄", "圣女果", "紫甘蓝", "菠菜", "苦瓜", "玉米粒", "蘑菇");
        boolean cleanTasteSignal = containsAny(taste, "清淡", "原味");

        boolean positive = explicitSignal || (leanProteinSignal && (vegetableSignal || cleanTasteSignal));

        boolean heavyTitleSignal = containsAny(title,
                "红烧", "糖醋", "可乐", "干锅", "回锅", "香酥", "油焖", "炸", "排骨", "肘子", "猪蹄", "扣肉", "宫保", "鱼香",
                "炒饭", "煲仔饭", "焖饭", "盖饭", "锅贴", "饺子", "蒸饺", "包子", "馄饨", "蛋饼", "煎饼", "油条", "年糕",
                "肉丝", "肉卷");
        boolean heavyIngredientSignal = containsAnyList(ingredients,
                "排骨", "猪蹄", "五花肉", "猪肉", "猪里脊", "猪里脊肉", "肥牛", "肥羊", "腊肉", "培根", "香肠", "腊肠",
                "火腿", "年糕", "糯米", "面粉", "饺子皮", "豆腐皮", "油豆腐", "豆腐泡", "沙拉酱", "奶油", "黄油",
                "甜面酱", "蚝油");
        boolean heavyTasteSignal = containsAny(taste, "甜味", "酸甜");

        return positive && !(heavyTitleSignal || heavyIngredientSignal || heavyTasteSignal);
    }

    private static boolean containsDietFriendlyProtein(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }
        for (String ingredient : ingredients) {
            if (ingredient == null || ingredient.isBlank()) {
                continue;
            }
            if (containsAny(ingredient, "鸡胸", "鸡胸肉", "虾", "虾仁", "鳕鱼", "鱼片", "金枪鱼", "三文鱼", "蛋白", "魔芋", "牛腱")) {
                return true;
            }
            if (ingredient.contains("豆腐")
                    && !containsAny(ingredient, "豆腐皮", "油豆腐", "豆腐泡", "腐竹")) {
                return true;
            }
        }
        return false;
    }

    private static Integer parseMinutes(String timeText) {
        if (timeText == null || timeText.isBlank()) {
            return null;
        }
        String text = timeText.replaceAll("\\s+", "");
        if (text.contains("半小时")) {
            return 30;
        }
        if (text.contains("一刻钟")) {
            return 15;
        }
        if (text.contains("三刻钟")) {
            return 45;
        }
        if (text.contains("数天")) {
            return 24 * 60;
        }
        if (text.contains("一天")) {
            return 24 * 60;
        }
        if (text.contains("数小时")) {
            return 3 * 60;
        }

        if (text.contains("小时")) {
            Integer hour = parseNumberToken(text.substring(0, text.indexOf("小时")));
            return hour == null ? null : hour * 60;
        }
        if (text.contains("分钟")) {
            return parseNumberToken(text.substring(0, text.indexOf("分钟")));
        }
        if (text.endsWith("分")) {
            return parseNumberToken(text.substring(0, text.length() - 1));
        }

        return parseNumberToken(text);
    }

    private static Integer parseNumberToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim()
                .replace("兩", "二")
                .replace("两", "二")
                .replace("廿", "二十");
        if (normalized.matches("\\d+")) {
            return Integer.parseInt(normalized);
        }
        if (normalized.equals("半")) {
            return null;
        }
        if (normalized.contains("十")) {
            int idx = normalized.indexOf("十");
            String tensPart = normalized.substring(0, idx);
            String onesPart = normalized.substring(idx + 1);
            Integer tens = tensPart.isEmpty() ? 1 : chineseDigit(tensPart);
            Integer ones = onesPart.isEmpty() ? 0 : chineseDigit(onesPart);
            if (tens == null || ones == null) {
                return null;
            }
            return tens * 10 + ones;
        }
        return chineseDigit(normalized);
    }

    private static Integer chineseDigit(String text) {
        return switch (text) {
            case "零" -> 0;
            case "一" -> 1;
            case "二" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            default -> null;
        };
    }

    private static boolean containsAny(String text, String... keys) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyList(List<String> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (containsAny(value, keys)) {
                return true;
            }
        }
        return false;
    }
}
