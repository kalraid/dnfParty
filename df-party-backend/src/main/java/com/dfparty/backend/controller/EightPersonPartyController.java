package com.dfparty.backend.controller;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.EightPersonPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/eight-person-party")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EightPersonPartyController {
    
    private final EightPersonPartyService eightPersonPartyService;
    private final CharacterService characterService;
    
    /**
     * 8인 파티 구성 생성
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createEightPersonParty(@RequestBody Map<String, Object> request) {
        log.info("8인 파티 구성 요청");
        
        try {
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            
            if (characterIds == null || characterIds.size() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "8인 파티 구성에는 최소 8명의 캐릭터가 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            if (characters.size() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효한 캐릭터가 8명 미만입니다."));
            }
            
            // 8인 파티 구성
            Map<String, Object> result = eightPersonPartyService.createEightPersonParty(characters, dungeonName);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            log.info("8인 파티 구성 완료");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("8인 파티 구성 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "8인 파티 구성에 실패했습니다."));
        }
    }
    
    /**
     * 8인 파티 구성 분석
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeEightPersonParty(@RequestBody Map<String, Object> request) {
        log.info("8인 파티 분석 요청");
        
        try {
            Map<String, Object> party1 = (Map<String, Object>) request.get("party1");
            Map<String, Object> party2 = (Map<String, Object>) request.get("party2");
            
            if (party1 == null || party2 == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티1과 파티2 정보가 필요합니다."));
            }
            
            // 파티 분석
            Map<String, Object> analysis = analyzeEightPersonPartyComposition(party1, party2);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            log.error("8인 파티 분석 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "8인 파티 분석에 실패했습니다."));
        }
    }
    
    /**
     * 8인 파티 최적화 제안
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeEightPersonParty(@RequestBody Map<String, Object> request) {
        log.info("8인 파티 최적화 요청");
        
        try {
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            Map<String, Object> currentParty = (Map<String, Object>) request.get("currentParty");
            
            if (characterIds == null || characterIds.size() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "8인 파티 최적화에는 최소 8명의 캐릭터가 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            
            if (characters.size() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "유효한 캐릭터가 8명 미만입니다."));
            }
            
            // 새로운 8인 파티 구성
            Map<String, Object> newParty = eightPersonPartyService.createEightPersonParty(characters, dungeonName);
            
            if (newParty.containsKey("error")) {
                return ResponseEntity.badRequest().body(newParty);
            }
            
            // 최적화 제안 생성
            Map<String, Object> optimization = generateOptimizationSuggestions(currentParty, newParty);
            
            Map<String, Object> result = new HashMap<>();
            result.put("newParty", newParty);
            result.put("optimization", optimization);
            
            log.info("8인 파티 최적화 완료");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("8인 파티 최적화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "8인 파티 최적화에 실패했습니다."));
        }
    }
    
    /**
     * 8인 파티 구성 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateEightPersonParty(@RequestBody Map<String, Object> request) {
        log.info("8인 파티 구성 검증 요청");
        
        try {
            List<Map<String, Object>> party1 = (List<Map<String, Object>>) request.get("party1");
            List<Map<String, Object>> party2 = (List<Map<String, Object>>) request.get("party2");
            String dungeonName = (String) request.get("dungeonName");
            
            if (party1 == null || party2 == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티1과 파티2 정보가 필요합니다."));
            }
            
            // 8인 파티 구성 검증
            Map<String, Object> validation = validateEightPersonPartyComposition(party1, party2, dungeonName);
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            log.error("8인 파티 구성 검증 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "8인 파티 구성 검증에 실패했습니다."));
        }
    }
    
    /**
     * 8인 파티 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEightPersonPartyStats() {
        log.info("8인 파티 통계 조회 요청");
        
        try {
            // TODO: 8인 파티 통계 데이터 구현
            Map<String, Object> stats = Map.of(
                "totalParties", 0,
                "averageEfficiency", 0.0,
                "mostUsedDungeon", "없음",
                "successRate", 0.0
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("8인 파티 통계 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "8인 파티 통계 조회에 실패했습니다."));
        }
    }
    
    // 유틸리티 메서드들
    private Map<String, Object> analyzeEightPersonPartyComposition(Map<String, Object> party1, Map<String, Object> party2) {
        Map<String, Object> analysis = new HashMap<>();
        
        List<Map<String, Object>> slots1 = (List<Map<String, Object>>) party1.get("slots");
        List<Map<String, Object>> slots2 = (List<Map<String, Object>>) party2.get("slots");
        
        // 전체 통계
        List<Map<String, Object>> allSlots = new ArrayList<>();
        allSlots.addAll(slots1);
        allSlots.addAll(slots2);
        
        long totalDealers = allSlots.stream()
            .filter(slot -> "딜러".equals(slot.get("role")))
            .count();
        
        long totalBuffers = allSlots.stream()
            .filter(slot -> "버퍼".equals(slot.get("role")))
            .count();
        
        long totalUpdoongis = allSlots.stream()
            .filter(slot -> Boolean.TRUE.equals(slot.get("isFavorite")))
            .count();
        
        double totalFame = allSlots.stream()
            .mapToDouble(slot -> (Double) slot.get("fame"))
            .sum();
        
        double avgFame = totalFame / allSlots.size();
        
        // 파티별 효율성
        double party1Efficiency = (Double) party1.get("efficiency");
        double party2Efficiency = (Double) party2.get("efficiency");
        double efficiencyGap = Math.abs(party1Efficiency - party2Efficiency);
        
        analysis.put("totalDealers", totalDealers);
        analysis.put("totalBuffers", totalBuffers);
        analysis.put("totalUpdoongis", totalUpdoongis);
        analysis.put("totalFame", totalFame);
        analysis.put("averageFame", avgFame);
        analysis.put("party1Efficiency", party1Efficiency);
        analysis.put("party2Efficiency", party2Efficiency);
        analysis.put("efficiencyGap", efficiencyGap);
        analysis.put("isBalanced", efficiencyGap < 1000); // 효율성 차이가 1000 미만이면 밸런스됨
        
        return analysis;
    }
    
    private Map<String, Object> generateOptimizationSuggestions(Map<String, Object> currentParty, Map<String, Object> newParty) {
        Map<String, Object> optimization = new HashMap<>();
        List<String> suggestions = new ArrayList<>();
        
        if (currentParty == null) {
            suggestions.add("새로운 8인 파티가 구성되었습니다.");
            optimization.put("suggestions", suggestions);
            optimization.put("improvement", "NEW");
            return optimization;
        }
        
        // 현재 파티와 새 파티 비교
        double currentEfficiency = (Double) currentParty.get("totalEfficiency");
        double newEfficiency = (Double) newParty.get("totalEfficiency");
        
        if (newEfficiency > currentEfficiency) {
            double improvement = ((newEfficiency - currentEfficiency) / currentEfficiency) * 100;
            suggestions.add(String.format("파티 효율성이 %.1f%% 향상되었습니다.", improvement));
            suggestions.add("새로운 파티 구성을 권장합니다.");
            optimization.put("improvement", "BETTER");
        } else if (newEfficiency < currentEfficiency) {
            double decline = ((currentEfficiency - newEfficiency) / currentEfficiency) * 100;
            suggestions.add(String.format("파티 효율성이 %.1f%% 감소했습니다.", decline));
            suggestions.add("현재 파티 구성을 유지하는 것을 권장합니다.");
            optimization.put("improvement", "WORSE");
        } else {
            suggestions.add("파티 효율성이 동일합니다.");
            suggestions.add("현재 파티 구성을 유지하는 것을 권장합니다.");
            optimization.put("improvement", "SAME");
        }
        
        // 추가 최적화 제안
        Map<String, Object> analysis = (Map<String, Object>) newParty.get("analysis");
        boolean isBalanced = (Boolean) analysis.get("isBalanced");
        
        if (!isBalanced) {
            suggestions.add("파티 간 효율성 차이가 큽니다. 밸런스를 맞추는 것을 권장합니다.");
        }
        
        optimization.put("suggestions", suggestions);
        optimization.put("currentEfficiency", currentEfficiency);
        optimization.put("newEfficiency", newEfficiency);
        
        return optimization;
    }
    
    private Map<String, Object> validateEightPersonPartyComposition(List<Map<String, Object>> party1, List<Map<String, Object>> party2, String dungeonName) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        List<Map<String, Object>> slots1 = party1;
        List<Map<String, Object>> slots2 = party2;
        
        // 1. 빈 슬롯 체크
        long emptySlots1 = slots1.stream()
            .filter(slot -> slot.get("characterId") == null)
            .count();
        
        long emptySlots2 = slots2.stream()
            .filter(slot -> slot.get("characterId") == null)
            .count();
        
        if (emptySlots1 > 0) {
            errors.add("1파티에 빈 슬롯이 " + emptySlots1 + "개 있습니다.");
        }
        
        if (emptySlots2 > 0) {
            errors.add("2파티에 빈 슬롯이 " + emptySlots2 + "개 있습니다.");
        }
        
        // 2. 직업 구성 체크
        long dealers1 = slots1.stream()
            .filter(slot -> "딜러".equals(slot.get("role")))
            .count();
        
        long buffers1 = slots1.stream()
            .filter(slot -> "버퍼".equals(slot.get("role")))
            .count();
        
        long dealers2 = slots2.stream()
            .filter(slot -> "딜러".equals(slot.get("role")))
            .count();
        
        long buffers2 = slots2.stream()
            .filter(slot -> "버퍼".equals(slot.get("role")))
            .count();
        
        if (dealers1 < 3) {
            errors.add("1파티에 딜러가 부족합니다. (필요: 3명, 현재: " + dealers1 + "명)");
        }
        
        if (buffers1 < 1) {
            errors.add("1파티에 버퍼가 부족합니다. (필요: 1명, 현재: " + buffers1 + "명)");
        }
        
        if (dealers2 < 3) {
            errors.add("2파티에 딜러가 부족합니다. (필요: 3명, 현재: " + dealers2 + "명)");
        }
        
        if (buffers2 < 1) {
            errors.add("2파티에 버퍼가 부족합니다. (필요: 1명, 현재: " + buffers2 + "명)");
        }
        
        // 3. 중복 캐릭터 체크
        List<String> allCharacterIds = new ArrayList<>();
        slots1.stream()
            .filter(slot -> slot.get("characterId") != null)
            .forEach(slot -> allCharacterIds.add((String) slot.get("characterId")));
        
        slots2.stream()
            .filter(slot -> slot.get("characterId") != null)
            .forEach(slot -> allCharacterIds.add((String) slot.get("characterId")));
        
        Set<String> uniqueCharacterIds = new HashSet<>(allCharacterIds);
        
        if (allCharacterIds.size() > uniqueCharacterIds.size()) {
            errors.add("중복된 캐릭터가 있습니다.");
        }
        
        // 4. 던전별 요구사항 체크
        Map<String, Object> dungeonRequirements = getDungeonRequirements(dungeonName);
        long minFame = (Long) dungeonRequirements.get("minFame");
        
        List<Map<String, Object>> allSlots = new ArrayList<>();
        allSlots.addAll(slots1);
        allSlots.addAll(slots2);
        
        for (Map<String, Object> slot : allSlots) {
            if (slot.get("characterId") != null) {
                Double fame = (Double) slot.get("fame");
                if (fame != null && fame < minFame) {
                    warnings.add(slot.get("characterName") + "의 명성이 " + minFame + " 미만입니다.");
                }
            }
        }
        
        validation.put("isValid", errors.isEmpty());
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        validation.put("totalEmptySlots", emptySlots1 + emptySlots2);
        validation.put("totalCharacters", allCharacterIds.size());
        
        return validation;
    }
    
    private Map<String, Object> getDungeonRequirements(String dungeonName) {
        Map<String, Object> requirements = new HashMap<>();
        
        switch (dungeonName != null ? dungeonName.toLowerCase() : "") {
            case "나벨":
            case "navel":
                requirements.put("minFame", 63000L);
                requirements.put("minDealers", 3);
                requirements.put("minBuffers", 1);
                break;
                
            case "베누스":
            case "venus":
                requirements.put("minFame", 41929L);
                requirements.put("minDealers", 3);
                requirements.put("minBuffers", 1);
                break;
                
            case "안개신":
            case "fog":
                requirements.put("minFame", 32253L);
                requirements.put("minDealers", 2);
                requirements.put("minBuffers", 2);
                break;
                
            default:
                requirements.put("minFame", 30000L);
                requirements.put("minDealers", 3);
                requirements.put("minBuffers", 1);
                break;
        }
        
        return requirements;
    }
}
