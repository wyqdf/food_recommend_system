package com.foodrecommend.letmecook.dto;

import lombok.Data;
import java.util.List;

@Data
public class RecipeListDTO {
    private Integer id;
    private String name;
    private String author;
    private String authorUid;
    private String image;
    private String difficulty;
    private String time;
    private String taste;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer replyCount;
    private List<String> categories;
    private List<String> ingredients;
    private List<String> sceneTags;
    private List<String> reasons;
}
