package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecommendResponse {
    private List<RecipeListDTO> list;
    private String reason;
}
