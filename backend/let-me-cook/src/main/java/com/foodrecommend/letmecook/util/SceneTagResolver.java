package com.foodrecommend.letmecook.util;

import com.foodrecommend.letmecook.dto.SceneTagDTO;
import com.foodrecommend.letmecook.dto.RecipeListDTO;
import com.foodrecommend.letmecook.entity.Recipe;

import java.util.*;

public final class SceneTagResolver {

    private static final LinkedHashMap<String, SceneTagDTO> SCENES = new LinkedHashMap<>();

    static {
        SCENES.put("quick", new SceneTagDTO("quick", "快手", "20 分钟内可完成"));
        SCENES.put("diet", new SceneTagDTO("diet", "减脂", "偏低卡、清爽、控油"));
        SCENES.put("solo", new SceneTagDTO("solo", "一人食", "分量轻、流程简单"));
        SCENES.put("family", new SceneTagDTO("family", "家庭餐", "家常高频、适合多人"));
        SCENES.put("banquet", new SceneTagDTO("banquet", "宴客", "适合招待或聚会"));
        SCENES.put("supper", new SceneTagDTO("supper", "夜宵", "操作快、口味集中"));
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

        if (containsAny(time, "10", "15", "20", "半小时", "30分钟") || containsAny(difficulty, "简单")) {
            codes.add("quick");
        }
        if (isDietRecipe(normalizedTitle, normalizedCategories, normalizedIngredients, taste)) {
            codes.add("diet");
        }
        if (containsAny(normalizedTitle, "一人食", "单人")
                || containsAnyList(normalizedCategories, "一人食")) {
            codes.add("solo");
        }
        if (containsAny(normalizedTitle, "家常", "家庭")
                || containsAnyList(normalizedCategories, "家常", "家庭")) {
            codes.add("family");
        }
        if (containsAny(normalizedTitle, "宴客", "聚会", "硬菜", "招待")) {
            codes.add("banquet");
        }
        if (containsAny(normalizedTitle, "夜宵", "宵夜")) {
            codes.add("supper");
        }
        if (containsAny(normalizedTitle, "宝宝", "辅食", "儿童")
                || containsAnyList(normalizedCategories, "宝宝餐", "辅食")) {
            codes.add("baby");
        }
        if (containsAny(normalizedTitle, "下饭", "鱼香", "宫保", "红烧", "麻婆", "辣子")) {
            codes.add("with_rice");
        }

        if (codes.isEmpty()) {
            codes.add("family");
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

    private static boolean isDietRecipe(String title, List<String> categories, List<String> ingredients, String taste) {
        boolean explicitSignal = containsAny(title, "减脂", "轻食", "低卡", "健身餐", "沙拉")
                || containsAnyList(categories, "减脂", "轻食", "健身", "沙拉", "瘦身");

        boolean leanProteinSignal = containsAnyList(ingredients,
                "鸡胸", "虾", "虾仁", "鳕鱼", "鱼片", "金枪鱼", "三文鱼", "鸡蛋白", "蛋白", "豆腐", "魔芋");
        boolean vegetableSignal = containsAnyList(ingredients,
                "生菜", "西兰花", "黄瓜", "番茄", "圣女果", "紫甘蓝", "菠菜", "芦笋", "苦瓜", "玉米粒");
        boolean cleanTasteSignal = containsAny(taste, "清淡", "原味");

        boolean positive = explicitSignal || (leanProteinSignal && (vegetableSignal || cleanTasteSignal));

        boolean heavyTitleSignal = containsAny(title,
                "红烧", "糖醋", "可乐", "干锅", "回锅", "香酥", "油焖", "炸", "排骨", "肘子", "猪蹄", "扣肉", "宫保", "鱼香",
                "炒饭", "锅贴", "蛋饼", "煎饼", "薯条", "年糕");
        boolean heavyIngredientSignal = containsAnyList(ingredients,
                "排骨", "猪蹄", "五花肉", "肥牛", "肥羊", "腊肉", "培根", "香肠", "腊肠", "火腿", "年糕", "沙拉酱", "奶油", "黄油");
        boolean heavyTasteSignal = containsAny(taste, "甜味", "酸甜");

        return positive && !(heavyTitleSignal || heavyIngredientSignal || heavyTasteSignal);
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
