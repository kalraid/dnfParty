package com.dfparty.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute("requestId", requestId);
        request.setAttribute("startTime", System.currentTimeMillis());
        
        log.info("=== API 요청 시작 [{}] ===", requestId);
        log.info("요청 URL: {} {}", request.getMethod(), request.getRequestURI());
        log.info("요청 헤더:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            String headerValue = request.getHeader(headerName);
            if (headerName.toLowerCase().contains("authorization")) {
                log.info("  {}: {}", headerName, headerValue != null ? "***" : "null");
            } else {
                log.info("  {}: {}", headerName, headerValue);
            }
        });
        
        // 요청 본문 로깅 (POST/PUT 요청의 경우) - ContentCachingRequestWrapper 사용 권장
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            String contentType = request.getContentType();
            log.info("요청 Content-Type: {}", contentType);
            
            // JSON 요청인 경우에만 본문 로깅 시도
            if (contentType != null && contentType.contains("application/json")) {
                try {
                    // 요청 본문을 캐시할 수 있는지 확인
                    if (request.getContentLength() > 0) {
                        log.info("요청 본문 크기: {} bytes", request.getContentLength());
                        // 실제 본문은 ContentCachingRequestWrapper에서 처리하는 것이 좋음
                    }
                } catch (Exception e) {
                    log.warn("요청 본문 정보 확인 실패: {}", e.getMessage());
                }
            }
        }
        
        log.info("요청 파라미터:");
        request.getParameterMap().forEach((key, values) -> {
            log.info("  {}: {}", key, String.join(", ", values));
        });
        
        log.info("클라이언트 IP: {}", getClientIpAddress(request));
        log.info("사용자 에이전트: {}", request.getHeader("User-Agent"));
        log.info("=== API 요청 로깅 완료 [{}] ===", requestId);
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String requestId = (String) request.getAttribute("requestId");
        log.info("=== API 응답 처리 완료 [{}] ===", requestId);
        log.info("응답 상태: {}", response.getStatus());
        log.info("응답 헤더:");
        response.getHeaderNames().forEach(headerName -> {
            String headerValue = response.getHeader(headerName);
            log.info("  {}: {}", headerName, headerValue);
        });
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = (String) request.getAttribute("requestId");
        log.info("=== API 요청 완료 [{}] ===", requestId);
        log.info("최종 응답 상태: {}", response.getStatus());
        
        // 처리 시간 계산 (타입 안전하게)
        Object startTimeObj = request.getAttribute("startTime");
        if (startTimeObj instanceof Long) {
            long startTime = (Long) startTimeObj;
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("처리 시간: {}ms", processingTime);
        } else {
            log.warn("시작 시간을 찾을 수 없음, 처리 시간 계산 불가");
        }
        
        if (ex != null) {
            // SSE 연결 끊김은 간단한 로그만 출력
            if (ex instanceof IOException && ex.getMessage() != null && 
                ex.getMessage().contains("Broken pipe") && 
                request.getRequestURI().contains("/api/sse/")) {
                log.info("🔌 SSE 연결 끊김 [{}]: {}", requestId, request.getRequestURI());
            } else {
                log.error("=== API 요청 중 예외 발생 [{}] ===", requestId);
                log.error("예외 타입: {}", ex.getClass().getName());
                log.error("예외 메시지: {}", ex.getMessage());
                log.error("예외 원인: {}", ex.getCause() != null ? ex.getCause().getMessage() : "원인 없음");
                
                // 스택 트레이스 상세 분석
                StackTraceElement[] stackTrace = ex.getStackTrace();
                log.error("스택 트레이스 (최대 15개):");
                for (int i = 0; i < Math.min(stackTrace.length, 15); i++) {
                    StackTraceElement element = stackTrace[i];
                    log.error("  {}: {}.{}({}:{})", 
                        i, element.getClassName(), element.getMethodName(), 
                        element.getFileName(), element.getLineNumber());
                }
                log.error("=== API 요청 예외 로깅 완료 [{}] ===", requestId);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
