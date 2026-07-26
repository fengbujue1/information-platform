package com.informationplatform.hub.ingestion.api;

import com.informationplatform.hub.common.api.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CollectorRequestSizeFilter extends OncePerRequestFilter {

    /** 第一阶段采集器写入接口路径。 */
    private static final String COLLECTOR_ITEMS_PATH = "/api/v1/collector/items";

    /** 采集接口请求体大小配置。 */
    private final CollectorApiProperties properties;

    /** 用于在过滤器阶段输出统一错误响应。 */
    private final ApiErrorWriter errorWriter;

    public CollectorRequestSizeFilter(
            CollectorApiProperties properties, ApiErrorWriter errorWriter) {
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    /** 仅对采集器写入接口限制请求体大小。 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !COLLECTOR_ITEMS_PATH.equals(request.getRequestURI());
    }

    /** 同时检查 Content-Length 和实际读取字节数，防止分块请求绕过限制。 */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        long maximumBytes = properties.getMaxRequestBytes();
        // 已声明长度的请求可在读取请求体前直接拒绝。
        if (request.getContentLengthLong() > maximumBytes) {
            writeTooLarge(response);
            return;
        }

        try {
            // 包装输入流以限制未声明长度或分块传输请求的实际读取量。
            filterChain.doFilter(new LimitedRequestWrapper(request, maximumBytes), response);
        } catch (RequestSizeLimitExceededException exception) {
            // 响应尚未提交时转换为统一 413；已提交时交由容器处理异常。
            if (!response.isCommitted()) {
                writeTooLarge(response);
                return;
            }
            throw exception;
        }
    }

    /** 输出统一的请求体超限响应。 */
    private void writeTooLarge(HttpServletResponse response) throws IOException {
        errorWriter.write(
                response,
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "REQUEST_TOO_LARGE",
                "Request body exceeds the configured size limit");
    }

    private static final class LimitedRequestWrapper extends HttpServletRequestWrapper {

        /** 带实际字节计数限制的请求输入流。 */
        private final ServletInputStream inputStream;

        private LimitedRequestWrapper(HttpServletRequest request, long maximumBytes)
                throws IOException {
            super(request);
            this.inputStream =
                    new LimitedServletInputStream(request.getInputStream(), maximumBytes);
        }

        @Override
        public ServletInputStream getInputStream() {
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset =
                    encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {

        /** 原始 Servlet 请求输入流。 */
        private final ServletInputStream delegate;

        /** 允许读取的最大字节数。 */
        private final long maximumBytes;

        /** 已从请求体读取的累计字节数。 */
        private long bytesRead;

        private LimitedServletInputStream(ServletInputStream delegate, long maximumBytes) {
            this.delegate = delegate;
            this.maximumBytes = maximumBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                enforceLimit(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                enforceLimit(count);
            }
            return count;
        }

        /** 累加实际读取量并在超过配置上限时立即终止解析。 */
        private void enforceLimit(int count) throws RequestSizeLimitExceededException {
            bytesRead += count;
            if (bytesRead > maximumBytes) {
                throw new RequestSizeLimitExceededException(maximumBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
