package com.dfparty.backend.controller;

import com.dfparty.backend.service.DungeonClearService;
import com.dfparty.backend.service.DungeonClearResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dungeon-clear")
@CrossOrigin(origins = "*")
public class DungeonClearController {

    @Autowired
    private DungeonClearService dungeonClearService;

    @Autowired
    private DungeonClearResetService dungeonClearResetService;

    @GetMapping("/{serverId}/{characterId}")
    public ResponseEntity<Map<String, Object>> getDungeonClearStatus(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        try {
            Map<String, Object> result = dungeonClearService.getDungeonClearStatus(serverId, characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{serverId}/bulk")
    public ResponseEntity<Map<String, Object>> getBulkDungeonClearStatus(
            @PathVariable String serverId,
            @RequestBody String[] characterIds) {
        try {
            Map<String, Object> result = dungeonClearService.getBulkDungeonClearStatus(serverId, characterIds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 수동으로 던전 클리어 상태 초기화 (관리자용)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> manualResetDungeonClearStatus() {
        try {
            dungeonClearResetService.manualResetDungeonClearStatus();
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "던전 클리어 상태가 수동으로 초기화되었습니다."
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "던전 클리어 상태 초기화 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 목요일 리셋 시간 확인
     */
    @GetMapping("/reset-status")
    public ResponseEntity<Map<String, Object>> getResetStatus() {
        try {
            boolean isThursday = dungeonClearResetService.isThursday();
            boolean isNearResetTime = dungeonClearResetService.isNearThursdayResetTime();
            
            Map<String, Object> result = Map.of(
                "success", true,
                "isThursday", isThursday,
                "isNearResetTime", isNearResetTime,
                "message", isNearResetTime ? "현재 목요일 리셋 시간대입니다." : "목요일 리셋 시간이 아닙니다."
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "리셋 상태 확인 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
