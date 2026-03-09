package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserProfileDTO {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private LocalDateTime createTime;
    private Integer favoritesCount;
    private Integer commentsCount;
}
