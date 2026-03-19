package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.SearchReindexStatusDTO;
import com.foodrecommend.letmecook.search.RecipeSearchReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/search/index")
@RequiredArgsConstructor
public class AdminSearchController {

    private final RecipeSearchReindexService recipeSearchReindexService;

    @PostMapping("/rebuild")
    public Result<String> rebuildSearchIndex() {
        return Result.success(recipeSearchReindexService.startRebuild());
    }

    @GetMapping("/status")
    public Result<SearchReindexStatusDTO> getSearchIndexStatus() {
        return Result.success(recipeSearchReindexService.getStatus());
    }
}
