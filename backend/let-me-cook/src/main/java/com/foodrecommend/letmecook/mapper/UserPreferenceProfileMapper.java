package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.UserPreferenceProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserPreferenceProfileMapper {

    @Select("SELECT * FROM user_preference_profiles WHERE user_id = #{userId} LIMIT 1")
    UserPreferenceProfile findByUserId(Integer userId);

    @Insert("INSERT INTO user_preference_profiles (" +
            "user_id, diet_goal, cooking_skill, time_budget, preferred_tastes_json, taboo_ingredients_json, " +
            "available_cookwares_json, preferred_scenes_json, onboarding_completed, create_time, update_time" +
            ") VALUES (" +
            "#{userId}, #{dietGoal}, #{cookingSkill}, #{timeBudget}, #{preferredTastesJson}, #{tabooIngredientsJson}, " +
            "#{availableCookwaresJson}, #{preferredScenesJson}, #{onboardingCompleted}, NOW(), NOW()" +
            ") ON DUPLICATE KEY UPDATE " +
            "diet_goal = VALUES(diet_goal), " +
            "cooking_skill = VALUES(cooking_skill), " +
            "time_budget = VALUES(time_budget), " +
            "preferred_tastes_json = VALUES(preferred_tastes_json), " +
            "taboo_ingredients_json = VALUES(taboo_ingredients_json), " +
            "available_cookwares_json = VALUES(available_cookwares_json), " +
            "preferred_scenes_json = VALUES(preferred_scenes_json), " +
            "onboarding_completed = VALUES(onboarding_completed), " +
            "update_time = NOW()")
    int upsert(UserPreferenceProfile profile);
}
