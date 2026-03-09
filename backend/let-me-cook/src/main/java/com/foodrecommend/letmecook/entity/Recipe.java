package com.foodrecommend.letmecook.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Recipe {
    private Integer id;
    private Integer oldId;
    private String title;
    private String author;
    private String authorUid;
    private String description;
    private String tips;
    private String cookware;
    private String image;
    private Integer tasteId;
    private Integer techniqueId;
    private Integer timeCostId;
    private Integer difficultyId;
    private Integer replyCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer ratingCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String tasteName;
    private String techniqueName;
    private String timeCostName;
    private String difficultyName;
}
