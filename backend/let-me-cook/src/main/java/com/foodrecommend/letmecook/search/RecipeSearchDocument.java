package com.foodrecommend.letmecook.search;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(indexName = "recipes_search")
public class RecipeSearchDocument {

    @Id
    private String id;
    private Integer recipeId;
    private String title;
    private String author;
    private String authorUid;
    private List<String> categories;
    private List<String> ingredients;
    private String tasteName;
    private String techniqueName;
    private String timeCostName;
    private String difficultyName;
    private String searchText;
    private List<String> titleSuggestInputs;
    private List<String> ingredientSuggestInputs;
    private List<String> categorySuggestInputs;
    private List<String> authorSuggestInputs;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
