package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.service.OssUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssUploadService ossUploadService;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        String imageUrl = ossUploadService.uploadImage(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", imageUrl);
        return Result.success(data);
    }

    @PostMapping("/recipe-image")
    public Result<Map<String, String>> uploadRecipeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("recipeId") Long recipeId) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        String imageUrl = ossUploadService.uploadRecipeImage(file, recipeId);
        Map<String, String> data = new HashMap<>();
        data.put("url", imageUrl);
        return Result.success(data);
    }

    @DeleteMapping("/image")
    public Result<Void> deleteImage(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("url");
        ossUploadService.deleteImage(imageUrl);
        return Result.success(null);
    }
}
