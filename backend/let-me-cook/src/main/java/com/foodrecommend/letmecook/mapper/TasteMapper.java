package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Taste;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TasteMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM tastes ORDER BY id")
    List<Taste> findAll();

    @Select("SELECT * FROM tastes WHERE id = #{id}")
    Taste findById(Integer id);

    @Select("SELECT * FROM tastes WHERE name = #{name}")
    Taste findByName(String name);

    @Insert("INSERT INTO tastes (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Taste taste);

    @Update("UPDATE tastes SET name = #{name} WHERE id = #{id}")
    int update(Taste taste);

    @Delete("DELETE FROM tastes WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE tastes SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
