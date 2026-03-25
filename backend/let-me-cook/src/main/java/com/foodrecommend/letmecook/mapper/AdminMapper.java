package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.Admin;
import org.apache.ibatis.annotations.*;

import java.util.Date;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admins WHERE username = #{username}")
    Admin findByUsername(@Param("username") String username);

    @Select("SELECT * FROM admins WHERE id = #{id}")
    Admin findById(@Param("id") Integer id);

    @Insert("INSERT INTO admins(username, password, email, role, status) VALUES(#{username}, #{password}, #{email}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Admin admin);

    @Update("UPDATE admins SET email = #{email}, role = #{role}, status = #{status} WHERE id = #{id}")
    int update(Admin admin);

    @Update("UPDATE admins SET last_login_time = #{lastLoginTime}, last_login_ip = #{lastLoginIp} WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Integer id,
            @Param("lastLoginTime") Date lastLoginTime,
            @Param("lastLoginIp") String lastLoginIp);

    @Update("UPDATE admins SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);
}
