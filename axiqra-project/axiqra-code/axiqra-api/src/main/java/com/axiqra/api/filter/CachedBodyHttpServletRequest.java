package com.axiqra.api.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可缓存请求体的 HttpServletRequest 包装器
 * <p>
 * 用途：ApiSignatureFilter 需要读取请求体进行签名校验，
 * 但请求体只能读一次，需要包装后缓存供后续使用。
 * <p>
 * 安全：构造函数限制最大缓存大小，防止 OOM 攻击。
 *
 * @author Axiqra Team
 * @date 2026-06-06
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBodySize) throws IOException {
        super(request);
        this.cachedBody = readBoundedBody(request.getInputStream(), maxBodySize);
    }

    private byte[] readBoundedBody(java.io.InputStream inputStream, int maxBodySize) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            total += read;
            if (total > maxBodySize) {
                throw new PayloadTooLargeException(
                        "Request body exceeds maximum allowed size of " + maxBodySize + " bytes");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(this.cachedBody), StandardCharsets.UTF_8));
    }

    /**
     * 请求体超限异常
     */
    public static class PayloadTooLargeException extends IOException {
        public PayloadTooLargeException(String message) {
            super(message);
        }
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
