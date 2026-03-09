package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.Date;

@Data
public class UserListDTO {
    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private Date lastLoginTime;
    private Date createTime;
    private Integer favoritesCount;
    private Integer commentsCount;
}
