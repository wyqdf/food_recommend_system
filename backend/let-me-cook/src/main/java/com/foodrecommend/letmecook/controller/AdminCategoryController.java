package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.entity.Category;
import com.foodrecommend.letmecook.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryMapper categoryMapper;

    @GetMapping
    public Result<List<Category>> getList() {
        List<Category> categories = categoryMapper.findAllWithCount();
        return Result.success(categories);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }
        
        Category existing = categoryMapper.findByName(name);
        if (existing != null) {
            return Result.error(400, "该分类名称已存在");
        }
        
        Category category = new Category();
        category.setName(name);
        categoryMapper.insert(category);
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", category.getId());
        data.put("name", category.getName());
        data.put("createTime", category.getCreateTime());
        
        return Result.success(data, "创建成功");
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "名称不能为空");
        }
        
        Category existing = categoryMapper.findById(id);
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        
        Category duplicate = categoryMapper.findByName(name);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            return Result.error(400, "该分类名称已存在");
        }
        
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        categoryMapper.update(category);
        
        return Result.success(null, "更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            return Result.error(404, "分类不存在");
        }
        
        int recipeCount = categoryMapper.countRecipesByCategory(id);
        if (recipeCount > 0) {
            return Result.error(400, "无法删除：该分类下还有 " + recipeCount + " 个食谱");
        }
        
        categoryMapper.deleteById(id);
        return Result.success(null, "删除成功");
    }
}
