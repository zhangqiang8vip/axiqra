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
import java.util.regex.Pattern;

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

    /** 匹配路径段中的敏感值：纯数字 ID、UUID、长十六进制串、token 串 */
    private static final Pattern SENSITIVE_PATH_SEGMENT = Pattern.compile(
            "(?<![\\w/])[0-9]{6,}(?![\\w])|"        // 6位以上纯数字
                    + "(?<![\\w-])[0-9a-f]{8}[-]?[0-9a-f]{4}[-]?[0-9a-f]{4}[-]?[0-9a-f]{4}[-]?[0-9a-f]{12}(?![\\w-])|" // UUID
                    + "(?<![\\w/])[0-9a-f]{32,}(?![\\w])|" // 长十六进制
                    + "(?<![\\w=])[A-Za-z0-9]{40,}(?![\\w=])" // 长 token-like 串
    );

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

        String sanitizedUri = sanitizeUri(httpRequest.getRequestURI());
        log.info("【请求入口】method={}, uri={}, traceId={}",
                httpRequest.getMethod(), sanitizedUri, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            // 4. 请求结束后清理 MDC
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 脱敏请求 URI 中的敏感路径段，防止日志泄露 ID/UUID/token 等信息。
     */
    static String sanitizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return uri;
        }
        return SENSITIVE_PATH_SEGMENT.matcher(uri).replaceAll("{redacted}");
    }
}
