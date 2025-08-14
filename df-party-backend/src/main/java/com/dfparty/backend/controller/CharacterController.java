package com.dfparty.backend.controller;

import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.DungeonClearService;
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
    private DungeonClearService dungeonClearService;

    /**
     * 통합 캐릭터 정보 조회 (DFO API + Dundam 정보)
     */
    @GetMapping("/{serverId}/{characterName}/complete")
    public ResponseEntity<Map<String, Object>> getCompleteCharacterInfo(
            @PathVariable String serverId,
            @PathVariable String characterName) {
        
        try {
            Map<String, Object> result = characterService.getCompleteCharacterInfo(serverId, characterName);
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
            Map<String, Object> error = Map.of(
                "success", false,
                "message", "캐릭터 검색 중 오류가 발생했습니다: " + e.getMessage()
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
     * 모험단별 캐릭터 목록 조회
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
}
