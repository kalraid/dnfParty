package com.dfparty.backend.controller;

import com.dfparty.backend.service.CharacterSyncSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/character-sync")
@CrossOrigin(origins = "*")
public class CharacterSyncController {

    @Autowired
    private CharacterSyncSchedulerService characterSyncSchedulerService;

    /**
     * 동기화 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            Map<String, Object> status = characterSyncSchedulerService.getSyncStatus();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", status
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "동기화 상태 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 수동으로 동기화 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startManualSync() {
        try {
            characterSyncSchedulerService.startManualSync();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "수동 동기화가 시작되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "수동 동기화 시작 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
