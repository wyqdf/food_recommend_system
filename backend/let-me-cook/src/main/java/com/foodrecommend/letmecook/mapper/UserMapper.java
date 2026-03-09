package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Integer id);
    
    @Insert("INSERT INTO users(username, password, nickname, email, avatar, status) VALUES(#{username}, #{password}, #{nickname}, #{email}, #{avatar}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET nickname = #{nickname}, email = #{email}, avatar = #{avatar} WHERE id = #{id}")
    int update(User user);
    
    @Select("SELECT COUNT(*) FROM interactions WHERE user_id = #{userId} AND interaction_type = 'favorite'")
    int countFavoritesByUserId(Integer userId);
    
    @Select("SELECT COUNT(*) FROM comments WHERE user_id = #{userId}")
    int countCommentsByUserId(Integer userId);
}
