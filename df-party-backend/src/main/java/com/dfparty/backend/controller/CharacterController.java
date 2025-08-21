package com.dfparty.backend.controller;

import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.DungeonClearService;
import com.dfparty.backend.dto.ManualStatsUpdateDto;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private DundamService dundamService;

    @Autowired
    private DungeonClearService dungeonClearService;

    /**
     * 통합 캐릭터 정보 조회 (DFO API + Dundam 정보)
     */
    @GetMapping("/{serverId}/{characterName}/complete")
    public ResponseEntity<Map<String, Object>> getCompleteCharacterInfo(
            @PathVariable String serverId,
            @PathVariable String characterName) {
        
        System.out.println("=== API 호출: getCompleteCharacterInfo ===");
        System.out.println("서버 ID: " + serverId);
        System.out.println("캐릭터명: " + characterName);
        
        try {
            System.out.println("서비스 호출 시작...");
            Map<String, Object> result = characterService.getCompleteCharacterInfo(serverId, characterName);
            System.out.println("서비스 호출 성공, 결과 반환");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.out.println("=== API 에러 발생 ===");
            System.out.println("에러 타입: " + e.getClass().getName());
            System.out.println("에러 메시지: " + e.getMessage());
            System.out.println("에러 원인: " + (e.getCause() != null ? e.getCause().getMessage() : "원인 없음"));
            
            // 스택 트레이스 출력
            System.out.println("스택 트레이스:");
            e.printStackTrace();
            
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 모험단별 캐릭터 조회
     */
    @GetMapping("/adventure/{adventureName}")
    public ResponseEntity<Map<String, Object>> getCharactersByAdventure(
            @PathVariable String adventureName) {
        try {
            Map<String, Object> result = characterService.getCharactersByAdventure(adventureName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "모험단별 캐릭터 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 모험단 목록 조회
     */
    @GetMapping("/adventures")
    public ResponseEntity<Map<String, Object>> getAllAdventures() {
        try {
            Map<String, Object> result = characterService.getAllAdventures();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "모험단 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 던전별 업둥이 설정
     */
    @PostMapping("/{characterId}/favorite/{dungeonType}")
    public ResponseEntity<Map<String, Object>> setDungeonFavorite(
            @PathVariable String characterId,
            @PathVariable String dungeonType,
            @RequestParam boolean isFavorite) {
        try {
            Map<String, Object> result = characterService.setDungeonFavorite(characterId, dungeonType, isFavorite);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "업둥이 설정 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 하드 나벨 대상자 여부 업데이트
     */
    @PostMapping("/{characterId}/hard-nabel-eligibility")
    public ResponseEntity<Map<String, Object>> updateHardNabelEligibility(
            @PathVariable String characterId) {
        try {
            Map<String, Object> result = characterService.updateHardNabelEligibility(characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "하드 나벨 대상자 여부 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 수동 스탯 입력
     */
    @PostMapping("/{characterId}/manual-stats")
    public ResponseEntity<Map<String, Object>> updateManualStats(
            @PathVariable String characterId,
            @RequestBody ManualStatsUpdateDto manualStatsDto) {
        try {
            Map<String, Object> result = characterService.updateManualStats(characterId, manualStatsDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "수동 스탯 입력 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터의 수동/동기화 스탯 조회
     */
    @GetMapping("/{characterId}/stats")
    public ResponseEntity<Map<String, Object>> getCharacterStats(@PathVariable String characterId) {
        try {
            Map<String, Object> result = characterService.getCharacterStats(characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 스탯 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 던전별 업둥이 상태 조회
     */
    @GetMapping("/{characterId}/favorites")
    public ResponseEntity<Map<String, Object>> getDungeonFavorites(
            @PathVariable String characterId) {
        try {
            Map<String, Object> result = characterService.getDungeonFavorites(characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "업둥이 상태 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 셀레니움으로 CSS 셀렉터 찾기 (디버깅용)
     */


    /**
     * 빠른 Playwright 크롤링 (CSS 셀렉터 기반)
     */
    @PostMapping("/{characterId}/dundam-playwright-fast")
    public ResponseEntity<Map<String, Object>> getCharacterInfoWithPlaywrightFast(
            @PathVariable String characterId,
            @RequestParam String serverId) {
        try {
            Map<String, Object> result = dundamService.getCharacterInfoWithPlaywrightFast(serverId, characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "빠른 크롤링 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }



    /**
     * Playwright를 사용한 Dundam 크롤링 테스트
     */
    @PostMapping("/{characterId}/dundam-playwright")
    public ResponseEntity<Map<String, Object>> testDundamPlaywright(
            @PathVariable String characterId,
            @RequestParam String serverId) {
        try {
            Map<String, Object> result = dundamService.getCharacterInfoWithPlaywrightFast(serverId, characterId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "Playwright 크롤링 테스트 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터 검색 (이름으로 검색)
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCharacters(
            @RequestParam String characterName,
            @RequestParam(required = false, defaultValue = "all") String serverId) {
        
        try {
            Map<String, Object> result = characterService.searchCharacters(characterName, serverId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // 서버 로그에 상세한 에러 정보 기록
            System.err.println("=== CharacterController 에러 ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Stack Trace: " + e.getStackTrace()[0]);
            System.err.println("=============================");
            
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 검색에 실패했습니다."
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터 정보 저장
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveCharacter(@RequestBody Map<String, Object> characterData) {
        try {
            Map<String, Object> result = characterService.saveCharacter(characterData);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 저장 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 개별 캐릭터 정보 조회
     */
    @GetMapping("/{serverId}/{characterId}")
    public ResponseEntity<Map<String, Object>> getCharacter(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        
        try {
            Map<String, Object> result = characterService.getCharacter(serverId, characterId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }



    /**
     * 캐릭터 정보 새로고침
     */
    @GetMapping("/{serverId}/{characterId}/refresh")
    public ResponseEntity<Map<String, Object>> refreshCharacter(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        
        try {
            Map<String, Object> result = characterService.refreshCharacter(serverId, characterId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 정보 새로고침 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터 삭제
     */
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacter(@PathVariable String characterId) {
        try {
            Map<String, Object> result = characterService.deleteCharacter(characterId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 삭제 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터 타임라인 조회
     */
    @GetMapping("/{serverId}/{characterId}/timeline")
    public ResponseEntity<Map<String, Object>> getCharacterTimeline(
            @PathVariable String serverId,
            @PathVariable String characterId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            Map<String, Object> result = characterService.getCharacterTimeline(
                serverId, characterId, limit, startDate, endDate);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "타임라인 조회 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 캐릭터 스펙 정보 업데이트 (Dundam)
     */
    @GetMapping("/{serverId}/{characterId}/update-stats")
    public ResponseEntity<Map<String, Object>> updateCharacterStats(
            @PathVariable String serverId,
            @PathVariable String characterId) {
        
        try {
            Map<String, Object> result = characterService.updateCharacterStats(serverId, characterId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 스펙 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PatchMapping("/{serverId}/{characterId}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable String serverId,
            @PathVariable String characterId,
            @RequestBody Map<String, Object> request) {
        
        try {
            Boolean isFavorite = (Boolean) request.get("isFavorite");
            Map<String, Object> result = characterService.toggleFavorite(serverId, characterId, isFavorite);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "에러  " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PatchMapping("/{serverId}/{characterId}/exclude")
    public ResponseEntity<Map<String, Object>> setDungeonExclusion(
            @PathVariable String serverId,
            @PathVariable String characterId,
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<String> excludedDungeons = (java.util.List<String>) request.get("excludedDungeons");
            Map<String, Object> result = characterService.setDungeonExclusion(serverId, characterId, excludedDungeons);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "에러: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 던전별 안감 상태 업데이트
     */
    @PatchMapping("/{characterId}/exclude-dungeon")
    public ResponseEntity<Map<String, Object>> updateDungeonExcludeStatus(
            @PathVariable String characterId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String dungeonType = (String) request.get("dungeonType"); // "nabel", "venus", "fog"
            Boolean isExcluded = (Boolean) request.get("isExcluded");
            
            Map<String, Object> result = characterService.updateDungeonExcludeStatus(characterId, dungeonType, isExcluded);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "던전 안감 상태 업데이트 실패: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 던전별 업둥 상태 업데이트
     */
    @PatchMapping("/{characterId}/skip-dungeon")
    public ResponseEntity<Map<String, Object>> updateDungeonSkipStatus(
            @PathVariable String characterId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String dungeonType = (String) request.get("dungeonType"); // "nabel", "venus", "fog"
            Boolean isSkip = (Boolean) request.get("isSkip");
            
            Map<String, Object> result = characterService.updateDungeonSkipStatus(characterId, dungeonType, isSkip);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "던전 업둥 상태 업데이트 실패: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 나벨 난이도 선택 저장
     */
    @PostMapping("/{characterId}/nabel-difficulty")
    public ResponseEntity<Map<String, Object>> saveNabelDifficulty(
            @PathVariable String characterId,
            @RequestParam String difficulty) {
        try {
            Map<String, Object> result = characterService.saveNabelDifficultySelection(characterId, difficulty);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "나벨 난이도 선택 저장 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
    /**
     * 황혼전 대상자 여부 업데이트
     */
    @PostMapping("/{characterId}/twilight-eligibility")
    public ResponseEntity<Map<String, Object>> updateTwilightEligibility(
            @PathVariable String characterId) {
        try {
            // Map<String, Object> result = characterService.updateTwilightEligibility(characterId);
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "황혼전 대상자 여부 업데이트 완료"
            );  
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "황혼전 대상자 여부 업데이트 중 오류가 발생했습니다: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }


    /**
     * 모험단 전체 캐릭터 최신화
     */
    @PostMapping("/adventure/{adventureName}/refresh")
    public ResponseEntity<Map<String, Object>> refreshAdventureCharacters(
            @PathVariable String adventureName) {
        
        try {
            Map<String, Object> result = characterService.refreshAdventureCharacters(adventureName);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "모험단 전체 최신화 실패: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
