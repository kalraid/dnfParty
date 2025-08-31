package com.dfparty.backend.controller;

import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.RealtimeEventService;
import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RealtimeUpdateController {

    private final CharacterService characterService;
    private final DundamService dundamService;
    private final RealtimeEventService realtimeEventService;
    private final CharacterRepository characterRepository;

    /**
     * 모험단별 캐릭터 실시간 업데이트 (던담 재크롤링)
     */
    @PostMapping("/adventure/{adventureName}/refresh")
    public ResponseEntity<Map<String, Object>> refreshAdventureCharacters(
            @PathVariable String adventureName,
            @RequestBody Map<String, Object> requestBody) {
        
        String userId = (String) requestBody.getOrDefault("userId", "anonymous");
        
        try {
            log.info("모험단 '{}' 실시간 업데이트 시작 (사용자: {})", adventureName, userId);
            
            // 해당 모험단의 캐릭터들 조회
            List<Character> characters = characterRepository.findByAdventure_AdventureName(adventureName);
            
            if (characters.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "해당 모험단의 캐릭터를 찾을 수 없습니다."
                ));
            }
            
            // 시스템 알림 전송 (업데이트 시작)
            realtimeEventService.sendSystemNotification(
                String.format("'%s' 모험단 캐릭터 정보 업데이트를 시작합니다.", adventureName),
                Map.of("adventureName", adventureName, "characterCount", characters.size())
            );
            
            // 비동기로 각 캐릭터 업데이트
            CompletableFuture.runAsync(() -> {
                updateCharactersRealtime(characters, adventureName, userId);
            });
            
            // refresh API는 동기화 시작 신호만 반환 (상세 진행 상황은 SSE로 전송)
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("'%s' 모험단 %d개 캐릭터 동기화를 시작합니다.", adventureName, characters.size()),
                "adventureName", adventureName,
                "characterCount", characters.size(),
                "status", "started",
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("모험단 실시간 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "실시간 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 개별 캐릭터 실시간 업데이트
     */
    @PostMapping("/character/{characterId}/refresh")
    public ResponseEntity<Map<String, Object>> refreshCharacter(
            @PathVariable String characterId,
            @RequestParam(defaultValue = "anonymous") String userId) {
        
        try {
            log.info("캐릭터 '{}' 실시간 업데이트 시작 (사용자: {})", characterId, userId);
            
            Character character = characterRepository.findByCharacterId(characterId)
                .orElse(null);
            
            if (character == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "캐릭터를 찾을 수 없습니다."
                ));
            }
            
            // 비동기로 캐릭터 업데이트
            CompletableFuture.runAsync(() -> {
                updateSingleCharacterRealtime(character, userId);
            });
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "캐릭터 업데이트가 시작되었습니다.",
                "characterId", characterId,
                "characterName", character.getCharacterName()
            ));
            
        } catch (Exception e) {
            log.error("캐릭터 실시간 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "캐릭터 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 모든 캐릭터 일괄 업데이트
     */
    @PostMapping("/all/refresh")
    public ResponseEntity<Map<String, Object>> refreshAllCharacters(
            @RequestParam(defaultValue = "anonymous") String userId) {
        
        try {
            log.info("모든 캐릭터 실시간 업데이트 시작 (사용자: {})", userId);
            
            List<Character> allCharacters = characterRepository.findAll();
            
            if (allCharacters.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "업데이트할 캐릭터가 없습니다."
                ));
            }
            
            // 시스템 알림 전송 (업데이트 시작)
            realtimeEventService.sendSystemNotification(
                String.format("전체 %d명의 캐릭터 정보 업데이트를 시작합니다.", allCharacters.size()),
                Map.of("characterCount", allCharacters.size())
            );
            
            // 비동기로 모든 캐릭터 업데이트
            CompletableFuture.runAsync(() -> {
                updateCharactersRealtime(allCharacters, "전체", userId);
            });
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 캐릭터 업데이트가 시작되었습니다.",
                "characterCount", allCharacters.size()
            ));
            
        } catch (Exception e) {
            log.error("전체 캐릭터 실시간 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "전체 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 비동기로 캐릭터들 업데이트 실행
     */
    private void updateCharactersRealtime(List<Character> characters, String targetName, String userId) {
        int totalCount = characters.size();
        int currentCount = 0;
        int successCount = 0;
        int failureCount = 0;
        
        for (Character character : characters) {
            currentCount++;
            
            try {
                // 진행률 알림
                if (currentCount % 5 == 0 || currentCount == totalCount) {
                    int progressPercent = (int) ((double) currentCount / totalCount * 100);
                    realtimeEventService.sendSystemNotification(
                        String.format("'%s' 업데이트 진행률: %d%% (%d/%d)", targetName, progressPercent, currentCount, totalCount),
                        Map.of(
                            "targetName", targetName,
                            "progress", progressPercent,
                            "current", currentCount,
                            "total", totalCount
                        )
                    );
                }
                
                // 던담에서 최신 정보 크롤링
                Map<String, Object> dundamInfo = dundamService.getCharacterInfo(
                    character.getServerId(), 
                    character.getCharacterId()
                );
                
                if (dundamInfo != null && Boolean.TRUE.equals(dundamInfo.get("success"))) {
                    // 캐릭터 정보 업데이트
                    Long buffPower = (Long) dundamInfo.get("buffPower");
                    Long totalDamage = (Long) dundamInfo.get("totalDamage");
                    
                    if (buffPower != null && buffPower > 0) {
                        character.setBuffPower(buffPower);
                    }
                    if (totalDamage != null && totalDamage > 0) {
                        character.setTotalDamage(totalDamage);
                    }
                    
                    characterRepository.save(character);
                    successCount++;
                    
                    // 개별 캐릭터 업데이트 알림
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("characterId", character.getCharacterId());
                    updateData.put("characterName", character.getCharacterName());
                    updateData.put("adventureName", character.getAdventureName());
                    updateData.put("buffPower", buffPower);
                    updateData.put("totalDamage", totalDamage);
                    updateData.put("progress", (int) ((double) currentCount / totalCount * 100));
                    
                    realtimeEventService.notifyCharacterUpdated(
                        character.getCharacterId(), 
                        userId, 
                        updateData
                    );
                    
                    log.info("캐릭터 업데이트 완료: {} (버프력: {}, 전투력: {})", 
                        character.getCharacterName(), buffPower, totalDamage);
                } else {
                    failureCount++;
                    log.warn("캐릭터 업데이트 실패: {} - 던담 정보 조회 실패", character.getCharacterName());
                }
                
                // API 호출 간격 조절 (과부하 방지)
                Thread.sleep(2000);
                
            } catch (Exception e) {
                failureCount++;
                log.error("캐릭터 업데이트 중 오류: {} - {}", character.getCharacterName(), e.getMessage());
            }
        }
        
        // 최종 완료 알림
        realtimeEventService.sendSystemNotification(
            String.format("'%s' 업데이트 완료! 성공: %d명, 실패: %d명", targetName, successCount, failureCount),
            Map.of(
                "targetName", targetName,
                "total", totalCount,
                "success", successCount,
                "failure", failureCount,
                "completed", true
            )
        );
        
        log.info("'{}' 캐릭터 업데이트 완료: 총 {}명, 성공 {}명, 실패 {}명", 
            targetName, totalCount, successCount, failureCount);
    }

    /**
     * 단일 캐릭터 업데이트 실행
     */
    private void updateSingleCharacterRealtime(Character character, String userId) {
        try {
            // 던담에서 최신 정보 크롤링
            Map<String, Object> dundamInfo = dundamService.getCharacterInfo(
                character.getServerId(), 
                character.getCharacterId()
            );
            
            if (dundamInfo != null && Boolean.TRUE.equals(dundamInfo.get("success"))) {
                // 캐릭터 정보 업데이트
                Long buffPower = (Long) dundamInfo.get("buffPower");
                Long totalDamage = (Long) dundamInfo.get("totalDamage");
                
                if (buffPower != null && buffPower > 0) {
                    character.setBuffPower(buffPower);
                }
                if (totalDamage != null && totalDamage > 0) {
                    character.setTotalDamage(totalDamage);
                }
                
                characterRepository.save(character);
                
                // 업데이트 완료 알림
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("characterId", character.getCharacterId());
                updateData.put("characterName", character.getCharacterName());
                updateData.put("adventureName", character.getAdventureName());
                updateData.put("buffPower", buffPower);
                updateData.put("totalDamage", totalDamage);
                updateData.put("completed", true);
                
                realtimeEventService.notifyCharacterUpdated(
                    character.getCharacterId(), 
                    userId, 
                    updateData
                );
                
                log.info("캐릭터 업데이트 완료: {} (버프력: {}, 전투력: {})", 
                    character.getCharacterName(), buffPower, totalDamage);
            } else {
                log.warn("캐릭터 업데이트 실패: {} - 던담 정보 조회 실패", character.getCharacterName());
            }
            
        } catch (Exception e) {
            log.error("캐릭터 업데이트 중 오류: {} - {}", character.getCharacterName(), e.getMessage());
        }
    }
}
