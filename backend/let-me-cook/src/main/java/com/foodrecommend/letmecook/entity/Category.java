package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Category {
    private Integer id;
    private String name;
    private LocalDateTime createTime;
    private Integer count;
    private Integer recipeCount;
}
