package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class AdminInfo {
    private Integer id;
    private String username;
    private String email;
    private String role;
    private Integer status;
    private String lastLoginTime;
}
