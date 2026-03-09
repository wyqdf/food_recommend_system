package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.RecipeIngredient;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecipeIngredientMapper {

        @Select("SELECT ri.*, i.name as ingredientName " +
                        "FROM recipe_ingredients ri " +
                        "INNER JOIN ingredients i ON ri.ingredient_id = i.id " +
                        "WHERE ri.recipe_id = #{recipeId}")
        List<RecipeIngredient> findByRecipeId(Integer recipeId);

        @Select("<script>SELECT ri.*, i.name as ingredientName " +
                        "FROM recipe_ingredients ri " +
                        "INNER JOIN ingredients i ON ri.ingredient_id = i.id " +
                        "WHERE ri.recipe_id IN " +
                        "<foreach item='id' collection='recipeIds' open='(' separator=',' close=')'>#{id}</foreach>" +
                        "</script>")
        List<RecipeIngredient> findByRecipeIds(@Param("recipeIds") List<Integer> recipeIds);

        @Insert("INSERT INTO recipe_ingredients (recipe_id, ingredient_id, ingredient_type, quantity) " +
                        "VALUES (#{recipeId}, #{ingredientId}, #{ingredientType}, #{quantity})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(RecipeIngredient ingredient);

        @Delete("DELETE FROM recipe_ingredients WHERE recipe_id = #{recipeId}")
        int deleteByRecipeId(Integer recipeId);
}
