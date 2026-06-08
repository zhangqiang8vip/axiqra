package com.axiqra.api.handler;

import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.exception.ParamException;
import com.axiqra.common.exception.SysException;
import com.axiqra.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 单元测试
 * 验证 8 种异常类型的统一响应格式
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BizException should return httpStatus from ErrorCode, default 409 if null")
    void handleBizException() {
        BizException ex = new BizException(ErrorCode.STATUS_TRANSITION_INVALID);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBizException(ex);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getCode(), resp.getBody().getCode());
        assertEquals(ErrorCode.STATUS_TRANSITION_INVALID.getMessage(), resp.getBody().getMessage());
    }

    @Test
    @DisplayName("ParamException 应返回 400 + 错误码")
    void handleParamException() {
        ParamException ex = new ParamException("参数不能为空");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleParamException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), resp.getBody().getCode());
    }

    @Test
    @DisplayName("SysException 应返回 500 + 错误码")
    void handleSysException() {
        SysException ex = new SysException("数据库连接失败");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleSysException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), resp.getBody().getCode());
        assertEquals("数据库连接失败", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("SysException 带错误码应返回指定码")
    void handleSysException_withCode() {
        SysException ex = new SysException(50003, "缓存故障");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleSysException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(50003, resp.getBody().getCode());
    }

    @Test
    @DisplayName("ConstraintViolationException 应返回参数校验错误码")
    void handleConstraintViolation() {
        ConstraintViolation<Object> v1 = mock(ConstraintViolation.class);
        when(v1.getMessage()).thenReturn("不能为空");
        when(v1.getPropertyPath()).thenReturn(mock(Path.class));

        ConstraintViolation<Object> v2 = mock(ConstraintViolation.class);
        when(v2.getMessage()).thenReturn("格式错误");
        when(v2.getPropertyPath()).thenReturn(mock(Path.class));

        ConstraintViolationException ex = new ConstraintViolationException(
                Set.of(v1, v2));

        ResponseEntity<ApiResponse<Void>> resp = handler.handleConstraintViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(ErrorCode.PARAM_VALIDATION_FAILED.getCode(), resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("参数校验失败"));
    }

    @Test
    @DisplayName("MissingServletRequestParameterException 应返回缺少参数错误码")
    void handleMissingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("userId", "Long");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMissingParam(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(ErrorCode.PARAM_MISSING.getCode(), resp.getBody().getCode());
        assertTrue(resp.getBody().getMessage().contains("userId"));
    }

    @Test
    @DisplayName("NoHandlerFoundException 应返回 404")
    void handleNoHandlerFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException(
                "GET", "/api/nonexistent", null);
        ResponseEntity<ApiResponse<Void>> resp = handler.handleNoHandlerFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), resp.getBody().getCode());
    }

    @Test
    @DisplayName("Throwable fallback should return 500 + UNKNOWN_ERROR")
    void handleThrowable() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleThrowable(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(99999, resp.getBody().getCode());
        assertEquals("Internal server error. Please contact administrator.", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("BizException httpStatus comes from ErrorCode; null httpStatus falls back to 409")
    void bizException_httpStatus() {
        assertEquals(HttpStatus.NOT_FOUND.value(), ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus());
        assertEquals(HttpStatus.CONFLICT.value(), ErrorCode.CONCURRENT_MODIFICATION.getHttpStatus());
    }
}
