package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private Integer status;
}
