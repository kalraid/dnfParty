package com.dfparty.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // POST/PUT 요청인 경우에만 ContentCachingRequestWrapper 사용
        if (isPostOrPutRequest(request)) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            
            try {
                filterChain.doFilter(wrappedRequest, response);
            } finally {
                // 요청 본문 로깅
                logRequestBody(wrappedRequest);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isPostOrPutRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method);
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.info("=== 요청 본문 로깅 ===");
                log.info("Content-Type: {}", request.getContentType());
                log.info("Content-Length: {} bytes", content.length);
                log.info("요청 본문: {}", body);
                log.info("=== 요청 본문 로깅 완료 ===");
            }
        } catch (Exception e) {
            log.warn("요청 본문 로깅 실패: {}", e.getMessage());
        }
    }
}
