package com.axiqra.api.advice;

import com.axiqra.api.filter.TraceIdFilter;
import com.axiqra.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 将 traceId/requestId 注入所有 ApiResponse 响应体。
 *
 * <p>执行顺序：
 * 1. TraceIdFilter 生成 traceId 并存入 MDC
 * 2. Controller 返回 ApiResponse
 * 3. 本 Advice 从 MDC 取出 traceId，写入 ApiResponse.requestId
 * 4. 响应序列化时 requestId 已在 body 中
 *
 * @author Axiqra Team
 * @date 2026-06-08
 */
@Slf4j
@ControllerAdvice
public class TraceIdResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @SuppressWarnings("unused")
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId != null && body instanceof ApiResponse<?> apiResponse) {
            apiResponse.setRequestId(traceId);
        }
        return body;
    }
}
