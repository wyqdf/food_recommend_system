package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AttributeController {

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

    @PostMapping("/tastes")
    public Result<Map<String, Object>> createTaste(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Taste existing = tasteMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        Taste taste = new Taste();
        taste.setName(name);
        tasteMapper.insert(taste);

        Map<String, Object> data = new HashMap<>();
        data.put("id", taste.getId());
        data.put("name", taste.getName());
        data.put("recipeCount", 0);
        data.put("createTime", taste.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/tastes/{id}")
    public Result<Void> updateTaste(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Taste existing = tasteMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        Taste duplicate = tasteMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        Taste taste = new Taste();
        taste.setId(id);
        taste.setName(name);
        tasteMapper.update(taste);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/tastes/{id}")
    public Result<Void> deleteTaste(@PathVariable Integer id) {
        Taste taste = tasteMapper.findById(id);
        if (taste == null) {
            return Result.error(404, "属性不存在");
        }

        if (taste.getRecipeCount() != null && taste.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + taste.getRecipeCount() + " 个食谱使用");
        }

        tasteMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/techniques")
    public Result<List<Technique>> getTechniques() {
        return Result.success(techniqueMapper.findAll());
    }

    @PostMapping("/techniques")
    public Result<Map<String, Object>> createTechnique(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Technique existing = techniqueMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        Technique technique = new Technique();
        technique.setName(name);
        techniqueMapper.insert(technique);

        Map<String, Object> data = new HashMap<>();
        data.put("id", technique.getId());
        data.put("name", technique.getName());
        data.put("recipeCount", 0);
        data.put("createTime", technique.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/techniques/{id}")
    public Result<Void> updateTechnique(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Technique existing = techniqueMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        Technique duplicate = techniqueMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        Technique technique = new Technique();
        technique.setId(id);
        technique.setName(name);
        techniqueMapper.update(technique);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/techniques/{id}")
    public Result<Void> deleteTechnique(@PathVariable Integer id) {
        Technique technique = techniqueMapper.findById(id);
        if (technique == null) {
            return Result.error(404, "属性不存在");
        }

        if (technique.getRecipeCount() != null && technique.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + technique.getRecipeCount() + " 个食谱使用");
        }

        techniqueMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/time-costs")
    public Result<List<TimeCost>> getTimeCosts() {
        return Result.success(timeCostMapper.findAll());
    }

    @PostMapping("/time-costs")
    public Result<Map<String, Object>> createTimeCost(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        TimeCost existing = timeCostMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        TimeCost timeCost = new TimeCost();
        timeCost.setName(name);
        timeCostMapper.insert(timeCost);

        Map<String, Object> data = new HashMap<>();
        data.put("id", timeCost.getId());
        data.put("name", timeCost.getName());
        data.put("recipeCount", 0);
        data.put("createTime", timeCost.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/time-costs/{id}")
    public Result<Void> updateTimeCost(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        TimeCost existing = timeCostMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        TimeCost duplicate = timeCostMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        TimeCost timeCost = new TimeCost();
        timeCost.setId(id);
        timeCost.setName(name);
        timeCostMapper.update(timeCost);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/time-costs/{id}")
    public Result<Void> deleteTimeCost(@PathVariable Integer id) {
        TimeCost timeCost = timeCostMapper.findById(id);
        if (timeCost == null) {
            return Result.error(404, "属性不存在");
        }

        if (timeCost.getRecipeCount() != null && timeCost.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + timeCost.getRecipeCount() + " 个食谱使用");
        }

        timeCostMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/difficulties")
    public Result<List<Difficulty>> getDifficulties() {
        return Result.success(difficultyMapper.findAll());
    }

    @PostMapping("/difficulties")
    public Result<Map<String, Object>> createDifficulty(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Difficulty existing = difficultyMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        Difficulty difficulty = new Difficulty();
        difficulty.setName(name);
        difficultyMapper.insert(difficulty);

        Map<String, Object> data = new HashMap<>();
        data.put("id", difficulty.getId());
        data.put("name", difficulty.getName());
        data.put("recipeCount", 0);
        data.put("createTime", difficulty.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/difficulties/{id}")
    public Result<Void> updateDifficulty(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Difficulty existing = difficultyMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        Difficulty duplicate = difficultyMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        Difficulty difficulty = new Difficulty();
        difficulty.setId(id);
        difficulty.setName(name);
        difficultyMapper.update(difficulty);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/difficulties/{id}")
    public Result<Void> deleteDifficulty(@PathVariable Integer id) {
        Difficulty difficulty = difficultyMapper.findById(id);
        if (difficulty == null) {
            return Result.error(404, "属性不存在");
        }

        if (difficulty.getRecipeCount() != null && difficulty.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + difficulty.getRecipeCount() + " 个食谱使用");
        }

        difficultyMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/ingredients")
    public Result<List<Ingredient>> getIngredients() {
        return Result.success(ingredientMapper.findAll());
    }

    @PostMapping("/ingredients")
    public Result<Map<String, Object>> createIngredient(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Ingredient existing = ingredientMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredientMapper.insert(ingredient);

        Map<String, Object> data = new HashMap<>();
        data.put("id", ingredient.getId());
        data.put("name", ingredient.getName());
        data.put("recipeCount", 0);
        data.put("createTime", ingredient.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/ingredients/{id}")
    public Result<Void> updateIngredient(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Ingredient existing = ingredientMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        Ingredient duplicate = ingredientMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredientMapper.update(ingredient);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/ingredients/{id}")
    public Result<Void> deleteIngredient(@PathVariable Integer id) {
        Ingredient ingredient = ingredientMapper.findById(id);
        if (ingredient == null) {
            return Result.error(404, "属性不存在");
        }

        if (ingredient.getRecipeCount() != null && ingredient.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + ingredient.getRecipeCount() + " 个食谱使用");
        }

        ingredientMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }

    @GetMapping("/cookwares")
    public Result<List<Cookware>> getCookwares() {
        return Result.success(cookwareMapper.findAll());
    }

    @PostMapping("/cookwares")
    public Result<Map<String, Object>> createCookware(@RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Cookware existing = cookwareMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该名称已存在");
        }

        Cookware cookware = new Cookware();
        cookware.setName(name);
        cookwareMapper.insert(cookware);

        Map<String, Object> data = new HashMap<>();
        data.put("id", cookware.getId());
        data.put("name", cookware.getName());
        data.put("recipeCount", 0);
        data.put("createTime", cookware.getCreateTime());

        return Result.success(data, "创建成功");
    }

    @PutMapping("/cookwares/{id}")
    public Result<Void> updateCookware(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");

        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }

        Cookware existing = cookwareMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "属性不存在");
        }

        Cookware duplicate = cookwareMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该名称已存在");
        }

        Cookware cookware = new Cookware();
        cookware.setId(id);
        cookware.setName(name);
        cookwareMapper.update(cookware);

        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/cookwares/{id}")
    public Result<Void> deleteCookware(@PathVariable Integer id) {
        Cookware cookware = cookwareMapper.findById(id);
        if (cookware == null) {
            return Result.error(404, "属性不存在");
        }

        if (cookware.getRecipeCount() != null && cookware.getRecipeCount() > 0) {
            return Result.error(400, "无法删除：该属性已被 " + cookware.getRecipeCount() + " 个食谱使用");
        }

        cookwareMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }
}
