package com.dfparty.backend.controller;

import com.dfparty.backend.config.MockConfig;
import com.dfparty.backend.model.MockApiData;
import com.dfparty.backend.service.MockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MockManagementController {
    
    private final MockDataService mockDataService;
    private final MockConfig mockConfig;
    
    /**
     * Mock 모드 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMockStatus() {
        log.info("Mock 모드 상태 확인");
        
        Map<String, Object> status = Map.of(
            "enabled", mockConfig.isEnabled(),
            "autoSave", mockConfig.isAutoSave(),
            "serverUrl", mockConfig.getServerUrl(),
            "dataPath", mockConfig.getDataPath()
        );
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Mock 모드 활성화/비활성화
     */
    @PatchMapping("/status")
    public ResponseEntity<Map<String, String>> updateMockStatus(@RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        
        if (enabled != null) {
            // TODO: MockConfig의 enabled 값을 동적으로 변경하는 로직 구현
            log.info("Mock 모드 상태 변경: {}", enabled);
            return ResponseEntity.ok(Map.of("message", "Mock 모드가 " + (enabled ? "활성화" : "비활성화") + "되었습니다."));
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "enabled 값이 필요합니다."));
    }
    
    /**
     * 모든 Mock 데이터 조회
     */
    @GetMapping("/data")
    public ResponseEntity<List<MockApiData>> getAllMockData() {
        log.info("모든 Mock 데이터 조회");
        
        try {
            List<MockApiData> mockData = mockDataService.getAllMockData();
            return ResponseEntity.ok(mockData);
            
        } catch (Exception e) {
            log.error("Mock 데이터 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API 타입별 Mock 데이터 조회
     */
    @GetMapping("/data/{apiType}")
    public ResponseEntity<List<MockApiData>> getMockDataByType(@PathVariable String apiType) {
        log.info("Mock 데이터 조회: {}", apiType);
        
        try {
            MockApiData.ApiType type = MockApiData.ApiType.valueOf(apiType.toUpperCase());
            List<MockApiData> mockData = mockDataService.getMockDataByType(type);
            return ResponseEntity.ok(mockData);
            
        } catch (IllegalArgumentException e) {
            log.error("잘못된 API 타입: {}", apiType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Mock 데이터 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Mock 데이터 비활성화
     */
    @PatchMapping("/data/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateMockData(@PathVariable Long id) {
        log.info("Mock 데이터 비활성화: {}", id);
        
        try {
            mockDataService.deactivateMockData(id);
            return ResponseEntity.ok(Map.of("message", "Mock 데이터가 비활성화되었습니다."));
            
        } catch (Exception e) {
            log.error("Mock 데이터 비활성화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "비활성화 실패"));
        }
    }
    
    /**
     * Mock 데이터 삭제
     */
    @DeleteMapping("/data/{id}")
    public ResponseEntity<Map<String, String>> deleteMockData(@PathVariable Long id) {
        log.info("Mock 데이터 삭제: {}", id);
        
        try {
            mockDataService.deleteMockData(id);
            return ResponseEntity.ok(Map.of("message", "Mock 데이터가 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Mock 데이터 삭제 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "삭제 실패"));
        }
    }
    
    /**
     * 모든 Mock 데이터 초기화
     */
    @PostMapping("/data/reset")
    public ResponseEntity<Map<String, String>> resetAllMockData() {
        log.info("모든 Mock 데이터 초기화");
        
        try {
            mockDataService.resetAllMockData();
            return ResponseEntity.ok(Map.of("message", "모든 Mock 데이터가 초기화되었습니다."));
            
        } catch (Exception e) {
            log.error("Mock 데이터 초기화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "초기화 실패"));
        }
    }
}
