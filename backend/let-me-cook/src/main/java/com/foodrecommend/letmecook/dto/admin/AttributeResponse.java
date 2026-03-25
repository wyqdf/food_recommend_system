package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class AttributeResponse {
    private Integer id;
    private String name;
    private Integer recipeCount;
    private String createTime;
}
