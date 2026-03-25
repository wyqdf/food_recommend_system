package com.foodrecommend.letmecook.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nickname;
    private String email;
    private String avatar;
}
