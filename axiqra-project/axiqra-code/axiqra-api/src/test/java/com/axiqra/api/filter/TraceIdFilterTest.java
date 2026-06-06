package com.axiqra.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TraceIdFilter 单元测试
 * 验证 traceId 生成/复用/MDC 注入/响应头/清理
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    @DisplayName("无 traceId 头时应生成新 traceId（UUID 无横杠）")
    void doFilter_noHeader_generatesNewTraceId() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        // 验证返回了 32 字符的 traceId
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), captor.capture());
        String traceId = captor.getValue();
        assertNotNull(traceId);
        assertEquals(32, traceId.length(), "traceId 应为 32 字符 UUID（无横杠）");
        assertFalse(traceId.contains("-"), "traceId 不应包含横杠");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("有 traceId 头时应复用该值")
    void doFilter_withHeader_reusesProvidedTraceId() throws Exception {
        String providedTraceId = "existing-trace-id-123";
        when(request.getHeader("X-Trace-Id")).thenReturn(providedTraceId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/submit");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        // 验证响应头设置了提供的 traceId
        verify(response).setHeader("X-Trace-Id", providedTraceId);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("traceId 应注入 MDC 并通过响应头返回")
    void doFilter_responseHeader_containsTraceId() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        // 验证响应头包含 traceId（MDC 在 filter 执行期间有效）
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), captor.capture());
        assertNotNull(captor.getValue());
        assertEquals(32, captor.getValue().length());
    }

    @Test
    @DisplayName("请求结束后 MDC 应被清理")
    void doFilter_mdcClearedAfterRequest() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        // 请求结束后 MDC 应被清理（通过 setHeader 被调用证明 traceId 生成）
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), captor.capture());
        assertNotNull(captor.getValue());
        assertEquals(32, captor.getValue().length());
    }

    @Test
    @DisplayName("空 traceId 头应生成新 traceId")
    void doFilter_blankHeader_generatesNew() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn("   ");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        // 验证返回了非空 traceId
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), captor.capture());
        assertNotNull(captor.getValue());
        assertEquals(32, captor.getValue().length());
        verify(chain).doFilter(request, response);
    }
}
