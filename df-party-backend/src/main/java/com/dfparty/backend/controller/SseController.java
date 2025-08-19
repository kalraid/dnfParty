package com.dfparty.backend.controller;

import com.dfparty.backend.model.RealtimeEvent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = "*")
public class SseController {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    public SseController() {
        // 기본 생성자
    }

    /**
     * SSE 연결 생성
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String clientId) {
        System.out.println("SSE 연결 요청: " + clientId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // 연결 성공 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connect")
                .data(Map.of("message", "SSE 연결 성공", "clientId", clientId)));
        } catch (IOException e) {
            System.err.println("SSE 연결 이벤트 전송 실패: " + e.getMessage());
        }
        
        // 클라이언트별 에미터 저장
        emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
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
}
