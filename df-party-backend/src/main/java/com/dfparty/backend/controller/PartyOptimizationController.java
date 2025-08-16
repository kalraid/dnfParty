package com.dfparty.backend.controller;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.PartyOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/party-optimization")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartyOptimizationController {
    
    private final PartyOptimizationService partyOptimizationService;
    private final CharacterService characterService;
    
    /**
     * 업둥이 우선 파티 구성
     */
    @PostMapping("/updoongi-priority")
    public ResponseEntity<Map<String, Object>> createUpdoongiPriorityParty(@RequestBody Map<String, Object> request) {
        log.info("업둥이 우선 파티 구성 요청");
        
        try {
            Integer partySize = (Integer) request.get("partySize");
            List<String> characterIds = (List<String>) request.get("characterIds");
            
            if (partySize == null || characterIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 크기와 캐릭터 ID 목록이 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            if (characters.size() < partySize) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 구성에 필요한 캐릭터가 부족합니다."));
            }
            
            // 업둥이 우선 파티 구성
            Map<String, Object> result = partyOptimizationService.createUpdoongiPriorityParty(characters, partySize);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("업둥이 우선 파티 구성 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 구성에 실패했습니다."));
        }
    }
    
    /**
     * 평균 파티 구성 (밸런스 최적화)
     */
    @PostMapping("/balanced")
    public ResponseEntity<Map<String, Object>> createBalancedParty(@RequestBody Map<String, Object> request) {
        log.info("평균 파티 구성 요청");
        
        try {
            Integer partySize = (Integer) request.get("partySize");
            List<String> characterIds = (List<String>) request.get("characterIds");
            
            if (partySize == null || characterIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 크기와 캐릭터 ID 목록이 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            if (characters.size() < partySize) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 구성에 필요한 캐릭터가 부족합니다."));
            }
            
            // 평균 파티 구성
            Map<String, Object> result = partyOptimizationService.createBalancedParty(characters, partySize);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("평균 파티 구성 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 구성에 실패했습니다."));
        }
    }
    
    /**
     * 파티 최적화 메인 엔드포인트
     * type: "balanced", "updoongi", "nabel"
     * dungeonType: "nabel", "venus", "fog" (nabel 타입일 때)
     * difficulty: "normal", "hard" (nabel 타입일 때)
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeParty(@RequestBody Map<String, Object> request) {
        log.info("파티 최적화 요청: {}", request);
        
        try {
            Map<String, Object> result = partyOptimizationService.optimizeParty(request);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("파티 최적화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 최적화에 실패했습니다."));
        }
    }
    
    /**
     * 파티 구성 분석
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeParty(@RequestBody Map<String, Object> request) {
        log.info("파티 구성 분석 요청");
        
        try {
            List<String> characterIds = (List<String>) request.get("characterIds");
            
            if (characterIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "캐릭터 ID 목록이 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            // 파티 분석
            Map<String, Object> analysis = analyzePartyComposition(characters);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("파티 구성 분석 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 분석에 실패했습니다."));
        }
    }
    
    /**
     * 파티 구성 추천
     */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommendParty(@RequestBody Map<String, Object> request) {
        log.info("파티 구성 추천 요청");
        
        try {
            Integer partySize = (Integer) request.get("partySize");
            String dungeonName = (String) request.get("dungeonName");
            List<String> characterIds = (List<String>) request.get("characterIds");
            
            if (partySize == null || characterIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 크기와 캐릭터 ID 목록이 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            if (characters.size() < partySize) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 구성에 필요한 캐릭터가 부족합니다."));
            }
            
            // 던전별 파티 구성 추천
            Map<String, Object> recommendation = recommendPartyByDungeon(characters, partySize, dungeonName);
            
            return ResponseEntity.ok(recommendation);
            
        } catch (Exception e) {
            log.error("파티 구성 추천 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 추천에 실패했습니다."));
        }
    }
    
    // 유틸리티 메서드들
    private Map<String, Object> analyzePartyComposition(List<Character> characters) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 캐릭터 분류
        long dealerCount = characters.stream()
            .filter(c -> c.getJobName() != null && !c.getJobName().contains("버퍼"))
            .count();
        
        long bufferCount = characters.stream()
            .filter(c -> c.getJobName() != null && c.getJobName().contains("버퍼"))
            .count();
        
        long updoongiCount = characters.stream()
            .filter(c -> c.getIsFavorite() != null && c.getIsFavorite())
            .count();
        
        // 평균 명성
        double avgFame = characters.stream()
            .filter(c -> c.getFame() != null)
            .mapToDouble(Character::getFame)
            .average()
            .orElse(0.0);
        
        // 최고 명성
        double maxFame = characters.stream()
            .filter(c -> c.getFame() != null)
            .mapToDouble(Character::getFame)
            .max()
            .orElse(0.0);
        
        analysis.put("totalCharacters", characters.size());
        analysis.put("dealerCount", dealerCount);
        analysis.put("bufferCount", bufferCount);
        analysis.put("updoongiCount", updoongiCount);
        analysis.put("averageFame", avgFame);
        analysis.put("maxFame", maxFame);
        analysis.put("recommendedPartySize", recommendPartySize(characters));
        
        return analysis;
    }
    
    private Map<String, Object> recommendPartyByDungeon(List<Character> characters, int partySize, String dungeonName) {
        Map<String, Object> recommendation = new HashMap<>();
        
        // 던전별 권장 파티 구성
        switch (dungeonName != null ? dungeonName.toLowerCase() : "") {
            case "나벨":
            case "navel":
                recommendation.put("recommendedComposition", "3딜러 + 1버퍼");
                recommendation.put("minFame", 63000);
                recommendation.put("difficulty", "하드/일반");
                break;
                
            case "베누스":
            case "venus":
                recommendation.put("recommendedComposition", "3딜러 + 1버퍼");
                recommendation.put("minFame", 41929);
                recommendation.put("difficulty", "일반");
                break;
                
            case "안개신":
            case "fog":
                recommendation.put("recommendedComposition", "2딜러 + 2버퍼");
                recommendation.put("minFame", 32253);
                recommendation.put("difficulty", "일반");
                break;
                
            default:
                recommendation.put("recommendedComposition", "3딜러 + 1버퍼");
                recommendation.put("minFame", 30000);
                recommendation.put("difficulty", "일반");
                break;
        }
        
        // 파티 구성 추천
        if (partySize == 4) {
            recommendation.put("updoongiPriority", partyOptimizationService.createUpdoongiPriorityParty(characters, 4));
            recommendation.put("balanced", partyOptimizationService.createBalancedParty(characters, 4));
        } else if (partySize == 8) {
            recommendation.put("updoongiPriority", partyOptimizationService.createUpdoongiPriorityParty(characters, 8));
            recommendation.put("balanced", partyOptimizationService.createBalancedParty(characters, 8));
        }
        
        return recommendation;
    }
    
    private int recommendPartySize(List<Character> characters) {
        int totalCharacters = characters.size();
        
        if (totalCharacters >= 8) {
            return 8;
        } else if (totalCharacters >= 4) {
            return 4;
        } else {
            return totalCharacters;
        }
    }
}
