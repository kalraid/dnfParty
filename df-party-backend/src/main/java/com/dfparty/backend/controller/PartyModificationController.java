package com.dfparty.backend.controller;

import com.dfparty.backend.model.Character;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.PartyModificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/party-modification")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartyModificationController {
    
    private final PartyModificationService partyModificationService;
    private final CharacterService characterService;
    
    /**
     * 파티 슬롯 간 캐릭터 교체
     */
    @PostMapping("/swap")
    public ResponseEntity<Map<String, Object>> swapCharacters(@RequestBody Map<String, Object> request) {
        log.info("캐릭터 교체 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            Integer sourceSlot = (Integer) request.get("sourceSlot");
            Integer targetSlot = (Integer) request.get("targetSlot");
            
            if (party == null || sourceSlot == null || targetSlot == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보와 슬롯 번호가 필요합니다."));
            }
            
            // 캐릭터 교체
            Map<String, Object> result = partyModificationService.swapCharacters(party, sourceSlot, targetSlot);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            // 수정 히스토리 저장
            partyModificationService.saveModificationHistory(
                party, 
                "SWAP", 
                "슬롯 " + sourceSlot + " ↔ 슬롯 " + targetSlot + " 캐릭터 교체"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("캐릭터 교체 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "캐릭터 교체에 실패했습니다."));
        }
    }
    
    /**
     * 파티 간 캐릭터 이동
     */
    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> moveCharacterBetweenParties(@RequestBody Map<String, Object> request) {
        log.info("파티 간 캐릭터 이동 요청");
        
        try {
            Map<String, Object> sourceParty = (Map<String, Object>) request.get("sourceParty");
            Map<String, Object> targetParty = (Map<String, Object>) request.get("targetParty");
            Integer sourceSlot = (Integer) request.get("sourceSlot");
            Integer targetSlot = (Integer) request.get("targetSlot");
            
            if (sourceParty == null || targetParty == null || sourceSlot == null || targetSlot == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "소스/타겟 파티와 슬롯 번호가 필요합니다."));
            }
            
            // 캐릭터 이동
            Map<String, Object> result = partyModificationService.moveCharacterBetweenParties(
                sourceParty, targetParty, sourceSlot, targetSlot
            );
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            // 수정 히스토리 저장
            partyModificationService.saveModificationHistory(
                sourceParty, 
                "MOVE_OUT", 
                "슬롯 " + sourceSlot + "에서 캐릭터 이동"
            );
            partyModificationService.saveModificationHistory(
                targetParty, 
                "MOVE_IN", 
                "슬롯 " + targetSlot + "에 캐릭터 이동"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("파티 간 캐릭터 이동 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "캐릭터 이동에 실패했습니다."));
        }
    }
    
    /**
     * 파티에 새 캐릭터 추가
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addCharacterToParty(@RequestBody Map<String, Object> request) {
        log.info("파티에 캐릭터 추가 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            String characterId = (String) request.get("characterId");
            Integer slotNumber = (Integer) request.get("slotNumber");
            
            if (party == null || characterId == null || slotNumber == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보, 캐릭터 ID, 슬롯 번호가 필요합니다."));
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(List.of(characterId));
            if (characters.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "캐릭터를 찾을 수 없습니다."));
            }
            
            Character character = characters.get(0);
            
            // 파티에 캐릭터 추가
            Map<String, Object> result = partyModificationService.addCharacterToParty(party, character, slotNumber);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            // 수정 히스토리 저장
            partyModificationService.saveModificationHistory(
                party, 
                "ADD", 
                character.getCharacterName() + "을(를) 슬롯 " + slotNumber + "에 추가"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("파티에 캐릭터 추가 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "캐릭터 추가에 실패했습니다."));
        }
    }
    
    /**
     * 파티에서 캐릭터 제거
     */
    @PostMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeCharacterFromParty(@RequestBody Map<String, Object> request) {
        log.info("파티에서 캐릭터 제거 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            Integer slotNumber = (Integer) request.get("slotNumber");
            
            if (party == null || slotNumber == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보와 슬롯 번호가 필요합니다."));
            }
            
            // 파티에서 캐릭터 제거
            Map<String, Object> result = partyModificationService.removeCharacterFromParty(party, slotNumber);
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            // 수정 히스토리 저장
            partyModificationService.saveModificationHistory(
                party, 
                "REMOVE", 
                "슬롯 " + slotNumber + "에서 캐릭터 제거"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("파티에서 캐릭터 제거 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "캐릭터 제거에 실패했습니다."));
        }
    }
    
    /**
     * 파티 구성 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePartyComposition(@RequestBody Map<String, Object> request) {
        log.info("파티 구성 검증 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            String dungeonName = (String) request.get("dungeonName");
            
            if (party == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보가 필요합니다."));
            }
            
            // 파티 구성 검증
            Map<String, Object> validation = partyModificationService.validatePartyComposition(party, dungeonName);
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            log.error("파티 구성 검증 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 구성 검증에 실패했습니다."));
        }
    }
    
    /**
     * 파티 구성 최적화 제안
     */
    @PostMapping("/suggest-optimization")
    public ResponseEntity<Map<String, Object>> suggestPartyOptimization(@RequestBody Map<String, Object> request) {
        log.info("파티 구성 최적화 제안 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            String dungeonName = (String) request.get("dungeonName");
            
            if (party == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보가 필요합니다."));
            }
            
            // 파티 구성 검증
            Map<String, Object> validation = partyModificationService.validatePartyComposition(party, dungeonName);
            
            // 최적화 제안 생성
            Map<String, Object> suggestions = generateOptimizationSuggestions(party, validation, dungeonName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("validation", validation);
            result.put("suggestions", suggestions);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("파티 구성 최적화 제안 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "최적화 제안에 실패했습니다."));
        }
    }
    
    /**
     * 파티 수정 실행 (여러 작업을 한 번에)
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executePartyModifications(@RequestBody Map<String, Object> request) {
        log.info("파티 수정 실행 요청");
        
        try {
            Map<String, Object> party = (Map<String, Object>) request.get("party");
            List<Map<String, Object>> modifications = (List<Map<String, Object>>) request.get("modifications");
            
            if (party == null || modifications == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파티 정보와 수정 작업 목록이 필요합니다."));
            }
            
            // 수정 작업들을 순차적으로 실행
            Map<String, Object> currentParty = new HashMap<>(party);
            
            for (Map<String, Object> modification : modifications) {
                String type = (String) modification.get("type");
                
                switch (type) {
                    case "SWAP":
                        currentParty = (Map<String, Object>) partyModificationService.swapCharacters(
                            currentParty, 
                            (Integer) modification.get("sourceSlot"), 
                            (Integer) modification.get("targetSlot")
                        );
                        break;
                        
                    case "ADD":
                        String characterId = (String) modification.get("characterId");
                        List<Character> characters = characterService.getCharactersByIds(List.of(characterId));
                        if (!characters.isEmpty()) {
                            currentParty = (Map<String, Object>) partyModificationService.addCharacterToParty(
                                currentParty, 
                                characters.get(0), 
                                (Integer) modification.get("slotNumber")
                            );
                        }
                        break;
                        
                    case "REMOVE":
                        currentParty = (Map<String, Object>) partyModificationService.removeCharacterFromParty(
                            currentParty, 
                            (Integer) modification.get("slotNumber")
                        );
                        break;
                        
                    default:
                        log.warn("알 수 없는 수정 작업 타입: {}", type);
                        break;
                }
                
                if (currentParty.containsKey("error")) {
                    return ResponseEntity.badRequest().body(currentParty);
                }
            }
            
            // 수정 히스토리 저장
            partyModificationService.saveModificationHistory(
                currentParty, 
                "BATCH_MODIFY", 
                modifications.size() + "개의 수정 작업 실행"
            );
            
            return ResponseEntity.ok(currentParty);
            
        } catch (Exception e) {
            log.error("파티 수정 실행 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "파티 수정 실행에 실패했습니다."));
        }
    }
    
    // 유틸리티 메서드들
    private Map<String, Object> generateOptimizationSuggestions(Map<String, Object> party, Map<String, Object> validation, String dungeonName) {
        Map<String, Object> suggestions = new HashMap<>();
        List<String> suggestionList = new ArrayList<>();
        
        if ((Boolean) validation.get("isValid")) {
            suggestionList.add("현재 파티 구성이 최적화되어 있습니다.");
        } else {
            List<String> errors = (List<String>) validation.get("errors");
            
            if (errors.contains("빈 슬롯이 있습니다.")) {
                suggestionList.add("빈 슬롯을 채워주세요.");
            }
            
            if (errors.stream().anyMatch(error -> error.contains("딜러가 부족합니다"))) {
                suggestionList.add("딜러 캐릭터를 더 추가해주세요.");
            }
            
            if (errors.stream().anyMatch(error -> error.contains("버퍼가 부족합니다"))) {
                suggestionList.add("버퍼 캐릭터를 더 추가해주세요.");
            }
            
            if (errors.contains("중복된 캐릭터가 있습니다.")) {
                suggestionList.add("중복된 캐릭터를 제거하거나 교체해주세요.");
            }
        }
        
        // 던전별 추가 제안
        if (dungeonName != null) {
            switch (dungeonName.toLowerCase()) {
                case "나벨":
                case "navel":
                    suggestionList.add("나벨 던전은 3딜러 + 1버퍼 구성이 권장됩니다.");
                    break;
                    
                case "안개신":
                case "fog":
                    suggestionList.add("안개신 던전은 2딜러 + 2버퍼 구성이 권장됩니다.");
                    break;
            }
        }
        
        suggestions.put("suggestions", suggestionList);
        suggestions.put("priority", determinePriority(validation));
        
        return suggestions;
    }
    
    private String determinePriority(Map<String, Object> validation) {
        List<String> errors = (List<String>) validation.get("errors");
        
        if (errors.isEmpty()) {
            return "LOW";
        } else if (errors.stream().anyMatch(error -> 
            error.contains("빈 슬롯") || error.contains("중복"))) {
            return "HIGH";
        } else {
            return "MEDIUM";
        }
    }
}
