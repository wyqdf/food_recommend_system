package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

        @Select("SELECT c.*, u.username, u.avatar " +
                        "FROM comments c " +
                        "LEFT JOIN users u ON c.user_id = u.id " +
                        "WHERE c.recipe_id = #{recipeId} " +
                        "ORDER BY c.publish_time DESC, c.id DESC " +
                        "LIMIT #{offset}, #{pageSize}")
        List<Comment> findByRecipeId(@Param("recipeId") Integer recipeId,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        @Select("SELECT COUNT(*) FROM comments WHERE recipe_id = #{recipeId}")
        long countByRecipeId(Integer recipeId);

        @Insert("INSERT INTO comments(recipe_id, user_id, content, publish_time, likes) " +
                        "VALUES(#{recipeId}, #{userId}, #{content}, #{publishTime}, 0)")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Comment comment);

        @Update("UPDATE comments SET likes = likes + 1 WHERE id = #{id}")
        int incrementLikes(Integer id);

        @Select("SELECT COUNT(*) FROM comments WHERE user_id = #{userId}")
        int countByUserId(Integer userId);
}
