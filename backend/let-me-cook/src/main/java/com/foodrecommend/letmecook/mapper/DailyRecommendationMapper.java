package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.DailyRecipeRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyRecommendationMapper {

    @Select("<script>" +
            "SELECT id, user_id, biz_date, recipe_id, rank_no, selected_for_delivery, model_score, reason_json, model_version, created_at " +
            "FROM daily_recipe_recommendations " +
            "WHERE user_id = #{userId} " +
            "AND biz_date = #{bizDate} " +
            "ORDER BY rank_no ASC " +
            "LIMIT #{limit}" +
            "</script>")
    List<DailyRecipeRecommendation> findTodayRankedByUser(@Param("userId") Integer userId,
                                                          @Param("bizDate") LocalDate bizDate,
                                                          @Param("limit") Integer limit);
}
