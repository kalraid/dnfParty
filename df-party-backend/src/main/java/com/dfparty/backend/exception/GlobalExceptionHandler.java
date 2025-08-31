package com.dfparty.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
     * SSE 관련 예외 처리
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex, WebRequest request) {
        System.out.println("=== SSE 관련 IOException 처리기에서 에러 발생 ===");
        System.out.println("요청 URI: " + request.getDescription(false));
        System.out.println("에러 메시지: " + ex.getMessage());
        System.out.println("발생 시간: " + LocalDateTime.now());
        
        // SSE 연결 끊김 관련 에러인지 확인
        if (ex.getMessage() != null && ex.getMessage().contains("Broken pipe")) {
            System.out.println("🔌 SSE Broken pipe 에러 감지 - 연결이 끊어짐");
            // SSE 연결 끊김은 정상적인 상황이므로 간단한 메시지 반환
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("SSE 연결이 끊어졌습니다.");
        }
        
        // 일반적인 IOException
        System.out.println("⚠️ 일반적인 IOException - 상세 정보 로깅");
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.TEXT_PLAIN)
            .body("SSE 연결 중 오류가 발생했습니다: " + ex.getMessage());
    }

    /**
     * SSE 응답 형식 관련 예외 처리
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotWritableException.class)
    public ResponseEntity<String> handleHttpMessageNotWritableException(
            org.springframework.http.converter.HttpMessageNotWritableException ex, WebRequest request) {
        System.out.println("=== SSE 응답 형식 예외 처리기에서 에러 발생 ===");
        System.out.println("요청 URI: " + request.getDescription(false));
        System.out.println("에러 메시지: " + ex.getMessage());
        System.out.println("발생 시간: " + LocalDateTime.now());
        
        // SSE 관련 요청인지 확인
        if (request.getDescription(false).contains("/api/sse/")) {
            System.out.println("🔌 SSE 응답 형식 에러 감지 - 적절한 형식으로 변환 필요");
            
            // SSE 응답에 적합한 형식으로 에러 메시지 반환
            String errorMessage = "data: {\"type\":\"error\",\"message\":\"SSE 응답 형식 오류가 발생했습니다.\"}\n\n";
            
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(errorMessage);
        }
        
        // 일반적인 응답 형식 에러
        System.out.println("⚠️ 일반적인 응답 형식 에러 - 상세 정보 로깅");
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.TEXT_PLAIN)
            .body("응답 형식 오류가 발생했습니다: " + ex.getMessage());
    }
}
