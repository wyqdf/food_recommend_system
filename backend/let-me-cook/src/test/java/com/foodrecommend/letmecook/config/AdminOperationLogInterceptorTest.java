package com.foodrecommend.letmecook.config;

import com.foodrecommend.letmecook.service.AdminLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogInterceptorTest {

    @Mock
    private AdminLogService adminLogService;

    private AdminOperationLogInterceptor interceptor;
    private HandlerMethod handlerMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        interceptor = new AdminOperationLogInterceptor(adminLogService);
        handlerMethod = new HandlerMethod(new DummyController(), DummyController.class.getMethod("handle"));
    }

    @Test
    void shouldRecordWriteOperationLog() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/admin/users/123");
        request.setAttribute("adminId", 1);
        request.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handlerMethod, null);

        ArgumentCaptor<String> operationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> moduleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> targetIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        verify(adminLogService).recordLog(
                eq(1),
                operationCaptor.capture(),
                moduleCaptor.capture(),
                contentCaptor.capture(),
                targetIdCaptor.capture(),
                anyString(),
                eq("JUnit"));

        assertEquals("DELETE_USER", operationCaptor.getValue());
        assertEquals("用户管理", moduleCaptor.getValue());
        assertEquals(123, targetIdCaptor.getValue());
        assertTrue(contentCaptor.getValue().contains("删除用户"));
        assertTrue(contentCaptor.getValue().contains("ID=123"));
        assertTrue(contentCaptor.getValue().contains("HTTP 200"));
    }

    @Test
    void shouldSkipReadOnlyRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setAttribute("adminId", 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, handlerMethod, null);

        verify(adminLogService, never()).recordLog(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRedactSensitiveQueryParams() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/admin/logs/cleanup");
        request.setAttribute("adminId", 1);
        request.setQueryString("beforeDays=30&token=abc123&password=secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handlerMethod, null);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminLogService).recordLog(
                eq(1),
                eq("CLEANUP_LOG"),
                eq("日志管理"),
                contentCaptor.capture(),
                isNull(),
                anyString(),
                any());

        String content = contentCaptor.getValue();
        assertTrue(content.contains("beforeDays=30"));
        assertTrue(content.contains("token=***"));
        assertTrue(content.contains("password=***"));
        assertFalse(content.contains("abc123"));
        assertFalse(content.contains("secret"));
    }

    @Test
    void shouldTruncateLongUserAgent() {
        String longUserAgent = "a".repeat(600);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/admin/users/123");
        request.setAttribute("adminId", 1);
        request.addHeader("User-Agent", longUserAgent);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handlerMethod, null);

        ArgumentCaptor<String> userAgentCaptor = ArgumentCaptor.forClass(String.class);
        verify(adminLogService).recordLog(
                eq(1),
                eq("DELETE_USER"),
                eq("用户管理"),
                anyString(),
                eq(123),
                anyString(),
                userAgentCaptor.capture());

        assertEquals(512, userAgentCaptor.getValue().length());
    }

    @Test
    void shouldResolveOperationWhenContextPathExists() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/app/api/admin/users/123");
        request.setContextPath("/app");
        request.setRequestURI("/app/api/admin/users/123");
        request.setAttribute("adminId", 1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handlerMethod, null);

        verify(adminLogService).recordLog(
                eq(1),
                eq("DELETE_USER"),
                eq("用户管理"),
                anyString(),
                eq(123),
                anyString(),
                any());
    }

    private static class DummyController {
        public void handle() {
            // no-op
        }
    }
}
