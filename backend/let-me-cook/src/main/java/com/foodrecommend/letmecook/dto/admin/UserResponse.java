package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class UserResponse {
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private String avatar;
    private String lastLoginTime;
    private String createTime;
    private Integer favoritesCount;
    private Integer commentsCount;
}
