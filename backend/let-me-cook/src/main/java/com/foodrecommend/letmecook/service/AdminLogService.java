package com.foodrecommend.letmecook.service;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.entity.SystemLog;

import java.util.List;

public interface AdminLogService {

    PageResult<SystemLog> getLogs(int page, int pageSize, Integer adminId, String adminName,
                                  String module, String operation, String startTime, String endTime);

    SystemLog getLogById(Integer id);

    void deleteLogById(Integer id);

    int batchDeleteLogs(Integer[] ids);

    int cleanupLogs(int beforeDays);

    List<String> getModules();

    List<String> getOperations();

    void recordLog(Integer adminId, String operation, String module, String content, Integer targetId,
                   String ip, String userAgent);
}
