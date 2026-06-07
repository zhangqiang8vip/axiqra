package com.axiqra.api.handler;

import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.common.exception.BizException;
import com.axiqra.common.exception.ErrorCode;
import com.axiqra.common.exception.ParamException;
import com.axiqra.common.exception.SysException;
import com.axiqra.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局统一异常处理器
 * <p>
 * 职责：
 * 1. 捕获所有未处理异常，返回统一格式
 * 2. 业务异常（BizException）：记录 warn 日志，返回业务错误码
 * 3. 系统异常（SysException）：记录 error 日志（含堆栈），返回系统错误码
 * 4. 参数异常（ParamException/ConstraintViolationException）：返回参数错误码
 * 5. 其他未预期异常：记录 error 日志（含堆栈），返回 UNKNOWN_ERROR
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getTraceId() {
        return MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        log.warn("【业务异常】code={}, message={}, traceId={}",
                ex.getCode(), ex.getMessage(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(ex.getCode(), ex.getMessage(), getTraceId());
        Integer status = ex.httpStatus();
        return ResponseEntity.status(status != null ? status : HttpStatus.CONFLICT.value()).body(resp);
    }

    // ==================== 参数异常 ====================

    @ExceptionHandler(ParamException.class)
    public ResponseEntity<ApiResponse<Void>> handleParamException(ParamException ex) {
        log.warn("【参数异常】code={}, message={}, traceId={}",
                ex.getCode(), ex.getMessage(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(ex.getCode(), ex.getMessage(), getTraceId());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("【参数校验异常】message={}, traceId={}", message, getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                "参数校验失败: " + message,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("【请求体校验异常】message={}, traceId={}", message, getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_VALIDATION_FAILED.getCode(),
                "请求体校验失败: " + message,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("【绑定异常】message={}, traceId={}", message, getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_INVALID.getCode(),
                "参数绑定失败: " + message,
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("【缺少参数】param={}, traceId={}", ex.getParameterName(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_MISSING.getCode(),
                "缺少必需参数: " + ex.getParameterName(),
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("【参数类型不匹配】param={}, expected={}, traceId={}",
                ex.getName(), ex.getRequiredType(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_TYPE_MISMATCH.getCode(),
                "参数类型不匹配: " + ex.getName(),
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }

    // ==================== 系统异常 ====================

    @ExceptionHandler(SysException.class)
    public ResponseEntity<ApiResponse<Void>> handleSysException(SysException ex) {
        log.error("【系统异常】code={}, message={}, traceId={}",
                ex.getCode(), ex.getMessage(), getTraceId(), ex);
        ApiResponse<Void> resp = ApiResponse.fail(ex.getCode(), ex.getMessage(), getTraceId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("【请求方法不支持】method={}, traceId={}", ex.getMethod(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.PARAM_INVALID.getCode(),
                "不支持的请求方法: " + ex.getMethod(),
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(resp);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("【无对应处理器】path={}, traceId={}", ex.getRequestURL(), getTraceId());
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                "接口不存在: " + ex.getRequestURL(),
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    // ==================== Uncaught exceptions (fallback) ====================
    // Explicitly handle RuntimeException (unchecked) so unexpected issues are clear.
    // Error subclasses are rethrown — they should never be caught and serialized.
    // Anything else (checked exceptions, etc.) falls through to the Throwable handler.

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("【运行时异常】type={}, message={}, traceId={}",
                ex.getClass().getName(), ex.getMessage(), getTraceId(), ex);
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.UNKNOWN_ERROR.getCode(),
                "Internal server error. Please contact administrator.",
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<Void>> handleThrowable(Throwable ex) {
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        log.error("【未知异常】type={}, message={}, traceId={}",
                ex.getClass().getName(), ex.getMessage(), getTraceId(), ex);
        ApiResponse<Void> resp = ApiResponse.fail(
                ErrorCode.UNKNOWN_ERROR.getCode(),
                "Internal server error. Please contact administrator.",
                getTraceId()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}
