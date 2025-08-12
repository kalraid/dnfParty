package com.dfparty.backend.service;

import com.dfparty.backend.model.Character;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedPartyOptimizationService {

    /**
     * 고급 파티 최적화 실행
     */
    public Map<String, Object> executeAdvancedOptimization(List<Character> characters, String dungeonName, int partySize, String optimizationStrategy) {
        try {
            log.info("고급 파티 최적화 시작: 던전={}, 파티크기={}, 전략={}", dungeonName, partySize, optimizationStrategy);
            
            Map<String, Object> result = new HashMap<>();
            
            switch (optimizationStrategy.toLowerCase()) {
                case "efficiency":
                    result = optimizeForEfficiency(characters, dungeonName, partySize);
                    break;
                case "balance":
                    result = optimizeForBalance(characters, dungeonName, partySize);
                    break;
                case "synergy":
                    result = optimizeForSynergy(characters, dungeonName, partySize);
                    break;
                case "safety":
                    result = optimizeForSafety(characters, dungeonName, partySize);
                    break;
                case "hybrid":
                    result = optimizeHybrid(characters, dungeonName, partySize);
                    break;
                default:
                    result = optimizeForEfficiency(characters, dungeonName, partySize);
            }
            
            result.put("optimizationStrategy", optimizationStrategy);
            result.put("executionTime", System.currentTimeMillis());
            
            log.info("고급 파티 최적화 완료: 전략={}", optimizationStrategy);
            return result;
            
        } catch (Exception e) {
            log.error("고급 파티 최적화 실패", e);
            return createErrorResult("최적화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 효율성 중심 최적화
     */
    private Map<String, Object> optimizeForEfficiency(List<Character> characters, String dungeonName, int partySize) {
        Map<String, Object> result = new HashMap<>();
        
        // 명성 기준으로 정렬
        List<Character> sortedCharacters = characters.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .collect(Collectors.toList());
        
        // 직업별 분류
        List<Character> dealers = filterDealers(sortedCharacters);
        List<Character> buffers = filterBuffers(sortedCharacters);
        List<Character> updoongis = filterUpdoongis(sortedCharacters);
        List<Character> others = filterOthers(sortedCharacters);
        
        if (partySize == 8) {
            // 8인 파티 최적화
            Map<String, Object> party1 = createOptimalParty(dealers, buffers, updoongis, others, 1, dungeonName);
            Map<String, Object> party2 = createOptimalParty(dealers, buffers, updoongis, others, 2, dungeonName);
            
            result.put("party1", party1);
            result.put("party2", party2);
            result.put("totalEfficiency", calculateTotalEfficiency(party1, party2));
            result.put("optimizationType", "efficiency");
            
        } else {
            // 4인 파티 최적화
            Map<String, Object> party = createOptimalParty(dealers, buffers, updoongis, others, 1, dungeonName);
            result.put("party", party);
            result.put("efficiency", calculatePartyEfficiency((List<Map<String, Object>>) party.get("slots")));
            result.put("optimizationType", "efficiency");
        }
        
        return result;
    }

    /**
     * 밸런스 중심 최적화
     */
    private Map<String, Object> optimizeForBalance(List<Character> characters, String dungeonName, int partySize) {
        Map<String, Object> result = new HashMap<>();
        
        // 직업별 균등 분배
        List<Character> dealers = filterDealers(characters);
        List<Character> buffers = filterBuffers(characters);
        List<Character> updoongis = filterUpdoongis(characters);
        List<Character> others = filterOthers(characters);
        
        if (partySize == 8) {
            // 8인 파티 밸런스 최적화
            Map<String, Object> party1 = createBalancedParty(dealers, buffers, updoongis, others, 1, dungeonName);
            Map<String, Object> party2 = createBalancedParty(dealers, buffers, updoongis, others, 2, dungeonName);
            
            result.put("party1", party1);
            result.put("party2", party2);
            result.put("balanceScore", calculateBalanceScore(party1, party2));
            result.put("optimizationType", "balance");
            
        } else {
            // 4인 파티 밸런스 최적화
            Map<String, Object> party = createBalancedParty(dealers, buffers, updoongis, others, 1, dungeonName);
            result.put("party", party);
            result.put("balanceScore", calculatePartyBalanceScore((List<Map<String, Object>>) party.get("slots")));
            result.put("optimizationType", "balance");
        }
        
        return result;
    }

    /**
     * 시너지 중심 최적화
     */
    private Map<String, Object> optimizeForSynergy(List<Character> characters, String dungeonName, int partySize) {
        Map<String, Object> result = new HashMap<>();
        
        // 직업 조합 시너지 분석
        Map<String, Double> jobSynergies = analyzeJobSynergies(characters, dungeonName);
        
        // 시너지가 높은 조합 우선 선택
        List<Character> optimizedCharacters = selectCharactersBySynergy(characters, jobSynergies, partySize);
        
        if (partySize == 8) {
            Map<String, Object> party1 = createSynergyParty(optimizedCharacters, 1, dungeonName);
            Map<String, Object> party2 = createSynergyParty(optimizedCharacters, 2, dungeonName);
            
            result.put("party1", party1);
            result.put("party2", party2);
            result.put("synergyScore", calculateSynergyScore(party1, party2));
            result.put("optimizationType", "synergy");
            
        } else {
            Map<String, Object> party = createSynergyParty(optimizedCharacters, 1, dungeonName);
            result.put("party", party);
            result.put("synergyScore", calculatePartySynergyScore((List<Map<String, Object>>) party.get("slots")));
            result.put("optimizationType", "synergy");
        }
        
        return result;
    }

    /**
     * 안전성 중심 최적화
     */
    private Map<String, Object> optimizeForSafety(List<Character> characters, String dungeonName, int partySize) {
        Map<String, Object> result = new HashMap<>();
        
        // 안전 마진이 있는 캐릭터 선택
        List<Character> safeCharacters = selectSafeCharacters(characters, dungeonName);
        
        if (partySize == 8) {
            Map<String, Object> party1 = createSafeParty(safeCharacters, 1, dungeonName);
            Map<String, Object> party2 = createSafeParty(safeCharacters, 2, dungeonName);
            
            result.put("party1", party1);
            result.put("party2", party2);
            result.put("safetyScore", calculateSafetyScore(party1, party2, dungeonName));
            result.put("optimizationType", "safety");
            
        } else {
            Map<String, Object> party = createSafeParty(safeCharacters, 1, dungeonName);
            result.put("party", party);
            result.put("safetyScore", calculatePartySafetyScore((List<Map<String, Object>>) party.get("slots"), dungeonName));
            result.put("optimizationType", "safety");
        }
        
        return result;
    }

    /**
     * 하이브리드 최적화
     */
    private Map<String, Object> optimizeHybrid(List<Character> characters, String dungeonName, int partySize) {
        Map<String, Object> result = new HashMap<>();
        
        // 여러 전략을 조합하여 최적화
        Map<String, Object> efficiencyResult = optimizeForEfficiency(characters, dungeonName, partySize);
        Map<String, Object> balanceResult = optimizeForBalance(characters, dungeonName, partySize);
        Map<String, Object> synergyResult = optimizeForSynergy(characters, dungeonName, partySize);
        
        // 결과 비교 및 최적 조합 선택
        Map<String, Object> bestResult = selectBestHybridResult(
                efficiencyResult, balanceResult, synergyResult, dungeonName
        );
        
        result.putAll(bestResult);
        result.put("optimizationType", "hybrid");
        result.put("hybridScores", Map.of(
                "efficiency", calculateHybridScore(efficiencyResult),
                "balance", calculateHybridScore(balanceResult),
                "synergy", calculateHybridScore(synergyResult)
        ));
        
        return result;
    }

    /**
     * 최적 파티 생성
     */
    private Map<String, Object> createOptimalParty(List<Character> dealers, List<Character> buffers, 
                                                   List<Character> updoongis, List<Character> others, 
                                                   int partyNumber, String dungeonName) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 업둥이 우선 배치
        int updoongiCount = Math.min(updoongis.size(), 2);
        for (int i = 0; i < updoongiCount; i++) {
            slots.add(createSlotInfo(updoongis.get(i), "updoongi", i + 1));
        }
        
        // 딜러 배치
        int dealerCount = Math.min(dealers.size(), 3);
        for (int i = 0; i < dealerCount; i++) {
            slots.add(createSlotInfo(dealers.get(i), "dealer", slots.size() + 1));
        }
        
        // 버퍼 배치
        if (!buffers.isEmpty()) {
            slots.add(createSlotInfo(buffers.get(0), "buffer", slots.size() + 1));
        }
        
        // 나머지 슬롯 채우기
        while (slots.size() < 4) {
            Character remainingChar = selectRemainingCharacter(dealers, buffers, updoongis, others, slots);
            if (remainingChar != null) {
                slots.add(createSlotInfo(remainingChar, determineRole(remainingChar), slots.size() + 1));
            } else {
                slots.add(createEmptySlot(slots.size() + 1));
            }
        }
        
        party.put("partyNumber", partyNumber);
        party.put("slots", slots);
        party.put("efficiency", calculatePartyEfficiency(slots));
        party.put("dungeonName", dungeonName);
        
        return party;
    }

    /**
     * 밸런스 파티 생성
     */
    private Map<String, Object> createBalancedParty(List<Character> dealers, List<Character> buffers, 
                                                    List<Character> updoongis, List<Character> others, 
                                                    int partyNumber, String dungeonName) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 직업별 균등 분배
        int dealerSlots = 2;
        int bufferSlots = 1;
        int updoongiSlots = 1;
        
        // 업둥이 배치
        for (int i = 0; i < Math.min(updoongis.size(), updoongiSlots); i++) {
            slots.add(createSlotInfo(updoongis.get(i), "updoongi", i + 1));
        }
        
        // 딜러 배치
        for (int i = 0; i < Math.min(dealers.size(), dealerSlots); i++) {
            slots.add(createSlotInfo(dealers.get(i), "dealer", slots.size() + 1));
        }
        
        // 버퍼 배치
        if (!buffers.isEmpty()) {
            slots.add(createSlotInfo(buffers.get(0), "buffer", slots.size() + 1));
        }
        
        // 나머지 슬롯 채우기
        while (slots.size() < 4) {
            Character remainingChar = selectRemainingCharacter(dealers, buffers, updoongis, others, slots);
            if (remainingChar != null) {
                slots.add(createSlotInfo(remainingChar, determineRole(remainingChar), slots.size() + 1));
            } else {
                slots.add(createEmptySlot(slots.size() + 1));
            }
        }
        
        party.put("partyNumber", partyNumber);
        party.put("slots", slots);
        party.put("balanceScore", calculatePartyBalanceScore(slots));
        party.put("dungeonName", dungeonName);
        
        return party;
    }

    /**
     * 시너지 파티 생성
     */
    private Map<String, Object> createSynergyParty(List<Character> characters, int partyNumber, String dungeonName) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 시너지 기반으로 캐릭터 선택 및 배치
        for (int i = 0; i < Math.min(characters.size(), 4); i++) {
            Character character = characters.get(i);
            slots.add(createSlotInfo(character, determineRole(character), i + 1));
        }
        
        // 빈 슬롯 채우기
        while (slots.size() < 4) {
            slots.add(createEmptySlot(slots.size() + 1));
        }
        
        party.put("partyNumber", partyNumber);
        party.put("slots", slots);
        party.put("synergyScore", calculatePartySynergyScore(slots));
        party.put("dungeonName", dungeonName);
        
        return party;
    }

    /**
     * 안전 파티 생성
     */
    private Map<String, Object> createSafeParty(List<Character> characters, int partyNumber, String dungeonName) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 안전 마진이 있는 캐릭터들로 파티 구성
        for (int i = 0; i < Math.min(characters.size(), 4); i++) {
            Character character = characters.get(i);
            slots.add(createSlotInfo(character, determineRole(character), i + 1));
        }
        
        // 빈 슬롯 채우기
        while (slots.size() < 4) {
            slots.add(createEmptySlot(slots.size() + 1));
        }
        
        party.put("partyNumber", partyNumber);
        party.put("slots", slots);
        party.put("safetyScore", calculatePartySafetyScore(slots, dungeonName));
        party.put("dungeonName", dungeonName);
        
        return party;
    }

    // Utility methods
    private List<Character> filterDealers(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getJob().contains("딜러") || c.getJob().contains("Dealer"))
                .collect(Collectors.toList());
    }

    private List<Character> filterBuffers(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getJob().contains("버퍼") || c.getJob().contains("Buffer"))
                .collect(Collectors.toList());
    }

    private List<Character> filterUpdoongis(List<Character> characters) {
        return characters.stream()
                .filter(Character::getIsFavorite)
                .collect(Collectors.toList());
    }

    private List<Character> filterOthers(List<Character> characters) {
        return characters.stream()
                .filter(c -> !c.getJob().contains("딜러") && !c.getJob().contains("버퍼") && !c.getIsFavorite())
                .collect(Collectors.toList());
    }

    private Map<String, Object> createSlotInfo(Character character, String role, int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber);
        slot.put("character", character);
        slot.put("role", role);
        slot.put("isOccupied", true);
        return slot;
    }

    private Map<String, Object> createEmptySlot(int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber);
        slot.put("character", null);
        slot.put("role", "empty");
        slot.put("isOccupied", false);
        return slot;
    }

    private String determineRole(Character character) {
        if (character.getJob().contains("딜러") || character.getJob().contains("Dealer")) {
            return "dealer";
        } else if (character.getJob().contains("버퍼") || character.getJob().contains("Buffer")) {
            return "buffer";
        } else if (character.getIsFavorite()) {
            return "updoongi";
        } else {
            return "other";
        }
    }

    private Character selectRemainingCharacter(List<Character> dealers, List<Character> buffers, 
                                              List<Character> updoongis, List<Character> others, 
                                              List<Map<String, Object>> existingSlots) {
        Set<String> usedCharacterIds = existingSlots.stream()
                .filter(slot -> slot.get("character") != null)
                .map(slot -> ((Character) slot.get("character")).getCharacterId())
                .collect(Collectors.toSet());
        
        // 사용되지 않은 캐릭터 중에서 선택
        List<Character> allCharacters = new ArrayList<>();
        allCharacters.addAll(dealers);
        allCharacters.addAll(buffers);
        allCharacters.addAll(updoongis);
        allCharacters.addAll(others);
        
        return allCharacters.stream()
                .filter(c -> !usedCharacterIds.contains(c.getCharacterId()))
                .max(Comparator.comparing(Character::getFame))
                .orElse(null);
    }

    private double calculatePartyEfficiency(List<Map<String, Object>> slots) {
        return slots.stream()
                .filter(slot -> slot.get("character") != null)
                .mapToDouble(slot -> {
                    Character character = (Character) slot.get("character");
                    return character.getFame() * 0.7 + character.getCombatPower() * 0.3;
                })
                .average()
                .orElse(0.0);
    }

    private double calculateTotalEfficiency(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = calculatePartyEfficiency((List<Map<String, Object>>) party1.get("slots"));
        double efficiency2 = calculatePartyEfficiency((List<Map<String, Object>>) party2.get("slots"));
        return (efficiency1 + efficiency2) / 2.0;
    }

    private double calculateBalanceScore(Map<String, Object> party1, Map<String, Object> party2) {
        double balance1 = calculatePartyBalanceScore((List<Map<String, Object>>) party1.get("slots"));
        double balance2 = calculatePartyBalanceScore((List<Map<String, Object>>) party2.get("slots"));
        return (balance1 + balance2) / 2.0;
    }

    private double calculatePartyBalanceScore(List<Map<String, Object>> slots) {
        // 직업별 균등성 점수 계산
        Map<String, Long> roleCounts = slots.stream()
                .filter(slot -> slot.get("character") != null)
                .collect(Collectors.groupingBy(
                        slot -> (String) slot.get("role"),
                        Collectors.counting()
                ));
        
        // 이상적인 분배: 딜러 2, 버퍼 1, 업둥이 1
        double idealDealer = 2.0;
        double idealBuffer = 1.0;
        double idealUpdoongi = 1.0;
        
        double actualDealer = roleCounts.getOrDefault("dealer", 0L);
        double actualBuffer = roleCounts.getOrDefault("buffer", 0L);
        double actualUpdoongi = roleCounts.getOrDefault("updoongi", 0L);
        
        double dealerScore = 1.0 - Math.abs(actualDealer - idealDealer) / idealDealer;
        double bufferScore = 1.0 - Math.abs(actualBuffer - idealBuffer) / idealBuffer;
        double updoongiScore = 1.0 - Math.abs(actualUpdoongi - idealUpdoongi) / idealUpdoongi;
        
        return (dealerScore + bufferScore + updoongiScore) / 3.0;
    }

    private Map<String, Double> analyzeJobSynergies(List<Character> characters, String dungeonName) {
        Map<String, Double> synergies = new HashMap<>();
        
        // 던전별 직업 조합 시너지 점수
        if (dungeonName.contains("레이드")) {
            synergies.put("dealer-dealer", 0.8);
            synergies.put("dealer-buffer", 0.9);
            synergies.put("buffer-buffer", 0.7);
            synergies.put("updoongi-dealer", 0.85);
        } else {
            synergies.put("dealer-dealer", 0.7);
            synergies.put("dealer-buffer", 0.8);
            synergies.put("buffer-buffer", 0.6);
            synergies.put("updoongi-dealer", 0.75);
        }
        
        return synergies;
    }

    private List<Character> selectCharactersBySynergy(List<Character> characters, Map<String, Double> jobSynergies, int partySize) {
        // 시너지 점수 기반으로 캐릭터 선택
        return characters.stream()
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .limit(partySize)
                .collect(Collectors.toList());
    }

    private double calculateSynergyScore(Map<String, Object> party1, Map<String, Object> party2) {
        double synergy1 = calculatePartySynergyScore((List<Map<String, Object>>) party1.get("slots"));
        double synergy2 = calculatePartySynergyScore((List<Map<String, Object>>) party2.get("slots"));
        return (synergy1 + synergy2) / 2.0;
    }

    private double calculatePartySynergyScore(List<Map<String, Object>> slots) {
        // 직업 조합 시너지 점수 계산
        List<String> roles = slots.stream()
                .filter(slot -> slot.get("character") != null)
                .map(slot -> (String) slot.get("role"))
                .collect(Collectors.toList());
        
        double totalSynergy = 0.0;
        int synergyCount = 0;
        
        for (int i = 0; i < roles.size(); i++) {
            for (int j = i + 1; j < roles.size(); j++) {
                String role1 = roles.get(i);
                String role2 = roles.get(j);
                String synergyKey = role1 + "-" + role2;
                
                // 기본 시너지 점수
                double synergy = 0.7; // 기본값
                if (role1.equals("dealer") && role2.equals("buffer")) {
                    synergy = 0.9;
                } else if (role1.equals("dealer") && role2.equals("dealer")) {
                    synergy = 0.8;
                } else if (role1.equals("updoongi")) {
                    synergy = 0.85;
                }
                
                totalSynergy += synergy;
                synergyCount++;
            }
        }
        
        return synergyCount > 0 ? totalSynergy / synergyCount : 0.0;
    }

    private List<Character> selectSafeCharacters(List<Character> characters, String dungeonName) {
        // 던전 요구사항보다 높은 명성을 가진 캐릭터 선택
        int requiredFame = getDungeonRequiredFame(dungeonName);
        int safetyMargin = (int) (requiredFame * 0.2); // 20% 안전 마진
        
        return characters.stream()
                .filter(c -> c.getFame() >= (requiredFame + safetyMargin))
                .sorted(Comparator.comparing(Character::getFame).reversed())
                .collect(Collectors.toList());
    }

    private double calculateSafetyScore(Map<String, Object> party1, Map<String, Object> party2, String dungeonName) {
        double safety1 = calculatePartySafetyScore((List<Map<String, Object>>) party1.get("slots"), dungeonName);
        double safety2 = calculatePartySafetyScore((List<Map<String, Object>>) party2.get("slots"), dungeonName);
        return (safety1 + safety2) / 2.0;
    }

    private double calculatePartySafetyScore(List<Map<String, Object>> slots, String dungeonName) {
        int requiredFame = getDungeonRequiredFame(dungeonName);
        
        return slots.stream()
                .filter(slot -> slot.get("character") != null)
                .mapToDouble(slot -> {
                    Character character = (Character) slot.get("character");
                    double safetyMargin = (character.getFame() - requiredFame) / (double) requiredFame;
                    return Math.max(0.0, Math.min(1.0, safetyMargin));
                })
                .average()
                .orElse(0.0);
    }

    private int getDungeonRequiredFame(String dungeonName) {
        // 던전별 최소 명성 요구사항
        if (dungeonName.contains("시로코")) return 50000;
        if (dungeonName.contains("바칼")) return 60000;
        if (dungeonName.contains("카인")) return 70000;
        if (dungeonName.contains("디레지에")) return 80000;
        return 50000; // 기본값
    }

    private Map<String, Object> selectBestHybridResult(Map<String, Object> efficiencyResult, 
                                                       Map<String, Object> balanceResult, 
                                                       Map<String, Object> synergyResult, 
                                                       String dungeonName) {
        // 던전 특성에 따른 가중치 적용
        Map<String, Double> weights = getDungeonWeights(dungeonName);
        
        double efficiencyScore = calculateHybridScore(efficiencyResult) * weights.get("efficiency");
        double balanceScore = calculateHybridScore(balanceResult) * weights.get("balance");
        double synergyScore = calculateHybridScore(synergyResult) * weights.get("synergy");
        
        // 최고 점수 결과 선택
        if (efficiencyScore >= balanceScore && efficiencyScore >= synergyScore) {
            return efficiencyResult;
        } else if (balanceScore >= efficiencyScore && balanceScore >= synergyScore) {
            return balanceResult;
        } else {
            return synergyResult;
        }
    }

    private Map<String, Double> getDungeonWeights(String dungeonName) {
        if (dungeonName.contains("레이드")) {
            return Map.of("efficiency", 0.4, "balance", 0.3, "synergy", 0.3);
        } else {
            return Map.of("efficiency", 0.5, "balance", 0.3, "synergy", 0.2);
        }
    }

    private double calculateHybridScore(Map<String, Object> result) {
        if (result.containsKey("efficiency")) {
            return (Double) result.get("efficiency");
        } else if (result.containsKey("balanceScore")) {
            return (Double) result.get("balanceScore");
        } else if (result.containsKey("synergyScore")) {
            return (Double) result.get("synergyScore");
        } else if (result.containsKey("totalEfficiency")) {
            return (Double) result.get("totalEfficiency");
        }
        return 0.0;
    }

    private Map<String, Object> createErrorResult(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
