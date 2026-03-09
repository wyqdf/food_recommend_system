package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Ingredient;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IngredientMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM ingredients ORDER BY id LIMIT 1000")
    List<Ingredient> findAll();

    @Select("SELECT * FROM ingredients WHERE id = #{id}")
    Ingredient findById(Integer id);

    @Select("SELECT * FROM ingredients WHERE name = #{name}")
    Ingredient findByName(String name);

    @Insert("INSERT INTO ingredients (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ingredient ingredient);

    @Update("UPDATE ingredients SET name = #{name} WHERE id = #{id}")
    int update(Ingredient ingredient);

    @Delete("DELETE FROM ingredients WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE ingredients SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
