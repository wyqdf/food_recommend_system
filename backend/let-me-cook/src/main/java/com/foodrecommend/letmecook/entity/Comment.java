package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Integer id;
    private Integer recipeId;
    private Integer userId;
    private String content;
    private LocalDateTime publishTime;
    private Integer likes;
    private LocalDateTime createTime;
    
    private String username;
    private String avatar;
    private Boolean isLiked;
}
