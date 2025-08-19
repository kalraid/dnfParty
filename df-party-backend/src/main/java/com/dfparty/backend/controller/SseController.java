package com.dfparty.backend.controller;

import com.dfparty.backend.model.RealtimeEvent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = "*")
public class SseController {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final int MAX_EMITTERS_PER_CLIENT = 3; // 클라이언트당 최대 에미터 수 제한
    private static final int MAX_TOTAL_EMITTERS = 100; // 전체 최대 에미터 수 제한
    
    public SseController() {
        // 기본 생성자
    }

    /**
     * SSE 연결 생성
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String clientId) {
        System.out.println("SSE 연결 요청: " + clientId);
        
        // 30초 타임아웃으로 설정 (무한 대기 방지)
        SseEmitter emitter = new SseEmitter(30000L);
        
        // 연결 성공 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data(Map.of("message", "SSE 연결 성공", "clientId", clientId)));
        } catch (IOException e) {
            System.err.println("SSE 연결 이벤트 전송 실패: " + e.getMessage());
        }
        
        // 클라이언트별 에미터 저장 (제한 적용)
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>());
        
        // 클라이언트당 최대 에미터 수 제한
        if (clientEmitters.size() >= MAX_EMITTERS_PER_CLIENT) {
            System.out.println("클라이언트 " + clientId + "의 최대 에미터 수 초과, 오래된 연결 제거");
            SseEmitter oldEmitter = clientEmitters.remove(0);
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                System.err.println("오래된 에미터 정리 실패: " + e.getMessage());
            }
        }
        
        // 전체 최대 에미터 수 제한
        int totalEmitters = emitters.values().stream().mapToInt(List::size).sum();
        if (totalEmitters >= MAX_TOTAL_EMITTERS) {
            System.out.println("전체 최대 에미터 수 초과, 오래된 연결들 정리");
            cleanupOldEmitters();
        }
        
        clientEmitters.add(emitter);
        
        // 연결 해제 시 정리
        emitter.onCompletion(() -> {
            System.out.println("SSE 연결 완료: " + clientId);
            removeEmitter(clientId, emitter);
        });
        
        emitter.onTimeout(() -> {
            System.out.println("SSE 연결 타임아웃: " + clientId);
            removeEmitter(clientId, emitter);
        });
        
        emitter.onError((ex) -> {
            System.err.println("SSE 연결 오류: " + clientId + " - " + ex.getMessage());
            removeEmitter(clientId, emitter);
        });
        
        System.out.println("SSE 연결 생성 완료: " + clientId);
        return emitter;
    }

    /**
     * 특정 클라이언트에게 이벤트 전송
     */
    public void sendEventToUser(String clientId, RealtimeEvent event) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name(event.getType().toString().toLowerCase())
                        .data(event));
                    return false; // 유지
                } catch (IOException e) {
                    System.err.println("SSE 이벤트 전송 실패 (클라이언트: " + clientId + "): " + e.getMessage());
                    return true; // 제거
                }
            });
        }
    }
    
    /**
     * 모든 클라이언트에게 브로드캐스트
     */
    public void sendEventToAll(RealtimeEvent event) {
        System.out.println("SSE 브로드캐스트: " + event.getType());
        
        emitters.forEach((clientId, clientEmitters) -> {
            clientEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name(event.getType().toString().toLowerCase())
                        .data(event));
                    return false; // 유지
                } catch (IOException e) {
                    System.err.println("SSE 브로드캐스트 실패 (클라이언트: " + clientId + "): " + e.getMessage());
                    return true; // 제거
                }
            });
        });
    }
    
    /**
     * 특정 토픽으로 이벤트 전송
     */
    public void sendEventToTopic(String topic, RealtimeEvent event) {
        System.out.println("SSE 토픽 전송: " + topic + " -> " + event.getType());
        
        // 모든 클라이언트에게 전송 (토픽별 필터링은 클라이언트에서 처리)
        sendEventToAll(event);
    }

    /**
     * 에미터 제거
     */
    private void removeEmitter(String clientId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.remove(emitter);
            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        }
    }

    /**
     * 연결 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
            "activeConnections", emitters.size(),
            "totalEmitters", emitters.values().stream().mapToInt(CopyOnWriteArrayList::size).sum()
        );
        return ResponseEntity.ok(status);
    }
    
    /**
     * 오래된 에미터들을 정리하여 메모리 누수 방지
     */
    private void cleanupOldEmitters() {
        System.out.println("오래된 에미터 정리 시작");
        
        emitters.forEach((clientId, clientEmitters) -> {
            // 각 클라이언트의 에미터 수를 제한
            while (clientEmitters.size() > MAX_EMITTERS_PER_CLIENT) {
                SseEmitter oldEmitter = clientEmitters.remove(0);
                try {
                    oldEmitter.complete();
                    System.out.println("오래된 에미터 정리 완료: " + clientId);
                } catch (Exception e) {
                    System.err.println("오래된 에미터 정리 실패: " + e.getMessage());
                }
            }
        });
        
        // 빈 클라이언트 리스트 제거
        emitters.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        System.out.println("오래된 에미터 정리 완료");
    }
    
    /**
     * 메모리 사용량 모니터링
     */
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        System.out.println("=== SSE 메모리 사용량 현황 ===");
        System.out.println("사용 중인 메모리: " + (usedMemory / (1024 * 1024)) + " MB");
        System.out.println("사용 가능한 메모리: " + (freeMemory / (1024 * 1024)) + " MB");
        System.out.println("총 메모리: " + (totalMemory / (1024 * 1024)) + " MB");
        System.out.println("최대 메모리: " + (maxMemory / (1024 * 1024)) + " MB");
        System.out.println("메모리 사용률: " + ((usedMemory * 100) / maxMemory) + "%");
        System.out.println("활성 SSE 연결 수: " + emitters.values().stream().mapToInt(List::size).sum());
    }
}
