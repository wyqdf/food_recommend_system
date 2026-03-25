package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Interaction {
    private Integer id;
    private Integer userId;
    private Integer recipeId;
    private String interactionType;
    private LocalDateTime createTime;
}
