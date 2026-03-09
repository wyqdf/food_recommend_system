package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

        @Select("SELECT c.*, COALESCE((SELECT COUNT(*) FROM recipe_categories rc WHERE rc.category_id = c.id), 0) as recipeCount FROM categories c ORDER BY c.id")
        List<Category> findAllWithCount();

        @Select("SELECT c.name FROM categories c " +
                        "INNER JOIN recipe_categories rc ON c.id = rc.category_id " +
                        "WHERE rc.recipe_id = #{recipeId}")
        List<String> findByRecipeId(Integer recipeId);

        @Select("SELECT c.id, c.name FROM categories c " +
                        "INNER JOIN recipe_categories rc ON c.id = rc.category_id " +
                        "WHERE rc.recipe_id = #{recipeId} ORDER BY c.id")
        List<Category> findCategoryInfosByRecipeId(Integer recipeId);

        @Select("<script>SELECT rc.recipe_id, c.name FROM categories c " +
                        "INNER JOIN recipe_categories rc ON c.id = rc.category_id " +
                        "WHERE rc.recipe_id IN " +
                        "<foreach item='id' collection='recipeIds' open='(' separator=',' close=')'>#{id}</foreach>" +
                        "</script>")
        List<CategoryRecipeDTO> findByRecipeIds(@Param("recipeIds") List<Integer> recipeIds);

        class CategoryRecipeDTO {
                private Integer recipeId;
                private String name;

                public Integer getRecipeId() {
                        return recipeId;
                }

                public void setRecipeId(Integer recipeId) {
                        this.recipeId = recipeId;
                }

                public String getName() {
                        return name;
                }

                public void setName(String name) {
                        this.name = name;
                }
        }

        @Insert("INSERT INTO recipe_categories (recipe_id, category_id) VALUES (#{recipeId}, #{categoryId})")
        int insertRecipeCategory(@Param("recipeId") Integer recipeId, @Param("categoryId") Integer categoryId);

        @Delete("DELETE FROM recipe_categories WHERE recipe_id = #{recipeId}")
        int deleteRecipeCategoriesByRecipeId(Integer recipeId);

        @Select("SELECT * FROM categories WHERE name = #{name}")
        Category findByName(String name);

        @Insert("INSERT INTO categories (name, create_time) VALUES (#{name}, NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Category category);

        @Select("SELECT * FROM categories WHERE id = #{id}")
        Category findById(Integer id);

        @Update("UPDATE categories SET name = #{name} WHERE id = #{id}")
        int update(Category category);

        @Delete("DELETE FROM categories WHERE id = #{id}")
        int deleteById(Integer id);

        @Select("SELECT COUNT(*) FROM recipe_categories WHERE category_id = #{categoryId}")
        int countRecipesByCategory(Integer categoryId);
}
