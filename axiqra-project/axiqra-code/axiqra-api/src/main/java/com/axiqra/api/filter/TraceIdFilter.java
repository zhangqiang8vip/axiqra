package com.axiqra.api.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器
 * <p>
 * 职责：
 * 1. 每个请求生成唯一 traceId（UUID，不含横杠）
 * 2. 前端传入则复用，支持分布式追踪
 * 3. 将 traceId 放入 MDC，所有日志自动携带
 * 4. 将 traceId 返回给前端（响应头 X-Trace-Id）
 * 5. 请求结束时清理 MDC，防止内存泄漏
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 生成或复用 traceId
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // 2. 放入 MDC
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        // 3. 返回给前端
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        log.info("【请求入口】method={}, uri={}, traceId={}",
                httpRequest.getMethod(), httpRequest.getRequestURI(), traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            // 4. 请求结束后清理 MDC
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
