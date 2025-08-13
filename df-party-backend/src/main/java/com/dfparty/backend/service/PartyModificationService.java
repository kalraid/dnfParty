package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyModificationService {
    
    private final RealtimeEventService realtimeEventService;
    
    /**
     * 파티 슬롯 간 캐릭터 교체
     */
    public Map<String, Object> swapCharacters(Map<String, Object> party, int sourceSlot, int targetSlot) {
        log.info("캐릭터 교체: 슬롯 {} ↔ 슬롯 {}", sourceSlot, targetSlot);
        
        try {
            List<Map<String, Object>> slots = (List<Map<String, Object>>) party.get("slots");
            
            if (slots == null || sourceSlot < 0 || sourceSlot >= slots.size() || 
                targetSlot < 0 || targetSlot >= slots.size()) {
                return Map.of("error", "유효하지 않은 슬롯 번호입니다.");
            }
            
            // 캐릭터 교체
            Map<String, Object> temp = slots.get(sourceSlot);
            slots.set(sourceSlot, slots.get(targetSlot));
            slots.set(targetSlot, temp);
            
            // 슬롯 번호 업데이트
            updateSlotNumbers(slots);
            
            // 파티 효율성 재계산
            double efficiency = calculatePartyEfficiency(slots);
            party.put("efficiency", efficiency);
            
            // 실시간 이벤트 전송
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("partyId", party.get("id"));
            eventData.put("sourceSlot", sourceSlot);
            eventData.put("targetSlot", targetSlot);
            eventData.put("efficiency", efficiency);
            realtimeEventService.notifyPartyUpdated((String) party.get("id"), "system", eventData);
            
            log.info("캐릭터 교체 완료: 슬롯 {} ↔ 슬롯 {}", sourceSlot, targetSlot);
            return party;
            
        } catch (Exception e) {
            log.error("캐릭터 교체 실패", e);
            return Map.of("error", "캐릭터 교체에 실패했습니다.");
        }
    }
    
    /**
     * 파티 간 캐릭터 이동
     */
    public Map<String, Object> moveCharacterBetweenParties(
            Map<String, Object> sourceParty, 
            Map<String, Object> targetParty, 
            int sourceSlot, 
            int targetSlot) {
        log.info("파티 간 캐릭터 이동: {} → {}", sourceParty.get("type"), targetParty.get("type"));
        
        try {
            List<Map<String, Object>> sourceSlots = (List<Map<String, Object>>) sourceParty.get("slots");
            List<Map<String, Object>> targetSlots = (List<Map<String, Object>>) targetParty.get("slots");
            
            if (sourceSlots == null || targetSlots == null || 
                sourceSlot < 0 || sourceSlot >= sourceSlots.size() ||
                targetSlot < 0 || targetSlot >= targetSlots.size()) {
                return Map.of("error", "유효하지 않은 슬롯 번호입니다.");
            }
            
            // 캐릭터 이동
            Map<String, Object> character = sourceSlots.get(sourceSlot);
            targetSlots.set(targetSlot, character);
            
            // 원본 슬롯 비우기
            sourceSlots.set(sourceSlot, createEmptySlot(sourceSlot));
            
            // 슬롯 번호 업데이트
            updateSlotNumbers(sourceSlots);
            updateSlotNumbers(targetSlots);
            
            // 파티 효율성 재계산
            double sourceEfficiency = calculatePartyEfficiency(sourceSlots);
            double targetEfficiency = calculatePartyEfficiency(targetSlots);
            
            sourceParty.put("efficiency", sourceEfficiency);
            targetParty.put("efficiency", targetEfficiency);
            
            log.info("파티 간 캐릭터 이동 완료");
            return Map.of("sourceParty", sourceParty, "targetParty", targetParty);
            
        } catch (Exception e) {
            log.error("파티 간 캐릭터 이동 실패", e);
            return Map.of("error", "캐릭터 이동에 실패했습니다.");
        }
    }
    
    /**
     * 파티에 새 캐릭터 추가
     */
    public Map<String, Object> addCharacterToParty(Map<String, Object> party, Character character, int slotNumber) {
        log.info("파티에 캐릭터 추가: {} → 슬롯 {}", character.getCharacterName(), slotNumber);
        
        try {
            List<Map<String, Object>> slots = (List<Map<String, Object>>) party.get("slots");
            
            if (slots == null || slotNumber < 0 || slotNumber >= slots.size()) {
                return Map.of("error", "유효하지 않은 슬롯 번호입니다.");
            }
            
            // 슬롯에 캐릭터 추가
            Map<String, Object> slotInfo = createSlotInfo(character, determineRole(character), slotNumber);
            slots.set(slotNumber, slotInfo);
            
            // 파티 효율성 재계산
            double efficiency = calculatePartyEfficiency(slots);
            party.put("efficiency", efficiency);
            
            log.info("캐릭터 추가 완료: {} → 슬롯 {}", character.getCharacterName(), slotNumber);
            return party;
            
        } catch (Exception e) {
            log.error("캐릭터 추가 실패", e);
            return Map.of("error", "캐릭터 추가에 실패했습니다.");
        }
    }
    
    /**
     * 파티에서 캐릭터 제거
     */
    public Map<String, Object> removeCharacterFromParty(Map<String, Object> party, int slotNumber) {
        log.info("파티에서 캐릭터 제거: 슬롯 {}", slotNumber);
        
        try {
            List<Map<String, Object>> slots = (List<Map<String, Object>>) party.get("slots");
            
            if (slots == null || slotNumber < 0 || slotNumber >= slots.size()) {
                return Map.of("error", "유효하지 않은 슬롯 번호입니다.");
            }
            
            // 슬롯 비우기
            slots.set(slotNumber, createEmptySlot(slotNumber));
            
            // 파티 효율성 재계산
            double efficiency = calculatePartyEfficiency(slots);
            party.put("efficiency", efficiency);
            
            log.info("캐릭터 제거 완료: 슬롯 {}", slotNumber);
            return party;
            
        } catch (Exception e) {
            log.error("캐릭터 제거 실패", e);
            return Map.of("error", "캐릭터 제거에 실패했습니다.");
        }
    }
    
    /**
     * 파티 구성 검증
     */
    public Map<String, Object> validatePartyComposition(Map<String, Object> party, String dungeonName) {
        log.info("파티 구성 검증: {}", dungeonName);
        
        try {
            List<Map<String, Object>> slots = (List<Map<String, Object>>) party.get("slots");
            Map<String, Object> validation = new HashMap<>();
            
            if (slots == null) {
                validation.put("isValid", false);
                validation.put("errors", List.of("파티 슬롯 정보가 없습니다."));
                return validation;
            }
            
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // 1. 빈 슬롯 체크
            long emptySlots = slots.stream()
                .filter(slot -> slot.get("characterId") == null)
                .count();
            
            if (emptySlots > 0) {
                errors.add("빈 슬롯이 " + emptySlots + "개 있습니다.");
            }
            
            // 2. 던전별 요구사항 체크
            Map<String, Object> dungeonRequirements = getDungeonRequirements(dungeonName);
            long minFame = (Long) dungeonRequirements.get("minFame");
            
            List<Map<String, Object>> validCharacters = slots.stream()
                .filter(slot -> slot.get("characterId") != null)
                .collect(Collectors.toList());
            
            // 3. 명성 요구사항 체크
            for (Map<String, Object> slot : validCharacters) {
                Double fame = (Double) slot.get("fame");
                if (fame != null && fame < minFame) {
                    warnings.add(slot.get("characterName") + "의 명성이 " + minFame + " 미만입니다.");
                }
            }
            
            // 4. 직업 구성 체크
            Map<String, Object> compositionCheck = checkJobComposition(validCharacters, dungeonName);
            if (!(Boolean) compositionCheck.get("isValid")) {
                errors.addAll((List<String>) compositionCheck.get("errors"));
            }
            
            // 5. 중복 캐릭터 체크
            Set<String> characterIds = validCharacters.stream()
                .map(slot -> (String) slot.get("characterId"))
                .collect(Collectors.toSet());
            
            if (characterIds.size() < validCharacters.size()) {
                errors.add("중복된 캐릭터가 있습니다.");
            }
            
            validation.put("isValid", errors.isEmpty());
            validation.put("errors", errors);
            validation.put("warnings", warnings);
            validation.put("emptySlots", emptySlots);
            validation.put("validCharacters", validCharacters.size());
            validation.put("totalSlots", slots.size());
            
            log.info("파티 구성 검증 완료: 유효성 = {}", errors.isEmpty());
            return validation;
            
        } catch (Exception e) {
            log.error("파티 구성 검증 실패", e);
            return Map.of("error", "파티 구성 검증에 실패했습니다.");
        }
    }
    
    /**
     * 파티 수정 히스토리 저장
     */
    public void saveModificationHistory(Map<String, Object> party, String modificationType, String description) {
        try {
            // TODO: 파티 수정 히스토리를 DB에 저장하는 로직 구현
            log.info("파티 수정 히스토리 저장: {} - {}", modificationType, description);
            
        } catch (Exception e) {
            log.error("파티 수정 히스토리 저장 실패", e);
        }
    }
    
    // 유틸리티 메서드들
    private void updateSlotNumbers(List<Map<String, Object>> slots) {
        for (int i = 0; i < slots.size(); i++) {
            Map<String, Object> slot = slots.get(i);
            if (slot != null) {
                slot.put("slotNumber", i + 1);
            }
        }
    }
    
    private Map<String, Object> createEmptySlot(int slotNumber) {
        Map<String, Object> emptySlot = new HashMap<>();
        emptySlot.put("slotNumber", slotNumber + 1);
        emptySlot.put("characterId", null);
        emptySlot.put("characterName", "빈 슬롯");
        emptySlot.put("role", "빈 슬롯");
        emptySlot.put("fame", 0.0);
        return emptySlot;
    }
    
    private Map<String, Object> createSlotInfo(Character character, String role, int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber + 1);
        slot.put("characterId", character.getCharacterId());
        slot.put("characterName", character.getCharacterName());
        slot.put("serverId", character.getServerId());
        slot.put("jobName", character.getJobName());
        slot.put("fame", character.getFame() != null ? character.getFame().doubleValue() : 0.0);
        slot.put("role", role);
        slot.put("isFavorite", character.getIsFavorite());
        return slot;
    }
    
    private String determineRole(Character character) {
        if (character.getJobName() != null && character.getJobName().contains("버퍼")) {
            return "버퍼";
        } else if (character.getIsFavorite() != null && character.getIsFavorite()) {
            return "업둥이";
        } else {
            return "딜러";
        }
    }
    
    private double calculatePartyEfficiency(List<Map<String, Object>> slots) {
        if (slots.isEmpty()) return 0.0;
        
        List<Map<String, Object>> validSlots = slots.stream()
            .filter(slot -> slot.get("characterId") != null)
            .collect(Collectors.toList());
        
        if (validSlots.isEmpty()) return 0.0;
        
        double totalFame = validSlots.stream()
            .mapToDouble(slot -> (Double) slot.get("fame"))
            .sum();
        
        double avgFame = totalFame / validSlots.size();
        double variance = validSlots.stream()
            .mapToDouble(slot -> Math.pow((Double) slot.get("fame") - avgFame, 2))
            .sum() / validSlots.size();
        
        // 효율성 = 평균 명성 - 표준편차 (밸런스 고려)
        return avgFame - Math.sqrt(variance);
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
    
    private Map<String, Object> checkJobComposition(List<Map<String, Object>> characters, String dungeonName) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        long dealerCount = characters.stream()
            .filter(slot -> "딜러".equals(slot.get("role")))
            .count();
        
        long bufferCount = characters.stream()
            .filter(slot -> "버퍼".equals(slot.get("role")))
            .count();
        
        Map<String, Object> requirements = getDungeonRequirements(dungeonName);
        long minDealers = (Long) requirements.get("minDealers");
        long minBuffers = (Long) requirements.get("minBuffers");
        
        if (dealerCount < minDealers) {
            errors.add("딜러가 부족합니다. (필요: " + minDealers + "명, 현재: " + dealerCount + "명)");
        }
        
        if (bufferCount < minBuffers) {
            errors.add("버퍼가 부족합니다. (필요: " + minBuffers + "명, 현재: " + bufferCount + "명)");
        }
        
        result.put("isValid", errors.isEmpty());
        result.put("errors", errors);
        result.put("dealerCount", dealerCount);
        result.put("bufferCount", bufferCount);
        result.put("minDealers", minDealers);
        result.put("minBuffers", minBuffers);
        
        return result;
    }
}
