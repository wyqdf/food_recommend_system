package com.foodrecommend.letmecook.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchReindexStatusDTO {
    private String engine;
    private String indexAlias;
    private String indexName;
    private String currentIndex;
    private String targetIndex;
    private String phase;
    private boolean running;
    private long total;
    private long processed;
    private long success;
    private long failed;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String lastError;
}
