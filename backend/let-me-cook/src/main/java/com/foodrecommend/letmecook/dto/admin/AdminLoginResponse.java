package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String token;
    private AdminInfo admin;
}
