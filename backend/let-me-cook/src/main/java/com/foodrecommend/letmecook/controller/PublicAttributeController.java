package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.entity.Cookware;
import com.foodrecommend.letmecook.entity.Difficulty;
import com.foodrecommend.letmecook.entity.Ingredient;
import com.foodrecommend.letmecook.entity.Taste;
import com.foodrecommend.letmecook.entity.Technique;
import com.foodrecommend.letmecook.entity.TimeCost;
import com.foodrecommend.letmecook.mapper.CookwareMapper;
import com.foodrecommend.letmecook.mapper.DifficultyMapper;
import com.foodrecommend.letmecook.mapper.IngredientMapper;
import com.foodrecommend.letmecook.mapper.TasteMapper;
import com.foodrecommend.letmecook.mapper.TechniqueMapper;
import com.foodrecommend.letmecook.mapper.TimeCostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicAttributeController {

    private final TasteMapper tasteMapper;
    private final TechniqueMapper techniqueMapper;
    private final TimeCostMapper timeCostMapper;
    private final DifficultyMapper difficultyMapper;
    private final IngredientMapper ingredientMapper;
    private final CookwareMapper cookwareMapper;

    @GetMapping("/tastes")
    public Result<List<Taste>> getTastes() {
        return Result.success(tasteMapper.findAll());
    }

    @GetMapping("/techniques")
    public Result<List<Technique>> getTechniques() {
        return Result.success(techniqueMapper.findAll());
    }

    @GetMapping("/time-costs")
    public Result<List<TimeCost>> getTimeCosts() {
        return Result.success(timeCostMapper.findAll());
    }

    @GetMapping("/difficulties")
    public Result<List<Difficulty>> getDifficulties() {
        return Result.success(difficultyMapper.findAll());
    }

    @GetMapping("/ingredients")
    public Result<List<Ingredient>> getIngredients() {
        return Result.success(ingredientMapper.findAll());
    }

    @GetMapping("/cookwares")
    public Result<List<Cookware>> getCookwares() {
        return Result.success(cookwareMapper.findAll());
    }
}
