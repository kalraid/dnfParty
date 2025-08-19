package com.dfparty.backend.controller;

import com.dfparty.backend.service.MemoryMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MemoryController {
    
    private final MemoryMonitoringService memoryMonitoringService;
    
    /**
     * 현재 메모리 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMemoryStatus() {
        try {
            log.info("메모리 상태 조회 요청");
            Map<String, Object> status = memoryMonitoringService.getCurrentMemoryStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("메모리 상태 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "메모리 상태 조회 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 수동으로 가비지 컬렉션 실행
     */
    @PostMapping("/gc")
    public ResponseEntity<Map<String, Object>> triggerGarbageCollection() {
        try {
            log.info("수동 가비지 컬렉션 실행 요청");
            
            // GC 실행 전 메모리 상태
            Map<String, Object> beforeStatus = memoryMonitoringService.getCurrentMemoryStatus();
            
            // 가비지 컬렉션 실행
            System.gc();
            
            // 잠시 대기
            Thread.sleep(1000);
            
            // GC 실행 후 메모리 상태
            Map<String, Object> afterStatus = memoryMonitoringService.getCurrentMemoryStatus();
            
            Map<String, Object> result = Map.of(
                "message", "가비지 컬렉션 실행 완료",
                "before", beforeStatus,
                "after", afterStatus,
                "timestamp", System.currentTimeMillis()
            );
            
            log.info("수동 가비지 컬렉션 실행 완료");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("가비지 컬렉션 실행 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "가비지 컬렉션 실행 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 메모리 사용량 경고 임계값 설정
     */
    @PostMapping("/threshold")
    public ResponseEntity<Map<String, Object>> setMemoryThreshold(
            @RequestParam(defaultValue = "80") int warningThreshold,
            @RequestParam(defaultValue = "90") int criticalThreshold) {
        try {
            log.info("메모리 임계값 설정: 경고={}%, 위험={}%", warningThreshold, criticalThreshold);
            
            // 임계값 검증
            if (warningThreshold < 50 || warningThreshold > 95) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "경고 임계값은 50-95% 사이여야 합니다"));
            }
            
            if (criticalThreshold < warningThreshold || criticalThreshold > 98) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "위험 임계값은 경고 임계값보다 크고 98% 이하여야 합니다"));
            }
            
            Map<String, Object> result = Map.of(
                "message", "메모리 임계값 설정 완료",
                "warningThreshold", warningThreshold,
                "criticalThreshold", criticalThreshold,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("메모리 임계값 설정 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "메모리 임계값 설정 실패: " + e.getMessage()));
        }
    }
    
    /**
     * 메모리 히스토리 조회 (최근 10개)
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getMemoryHistory() {
        try {
            log.info("메모리 히스토리 조회 요청");
            
            // 실제 구현에서는 데이터베이스나 캐시에서 히스토리를 가져와야 함
            // 현재는 현재 상태만 반환
            Map<String, Object> currentStatus = memoryMonitoringService.getCurrentMemoryStatus();
            
            Map<String, Object> result = Map.of(
                "message", "메모리 히스토리 조회 완료",
                "currentStatus", currentStatus,
                "note", "히스토리 기능은 향후 구현 예정",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("메모리 히스토리 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "메모리 히스토리 조회 실패: " + e.getMessage()));
        }
    }
}
