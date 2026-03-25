package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.TimeCost;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TimeCostMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM time_costs ORDER BY id")
    List<TimeCost> findAll();

    @Select("SELECT * FROM time_costs WHERE id = #{id}")
    TimeCost findById(Integer id);

    @Select("SELECT * FROM time_costs WHERE name = #{name}")
    TimeCost findByName(String name);

    @Insert("INSERT INTO time_costs (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TimeCost timeCost);

    @Update("UPDATE time_costs SET name = #{name} WHERE id = #{id}")
    int update(TimeCost timeCost);

    @Delete("DELETE FROM time_costs WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE time_costs SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
