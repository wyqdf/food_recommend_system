package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeListDTO {
    private Integer id;
    private String name;
    private String image;
    private String difficulty;
    private String time;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer replyCount;
    private List<String> categories;
    private List<String> ingredients;
}
