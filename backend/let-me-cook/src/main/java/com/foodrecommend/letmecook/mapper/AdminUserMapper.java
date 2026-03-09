package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.dto.admin.UserDTO;
import com.foodrecommend.letmecook.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminUserMapper {

        @Select("<script>" +
                        "SELECT u.*, " +
                        "COALESCE((SELECT COUNT(*) FROM interactions i WHERE i.user_id = u.id AND i.interaction_type = 'favorite'), 0) as favorites_count, " +
                        "COALESCE((SELECT COUNT(*) FROM comments c WHERE c.user_id = u.id), 0) as comments_count " +
                        "FROM users u " +
                        "<where>" +
                        "<if test=\"keyword != null and keyword != ''\">" +
                        "  AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.email LIKE CONCAT('%', #{keyword}, '%'))"
                        +
                        "</if>" +
                        "<if test=\"status != null\">" +
                        "  AND u.status = #{status}" +
                        "</if>" +
                        "</where>" +
                        "ORDER BY u.create_time DESC " +
                        "LIMIT #{offset}, #{pageSize}" +
                        "</script>")
        List<UserDTO> findUsers(@Param("keyword") String keyword, @Param("status") Integer status,
                        @Param("offset") int offset, @Param("pageSize") int pageSize);

        @Select("<script>" +
                        "SELECT COUNT(*) FROM users u " +
                        "<where>" +
                        "<if test=\"keyword != null and keyword != ''\">" +
                        "  AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR u.email LIKE CONCAT('%', #{keyword}, '%'))"
                        +
                        "</if>" +
                        "<if test=\"status != null\">" +
                        "  AND u.status = #{status}" +
                        "</if>" +
                        "</where>" +
                        "</script>")
        long countUsers(@Param("keyword") String keyword, @Param("status") Integer status);

        @Select("SELECT u.*, " +
                        "COALESCE((SELECT COUNT(*) FROM interactions i WHERE i.user_id = u.id AND i.interaction_type = 'favorite'), 0) as favorites_count, " +
                        "COALESCE((SELECT COUNT(*) FROM comments c WHERE c.user_id = u.id), 0) as comments_count " +
                        "FROM users u " +
                        "WHERE u.id = #{id}")
        UserDTO findUserById(@Param("id") Integer id);

        @Select("SELECT * FROM users WHERE username = #{username}")
        User findByUsername(@Param("username") String username);

        @Select("SELECT * FROM users WHERE email = #{email}")
        User findByEmail(@Param("email") String email);

        @Insert("INSERT INTO users(username, password, nickname, email, phone, status, create_time) " +
                        "VALUES(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{status}, NOW())")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insertUser(User user);

        @Update("UPDATE users SET nickname = #{nickname}, email = #{email}, phone = #{phone}, status = #{status} WHERE id = #{id}")
        int updateUser(User user);

        @Update("UPDATE users SET status = #{status} WHERE id = #{id}")
        int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

        @Update("UPDATE users SET password = #{password} WHERE id = #{id}")
        int updatePassword(@Param("id") Integer id, @Param("password") String password);

        @Delete("DELETE FROM users WHERE id = #{id}")
        int deleteUser(@Param("id") Integer id);

        @Delete("<script>" +
                        "DELETE FROM users WHERE id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        int batchDeleteUsers(@Param("ids") Integer[] ids);
}
