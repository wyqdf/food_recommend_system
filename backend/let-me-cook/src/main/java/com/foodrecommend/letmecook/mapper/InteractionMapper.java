package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Interaction;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InteractionMapper {

    @Insert("INSERT INTO interactions(user_id, recipe_id, interaction_type) VALUES(#{userId}, #{recipeId}, #{interactionType})")
    int insert(Interaction interaction);

    @Delete("DELETE FROM interactions WHERE user_id = #{userId} AND recipe_id = #{recipeId} AND interaction_type = 'favorite'")
    int deleteFavorite(@Param("userId") Integer userId, @Param("recipeId") Integer recipeId);

    @Select("SELECT EXISTS(SELECT 1 FROM interactions WHERE user_id = #{userId} AND recipe_id = #{recipeId} AND interaction_type = 'favorite')")
    boolean existsFavorite(@Param("userId") Integer userId, @Param("recipeId") Integer recipeId);

    @Select("""
            SELECT i.recipe_id
            FROM interactions i
            INNER JOIN recipes r ON r.id = i.recipe_id
            WHERE i.user_id = #{userId}
            AND i.interaction_type = 'favorite'
            AND r.status = 1
            ORDER BY i.create_time DESC, i.id DESC
            LIMIT #{limit}
            """)
    List<Integer> findRecentPublicFavoriteRecipeIds(@Param("userId") Integer userId,
                                                    @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*)
            FROM interactions i
            INNER JOIN recipes r ON r.id = i.recipe_id
            WHERE i.user_id = #{userId}
            AND i.interaction_type = 'favorite'
            AND r.status = 1
            """)
    long countPublicFavoriteRecipes(Integer userId);

    @Select("""
            SELECT i.recipe_id
            FROM interactions i
            INNER JOIN recipes r ON r.id = i.recipe_id
            WHERE i.user_id = #{userId}
            AND i.interaction_type = 'favorite'
            AND r.status = 1
            ORDER BY i.create_time DESC, i.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<Integer> findPublicFavoriteRecipeIdsPage(@Param("userId") Integer userId,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM interactions WHERE recipe_id = #{recipeId} AND interaction_type = 'favorite'")
    int countFavoritesByRecipeId(Integer recipeId);

    @Select("SELECT recipe_id, COUNT(*) as count FROM interactions WHERE recipe_id IN " +
            "<foreach item='id' collection='recipeIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND interaction_type = 'favorite' GROUP BY recipe_id")
    List<FavoriteCountDTO> countFavoritesByRecipeIds(@Param("recipeIds") List<Integer> recipeIds);

    class FavoriteCountDTO {
        private Integer recipeId;
        private Integer count;

        public Integer getRecipeId() {
            return recipeId;
        }

        public void setRecipeId(Integer recipeId) {
            this.recipeId = recipeId;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
