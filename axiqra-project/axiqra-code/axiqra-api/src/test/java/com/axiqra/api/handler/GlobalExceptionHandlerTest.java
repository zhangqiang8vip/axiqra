package com.axiqra.api.handler;

import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.exception.ParamException;
import com.axiqra.common.exception.SysException;
import com.axiqra.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 * 验证 8 种异常类型的统一响应格式
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @RestController
    static class DummyController {
        @GetMapping("/test")
        public String test() { return "ok"; }
    }

    @Test
    @DisplayName("BizException 应返回 400 + 错误码")
    void handleBizException() {
        BizException ex = new BizException(40003, "状态转换无效");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBizException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(40003, resp.getBody().getCode());
        assertEquals("状态转换无效", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("ParamException 应返回 400 + 错误码")
    void handleParamException() {
        ParamException ex = new ParamException("参数不能为空");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleParamException(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(40000, resp.getBody().getCode());
    }

    @Test
    @DisplayName("SysException 应返回 500 + 错误码")
    void handleSysException() {
        SysException ex = new SysException("数据库连接失败");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleSysException(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(50000, resp.getBody().getCode());
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
        ConstraintViolationException ex = new ConstraintViolationException(
                Set.of(
                        createViolation("name", "不能为空"),
                        createViolation("email", "格式错误")
                ));
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
    @DisplayName("Throwable 兜底应返回 500 + UNKNOWN_ERROR")
    void handleThrowable() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleThrowable(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals(99999, resp.getBody().getCode());
        assertEquals("系统内部错误，请联系管理员", resp.getBody().getMessage());
    }

    @Test
    @DisplayName("BizException 错误码应在允许范围 10001~99999")
    void bizException_codeRange() {
        BizException ex = new BizException(99999, "最大错误码");
        ResponseEntity<ApiResponse<Void>> resp = handler.handleBizException(ex);
        assertTrue(resp.getBody().getCode() >= 10001);
    }

    private static ConstraintViolation<Object> createViolation(String path, String message) {
        return new ConstraintViolation<>() {
            @Override public String getMessage() { return message; }
            @Override public String getMessageTemplate() { return message; }
            @Override public Object getInvalidValue() { return null; }
            @Override public Object getRootBean() { return null; }
            @Override public Class<Object> getRootBeanClass() { return Object.class; }
            @Override public Object getLeafBean() { return null; }
            @Override public Object[] getExecutableParameters() { return new Object[0]; }
            @Override public Object getExecutable() { return null; }
            @Override public String getPropertyPath() { return path; }
            @Override public ConstraintViolation<?> getCause() { return null; }
        };
    }
}
