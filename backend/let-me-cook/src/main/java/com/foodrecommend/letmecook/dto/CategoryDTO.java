package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.Date;

@Data
public class CategoryDTO {
    private Integer id;
    private String name;
    private Integer recipeCount;
    private Date createTime;
}
