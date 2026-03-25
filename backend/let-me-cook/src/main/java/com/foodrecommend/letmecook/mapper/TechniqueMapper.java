package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Technique;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TechniqueMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM techniques ORDER BY id")
    List<Technique> findAll();

    @Select("SELECT * FROM techniques WHERE id = #{id}")
    Technique findById(Integer id);

    @Select("SELECT * FROM techniques WHERE name = #{name}")
    Technique findByName(String name);

    @Insert("INSERT INTO techniques (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Technique technique);

    @Update("UPDATE techniques SET name = #{name} WHERE id = #{id}")
    int update(Technique technique);

    @Delete("DELETE FROM techniques WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE techniques SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
