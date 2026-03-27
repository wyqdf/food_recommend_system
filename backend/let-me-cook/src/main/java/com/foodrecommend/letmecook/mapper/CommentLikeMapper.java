package com.foodrecommend.letmecook.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentLikeMapper {

    @Insert("INSERT INTO comment_likes(comment_id, user_id) VALUES(#{commentId}, #{userId})")
    int insert(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Select("<script>" +
            "SELECT comment_id FROM comment_likes " +
            "WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach item='commentId' collection='commentIds' open='(' separator=',' close=')'>" +
            "#{commentId}" +
            "</foreach>" +
            "</script>")
    List<Integer> findLikedCommentIds(@Param("userId") Integer userId, @Param("commentIds") List<Integer> commentIds);
}
