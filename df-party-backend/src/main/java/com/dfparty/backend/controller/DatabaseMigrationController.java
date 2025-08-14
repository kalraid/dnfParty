package com.dfparty.backend.controller;

import com.dfparty.backend.service.DatabaseMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/migration")
@CrossOrigin(origins = "*")
public class DatabaseMigrationController {

    @Autowired
    private DatabaseMigrationService migrationService;

    /**
     * Local Storage 데이터를 데이터베이스로 마이그레이션
     */
    @PostMapping("/from-local-storage")
    public ResponseEntity<Map<String, Object>> migrateFromLocalStorage(
            @RequestBody List<Map<String, Object>> localCharacters) {
        
        try {
            Map<String, Object> result = migrationService.migrateFromLocalStorage(localCharacters);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "마이그레이션 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 마이그레이션 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        try {
            Map<String, Object> status = migrationService.getMigrationStatus();
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "상태 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 마이그레이션 테스트 (빈 데이터로 테스트)
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testMigration() {
        try {
            // 빈 리스트로 테스트
            List<Map<String, Object>> testData = List.of();
            Map<String, Object> result = migrationService.migrateFromLocalStorage(testData);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "테스트 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
