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
public class EightPersonPartyService {
    
    /**
     * 8인 파티 구성 (3딜러+1버퍼 × 2파티)
     */
    public Map<String, Object> createEightPersonParty(List<Character> characters, String dungeonName) {
        log.info("8인 파티 구성 시작: {}명의 캐릭터, 던전: {}", characters.size(), dungeonName);
        
        try {
            if (characters.size() < 8) {
                return Map.of("error", "8인 파티 구성에는 최소 8명의 캐릭터가 필요합니다.");
            }
            
            // 캐릭터 분류
            List<Character> dealers = filterDealers(characters);
            List<Character> buffers = filterBuffers(characters);
            List<Character> updoongis = filterUpdoongis(characters);
            List<Character> others = filterOthers(characters);
            
            log.info("캐릭터 분류 완료 - 딜러: {}명, 버퍼: {}명, 업둥이: {}명, 기타: {}명", 
                dealers.size(), buffers.size(), updoongis.size(), others.size());
            
            // 8인 파티 구성
            Map<String, Object> result = createBalancedEightPersonParty(
                dealers, buffers, updoongis, others, dungeonName
            );
            
            if (result.containsKey("error")) {
                return result;
            }
            
            log.info("8인 파티 구성 완료");
            return result;
            
        } catch (Exception e) {
            log.error("8인 파티 구성 실패", e);
            return Map.of("error", "8인 파티 구성에 실패했습니다.");
        }
    }
    
    /**
     * 밸런스가 맞는 8인 파티 구성
     */
    private Map<String, Object> createBalancedEightPersonParty(
            List<Character> dealers, 
            List<Character> buffers, 
            List<Character> updoongis, 
            List<Character> others, 
            String dungeonName) {
        
        Map<String, Object> party = new HashMap<>();
        party.put("type", "8인 파티");
        party.put("dungeonName", dungeonName);
        party.put("createdAt", new Date());
        
        // 1파티 구성 (3딜러 + 1버퍼)
        Map<String, Object> party1 = createFirstParty(dealers, buffers, updoongis, others, dungeonName);
        
        // 2파티 구성 (3딜러 + 1버퍼)
        Map<String, Object> party2 = createSecondParty(dealers, buffers, updoongis, others, dungeonName);
        
        // 전체 파티 정보
        party.put("party1", party1);
        party.put("party2", party2);
        
        // 전체 효율성 계산
        double totalEfficiency = calculateTotalEfficiency(party1, party2);
        party.put("totalEfficiency", totalEfficiency);
        
        // 파티 구성 분석
        Map<String, Object> analysis = analyzeEightPersonParty(party1, party2);
        party.put("analysis", analysis);
        
        return party;
    }
    
    /**
     * 1파티 구성 (3딜러 + 1버퍼)
     */
    private Map<String, Object> createFirstParty(
            List<Character> dealers, 
            List<Character> buffers, 
            List<Character> updoongis, 
            List<Character> others, 
            String dungeonName) {
        
        Map<String, Object> party1 = new HashMap<>();
        party1.put("type", "1파티");
        party1.put("size", 4);
        party1.put("composition", "3딜러 + 1버퍼");
        
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 1번 슬롯: 최고 명성 딜러 (업둥이 우선)
        Character bestDealer = selectBestDealerForFirstParty(dealers, updoongis);
        slots.add(createSlotInfo(bestDealer, "딜러", 0));
        dealers.remove(bestDealer);
        
        // 2번 슬롯: 최고 명성 버퍼
        Character bestBuffer = selectBestBufferForFirstParty(buffers, updoongis);
        slots.add(createSlotInfo(bestBuffer, "버퍼", 1));
        buffers.remove(bestBuffer);
        
        // 3번 슬롯: 두 번째 딜러
        Character secondDealer = selectSecondDealer(dealers, updoongis);
        slots.add(createSlotInfo(secondDealer, "딜러", 2));
        dealers.remove(secondDealer);
        
        // 4번 슬롯: 세 번째 딜러
        Character thirdDealer = selectThirdDealer(dealers, updoongis);
        slots.add(createSlotInfo(thirdDealer, "딜러", 3));
        dealers.remove(thirdDealer);
        
        party1.put("slots", slots);
        
        // 파티 효율성 계산
        double efficiency = calculatePartyEfficiency(slots);
        party1.put("efficiency", efficiency);
        
        return party1;
    }
    
