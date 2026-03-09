package com.foodrecommend.letmecook.controller;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.ResponseDataBuilder;
import com.foodrecommend.letmecook.common.Result;
import com.foodrecommend.letmecook.dto.admin.BatchDeleteRequest;
import com.foodrecommend.letmecook.entity.SystemLog;
import com.foodrecommend.letmecook.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer adminId,
            @RequestParam(required = false) String adminName,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        PageResult<SystemLog> result = adminLogService.getLogs(
                page, pageSize, adminId, adminName, module, operation, startTime, endTime);

        return Result.success(ResponseDataBuilder.page(result));
    }

    @GetMapping("/{id}")
    public Result<SystemLog> getLogById(@PathVariable Integer id) {
        SystemLog log = adminLogService.getLogById(id);
        if (log == null) {
            return Result.error(404, "日志不存在");
        }
        return Result.success(log);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        adminLogService.deleteLogById(id);
        return Result.success(null, "删除成功");
    }

    @DeleteMapping("/batch")
    public Result<Map<String, Integer>> batchDelete(@RequestBody BatchDeleteRequest request) {
        Integer[] ids = request == null ? null : request.getIds();
        int deleted = adminLogService.batchDeleteLogs(ids);
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("deletedCount", deleted);
        return Result.success(data, "批量删除成功");
    }

    @DeleteMapping("/cleanup")
    public Result<Map<String, Integer>> cleanup(@RequestParam(defaultValue = "30") int beforeDays) {
        int deleted = adminLogService.cleanupLogs(beforeDays);
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("deletedCount", deleted);
        return Result.success(data, "清理成功");
    }

    @GetMapping("/meta")
    public Result<Map<String, Object>> getMeta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modules", adminLogService.getModules());
        data.put("operations", adminLogService.getOperations());
        return Result.success(data);
    }
}
