package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.SceneTagDTO;
import com.foodrecommend.letmecook.util.SceneTagResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenes")
public class SceneController {

    @GetMapping
    public Result<List<SceneTagDTO>> listScenes() {
        return Result.success(SceneTagResolver.catalog());
    }
}
