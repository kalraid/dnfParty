package com.dfparty.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

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
}
