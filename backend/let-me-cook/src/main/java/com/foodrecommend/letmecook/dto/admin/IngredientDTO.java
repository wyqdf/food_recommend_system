package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

@Data
public class IngredientDTO {
    private Integer ingredientId;
    private String type;
    private String quantity;
}
