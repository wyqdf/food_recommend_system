package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Recipe;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecipeMapper {

        @Select("<script>" +
                        "SELECT DISTINCT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "<if test='category != null'>" +
                        "INNER JOIN recipe_categories rc ON r.id = rc.recipe_id AND rc.category_id = #{category} " +
                        "</if>" +
                        "WHERE r.status = 1 " +
                        "<if test='difficulty != null'>" +
                        "AND d.name = #{difficulty} " +
                        "</if>" +
                        "<if test='time != null'>" +
                        "AND t.name = #{time} " +
                        "</if>" +
                        "<choose>" +
                        "<when test='sort == \"hot\"'>" +
                        "ORDER BY r.like_count DESC" +
                        "</when>" +
                        "<when test='sort == \"collect\"'>" +
                        "ORDER BY r.rating_count DESC" +
                        "</when>" +
                        "<otherwise>" +
                        "ORDER BY r.create_time DESC" +
                        "</otherwise>" +
                        "</choose>" +
                        " LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<Recipe> findByCondition(@Param("category") Integer category,
                        @Param("difficulty") String difficulty,
                        @Param("time") String time,
                        @Param("sort") String sort,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select("<script>" +
                        "SELECT COUNT(DISTINCT r.id) FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "<if test='category != null'>" +
                        "INNER JOIN recipe_categories rc ON r.id = rc.recipe_id AND rc.category_id = #{category} " +
                        "</if>" +
                        "WHERE r.status = 1 " +
                        "<if test='difficulty != null'>" +
                        "AND d.name = #{difficulty} " +
                        "</if>" +
                        "<if test='time != null'>" +
                        "AND t.name = #{time} " +
                        "</if>" +
                        "</script>")
        long countByCondition(@Param("category") Integer category,
                        @Param("difficulty") String difficulty,
                        @Param("time") String time);

        @Select("SELECT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "WHERE r.id = #{id}")
        Recipe findById(Integer id);

        @Select("SELECT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "WHERE r.id = #{id} AND r.status = 1")
        Recipe findPublicById(Integer id);

        @Select("<script>" +
                        "SELECT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "WHERE r.status = 1 AND r.id IN " +
                        "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        List<Recipe> findByIds(@Param("ids") List<Integer> ids);

        @Select("<script>" +
                        "SELECT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "WHERE r.status = 1 AND r.title LIKE CONCAT('%', #{keyword}, '%') " +
                        "ORDER BY r.create_time DESC " +
                        "LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<Recipe> searchByKeyword(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select("SELECT COUNT(*) FROM recipes WHERE status = 1 AND title LIKE CONCAT('%', #{keyword}, '%')")
        long countByKeyword(String keyword);

        @Select("SELECT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "WHERE r.id >= (SELECT FLOOR(RAND() * (SELECT MAX(id) FROM recipes))) " +
                        "AND r.status = 1 " +
                        "ORDER BY r.id LIMIT #{limit}")
        List<Recipe> findRandom(@Param("limit") int limit);

        @Select("<script>" +
                        "SELECT DISTINCT r.*, d.name as difficultyName, t.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs t ON r.time_cost_id = t.id " +
                        "INNER JOIN recipe_ingredients ri ON r.id = ri.recipe_id AND ri.ingredient_type = 'main' " +
                        "INNER JOIN ingredients i ON ri.ingredient_id = i.id " +
                        "WHERE r.id != #{recipeId} " +
                        "AND i.name IN " +
                        "<foreach item='ingredient' collection='mainIngredients' open='(' separator=',' close=')'>" +
                        "        #{ingredient} " +
                        "</foreach> " +
                        "AND r.status = 1 " +
                        "ORDER BY r.like_count DESC " +
                        "LIMIT #{limit}" +
                        "</script>")
        List<Recipe> findSimilarByIngredients(@Param("recipeId") Integer recipeId,
                        @Param("mainIngredients") List<String> mainIngredients,
                        @Param("limit") int limit);

        @Update("UPDATE recipes SET like_count = like_count + 1 WHERE id = #{id}")
        int incrementLikeCount(Integer id);

        @Update("UPDATE recipes SET reply_count = reply_count + 1 WHERE id = #{id}")
        int incrementReplyCount(Integer id);

        @Update("UPDATE recipes SET rating_count = rating_count + 1 WHERE id = #{id}")
        int incrementFavoriteCount(Integer id);

        @Update("UPDATE recipes SET rating_count = GREATEST(0, rating_count - 1) WHERE id = #{id}")
        int decrementFavoriteCount(Integer id);

        @Insert("INSERT INTO recipes (title, author, author_uid, description, tips, cookware, image, taste_id, technique_id, time_cost_id, difficulty_id, create_time, update_time) "
                        +
                        "VALUES (#{title}, #{author}, #{authorUid}, #{description}, #{tips}, #{cookware}, #{image}, #{tasteId}, #{techniqueId}, #{timeCostId}, #{difficultyId}, NOW(), NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Recipe recipe);

        @Delete("DELETE FROM recipes WHERE id = #{id}")
        int deleteById(Integer id);

        @Update("<script>" +
                        "UPDATE recipes SET " +
                        "title = #{title}, " +
                        "author = #{author}, " +
                        "author_uid = #{authorUid}, " +
                        "description = #{description}, " +
                        "tips = #{tips}, " +
                        "cookware = #{cookware}, " +
                        "image = #{image}, " +
                        "taste_id = #{tasteId}, " +
                        "technique_id = #{techniqueId}, " +
                        "time_cost_id = #{timeCostId}, " +
                        "difficulty_id = #{difficultyId}, " +
                        "update_time = #{updateTime} " +
                        "WHERE id = #{id}" +
                        "</script>")
        int update(Recipe recipe);

        @Update("UPDATE recipes SET status = #{status} WHERE id = #{id}")
        int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

        @Select("<script>" +
                        "SELECT r.*, " +
                        "d.name as difficultyName, " +
                        "t.name as tasteName, " +
                        "tech.name as techniqueName, " +
                        "tc.name as timeCostName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN tastes t ON r.taste_id = t.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "WHERE 1=1 " +
                        "<if test='keyword != null and keyword != \"\"'>" +
                        "AND (r.title LIKE CONCAT('%', #{keyword}, '%') OR r.author LIKE CONCAT('%', #{keyword}, '%')) " +
                        "</if>" +
                        "<if test='categoryId != null'>" +
                        "AND EXISTS (SELECT 1 FROM recipe_categories rc WHERE rc.recipe_id = r.id AND rc.category_id = #{categoryId}) " +
                        "</if>" +
                        "<if test='tasteId != null'>" +
                        "AND r.taste_id = #{tasteId} " +
                        "</if>" +
                        "<if test='techniqueId != null'>" +
                        "AND r.technique_id = #{techniqueId} " +
                        "</if>" +
                        "<if test='timeCostId != null'>" +
                        "AND r.time_cost_id = #{timeCostId} " +
                        "</if>" +
                        "<if test='difficultyId != null'>" +
                        "AND r.difficulty_id = #{difficultyId} " +
                        "</if>" +
                        "<if test='status != null'>" +
                        "AND r.status = #{status} " +
                        "</if>" +
                        "<if test='startTime != null and startTime != \"\"'>" +
                        "AND r.create_time >= #{startTime} " +
                        "</if>" +
                        "<if test='endTime != null and endTime != \"\"'>" +
                        "AND r.create_time &lt;= #{endTime} " +
                        "</if>" +
                        "ORDER BY r.create_time DESC " +
                        "LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<Recipe> findForAdmin(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        @Param("tasteId") Integer tasteId,
                        @Param("techniqueId") Integer techniqueId,
                        @Param("timeCostId") Integer timeCostId,
                        @Param("difficultyId") Integer difficultyId,
                        @Param("status") Integer status,
                        @Param("startTime") String startTime,
                        @Param("endTime") String endTime,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select("<script>" +
                        "SELECT COUNT(*) FROM recipes r " +
                        "WHERE 1=1 " +
                        "<if test='keyword != null and keyword != \"\"'>" +
                        "AND (r.title LIKE CONCAT('%', #{keyword}, '%') OR r.author LIKE CONCAT('%', #{keyword}, '%')) " +
                        "</if>" +
                        "<if test='categoryId != null'>" +
                        "AND EXISTS (SELECT 1 FROM recipe_categories rc WHERE rc.recipe_id = r.id AND rc.category_id = #{categoryId}) " +
                        "</if>" +
                        "<if test='tasteId != null'>" +
                        "AND r.taste_id = #{tasteId} " +
                        "</if>" +
                        "<if test='techniqueId != null'>" +
                        "AND r.technique_id = #{techniqueId} " +
                        "</if>" +
                        "<if test='timeCostId != null'>" +
                        "AND r.time_cost_id = #{timeCostId} " +
                        "</if>" +
                        "<if test='difficultyId != null'>" +
                        "AND r.difficulty_id = #{difficultyId} " +
                        "</if>" +
                        "<if test='status != null'>" +
                        "AND r.status = #{status} " +
                        "</if>" +
                        "<if test='startTime != null and startTime != \"\"'>" +
                        "AND r.create_time >= #{startTime} " +
                        "</if>" +
                        "<if test='endTime != null and endTime != \"\"'>" +
                        "AND r.create_time &lt;= #{endTime} " +
                        "</if>" +
                        "</script>")
        long countForAdmin(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId,
                        @Param("tasteId") Integer tasteId,
                        @Param("techniqueId") Integer techniqueId,
                        @Param("timeCostId") Integer timeCostId,
                        @Param("difficultyId") Integer difficultyId,
                        @Param("status") Integer status,
                        @Param("startTime") String startTime,
                        @Param("endTime") String endTime);
}
