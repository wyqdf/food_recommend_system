package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Recipe;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecipeMapper {

        @Select("<script>" +
                        "SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "<if test='category != null'>" +
                        "INNER JOIN recipe_categories rc ON r.id = rc.recipe_id AND rc.category_id = #{category} " +
                        "</if>" +
                        "WHERE r.status = 1 " +
                        "<if test='difficultyId != null'>" +
                        "AND r.difficulty_id = #{difficultyId} " +
                        "</if>" +
                        "<if test='timeCostId != null'>" +
                        "AND r.time_cost_id = #{timeCostId} " +
                        "</if>" +
                        "<choose>" +
                        "<when test='sort == \"hot\"'>" +
                        "ORDER BY r.like_count DESC, r.id DESC" +
                        "</when>" +
                        "<when test='sort == \"collect\"'>" +
                        "ORDER BY r.favorite_count DESC, r.id DESC" +
                        "</when>" +
                        "<otherwise>" +
                        "ORDER BY r.create_time DESC, r.id DESC" +
                        "</otherwise>" +
                        "</choose>" +
                        " LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<Recipe> findByCondition(@Param("category") Integer category,
                        @Param("difficultyId") Integer difficultyId,
                        @Param("timeCostId") Integer timeCostId,
                        @Param("sort") String sort,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select("<script>" +
                        "SELECT COUNT(*) FROM recipes r " +
                        "<if test='category != null'>" +
                        "INNER JOIN recipe_categories rc ON r.id = rc.recipe_id AND rc.category_id = #{category} " +
                        "</if>" +
                        "WHERE r.status = 1 " +
                        "<if test='difficultyId != null'>" +
                        "AND r.difficulty_id = #{difficultyId} " +
                        "</if>" +
                        "<if test='timeCostId != null'>" +
                        "AND r.time_cost_id = #{timeCostId} " +
                        "</if>" +
                        "</script>")
        long countByCondition(@Param("category") Integer category,
                        @Param("difficultyId") Integer difficultyId,
                        @Param("timeCostId") Integer timeCostId);

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.id = #{id}")
        Recipe findById(Integer id);

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.id = #{id} AND r.status = 1")
        Recipe findPublicById(Integer id);

        @Select("<script>" +
                        "SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 AND r.id IN " +
                        "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        List<Recipe> findByIds(@Param("ids") List<Integer> ids);

        @Select("SELECT COUNT(*) FROM recipes WHERE status = 1")
        long countPublicRecipes();

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 " +
                        "ORDER BY r.id ASC " +
                        "LIMIT #{offset}, #{pageSize}")
        List<Recipe> findPublicForSearchBatch(@Param("offset") int offset, @Param("pageSize") int pageSize);

        @Select({
                        "<script>",
                        "SELECT r.id",
                        "FROM recipe_categories rc",
                        "INNER JOIN recipes r ON r.id = rc.recipe_id",
                        "WHERE rc.category_id = #{categoryId}",
                        "AND r.status = 1",
                        "ORDER BY r.like_count DESC, r.id DESC",
                        "LIMIT #{limit}",
                        "</script>"
        })
        List<Integer> findHotIdsByCategory(@Param("categoryId") Integer categoryId, @Param("limit") int limit);

        @Select({
                        "<script>",
                        "SELECT r.*,",
                        "d.name as difficultyName,",
                        "tc.name as timeCostName,",
                        "ta.name as tasteName,",
                        "tech.name as techniqueName,",
                        "search_candidates.search_score AS searchScore",
                        "FROM recipes r",
                        "INNER JOIN (",
                        "  SELECT candidate.id, MAX(candidate.score) AS search_score",
                        "  FROM (",
                        "    SELECT r.id AS id,",
                        "      CASE",
                        "        WHEN r.title = #{keyword} THEN 120",
                        "        WHEN r.title LIKE CONCAT(#{keyword}, '%') THEN 100",
                        "        ELSE 80",
                        "      END AS score",
                        "    FROM recipes r",
                        "    WHERE r.status = 1",
                        "      AND r.title LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT r.id AS id, 30 AS score",
                        "    FROM recipes r",
                        "    WHERE r.status = 1",
                        "      AND COALESCE(r.author, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT r.id AS id, 18 AS score",
                        "    FROM recipes r",
                        "    LEFT JOIN tastes ta ON r.taste_id = ta.id",
                        "    WHERE r.status = 1",
                        "      AND COALESCE(ta.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT r.id AS id, 15 AS score",
                        "    FROM recipes r",
                        "    LEFT JOIN techniques tech ON r.technique_id = tech.id",
                        "    WHERE r.status = 1",
                        "      AND COALESCE(tech.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT r.id AS id, 12 AS score",
                        "    FROM recipes r",
                        "    LEFT JOIN time_costs tc ON r.time_cost_id = tc.id",
                        "    WHERE r.status = 1",
                        "      AND COALESCE(tc.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT r.id AS id, 12 AS score",
                        "    FROM recipes r",
                        "    LEFT JOIN difficulties d ON r.difficulty_id = d.id",
                        "    WHERE r.status = 1",
                        "      AND COALESCE(d.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT rc.recipe_id AS id, 24 AS score",
                        "    FROM recipe_categories rc",
                        "    INNER JOIN categories c ON c.id = rc.category_id",
                        "    INNER JOIN recipes r ON r.id = rc.recipe_id",
                        "    WHERE r.status = 1",
                        "      AND c.name LIKE CONCAT('%', #{keyword}, '%')",
                        "    UNION ALL",
                        "    SELECT ri.recipe_id AS id, 22 AS score",
                        "    FROM recipe_ingredients ri",
                        "    INNER JOIN ingredients i ON i.id = ri.ingredient_id",
                        "    INNER JOIN recipes r ON r.id = ri.recipe_id",
                        "    WHERE r.status = 1",
                        "      AND i.name LIKE CONCAT('%', #{keyword}, '%')",
                        "  ) candidate",
                        "  GROUP BY candidate.id",
                        ") search_candidates ON search_candidates.id = r.id",
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id",
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id",
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id",
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id",
                        "WHERE r.status = 1",
                        "<choose>",
                        "  <when test='sort == \"hot\"'>",
                        "    ORDER BY r.like_count DESC, r.create_time DESC",
                        "  </when>",
                        "  <when test='sort == \"new\"'>",
                        "    ORDER BY r.create_time DESC",
                        "  </when>",
                        "  <otherwise>",
                        "    ORDER BY searchScore DESC, r.like_count DESC, r.create_time DESC",
                        "  </otherwise>",
                        "</choose>",
                        "LIMIT #{offset}, #{pageSize}",
                        "</script>"
        })
        List<Recipe> searchByKeyword(@Param("keyword") String keyword,
                        @Param("sort") String sort,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select({
                        "<script>",
                        "SELECT COUNT(DISTINCT candidate.id)",
                        "FROM (",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  WHERE r.status = 1",
                        "    AND r.title LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  WHERE r.status = 1",
                        "    AND COALESCE(r.author, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  LEFT JOIN tastes ta ON r.taste_id = ta.id",
                        "  WHERE r.status = 1",
                        "    AND COALESCE(ta.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  LEFT JOIN techniques tech ON r.technique_id = tech.id",
                        "  WHERE r.status = 1",
                        "    AND COALESCE(tech.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  LEFT JOIN time_costs tc ON r.time_cost_id = tc.id",
                        "  WHERE r.status = 1",
                        "    AND COALESCE(tc.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT r.id AS id",
                        "  FROM recipes r",
                        "  LEFT JOIN difficulties d ON r.difficulty_id = d.id",
                        "  WHERE r.status = 1",
                        "    AND COALESCE(d.name, '') LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT rc.recipe_id AS id",
                        "  FROM recipe_categories rc",
                        "  INNER JOIN categories c ON c.id = rc.category_id",
                        "  INNER JOIN recipes r ON r.id = rc.recipe_id",
                        "  WHERE r.status = 1",
                        "    AND c.name LIKE CONCAT('%', #{keyword}, '%')",
                        "  UNION ALL",
                        "  SELECT ri.recipe_id AS id",
                        "  FROM recipe_ingredients ri",
                        "  INNER JOIN ingredients i ON i.id = ri.ingredient_id",
                        "  INNER JOIN recipes r ON r.id = ri.recipe_id",
                        "  WHERE r.status = 1",
                        "    AND i.name LIKE CONCAT('%', #{keyword}, '%')",
                        ") candidate",
                        "</script>"
        })
        long countByKeyword(@Param("keyword") String keyword);

        @Select({
                        "<script>",
                        "SELECT r.title",
                        "FROM recipes r",
                        "WHERE r.status = 1",
                        "AND r.title IS NOT NULL AND r.title != ''",
                        "AND r.title LIKE CONCAT('%', #{keyword}, '%')",
                        "GROUP BY r.title",
                        "ORDER BY",
                        "MIN(CASE",
                        "  WHEN r.title = #{keyword} THEN 0",
                        "  WHEN r.title LIKE CONCAT(#{keyword}, '%') THEN 1",
                        "  ELSE 2",
                        "END),",
                        "MAX(r.like_count) DESC, MAX(r.create_time) DESC",
                        "LIMIT #{limit}",
                        "</script>"
        })
        List<String> findTitleSuggestions(@Param("keyword") String keyword, @Param("limit") int limit);

        @Select({
                        "<script>",
                        "SELECT r.author",
                        "FROM recipes r",
                        "WHERE r.status = 1",
                        "AND r.author IS NOT NULL AND r.author != ''",
                        "AND r.author LIKE CONCAT('%', #{keyword}, '%')",
                        "GROUP BY r.author",
                        "ORDER BY COUNT(*) DESC, MAX(r.like_count) DESC",
                        "LIMIT #{limit}",
                        "</script>"
        })
        List<String> findAuthorSuggestions(@Param("keyword") String keyword, @Param("limit") int limit);

        @Select({
                        "<script>",
                        "SELECT c.name",
                        "FROM categories c",
                        "INNER JOIN recipe_categories rc ON rc.category_id = c.id",
                        "INNER JOIN recipes r ON r.id = rc.recipe_id",
                        "WHERE r.status = 1",
                        "AND c.name LIKE CONCAT('%', #{keyword}, '%')",
                        "GROUP BY c.id, c.name",
                        "ORDER BY COUNT(DISTINCT rc.recipe_id) DESC, c.name ASC",
                        "LIMIT #{limit}",
                        "</script>"
        })
        List<String> findCategorySuggestions(@Param("keyword") String keyword, @Param("limit") int limit);

        @Select({
                        "<script>",
                        "SELECT i.name",
                        "FROM ingredients i",
                        "INNER JOIN recipe_ingredients ri ON ri.ingredient_id = i.id",
                        "INNER JOIN recipes r ON r.id = ri.recipe_id",
                        "WHERE r.status = 1",
                        "AND i.name LIKE CONCAT('%', #{keyword}, '%')",
                        "GROUP BY i.id, i.name",
                        "ORDER BY COUNT(DISTINCT ri.recipe_id) DESC, i.name ASC",
                        "LIMIT #{limit}",
                        "</script>"
        })
        List<String> findIngredientSuggestions(@Param("keyword") String keyword, @Param("limit") int limit);

        @Select("SELECT COALESCE(MAX(id), 0) FROM recipes FORCE INDEX (idx_recipes_status_id) WHERE status = 1")
        Integer findMaxPublicRecipeId();

        @Select("SELECT COALESCE(MAX(id), 0) FROM recipes WHERE status = 1")
        Integer findMaxPublicRecipeIdFallback();

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes AS r FORCE INDEX (idx_recipes_status_id) " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 AND r.id >= #{seedId} " +
                        "ORDER BY r.id ASC " +
                        "LIMIT #{limit}")
        List<Recipe> findRandomFromSeed(@Param("seedId") int seedId, @Param("limit") int limit);

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes AS r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 AND r.id >= #{seedId} " +
                        "ORDER BY r.id ASC " +
                        "LIMIT #{limit}")
        List<Recipe> findRandomFromSeedFallback(@Param("seedId") int seedId, @Param("limit") int limit);

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes AS r FORCE INDEX (idx_recipes_status_id) " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 AND r.id < #{seedId} " +
                        "ORDER BY r.id ASC " +
                        "LIMIT #{limit}")
        List<Recipe> findRandomBeforeSeed(@Param("seedId") int seedId, @Param("limit") int limit);

        @Select("SELECT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes AS r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "WHERE r.status = 1 AND r.id < #{seedId} " +
                        "ORDER BY r.id ASC " +
                        "LIMIT #{limit}")
        List<Recipe> findRandomBeforeSeedFallback(@Param("seedId") int seedId, @Param("limit") int limit);

        @Select("<script>" +
                        "SELECT DISTINCT r.*, d.name as difficultyName, tc.name as timeCostName, ta.name as tasteName, tech.name as techniqueName " +
                        "FROM recipes r " +
                        "LEFT JOIN difficulties d ON r.difficulty_id = d.id " +
                        "LEFT JOIN time_costs tc ON r.time_cost_id = tc.id " +
                        "LEFT JOIN tastes ta ON r.taste_id = ta.id " +
                        "LEFT JOIN techniques tech ON r.technique_id = tech.id " +
                        "INNER JOIN recipe_ingredients ri ON r.id = ri.recipe_id AND ri.ingredient_type = 'main' " +
                        "WHERE r.id != #{recipeId} " +
                        "AND ri.ingredient_id IN " +
                        "<foreach item='ingredientId' collection='mainIngredientIds' open='(' separator=',' close=')'>" +
                        "        #{ingredientId} " +
                        "</foreach> " +
                        "AND r.status = 1 " +
                        "ORDER BY r.like_count DESC " +
                        "LIMIT #{limit}" +
                        "</script>")
        List<Recipe> findSimilarByIngredients(@Param("recipeId") Integer recipeId,
                        @Param("mainIngredientIds") List<Integer> mainIngredientIds,
                        @Param("limit") int limit);

        @Update("UPDATE recipes SET like_count = like_count + 1 WHERE id = #{id}")
        int incrementLikeCount(Integer id);

        @Update("UPDATE recipes SET reply_count = reply_count + 1 WHERE id = #{id}")
        int incrementReplyCount(Integer id);

        @Update("UPDATE recipes SET favorite_count = favorite_count + 1 WHERE id = #{id}")
        int incrementFavoriteCount(Integer id);

        @Update("UPDATE recipes SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = #{id}")
        int decrementFavoriteCount(Integer id);

        @Update("UPDATE recipes SET favorite_count = GREATEST(0, favorite_count - #{amount}) WHERE id = #{id}")
        int decrementFavoriteCountBy(@Param("id") Integer id, @Param("amount") int amount);

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
                        "SELECT r.id " +
                        "FROM recipes r " +
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
                        "ORDER BY r.create_time DESC, r.id DESC " +
                        "LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<Integer> findAdminRecipeIds(@Param("keyword") String keyword,
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
                        "WHERE r.id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        List<Recipe> findAdminByIds(@Param("ids") List<Integer> ids);

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
