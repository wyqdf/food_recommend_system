package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.entity.SystemLog;
import com.foodrecommend.letmecook.mapper.SystemLogMapper;
import com.foodrecommend.letmecook.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemLogMapper systemLogMapper;

    @Override
    public PageResult<SystemLog> getLogs(int page, int pageSize, Integer adminId, String adminName,
                                         String module, String operation, String startTime, String endTime) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        int offset = (safePage - 1) * safePageSize;

        List<SystemLog> logs = systemLogMapper.findByCondition(
                adminId, adminName, module, operation, normalizeDateTime(startTime), normalizeDateTime(endTime), offset, safePageSize);
        long total = systemLogMapper.countByCondition(
                adminId, adminName, module, operation, normalizeDateTime(startTime), normalizeDateTime(endTime));

        return new PageResult<>(logs, total, safePage, safePageSize);
    }

    @Override
    public SystemLog getLogById(Integer id) {
        return systemLogMapper.findById(id);
    }

    @Override
    public void deleteLogById(Integer id) {
        systemLogMapper.deleteById(id);
    }

    @Override
    @Transactional
    public int batchDeleteLogs(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<Integer> validIds = Arrays.stream(ids)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return 0;
        }
        return systemLogMapper.batchDeleteByIds(validIds);
    }

    @Override
    public int cleanupLogs(int beforeDays) {
        int safeDays = Math.max(beforeDays, 1);
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(safeDays);
        return systemLogMapper.deleteBeforeTime(beforeTime.format(DATE_TIME_FORMATTER));
    }

    @Override
    public List<String> getModules() {
        return systemLogMapper.findDistinctModules();
    }

    @Override
    public List<String> getOperations() {
        return systemLogMapper.findDistinctOperations();
    }

    @Override
    public void recordLog(Integer adminId, String operation, String module, String content, Integer targetId,
                          String ip, String userAgent) {
        if (adminId == null || operation == null || operation.isBlank()) {
            return;
        }
        SystemLog log = new SystemLog();
        log.setAdminId(adminId);
        log.setOperation(operation);
        log.setModule(module);
        log.setContent(content);
        log.setTargetId(targetId);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        systemLogMapper.insert(log);
    }

    private String normalizeDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return dateTime.trim().replace("T", " ");
    }
}
