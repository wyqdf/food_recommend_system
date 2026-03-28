package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.BehaviorEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface BehaviorEventMapper {

    @Insert("<script>" +
            "INSERT INTO behavior_events (" +
            "user_id, session_id, recipe_id, event_type, source_page, scene_code, step_number, duration_ms, extra_json, create_time" +
            ") VALUES " +
            "<foreach collection='events' item='event' separator=','>" +
            "(" +
            "#{event.userId}, #{event.sessionId}, #{event.recipeId}, #{event.eventType}, #{event.sourcePage}, #{event.sceneCode}, " +
            "#{event.stepNumber}, #{event.durationMs}, #{event.extraJson}, NOW()" +
            ")" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("events") List<BehaviorEvent> events);

    @Select("<script>" +
            "SELECT recipe_id " +
            "FROM behavior_events " +
            "WHERE user_id = #{userId} " +
            "AND recipe_id IS NOT NULL " +
            "ORDER BY create_time DESC, id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Integer> findRecentRecipeIdsByUser(@Param("userId") Integer userId, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT " +
            "scene_code, " +
            "COUNT(*) AS event_count " +
            "FROM behavior_events FORCE INDEX (idx_behavior_user_time_scene) " +
            "WHERE user_id = #{userId} " +
            "AND create_time &gt;= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND scene_code IS NOT NULL " +
            "AND scene_code &lt;&gt; '' " +
            "GROUP BY scene_code " +
            "ORDER BY event_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> countScenesInLastDays(@Param("userId") Integer userId,
                                                     @Param("days") int days,
                                                     @Param("limit") int limit);

    @Select("<script>" +
            "SELECT " +
            "COALESCE(t.name, '未知') AS taste_name, " +
            "COUNT(*) AS event_count " +
            "FROM behavior_events e FORCE INDEX (idx_behavior_user_event_time_recipe) " +
            "INNER JOIN recipes r ON e.recipe_id = r.id " +
            "LEFT JOIN tastes t ON r.taste_id = t.id " +
            "WHERE e.user_id = #{userId} " +
            "AND e.create_time &gt;= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND e.recipe_id IS NOT NULL " +
            "AND e.event_type IN ('recipe_view', 'recipe_click', 'favorite_add', 'cooking_start', 'cooking_finish') " +
            "GROUP BY COALESCE(t.name, '未知') " +
            "ORDER BY event_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> countTastePreferenceInLastDays(@Param("userId") Integer userId,
                                                              @Param("days") int days,
                                                              @Param("limit") int limit);

    @Select("<script>" +
            "SELECT " +
            "HOUR(create_time) AS hour_of_day, " +
            "COUNT(*) AS event_count " +
            "FROM behavior_events FORCE INDEX (idx_behavior_user_time) " +
            "WHERE user_id = #{userId} " +
            "AND create_time &gt;= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY HOUR(create_time) " +
            "ORDER BY event_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> countActiveHoursInLastDays(@Param("userId") Integer userId,
                                                          @Param("days") int days,
                                                          @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM behavior_events
            WHERE recipe_id = #{recipeId}
            AND event_type = 'recipe_view'
            """)
    int countRecipeViewsByRecipeId(@Param("recipeId") Integer recipeId);

    @Select("<script>" +
            "SELECT recipe_id AS recipeId, COUNT(*) AS total " +
            "FROM behavior_events " +
            "WHERE event_type = 'recipe_view' " +
            "AND recipe_id IN " +
            "<foreach item='id' collection='recipeIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY recipe_id" +
            "</script>")
    List<RecipeViewCountDTO> countRecipeViewsByRecipeIds(@Param("recipeIds") List<Integer> recipeIds);

    class RecipeViewCountDTO {
        private Integer recipeId;
        private Integer total;

        public Integer getRecipeId() {
            return recipeId;
        }

        public void setRecipeId(Integer recipeId) {
            this.recipeId = recipeId;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }
}
