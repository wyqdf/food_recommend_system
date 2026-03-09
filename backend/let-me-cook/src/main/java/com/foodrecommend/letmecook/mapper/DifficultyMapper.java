package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Difficulty;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DifficultyMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM difficulties ORDER BY id")
    List<Difficulty> findAll();

    @Select("SELECT * FROM difficulties WHERE id = #{id}")
    Difficulty findById(Integer id);

    @Select("SELECT * FROM difficulties WHERE name = #{name}")
    Difficulty findByName(String name);

    @Insert("INSERT INTO difficulties (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Difficulty difficulty);

    @Update("UPDATE difficulties SET name = #{name} WHERE id = #{id}")
    int update(Difficulty difficulty);

    @Delete("DELETE FROM difficulties WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE difficulties SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
