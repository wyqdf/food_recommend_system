package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class RecipeCreateRequest {
    private String title;
    private String author;
    private String authorUid;
    private String image;
    private String description;
    private String tips;
    private String cookware;
    private Integer tasteId;
    private Integer techniqueId;
    private Integer timeCostId;
    private Integer difficultyId;
    private List<Integer> categoryIds;
    private List<IngredientDTO> ingredients;
    private List<StepDTO> steps;
}
