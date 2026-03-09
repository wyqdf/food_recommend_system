package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Technique {
    private Integer id;
    private String name;
    private Integer recipeCount;
    private LocalDateTime createTime;
}
