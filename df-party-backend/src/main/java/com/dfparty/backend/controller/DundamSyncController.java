package com.dfparty.backend.controller;

import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.RealtimeEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.dfparty.backend.model.RealtimeEvent;

@Slf4j
@RestController
@RequestMapping("/api/dundam-sync")
@RequiredArgsConstructor
public class DundamSyncController {

    private final DundamService dundamService;
    private final CharacterService characterService;
    private final RealtimeEventService realtimeEventService;
    
    // 캐릭터별 마지막 동기화 시간 저장 (2분 제한용)
    private final Map<String, LocalDateTime> lastSyncTime = new ConcurrentHashMap<>();
    
    // 현재 진행 중인 동기화 작업 추적
    private final Map<String, LocalDateTime> activeSyncs = new ConcurrentHashMap<>();
    
    // 2분 제한 확인 메서드
    private boolean isWithinTimeLimit(String characterKey) {
        LocalDateTime lastSync = lastSyncTime.get(characterKey);
        if (lastSync == null) {
            return true; // 처음 동기화하는 경우
        }
        
        long minutesSinceLastSync = ChronoUnit.MINUTES.between(lastSync, LocalDateTime.now());
        return minutesSinceLastSync >= 2;
    }
    
    // 동기화 시간 업데이트
    private void updateSyncTime(String characterKey) {
        lastSyncTime.put(characterKey, LocalDateTime.now());
    }

    /**
     * 특정 캐릭터의 던담 정보 동기화
     */
    @PostMapping("/character/{serverId}/{characterId}")
    public ResponseEntity<Map<String, Object>> syncCharacterFromDundam(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        
        try {
            log.info("=== 던담 동기화 시작 ===");
            log.info("서버: {}, 캐릭터 ID: {}", serverId, characterId);
            
            // 2분 제한 확인
            String characterKey = serverId + "_" + characterId;
            if (!isWithinTimeLimit(characterKey)) {
                LocalDateTime lastSync = lastSyncTime.get(characterKey);
                long minutesSinceLastSync = ChronoUnit.MINUTES.between(lastSync, LocalDateTime.now());
                long remainingMinutes = 2 - minutesSinceLastSync;
                
                log.warn("던담 동기화 제한: {}분 {}초 후에 다시 시도 가능", remainingMinutes, 
                    ChronoUnit.SECONDS.between(lastSync, LocalDateTime.now()) % 60);
                
                Map<String, Object> limitResult = Map.of(
                    "success", false,
                    "timeLimitExceeded", true,
                    "remainingMinutes", remainingMinutes,
                    "message", String.format("던담 동기화는 2분에 한 번만 가능합니다. %d분 후에 다시 시도해주세요.", remainingMinutes)
                );
                
                return ResponseEntity.badRequest().body(limitResult);
            }
            
            // 던담에서 캐릭터 정보 가져오기 (수동 동기화용 - 크롤링 비활성화 무시)
            Map<String, Object> dundamResult = dundamService.getCharacterInfoWithSeleniumManual(serverId, characterId);
            
            if (!(Boolean) dundamResult.get("success")) {
                log.warn("던담 크롤링 실패: {}", dundamResult.get("message"));
                
                // 목요일 제한 확인 (수동 동기화는 크롤링 비활성화에 영향받지 않음)
                if (dundamResult.containsKey("thursdayRestriction") && (Boolean) dundamResult.get("thursdayRestriction")) {
                    Map<String, Object> thursdayResult = Map.of(
                        "success", false,
                        "thursdayRestriction", true,
                        "message", "목요일에는 던담 크롤링이 제한되어 데이터를 제공할 수 없습니다."
                    );
                    return ResponseEntity.badRequest().body(thursdayResult);
                }
                
                return ResponseEntity.badRequest().body(dundamResult);
            }
            
            // 동기화 시간 업데이트
            updateSyncTime(characterKey);
            
            // DB에 캐릭터 정보 업데이트
            Map<String, Object> characterInfo = (Map<String, Object>) dundamResult.get("characterInfo");
            Map<String, Object> updateResult = characterService.updateCharacterFromDundam(serverId, characterId, characterInfo);
            
            // WebSocket으로 실시간 업데이트 전송
            if ((Boolean) updateResult.get("success")) {
                try {
                    Map<String, Object> wsData = new HashMap<>();
                    wsData.put("characterId", characterId);
                    wsData.put("serverId", serverId);
                    wsData.put("updateType", "dundam_sync");
                    wsData.put("updateResult", updateResult);
                    wsData.put("timestamp", LocalDateTime.now());
                    
                    realtimeEventService.sendEventToTopic("character-updates", 
                        RealtimeEvent.builder()
                            .id(UUID.randomUUID().toString())
                            .type(RealtimeEvent.EventType.CHARACTER_UPDATED)
                            .targetId(characterId)
                            .data(wsData)
                            .timestamp(LocalDateTime.now())
                            .message("던담 동기화가 완료되었습니다.")
                            .broadcast(true)
                            .build()
                    );
                    
                    log.info("WebSocket으로 실시간 업데이트 전송 완료");
                } catch (Exception e) {
                    log.warn("WebSocket 전송 실패: {}", e.getMessage());
                }
            }
            
            log.info("=== 던담 동기화 완료 ===");
            log.info("업데이트 결과: {}", updateResult);
            
            return ResponseEntity.ok(updateResult);
            
        } catch (Exception e) {
            log.error("던담 동기화 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "던담 동기화 중 오류가 발생했습니다."
            );
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 모험단 전체 캐릭터 던담 동기화
     */
    @PostMapping("/adventure/{adventureName}")
    public ResponseEntity<Map<String, Object>> syncAdventureFromDundam(
            @PathVariable String adventureName) {
        
        try {
            log.info("=== 모험단 던담 동기화 시작 ===");
            log.info("모험단: {}", adventureName);
            
            // 모험단의 모든 캐릭터에 대해 던담 동기화 수행
            Map<String, Object> syncResult = characterService.syncAdventureFromDundam(adventureName);
            
            log.info("=== 모험단 던담 동기화 완료 ===");
            log.info("동기화 결과: {}", syncResult);
            
            return ResponseEntity.ok(syncResult);
            
        } catch (Exception e) {
            log.error("모험단 던담 동기화 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "모험단 던담 동기화 중 오류가 발생했습니다."
            );
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
}