    /**
     * 2파티 구성 (3딜러 + 1버퍼)
     */
    private Map<String, Object> createSecondParty(
            List<Character> dealers, 
            List<Character> buffers, 
            List<Character> updoongis, 
            List<Character> others, 
            String dungeonName) {
        
        Map<String, Object> party2 = new HashMap<>();
        party2.put("type", "2파티");
        party2.put("size", 4);
        party2.put("composition", "3딜러 + 1버퍼");
        
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 5번 슬롯: 남은 딜러 중 최고 명성
        Character fourthDealer = selectBestRemainingDealer(dealers, updoongis);
        slots.add(createSlotInfo(fourthDealer, "딜러", 4));
        if (fourthDealer != null) dealers.remove(fourthDealer);
        
        // 6번 슬롯: 남은 버퍼 중 최고 명성
        Character secondBuffer = selectBestRemainingBuffer(buffers, updoongis);
        slots.add(createSlotInfo(secondBuffer, "버퍼", 5));
        if (secondBuffer != null) buffers.remove(secondBuffer);
        
        // 7번 슬롯: 남은 딜러 또는 업둥이
        Character fifthDealer = selectRemainingDealerOrUpdoongi(dealers, updoongis, others);
        slots.add(createSlotInfo(fifthDealer, determineRole(fifthDealer), 6));
        if (fifthDealer != null) {
            if (dealers.contains(fifthDealer)) dealers.remove(fifthDealer);
            if (updoongis.contains(fifthDealer)) updoongis.remove(fifthDealer);
            if (others.contains(fifthDealer)) others.remove(fifthDealer);
        }
        
        // 8번 슬롯: 남은 캐릭터 중 최고 명성
        Character lastCharacter = selectBestRemainingCharacter(dealers, buffers, updoongis, others);
        slots.add(createSlotInfo(lastCharacter, determineRole(lastCharacter), 7));
        
        party2.put("slots", slots);
        
        // 파티 효율성 계산
        double efficiency = calculatePartyEfficiency(slots);
        party2.put("efficiency", efficiency);
        
        return party2;
    }
    
