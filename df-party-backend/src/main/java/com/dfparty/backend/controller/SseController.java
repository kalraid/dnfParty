package com.dfparty.backend.controller;

import com.dfparty.backend.model.RealtimeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(origins = "*")
public class SseController {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final int MAX_EMITTERS_PER_CLIENT = 3; // 클라이언트당 최대 에미터 수 제한
    private static final int MAX_TOTAL_EMITTERS = 100; // 전체 최대 에미터 수 제한
    private final ObjectMapper objectMapper; // JSON 변환용
    
    public SseController() {
        // Jackson JSR310 모듈 활성화로 LocalDateTime 직렬화 지원
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        System.out.println("✅ Jackson JSR310 모듈 활성화 완료 - LocalDateTime 직렬화 지원");
    }

    /**
     * SSE 연결 생성
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String clientId) {
        System.out.println("🚀 === SSE 연결 요청 시작 ===");
        System.out.println("🆔 클라이언트 ID: " + clientId);
        System.out.println("📊 현재 활성 연결 수: " + emitters.size());
        System.out.println("🔗 현재 총 에미터 수: " + emitters.values().stream().mapToInt(List::size).sum());
        
        // 기존 활성 에미터가 있는지 확인
        CopyOnWriteArrayList<SseEmitter> existingEmitters = emitters.get(clientId);
        if (existingEmitters != null && !existingEmitters.isEmpty()) {
            // 기존 활성 에미터 찾기
            for (SseEmitter existingEmitter : existingEmitters) {
                if (existingEmitter != null) {
                    // 에미터 상태 확인을 위해 테스트 이벤트 전송 시도
                    try {
                        String reconnectMessage = "SSE 재연결 성공: " + clientId;
                        existingEmitter.send(SseEmitter.event()
                            .name("reconnect")
                            .data(reconnectMessage));
                        
                        System.out.println("🔄 기존 활성 에미터 발견 - 재사용");
                        System.out.println("📊 클라이언트 " + clientId + "의 기존 에미터 수: " + existingEmitters.size());
                        System.out.println("✅ 재연결 성공 이벤트 전송 완료: " + reconnectMessage);
                        
                        System.out.println("🎯 === 기존 에미터 재사용 완료 ===");
                        System.out.println("🆔 클라이언트 ID: " + clientId);
                        System.out.println("📊 최종 활성 연결 수: " + emitters.size());
                        System.out.println("🔗 최종 총 에미터 수: " + emitters.values().stream().mapToInt(List::size).sum());
                        System.out.println("=================================");
                        
                        return existingEmitter;
                        
                    } catch (IOException e) {
                        System.err.println("❌ 기존 에미터 테스트 실패 - 만료됨: " + e.getMessage());
                        // 만료된 에미터는 나중에 정리
                        continue;
                    }
                }
            }
            
            // 모든 기존 에미터가 만료된 경우 정리
            System.out.println("⚠️ 기존 에미터들이 모두 만료됨 - 정리 후 새로 생성");
            existingEmitters.removeIf(emitter -> emitter == null);
            if (existingEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        }
        
        // 새로운 에미터 생성
        System.out.println("🆕 새 에미터 생성 시작");
        
        // 2분 타임아웃으로 설정 (SSE 연결 유지, 더 짧게 설정하여 안정성 향상)
        SseEmitter emitter = new SseEmitter(120000L);
        System.out.println("⏰ SSE 타임아웃 설정: 120초 (2분)");
        
        // 연결 성공 이벤트 전송
        try {
            String connectMessage = "SSE 연결 성공: " + clientId;
            emitter.send(SseEmitter.event()
                .name("connect")
                .data(connectMessage));
            System.out.println("✅ 연결 성공 이벤트 전송 완료: " + connectMessage);
        } catch (IOException e) {
            System.err.println("❌ SSE 연결 이벤트 전송 실패: " + e.getMessage());
        }
        
        // 클라이언트별 에미터 저장 (제한 적용)
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>());
        System.out.println("📋 클라이언트 " + clientId + "의 현재 에미터 수: " + clientEmitters.size());
        
        // 클라이언트당 최대 에미터 수 제한
        if (clientEmitters.size() >= MAX_EMITTERS_PER_CLIENT) {
            System.out.println("⚠️ 클라이언트 " + clientId + "의 최대 에미터 수 초과 (" + MAX_EMITTERS_PER_CLIENT + "개), 오래된 연결 제거");
            SseEmitter oldEmitter = clientEmitters.remove(0);
            try {
                oldEmitter.complete();
                System.out.println("🗑️ 오래된 에미터 정리 완료");
            } catch (Exception e) {
                System.err.println("❌ 오래된 에미터 정리 실패: " + e.getMessage());
            }
        }
        
        // 전체 최대 에미터 수 제한
        int totalEmitters = emitters.values().stream().mapToInt(List::size).sum();
        if (totalEmitters >= MAX_TOTAL_EMITTERS) {
            System.out.println("⚠️ 전체 최대 에미터 수 초과 (" + MAX_TOTAL_EMITTERS + "개), 오래된 연결들 정리");
            cleanupOldEmitters();
        }
        
        clientEmitters.add(emitter);
        System.out.println("➕ 새 에미터 추가 완료");
        
        // 연결 해제 시 정리
        emitter.onCompletion(() -> {
            System.out.println("✅ SSE 연결 완료: " + clientId);
            removeEmitter(clientId, emitter);
        });
        
        emitter.onTimeout(() -> {
            System.out.println("⏰ SSE 연결 타임아웃: " + clientId);
            removeEmitter(clientId, emitter);
        });
        
        emitter.onError((ex) -> {
            System.err.println("🚨 SSE 연결 오류: " + clientId + " - " + ex.getMessage());
            if (ex instanceof java.io.IOException && ex.getMessage().contains("Broken pipe")) {
                System.out.println("🔌 Broken pipe 감지 - 클라이언트 연결이 끊어짐");
            }
            removeEmitter(clientId, emitter);
        });
        
        // 하트비트 메커니즘 추가 (30초마다 연결 상태 확인)
        startHeartbeat(emitter, clientId);
        
        System.out.println("🎯 === 새 에미터 생성 완료 ===");
        System.out.println("🆔 클라이언트 ID: " + clientId);
        System.out.println("📊 최종 활성 연결 수: " + emitters.size());
        System.out.println("🔗 최종 총 에미터 수: " + emitters.values().stream().mapToInt(List::size).sum());
        System.out.println("=================================");
        
        return emitter;
    }

    /**
     * RealtimeEvent를 JSON 문자열로 변환
     */
    private String convertEventToJson(RealtimeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            System.err.println("이벤트 JSON 변환 실패: " + e.getMessage());
            // 기본 문자열 반환
            return "{\"type\":\"" + event.getType() + "\",\"message\":\"" + event.getMessage() + "\"}";
        }
    }

    /**
     * 특정 클라이언트에게 이벤트 전송
     */
    public void sendEventToUser(String clientId, RealtimeEvent event) {
        System.out.println("📤 === SSE 개별 이벤트 전송 시작 ===");
        System.out.println("🆔 대상 클라이언트: " + clientId);
        System.out.println("📋 이벤트 타입: " + event.getType());
        System.out.println("💬 이벤트 메시지: " + event.getMessage());
        
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            System.out.println("🔗 클라이언트 " + clientId + "의 에미터 수: " + clientEmitters.size());
            
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            
            clientEmitters.removeIf(emitter -> {
                try {
                    // RealtimeEvent를 JSON 문자열로 변환하여 전송
                    String eventJson = convertEventToJson(event);
                    emitter.send(SseEmitter.event()
                        .name(event.getType().toString().toLowerCase())
                        .data(eventJson));
                    successCount.incrementAndGet();
                    System.out.println("✅ 이벤트 전송 성공 (에미터 " + successCount.get() + "개)");
                    return false; // 유지
                } catch (IOException e) {
                    failureCount.incrementAndGet();
                    System.err.println("❌ SSE 이벤트 전송 실패 (클라이언트: " + clientId + "): " + e.getMessage());
                    return true; // 제거
                }
            });
            
            System.out.println("📊 === 이벤트 전송 결과 ===");
            System.out.println("✅ 성공: " + successCount.get() + "개");
            System.out.println("❌ 실패: " + failureCount.get() + "개");
            System.out.println("🔗 남은 에미터 수: " + clientEmitters.size());
            System.out.println("=================================");
        } else {
            System.out.println("⚠️ 클라이언트 " + clientId + "에 대한 활성 연결이 없음");
        }
    }
    
    /**
     * 모든 클라이언트에게 브로드캐스트
     */
    public void sendEventToAll(RealtimeEvent event) {
        System.out.println("📢 === SSE 브로드캐스트 시작 ===");
        System.out.println("📋 이벤트 타입: " + event.getType());
        System.out.println("💬 이벤트 메시지: " + event.getMessage());
        System.out.println("🌐 대상 클라이언트 수: " + emitters.size());
        
        AtomicInteger totalSuccessCount = new AtomicInteger(0);
        AtomicInteger totalFailureCount = new AtomicInteger(0);
        int totalClients = emitters.size();
        
        emitters.forEach((clientId, clientEmitters) -> {
            System.out.println("📤 클라이언트 " + clientId + "에게 전송 중... (에미터 " + clientEmitters.size() + "개)");
            
            AtomicInteger clientSuccessCount = new AtomicInteger(0);
            AtomicInteger clientFailureCount = new AtomicInteger(0);
            
            clientEmitters.removeIf(emitter -> {
                try {
                    // RealtimeEvent를 JSON 문자열로 변환하여 전송
                    String eventJson = convertEventToJson(event);
                    emitter.send(SseEmitter.event()
                        .name(event.getType().toString().toLowerCase())
                        .data(eventJson));
                    clientSuccessCount.incrementAndGet();
                    return false; // 유지
                } catch (IOException e) {
                    clientFailureCount.incrementAndGet();
                    System.err.println("❌ SSE 브로드캐스트 실패 (클라이언트: " + clientId + "): " + e.getMessage());
                    return true; // 제거
                }
            });
            
            totalSuccessCount.addAndGet(clientSuccessCount.get());
            totalFailureCount.addAndGet(clientFailureCount.get());
            
            System.out.println("📊 클라이언트 " + clientId + " 결과 - 성공: " + clientSuccessCount.get() + "개, 실패: " + clientFailureCount.get() + "개");
        });
        
        System.out.println("📊 === 브로드캐스트 최종 결과 ===");
        System.out.println("🌐 총 클라이언트 수: " + totalClients);
        System.out.println("✅ 총 성공: " + totalSuccessCount.get() + "개");
        System.out.println("❌ 총 실패: " + totalFailureCount.get() + "개");
        System.out.println("=================================");
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
     * 하트비트 메커니즘으로 연결 상태 확인
     */
    private void startHeartbeat(SseEmitter emitter, String clientId) {
        Thread heartbeatThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000); // 30초마다 하트비트 전송
                    
                    // 에미터가 여전히 유효한지 확인
                    if (emitter == null) {
                        System.out.println("💓 하트비트 중단 - 에미터가 null: " + clientId);
                        break;
                    }
                    
                    try {
                        // 하트비트 이벤트 전송
                        emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                        System.out.println("💓 하트비트 전송 완료: " + clientId);
                        
                    } catch (IOException e) {
                        // Broken pipe는 클라이언트 연결 해제를 의미 (정상적인 상황)
                        if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                            System.out.println("🔌 하트비트 전송 중 Broken pipe 감지: " + clientId + " - 클라이언트 연결이 정상적으로 해제됨");
                        } else {
                            System.err.println("❌ 하트비트 전송 실패: " + clientId + " - " + e.getMessage());
                        }
                        
                        // 에미터 정리 및 스레드 종료
                        try {
                            emitter.complete();
                        } catch (Exception cleanupEx) {
                            // 정리 중 오류는 무시
                        }
                        
                        // 클라이언트 에미터 목록에서 제거
                        removeEmitter(clientId, emitter);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("💓 하트비트 스레드 중단됨: " + clientId);
                Thread.currentThread().interrupt();
            }
        });
        
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("SSE-Heartbeat-" + clientId);
        heartbeatThread.start();
        
        System.out.println("💓 하트비트 스레드 시작: " + clientId);
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
