package com.foodrecommend.letmecook.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Integer id;
        private String username;
        private String nickname;
        private String avatar;
        private String email;
    }
}
