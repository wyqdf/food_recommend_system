package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.CookingSession;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CookingSessionMapper {

    @Insert("INSERT INTO cooking_sessions (" +
            "user_id, recipe_id, status, current_step, total_steps, duration_ms, started_at, last_active_time" +
            ") VALUES (" +
            "#{userId}, #{recipeId}, #{status}, #{currentStep}, #{totalSteps}, #{durationMs}, NOW(), NOW()" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CookingSession session);

    @Select("SELECT " +
            "id, user_id AS userId, recipe_id AS recipeId, status, current_step AS currentStep, " +
            "total_steps AS totalSteps, duration_ms AS durationMs, started_at AS startedAt, " +
            "last_active_time AS lastActiveTime, finished_at AS finishedAt " +
            "FROM cooking_sessions " +
            "WHERE id = #{id} AND user_id = #{userId} " +
            "LIMIT 1")
    CookingSession findByIdAndUser(@Param("id") Long id, @Param("userId") Integer userId);

    @Select("SELECT " +
            "id, user_id AS userId, recipe_id AS recipeId, status, current_step AS currentStep, " +
            "total_steps AS totalSteps, duration_ms AS durationMs, started_at AS startedAt, " +
            "last_active_time AS lastActiveTime, finished_at AS finishedAt " +
            "FROM cooking_sessions " +
            "WHERE user_id = #{userId} " +
            "AND recipe_id = #{recipeId} " +
            "AND status = 'in_progress' " +
            "ORDER BY last_active_time DESC, id DESC " +
            "LIMIT 1")
    CookingSession findLatestInProgressByUserAndRecipe(@Param("userId") Integer userId,
                                                        @Param("recipeId") Integer recipeId);

    @Update("<script>" +
            "UPDATE cooking_sessions " +
            "SET current_step = #{currentStep}, " +
            "duration_ms = CASE " +
            "   WHEN #{durationMs} IS NULL THEN duration_ms " +
            "   ELSE GREATEST(COALESCE(duration_ms, 0), #{durationMs}) " +
            "END, " +
            "last_active_time = NOW() " +
            "WHERE id = #{id} " +
            "AND user_id = #{userId} " +
            "AND status = 'in_progress'" +
            "</script>")
    int updateProgress(@Param("userId") Integer userId,
                       @Param("id") Long id,
                       @Param("currentStep") Integer currentStep,
                       @Param("durationMs") Integer durationMs);

    @Update("<script>" +
            "UPDATE cooking_sessions " +
            "SET status = 'completed', " +
            "current_step = CASE " +
            "   WHEN total_steps IS NULL OR total_steps &lt;= 0 THEN current_step " +
            "   ELSE total_steps " +
            "END, " +
            "duration_ms = CASE " +
            "   WHEN #{durationMs} IS NULL THEN duration_ms " +
            "   ELSE GREATEST(COALESCE(duration_ms, 0), #{durationMs}) " +
            "END, " +
            "finished_at = NOW(), " +
            "last_active_time = NOW() " +
            "WHERE id = #{id} " +
            "AND user_id = #{userId} " +
            "AND status = 'in_progress'" +
            "</script>")
    int finishSession(@Param("userId") Integer userId,
                      @Param("id") Long id,
                      @Param("durationMs") Integer durationMs);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM cooking_sessions " +
            "WHERE user_id = #{userId} " +
            "AND started_at &gt;= DATE_SUB(NOW(), INTERVAL #{days} DAY)" +
            "</script>")
    int countStartedInLastDays(@Param("userId") Integer userId, @Param("days") int days);

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM cooking_sessions " +
            "WHERE user_id = #{userId} " +
            "AND status = 'completed' " +
            "AND finished_at IS NOT NULL " +
            "AND finished_at &gt;= DATE_SUB(NOW(), INTERVAL #{days} DAY)" +
            "</script>")
    int countCompletedInLastDays(@Param("userId") Integer userId, @Param("days") int days);
}
