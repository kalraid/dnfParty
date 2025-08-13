package com.dfparty.backend.service;

import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.entity.Character;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyOptimizationService {
    
    /**
     * 업둥이 우선 파티 구성
     * 쌘딜러 1명 + 버퍼 1명 + 업둥이들로 구성
     */
    public Map<String, Object> createUpdoongiPriorityParty(List<Character> characters, int partySize) {
        log.info("업둥이 우선 파티 구성 시작: {}명", partySize);
        
        try {
            // 캐릭터 분류
            List<Character> dealers = filterDealers(characters);
            List<Character> buffers = filterBuffers(characters);
            List<Character> updoongis = filterUpdoongis(characters);
            List<Character> others = filterOthers(characters);
            
            // 파티 구성
            Map<String, Object> party = new HashMap<>();
            
            if (partySize == 4) {
                party = create4PersonParty(dealers, buffers, updoongis, others);
            } else if (partySize == 8) {
                party = create8PersonParty(dealers, buffers, updoongis, others);
            }
            
            log.info("업둥이 우선 파티 구성 완료");
            return party;
            
        } catch (Exception e) {
            log.error("업둥이 우선 파티 구성 실패", e);
            return Map.of("error", "파티 구성에 실패했습니다.");
        }
    }
    
    /**
     * 평균 파티 구성 (밸런스 최적화)
     */
    public Map<String, Object> createBalancedParty(List<Character> characters, int partySize) {
        log.info("평균 파티 구성 시작: {}명", partySize);
        
        try {
            // 캐릭터 분류
            List<Character> dealers = filterDealers(characters);
            List<Character> buffers = filterBuffers(characters);
            List<Character> others = filterOthers(characters);
            
            // 파티 구성
            Map<String, Object> party = new HashMap<>();
            
            if (partySize == 4) {
                party = createBalanced4PersonParty(dealers, buffers, others);
            } else if (partySize == 8) {
                party = createBalanced8PersonParty(dealers, buffers, others);
            }
            
            log.info("평균 파티 구성 완료");
            return party;
            
        } catch (Exception e) {
            log.error("평균 파티 구성 실패", e);
            return Map.of("error", "파티 구성에 실패했습니다.");
        }
    }
    
    /**
     * 4인 파티 구성 (업둥이 우선)
     */
    private Map<String, Object> create4PersonParty(List<Character> dealers, List<Character> buffers, 
                                                   List<Character> updoongis, List<Character> others) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 1. 쌘딜러 1명 선택 (명성 기준)
        Character bestDealer = selectBestDealer(dealers);
        if (bestDealer != null) {
            slots.add(createSlotInfo(bestDealer, "딜러", 1));
            dealers.remove(bestDealer);
        }
        
        // 2. 버퍼 1명 선택 (버프력 기준)
        Character bestBuffer = selectBestBuffer(buffers);
        if (bestBuffer != null) {
            slots.add(createSlotInfo(bestBuffer, "버퍼", 2));
            buffers.remove(bestBuffer);
        }
        
        // 3. 업둥이들 우선 배치
        int remainingSlots = 4 - slots.size();
        List<Character> selectedUpdoongis = selectUpdoongis(updoongis, remainingSlots);
        
        for (int i = 0; i < selectedUpdoongis.size(); i++) {
            Character updoongi = selectedUpdoongis.get(i);
            slots.add(createSlotInfo(updoongi, "업둥이", slots.size() + 1));
            updoongis.remove(updoongi);
        }
        
        // 4. 남은 슬롯을 다른 캐릭터로 채움
        remainingSlots = 4 - slots.size();
        if (remainingSlots > 0) {
            List<Character> allRemaining = new ArrayList<>();
            allRemaining.addAll(dealers);
            allRemaining.addAll(buffers);
            allRemaining.addAll(others);
            
            List<Character> selectedOthers = selectBestCharacters(allRemaining, remainingSlots);
            for (int i = 0; i < selectedOthers.size(); i++) {
                Character other = selectedOthers.get(i);
                slots.add(createSlotInfo(other, "일반", slots.size() + 1));
            }
        }
        
        party.put("slots", slots);
        party.put("partySize", 4);
        party.put("type", "업둥이 우선");
        party.put("efficiency", calculatePartyEfficiency(slots));
        
        return party;
    }
    
    /**
     * 8인 파티 구성 (업둥이 우선)
     */
    private Map<String, Object> create8PersonParty(List<Character> dealers, List<Character> buffers, 
                                                   List<Character> updoongis, List<Character> others) {
        Map<String, Object> party = new HashMap<>();
        
        // 8인 파티를 2개의 4인 파티로 분할
        Map<String, Object> party1 = create4PersonParty(dealers, buffers, updoongis, others);
        Map<String, Object> party2 = create4PersonParty(dealers, buffers, updoongis, others);
        
        party.put("party1", party1);
        party.put("party2", party2);
        party.put("partySize", 8);
        party.put("type", "업둥이 우선 8인");
        party.put("totalEfficiency", calculateTotalEfficiency(party1, party2));
        
        return party;
    }
    
    /**
     * 4인 파티 구성 (밸런스 최적화)
     */
    private Map<String, Object> createBalanced4PersonParty(List<Character> dealers, List<Character> buffers, 
                                                           List<Character> others) {
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 딜러와 버퍼의 비율을 3:1로 조정
        int dealerCount = Math.min(3, dealers.size());
        int bufferCount = Math.min(1, buffers.size());
        
        // 딜러 선택 (명성 기준)
        List<Character> selectedDealers = selectBestCharacters(dealers, dealerCount);
        for (int i = 0; i < selectedDealers.size(); i++) {
            Character dealer = selectedDealers.get(i);
            slots.add(createSlotInfo(dealer, "딜러", slots.size() + 1));
            dealers.remove(dealer);
        }
        
        // 버퍼 선택 (버프력 기준)
        List<Character> selectedBuffers = selectBestCharacters(buffers, bufferCount);
        for (int i = 0; i < selectedBuffers.size(); i++) {
            Character buffer = selectedBuffers.get(i);
            slots.add(createSlotInfo(buffer, "버퍼", slots.size() + 1));
            buffers.remove(buffer);
        }
        
        // 남은 슬롯을 다른 캐릭터로 채움
        int remainingSlots = 4 - slots.size();
        if (remainingSlots > 0) {
            List<Character> allRemaining = new ArrayList<>();
            allRemaining.addAll(dealers);
            allRemaining.addAll(buffers);
            allRemaining.addAll(others);
            
            List<Character> selectedOthers = selectBestCharacters(allRemaining, remainingSlots);
            for (int i = 0; i < selectedOthers.size(); i++) {
                Character other = selectedOthers.get(i);
                slots.add(createSlotInfo(other, "일반", slots.size() + 1));
            }
        }
        
        party.put("slots", slots);
        party.put("partySize", 4);
        party.put("type", "밸런스 최적화");
        party.put("efficiency", calculatePartyEfficiency(slots));
        
        return party;
    }
    
    /**
     * 8인 파티 구성 (밸런스 최적화)
     */
    private Map<String, Object> createBalanced8PersonParty(List<Character> dealers, List<Character> buffers, 
                                                           List<Character> others) {
        Map<String, Object> party = new HashMap<>();
        
        // 8인 파티를 2개의 4인 파티로 분할 (각각 3딜러+1버퍼)
        Map<String, Object> party1 = createBalanced4PersonParty(dealers, buffers, others);
        Map<String, Object> party2 = createBalanced4PersonParty(dealers, buffers, others);
        
        party.put("party1", party1);
        party.put("party2", party2);
        party.put("partySize", 8);
        party.put("type", "밸런스 최적화 8인");
        party.put("totalEfficiency", calculateTotalEfficiency(party1, party2));
        
        return party;
    }
    
    // 유틸리티 메서드들
    private List<Character> filterDealers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getJobName() != null && !c.getJobName().contains("버퍼"))
            .filter(c -> c.getFame() != null && c.getFame() > 0)
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
    }
    
    private List<Character> filterBuffers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getJobName() != null && c.getJobName().contains("버퍼"))
            .filter(c -> c.getFame() != null && c.getFame() > 0)
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
    }
    
    private List<Character> filterUpdoongis(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getIsFavorite() != null && c.getIsFavorite())
            .filter(c -> c.getFame() != null && c.getFame() > 0)
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
    }
    
    private List<Character> filterOthers(List<Character> characters) {
        return characters.stream()
            .filter(c -> c.getIsFavorite() == null || !c.getIsFavorite())
            .filter(c -> c.getJobName() != null && !c.getJobName().contains("버퍼"))
            .filter(c -> c.getFame() != null && c.getFame() > 0)
            .sorted(Comparator.comparing(Character::getFame).reversed())
            .collect(Collectors.toList());
    }
    
    private Character selectBestDealer(List<Character> dealers) {
        return dealers.stream()
            .max(Comparator.comparing(Character::getFame))
            .orElse(null);
    }
    
    private Character selectBestBuffer(List<Character> buffers) {
        return buffers.stream()
            .max(Comparator.comparing(Character::getFame))
            .orElse(null);
    }
    
    private List<Character> selectUpdoongis(List<Character> updoongis, int count) {
        return updoongis.stream()
            .limit(count)
            .collect(Collectors.toList());
    }
    
    private List<Character> selectBestCharacters(List<Character> characters, int count) {
        return characters.stream()
            .limit(count)
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> createSlotInfo(Character character, String role, int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber);
        slot.put("characterId", character.getCharacterId());
        slot.put("characterName", character.getCharacterName());
        slot.put("serverId", character.getServerId());
        slot.put("jobName", character.getJobName());
        slot.put("fame", character.getFame());
        slot.put("role", role);
        slot.put("isFavorite", character.getIsFavorite());
        return slot;
    }
    
    private double calculatePartyEfficiency(List<Map<String, Object>> slots) {
        if (slots.isEmpty()) return 0.0;
        
        double totalFame = slots.stream()
            .mapToDouble(slot -> (Double) slot.get("fame"))
            .sum();
        
        double avgFame = totalFame / slots.size();
        double variance = slots.stream()
            .mapToDouble(slot -> Math.pow((Double) slot.get("fame") - avgFame, 2))
            .sum() / slots.size();
        
        // 효율성 = 평균 명성 - 표준편차 (밸런스 고려)
        return avgFame - Math.sqrt(variance);
    }
    
    private double calculateTotalEfficiency(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = (Double) party1.get("efficiency");
        double efficiency2 = (Double) party2.get("efficiency");
        return (efficiency1 + efficiency2) / 2.0;
    }
}