    /**
     * 1파티용 최고 딜러 선택 (업둥이 우선)
     */
    private Character selectBestDealerForFirstParty(List<Character> dealers, List<Character> updoongis) {
        // 업둥이 딜러 우선
        List<Character> updoongiDealers = dealers.stream()
            .filter(dealer -> updoongis.contains(dealer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiDealers.isEmpty()) {
            return updoongiDealers.get(0);
        }
        
        // 일반 딜러 중 최고 명성
        return dealers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 1파티용 최고 버퍼 선택 (업둥이 우선)
     */
    private Character selectBestBufferForFirstParty(List<Character> buffers, List<Character> updoongis) {
        // 업둥이 버퍼 우선
        List<Character> updoongiBuffers = buffers.stream()
            .filter(buffer -> updoongis.contains(buffer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiBuffers.isEmpty()) {
            return updoongiBuffers.get(0);
        }
        
        // 일반 버퍼 중 최고 명성
        return buffers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 두 번째 딜러 선택
     */
    private Character selectSecondDealer(List<Character> dealers, List<Character> updoongis) {
        if (dealers.isEmpty()) return null;
        
        // 업둥이 딜러 우선
        List<Character> updoongiDealers = dealers.stream()
            .filter(dealer -> updoongis.contains(dealer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiDealers.isEmpty()) {
            return updoongiDealers.get(0);
        }
        
        // 일반 딜러 중 최고 명성
        return dealers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 세 번째 딜러 선택
     */
    private Character selectThirdDealer(List<Character> dealers, List<Character> updoongis) {
        if (dealers.isEmpty()) return null;
        
        // 업둥이 딜러 우선
        List<Character> updoongiDealers = dealers.stream()
            .filter(dealer -> updoongis.contains(dealer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiDealers.isEmpty()) {
            return updoongiDealers.get(0);
        }
        
        // 일반 딜러 중 최고 명성
        return dealers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 남은 딜러 중 최고 명성 선택
     */
    private Character selectBestRemainingDealer(List<Character> dealers, List<Character> updoongis) {
        if (dealers.isEmpty()) return null;
        
        // 업둥이 딜러 우선
        List<Character> updoongiDealers = dealers.stream()
            .filter(dealer -> updoongis.contains(dealer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiDealers.isEmpty()) {
            return updoongiDealers.get(0);
        }
        
        // 일반 딜러 중 최고 명성
        return dealers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 남은 버퍼 중 최고 명성 선택
     */
    private Character selectBestRemainingBuffer(List<Character> buffers, List<Character> updoongis) {
        if (buffers.isEmpty()) return null;
        
        // 업둥이 버퍼 우선
        List<Character> updoongiBuffers = buffers.stream()
            .filter(buffer -> updoongis.contains(buffer))
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
        
        if (!updoongiBuffers.isEmpty()) {
            return updoongiBuffers.get(0);
        }
        
        // 일반 버퍼 중 최고 명성
        return buffers.stream()
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 남은 딜러 또는 업둥이 선택
     */
    private Character selectRemainingDealerOrUpdoongi(
            List<Character> dealers, 
            List<Character> updoongis, 
            List<Character> others) {
        
        // 업둥이 우선
        if (!updoongis.isEmpty()) {
            return updoongis.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        // 딜러 우선
        if (!dealers.isEmpty()) {
            return dealers.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        // 기타 캐릭터
        if (!others.isEmpty()) {
            return others.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * 남은 캐릭터 중 최고 명성 선택
     */
    private Character selectBestRemainingCharacter(
            List<Character> dealers, 
            List<Character> buffers, 
            List<Character> updoongis, 
            List<Character> others) {
        
        // 업둥이 우선
        if (!updoongis.isEmpty()) {
            return updoongis.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        // 딜러 우선
        if (!dealers.isEmpty()) {
            return dealers.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        // 버퍼
        if (!buffers.isEmpty()) {
            return buffers.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        // 기타 캐릭터
        if (!others.isEmpty()) {
            return others.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .findFirst()
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * 8인 파티 분석
     */
    private Map<String, Object> analyzeEightPersonParty(Map<String, Object> party1, Map<String, Object> party2) {
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
    
    /**
     * 전체 효율성 계산
     */
    private double calculateTotalEfficiency(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = (Double) party1.get("efficiency");
        double efficiency2 = (Double) party2.get("efficiency");
        
        // 전체 효율성 = (파티1 효율성 + 파티2 효율성) / 2 - 효율성 차이 페널티
        double avgEfficiency = (efficiency1 + efficiency2) / 2;
        double efficiencyGap = Math.abs(efficiency1 - efficiency2);
        
        return avgEfficiency - (efficiencyGap * 0.1); // 효율성 차이에 페널티 적용
    }
    
    // 유틸리티 메서드들
    private List<Character> filterDealers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getJobName() != null && !c.getJobName().contains("버퍼"))
            .collect(Collectors.toList());
    }
    
    private List<Character> filterBuffers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getJobName() != null && c.getJobName().contains("버퍼"))
            .collect(Collectors.toList());
    }
    
    private List<Character> filterUpdoongis(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getIsFavorite() != null && c.getIsFavorite())
            .collect(Collectors.toList());
    }
    
    private List<Character> filterOthers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getJobName() == null || 
                (!c.getJobName().contains("버퍼") && 
                 (c.getIsFavorite() == null || !c.getIsFavorite())))
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> createSlotInfo(Character character, String role, int slotNumber) {
        if (character == null) {
            return createEmptySlot(slotNumber);
        }
        
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
    
    private Map<String, Object> createEmptySlot(int slotNumber) {
        Map<String, Object> emptySlot = new HashMap<>();
        emptySlot.put("slotNumber", slotNumber + 1);
        emptySlot.put("characterId", null);
        emptySlot.put("characterName", "빈 슬롯");
        emptySlot.put("role", "빈 슬롯");
        emptySlot.put("fame", 0.0);
        return emptySlot;
    }
    
    private String determineRole(Character character) {
        if (character == null) return "빈 슬롯";
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
}
