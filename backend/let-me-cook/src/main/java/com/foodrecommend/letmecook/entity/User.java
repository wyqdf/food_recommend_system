package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private Integer oldId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
