package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Admin {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String role;
    private Integer status;
    private Date lastLoginTime;
    private String lastLoginIp;
    private Date createTime;
    private Date updateTime;
}
