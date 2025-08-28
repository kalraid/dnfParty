package com.dfparty.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        // 상세한 에러 로깅
        System.out.println("=== 전역 예외 처리기에서 에러 발생 ===");
        System.out.println("요청 URI: " + request.getDescription(false));
        System.out.println("에러 타입: " + ex.getClass().getName());
        System.out.println("에러 메시지: " + ex.getMessage());
        System.out.println("에러 원인: " + (ex.getCause() != null ? ex.getCause().getMessage() : "원인 없음"));
        System.out.println("발생 시간: " + LocalDateTime.now());
        
        // 스택 트레이스 출력
        System.out.println("스택 트레이스:");
        ex.printStackTrace();
        
        
        System.out.println("=== 전역 예외 처리기 에러 로깅 완료 ===");
        
        // 클라이언트에게 반환할 에러 응답
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", ex.getClass().getSimpleName());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        System.out.println("=== RuntimeException 처리기에서 에러 발생 ===");
        System.out.println("요청 URI: " + request.getDescription(false));
        System.out.println("에러 메시지: " + ex.getMessage());
        System.out.println("에러 원인: " + (ex.getCause() != null ? ex.getCause().getMessage() : "원인 없음"));
        System.out.println("발생 시간: " + LocalDateTime.now());
        
        // 스택 트레이스 출력
        System.out.println("스택 트레이스:");
        ex.printStackTrace();
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "RuntimeException");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * SSE 연결 관련 IOException 처리 (Broken pipe 등)
     * SSE 연결 중 발생하는 네트워크 오류는 정상적인 상황이므로 별도 처리
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex, WebRequest request) {
        String requestUri = request.getDescription(false);
        
        // SSE 연결 관련 요청인지 확인
        if (requestUri.contains("/api/sse/") || requestUri.contains("uri=/api/sse/")) {
            // SSE 연결 끊김은 간단한 로그만 출력
            if (ex.getMessage() != null && ex.getMessage().contains("Broken pipe")) {
                System.out.println("🔌 SSE 연결 끊김: " + requestUri);
                // 정상적인 연결 해제이므로 상세 로그 제거
            } else {
                // Broken pipe가 아닌 다른 SSE 오류는 간단한 로그만
                System.out.println("⚠️ SSE 연결 오류: " + ex.getMessage());
            }
            
            // SSE 연결 오류는 클라이언트에게 에러 응답을 보내지 않음
            // 대신 로깅만 하고 정상적인 응답 반환
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "SSE 연결이 정상적으로 처리되었습니다.");
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        }
        
        // 일반적인 IOException은 기존 로직으로 처리
        System.out.println("=== 일반 IOException 처리 ===");
        System.out.println("요청 URI: " + requestUri);
        System.out.println("에러 메시지: " + ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("error", "IOException");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", requestUri);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
