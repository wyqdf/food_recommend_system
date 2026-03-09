package com.foodrecommend.letmecook.config;

import com.foodrecommend.letmecook.service.AdminLogService;
import com.foodrecommend.letmecook.util.RequestClientInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOperationLogInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final Pattern TARGET_ID_PATTERN = Pattern.compile(".*/(\\d+)(/.*)?$");
    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token", "password", "pwd", "secret", "authorization", "auth", "credential");
    private static final int MAX_ACTION_LENGTH = 50;
    private static final int MAX_MODULE_LENGTH = 50;
    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private static final List<OperationRule> OPERATION_RULES = List.of(
            rule("POST", "^/api/admin/users$", "CREATE_USER", "用户管理", "创建用户", false),
            rule("PUT", "^/api/admin/users/\\d+$", "UPDATE_USER", "用户管理", "更新用户", true),
            rule("DELETE", "^/api/admin/users/\\d+$", "DELETE_USER", "用户管理", "删除用户", true),
            rule("DELETE", "^/api/admin/users/batch$", "BATCH_DELETE_USER", "用户管理", "批量删除用户", false),
            rule("PUT", "^/api/admin/users/\\d+/status$", "UPDATE_USER_STATUS", "用户管理", "更新用户状态", true),
            rule("PUT", "^/api/admin/users/\\d+/password$", "RESET_USER_PASSWORD", "用户管理", "重置用户密码", true),

            rule("POST", "^/api/admin/recipes$", "CREATE_RECIPE", "食谱管理", "创建食谱", false),
            rule("PUT", "^/api/admin/recipes/\\d+$", "UPDATE_RECIPE", "食谱管理", "更新食谱", true),
            rule("DELETE", "^/api/admin/recipes/\\d+$", "DELETE_RECIPE", "食谱管理", "删除食谱", true),
            rule("DELETE", "^/api/admin/recipes/batch$", "BATCH_DELETE_RECIPE", "食谱管理", "批量删除食谱", false),
            rule("PUT", "^/api/admin/recipes/\\d+/audit$", "AUDIT_RECIPE", "食谱管理", "审核食谱", true),

            rule("POST", "^/api/admin/categories$", "CREATE_CATEGORY", "分类管理", "创建分类", false),
            rule("PUT", "^/api/admin/categories/\\d+$", "UPDATE_CATEGORY", "分类管理", "更新分类", true),
            rule("DELETE", "^/api/admin/categories/\\d+$", "DELETE_CATEGORY", "分类管理", "删除分类", true),

            rule("POST", "^/api/admin/(tastes|techniques|time-costs|difficulties|ingredients|cookwares)$",
                    "CREATE_ATTRIBUTE", "属性管理", "创建属性", false),
            rule("PUT", "^/api/admin/(tastes|techniques|time-costs|difficulties|ingredients|cookwares)/\\d+$",
                    "UPDATE_ATTRIBUTE", "属性管理", "更新属性", true),
            rule("DELETE", "^/api/admin/(tastes|techniques|time-costs|difficulties|ingredients|cookwares)/\\d+$",
                    "DELETE_ATTRIBUTE", "属性管理", "删除属性", true),

            rule("POST", "^/api/admin/logout$", "LOGOUT", "认证管理", "管理员退出登录", false),
            rule("PUT", "^/api/admin/password$", "UPDATE_ADMIN_PASSWORD", "认证管理", "修改管理员密码", false),

            rule("DELETE", "^/api/admin/logs/\\d+$", "DELETE_LOG", "日志管理", "删除日志", true),
            rule("DELETE", "^/api/admin/logs/batch$", "BATCH_DELETE_LOG", "日志管理", "批量删除日志", false),
            rule("DELETE", "^/api/admin/logs/cleanup$", "CLEANUP_LOG", "日志管理", "清理历史日志", false),

            rule("POST", "^/api/admin/statistics/refresh$", "REFRESH_STATISTICS", "统计管理", "刷新统计数据", false)
    );

    private final AdminLogService adminLogService;

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        String uri = normalizeUri(request.getRequestURI(), request.getContextPath());
        if (!uri.startsWith("/api/admin/")) {
            return;
        }
        if ("/api/admin/login".equals(uri)) {
            return;
        }
        if (!WRITE_METHODS.contains(request.getMethod().toUpperCase())) {
            return;
        }

        Integer adminId = (Integer) request.getAttribute("adminId");
        if (adminId == null) {
            return;
        }

        String method = request.getMethod().toUpperCase();
        OperationRule operationRule = resolveOperationRule(method, uri);
        String operation = limitLength(resolveOperation(method, operationRule), MAX_ACTION_LENGTH, "UNKNOWN");
        String module = limitLength(resolveModule(uri, operationRule), MAX_MODULE_LENGTH, "系统管理");
        Integer targetId = extractTargetId(uri);
        String content = limitLength(
                buildContent(request, response, operation, module, uri, targetId, ex, operationRule),
                MAX_CONTENT_LENGTH,
                "");
        String ip = limitLength(RequestClientInfoUtil.extractClientIp(request), MAX_IP_LENGTH, null);
        String userAgent = limitLength(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH, null);

        try {
            adminLogService.recordLog(
                    adminId,
                    operation,
                    module,
                    content,
                    targetId,
                    ip,
                    userAgent);
        } catch (Exception logEx) {
            log.warn("记录管理员操作日志失败: {}", logEx.getMessage());
        }
    }

    private String resolveOperation(String method, OperationRule operationRule) {
        if (operationRule != null) {
            return operationRule.operation();
        }
        return method + "_OTHER";
    }

    private String resolveModule(String uri, OperationRule operationRule) {
        if (operationRule != null) {
            return operationRule.module();
        }
        return resolveModuleByPath(uri);
    }

    private String resolveModuleByPath(String uri) {
        if (uri.startsWith("/api/admin/users")) return "用户管理";
        if (uri.startsWith("/api/admin/recipes")) return "食谱管理";
        if (uri.startsWith("/api/admin/categories")) return "分类管理";
        if (uri.startsWith("/api/admin/logs")) return "日志管理";
        if (uri.startsWith("/api/admin/statistics")) return "统计管理";
        if (uri.startsWith("/api/admin/tastes")
                || uri.startsWith("/api/admin/techniques")
                || uri.startsWith("/api/admin/time-costs")
                || uri.startsWith("/api/admin/difficulties")
                || uri.startsWith("/api/admin/ingredients")
                || uri.startsWith("/api/admin/cookwares")) {
            return "属性管理";
        }
        if (uri.startsWith("/api/admin/password") || uri.startsWith("/api/admin/logout")) return "认证管理";
        return "系统管理";
    }

    private OperationRule resolveOperationRule(String method, String uri) {
        for (OperationRule rule : OPERATION_RULES) {
            if (rule.matches(method, uri)) {
                return rule;
            }
        }
        return null;
    }

    private String normalizeUri(String uri, String contextPath) {
        if (uri == null || uri.isBlank()) {
            return "";
        }
        if (contextPath == null || contextPath.isBlank()) {
            return uri;
        }
        if (!uri.startsWith(contextPath)) {
            return uri;
        }
        String normalized = uri.substring(contextPath.length());
        return normalized.isBlank() ? "/" : normalized;
    }

    private Integer extractTargetId(String uri) {
        var matcher = TARGET_ID_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildContent(HttpServletRequest request,
                                HttpServletResponse response,
                                String operation,
                                String module,
                                String uri,
                                Integer targetId,
                                Exception ex,
                                OperationRule operationRule) {
        StringBuilder content = new StringBuilder();
        String actionDescription = resolveActionDescription(operationRule);
        content.append(actionDescription);
        if (operationRule != null && operationRule.includeTargetId() && targetId != null) {
            content.append(" (ID=").append(targetId).append(")");
        }

        content.append(" | 模块=").append(module)
                .append(" | 操作=").append(operation)
                .append(" | 路径=").append(uri);

        String sanitizedQuery = sanitizeQueryString(request.getQueryString());
        if (sanitizedQuery != null && !sanitizedQuery.isBlank()) {
            content.append(" | 参数=").append(sanitizedQuery);
        }

        content.append(" | HTTP ").append(response.getStatus());
        if (ex != null) {
            content.append(" | 异常=").append(ex.getClass().getSimpleName());
        }
        return content.toString();
    }

    private String resolveActionDescription(OperationRule operationRule) {
        if (operationRule == null || operationRule.description() == null || operationRule.description().isBlank()) {
            return "执行后台写操作";
        }
        return operationRule.description();
    }

    private String sanitizeQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return null;
        }
        return Arrays.stream(queryString.split("&"))
                .map(this::sanitizeQueryParam)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining("&"));
    }

    private String sanitizeQueryParam(String pair) {
        if (pair == null || pair.isBlank()) {
            return "";
        }

        int splitIndex = pair.indexOf('=');
        if (splitIndex < 0) {
            return pair;
        }

        String key = pair.substring(0, splitIndex);
        String value = pair.substring(splitIndex + 1);
        if (isSensitiveQueryKey(key)) {
            return key + "=***";
        }
        return key + "=" + limitLength(value, 120, "");
    }

    private boolean isSensitiveQueryKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim().toLowerCase();
        for (String sensitiveKey : SENSITIVE_QUERY_KEYS) {
            if (normalized.contains(sensitiveKey)) {
                return true;
            }
        }
        return false;
    }

    private String limitLength(String value, int maxLen, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }

    private static OperationRule rule(String method, String uriRegex, String operation,
                                      String module, String description, boolean includeTargetId) {
        return new OperationRule(method, Pattern.compile(uriRegex), operation, module, description, includeTargetId);
    }

    private record OperationRule(String method,
                                 Pattern uriPattern,
                                 String operation,
                                 String module,
                                 String description,
                                 boolean includeTargetId) {
        private boolean matches(String requestMethod, String requestUri) {
            return method.equalsIgnoreCase(requestMethod) && uriPattern.matcher(requestUri).matches();
        }
    }
}
