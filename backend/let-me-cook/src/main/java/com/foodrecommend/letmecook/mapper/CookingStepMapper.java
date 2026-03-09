package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.CookingStep;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CookingStepMapper {

    @Select("SELECT * FROM cooking_steps WHERE recipe_id = #{recipeId} ORDER BY step_number")
    List<CookingStep> findByRecipeId(Integer recipeId);

    @Insert("INSERT INTO cooking_steps (recipe_id, step_number, description, image) VALUES (#{recipeId}, #{stepNumber}, #{description}, #{image})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CookingStep step);

    @Delete("DELETE FROM cooking_steps WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Integer recipeId);
}
