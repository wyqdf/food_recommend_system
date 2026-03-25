package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
}
