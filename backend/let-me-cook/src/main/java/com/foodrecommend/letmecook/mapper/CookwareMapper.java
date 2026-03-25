package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Cookware;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CookwareMapper {

    @Select("SELECT *, COALESCE(recipe_count, 0) as recipeCount FROM cookwares ORDER BY id")
    List<Cookware> findAll();

    @Select("SELECT * FROM cookwares WHERE id = #{id}")
    Cookware findById(Integer id);

    @Select("SELECT * FROM cookwares WHERE name = #{name}")
    Cookware findByName(String name);

    @Insert("INSERT INTO cookwares (name, create_time) VALUES (#{name}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Cookware cookware);

    @Update("UPDATE cookwares SET name = #{name} WHERE id = #{id}")
    int update(Cookware cookware);

    @Delete("DELETE FROM cookwares WHERE id = #{id}")
    int deleteById(Integer id);

    @Update("UPDATE cookwares SET recipe_count = recipe_count + #{count} WHERE id = #{id}")
    int updateRecipeCount(@Param("id") Integer id, @Param("count") Integer count);
}
