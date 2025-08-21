package com.dfparty.backend.service;

import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import com.dfparty.backend.utils.CharacterUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyOptimizationService {
    
    private final CharacterService characterService;
    private final CharacterRepository characterRepository;
    private final CharacterUtils characterUtils;


    /**
     * 파티 최적화 메인 메서드
     */
    public Map<String, Object> optimizeParty(Map<String, Object> request) {
        try {
            String optimizationType = (String) request.getOrDefault("type", "balanced");
            Integer partySize = (Integer) request.getOrDefault("partySize", 4);
            String dungeonType = (String) request.get("dungeonType"); // "nabel", "venus", "fog"
            String difficulty = (String) request.get("difficulty"); // "normal", "hard" (나벨 전용)
            
            List<Character> availableCharacters = characterRepository.findByIsExcludedFalse();
            
            switch (optimizationType) {
                case "nabel":
                    // 나벨 전용 파티 구성
                    return createNabelOptimizedParty(difficulty, request);
                case "updoongi":
                    return createUpdoongiPriorityParty(availableCharacters, partySize);
                case "balanced":
                default:
                    return createBalancedParty(availableCharacters, partySize);
            }
        } catch (Exception e) {
            log.error("파티 최적화 중 오류 발생", e);
            return Map.of("error", "파티 최적화 실패: " + e.getMessage());
        }
    }

    /**
     * 프론트엔드 호환 파티 최적화 메서드
     */
    public Map<String, Object> optimizePartyForFrontend(Map<String, Object> request) {
        try {
            log.info("프론트엔드 파티 최적화 요청: {}", request);
            
            String dungeonType = (String) request.get("dungeonType");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> charactersData = (List<Map<String, Object>>) request.get("characters");
            
            if (charactersData == null || charactersData.isEmpty()) {
                return createErrorResponse("캐릭터 목록이 비어있습니다.");
            }
            
            // 캐릭터 데이터를 Character 객체로 변환
            List<Character> characters = charactersData.stream()
                .map(this::mapToCharacter)
                .collect(Collectors.toList());
            
            // 던전별 필터링 적용
            List<Character> filteredCharacters = filterCharactersByDungeon(characters, dungeonType);
            
            if (filteredCharacters.size() < 4) {
                return createErrorResponse("파티 구성에 필요한 캐릭터가 부족합니다. (최소 4명 필요)");
            }
            
            // 최적화된 파티 구성
            List<Character> optimizedParty = createOptimizedParty(filteredCharacters, dungeonType);
            
            // 응답 형식에 맞게 변환
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("party", optimizedParty.stream()
                .map(this::convertToPartyMember)
                .collect(Collectors.toList()));
            result.put("stats", calculatePartyStats(optimizedParty));
            result.put("dungeonType", dungeonType);
            
            log.info("파티 최적화 완료: {}명 파티", optimizedParty.size());
            return result;
            
        } catch (Exception e) {
            log.error("프론트엔드 파티 최적화 실패", e);
            return createErrorResponse("파티 최적화에 실패했습니다: " + e.getMessage());
        }
    }
    
    private Character mapToCharacter(Map<String, Object> data) {
        Character character = new Character();
        character.setCharacterId((String) data.get("characterId"));
        character.setServerId((String) data.get("serverId"));
        character.setCharacterName((String) data.get("characterName"));
        character.setAdventureName((String) data.get("adventureName"));
        character.setFame(((Number) data.getOrDefault("fame", 0)).longValue());
        character.setTotalDamage(((Number) data.getOrDefault("totalDamage", 0)).longValue());
        character.setBuffPower(((Number) data.getOrDefault("buffPower", 0)).longValue());
        character.setDungeonClearNabel((Boolean) data.getOrDefault("dungeonClearNabel", false));
        character.setDungeonClearVenus((Boolean) data.getOrDefault("dungeonClearVenus", false));
        character.setDungeonClearFog((Boolean) data.getOrDefault("dungeonClearFog", false));
        return character;
    }
    
    private List<Character> filterCharactersByDungeon(List<Character> characters, String dungeonType) {
        return characters.stream()
            .filter(character -> {
                switch (dungeonType) {
                    case "nabel":
                        return !Boolean.TRUE.equals(character.getDungeonClearNabel());
                    case "venus":
                        return !Boolean.TRUE.equals(character.getDungeonClearVenus());
                    case "fog":
                        return !Boolean.TRUE.equals(character.getDungeonClearFog());
                    default:
                        return true;
                }
            })
            .collect(Collectors.toList());
    }
    
    private List<Character> createOptimizedParty(List<Character> characters, String dungeonType) {
        // 캐릭터를 버프력과 전투력 기준으로 정렬
        List<Character> sortedCharacters = characters.stream()
            .sorted((a, b) -> {
                // 총 스탯 기준으로 정렬 (버프력 + 전투력)
                long totalA = (a.getBuffPower() != null ? a.getBuffPower() : 0L) + 
                             (a.getTotalDamage() != null ? a.getTotalDamage() : 0L);
                long totalB = (b.getBuffPower() != null ? b.getBuffPower() : 0L) + 
                             (b.getTotalDamage() != null ? b.getTotalDamage() : 0L);
                return Long.compare(totalB, totalA);
            })
            .collect(Collectors.toList());
        
        // 최적화된 4인 파티 구성
        List<Character> optimizedParty = new ArrayList<>();
        
        // 버퍼 우선 선택 (버프력이 전투력보다 높은 캐릭터)
        List<Character> buffers = sortedCharacters.stream()
            .filter(c -> (c.getBuffPower() != null ? c.getBuffPower() : 0L) > 
                        (c.getTotalDamage() != null ? c.getTotalDamage() : 0L))
            .collect(Collectors.toList());
        
        if (!buffers.isEmpty()) {
            optimizedParty.add(buffers.get(0));
            sortedCharacters.remove(buffers.get(0));
        }
        
        // 나머지 3명은 전투력 높은 순으로 선택
        optimizedParty.addAll(sortedCharacters.stream()
            .limit(3)
            .collect(Collectors.toList()));
        
        return optimizedParty;
    }
    
    private Map<String, Object> convertToPartyMember(Character character) {
        Map<String, Object> member = new HashMap<>();
        member.put("characterId", character.getCharacterId());
        member.put("serverId", character.getServerId());
        member.put("characterName", character.getCharacterName());
        member.put("adventureName", character.getAdventureName());
        member.put("totalDamage", character.getTotalDamage() != null ? character.getTotalDamage() : 0L);
        member.put("buffPower", character.getBuffPower() != null ? character.getBuffPower() : 0L);
        member.put("fame", character.getFame() != null ? character.getFame() : 0L);
        return member;
    }
    
    private Map<String, Object> calculatePartyStats(List<Character> party) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalDamage = party.stream()
            .mapToLong(c -> c.getTotalDamage() != null ? c.getTotalDamage() : 0L)
            .sum();
        
        long totalBuffPower = party.stream()
            .mapToLong(c -> c.getBuffPower() != null ? c.getBuffPower() : 0L)
            .sum();
        
        // 파티 전투력 계산 (간단한 공식: 전투력 + 버프력 * 1.5)
        long partyCombatPower = totalDamage + (long)(totalBuffPower * 1.5);
        
        stats.put("totalDamage", totalDamage);
        stats.put("totalBuffPower", totalBuffPower);
        stats.put("partyCombatPower", partyCombatPower);
        stats.put("memberCount", party.size());
        
        return stats;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
    
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
            .mapToDouble(slot -> ((Number) slot.get("fame")).doubleValue())
            .sum();
        
        double avgFame = totalFame / slots.size();
        double variance = slots.stream()
            .mapToDouble(slot -> Math.pow(((Number) slot.get("fame")).doubleValue() - avgFame, 2))
            .sum() / slots.size();
        
        // 효율성 = 평균 명성 - 표준편차 (밸런스 고려)
        return avgFame - Math.sqrt(variance);
    }
    
    private double calculateTotalEfficiency(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = ((Number) party1.get("efficiency")).doubleValue();
        double efficiency2 = ((Number) party2.get("efficiency")).doubleValue();
        return (efficiency1 + efficiency2) / 2.0;
    }
    
    /**
     * 나벨 최적화 파티 구성 (일반/하드 동시 지원)
     */
    private Map<String, Object> createNabelOptimizedParty(String difficulty, Map<String, Object> request) {
        log.info("나벨 최적화 파티 구성 시작: 난이도={}", difficulty);
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> adventureGroups = (List<Map<String, Object>>) request.get("adventureGroups");
            
            if (adventureGroups == null || adventureGroups.isEmpty()) {
                return Map.of("error", "모험단 그룹 정보가 필요합니다.");
            }
            
            // 1. 모험단별로 캐릭터 분류 및 필터링
            List<Map<String, Object>> eligibleCharacters = filterEligibleCharacters(difficulty, adventureGroups);
            
            if (eligibleCharacters.size() < 4) {
                return Map.of("error", "파티 구성에 필요한 캐릭터가 부족합니다. (최소 4명 필요)");
            }
            
            // 2. 최적화된 파티 구성
            List<Map<String, Object>> optimizedParties = createOptimizedNabelParties(eligibleCharacters, difficulty);
            
            // 3. 응답 구성
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("difficulty", difficulty);
            result.put("totalCharacters", eligibleCharacters.size());
            result.put("parties", optimizedParties);
            result.put("stats", calculateNabelPartyStats(optimizedParties));
            
            log.info("나벨 최적화 파티 구성 완료: {}개 파티", optimizedParties.size());
            return result;
            
        } catch (Exception e) {
            log.error("나벨 최적화 파티 구성 실패", e);
            return Map.of("error", "나벨 파티 구성에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 나벨 난이도별 적격 캐릭터 필터링
     */
    private List<Map<String, Object>> filterEligibleCharacters(String difficulty, List<Map<String, Object>> adventureGroups) {
        List<Map<String, Object>> eligibleCharacters = new ArrayList<>();
        
        for (Map<String, Object> adventureGroup : adventureGroups) {
            String adventureName = (String) adventureGroup.get("adventureName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> characters = (List<Map<String, Object>>) adventureGroup.get("characters");
            
            if (characters == null) continue;
            
            for (Map<String, Object> character : characters) {
                if (isEligibleForNabel(character, difficulty)) {
                    // 모험단명 추가
                    character.put("adventureName", adventureName);
                    eligibleCharacters.add(character);
                }
            }
        }
        
        return eligibleCharacters;
    }
    
    /**
     * 나벨 난이도별 적격 여부 판단
     */
    private boolean isEligibleForNabel(Map<String, Object> character, String difficulty) {
        // 나벨 클리어 여부 확인
        Boolean dungeonClearNabel = (Boolean) character.get("dungeonClearNabel");
        if (Boolean.TRUE.equals(dungeonClearNabel)) {
            return false; // 이미 클리어한 캐릭터는 제외
        }
        
        // 명성 확인
        Number fame = (Number) character.get("fame");
        if (fame == null || fame.longValue() < 50000) { // 명성 5만 이상
            return false;
        }
        
        // 난이도별 스탯 기준 확인
        if ("hard".equals(difficulty)) {
            return isHardNabelEligible(character);
        } else {
            // 일반 난이도: 하드 대상자는 제외하고 일반 대상자만
            return isNormalNabelEligible(character) && !isHardNabelEligible(character);
        }
    }
    
    /**
     * 하드 나벨 적격 여부
     */
    private boolean isHardNabelEligible(Map<String, Object> character) {
        Number totalDamage = (Number) character.get("totalDamage");
        Number buffPower = (Number) character.get("buffPower");
        
        if (totalDamage != null && totalDamage.longValue() >= 10000000000L) { // 100억 이상
            return true;
        }
        
        if (buffPower != null && buffPower.longValue() >= 5000000L) { // 500만 이상
            return true;
        }
        
        return false;
    }
    
    /**
     * 일반 나벨 적격 여부
     */
    private boolean isNormalNabelEligible(Map<String, Object> character) {
        Number totalDamage = (Number) character.get("totalDamage");
        Number buffPower = (Number) character.get("buffPower");
        
        if (totalDamage != null && totalDamage.longValue() >= 3000000000L) { // 30억 이상
            return true;
        }
        
        if (buffPower != null && buffPower.longValue() >= 4000000L) { // 400만 이상
            return true;
        }
        
        return false;
    }
    
    /**
     * 최적화된 나벨 파티 구성
     */
    private List<Map<String, Object>> createOptimizedNabelParties(List<Map<String, Object>> characters, String difficulty) {
        List<Map<String, Object>> parties = new ArrayList<>();
        List<Map<String, Object>> remainingCharacters = new ArrayList<>(characters);
        
        log.info("파티 구성 시작: 총 {}명의 캐릭터", characters.size());
        
        // 가능한 많은 파티를 구성 (4명씩)
        while (remainingCharacters.size() >= 4) {
            Map<String, Object> party = createBalancedPartyWithAdventureConstraint(remainingCharacters, difficulty);
            if (party != null) {
                parties.add(party);
                
                // 파티에 사용된 캐릭터들을 제거
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> partyMembers = (List<Map<String, Object>>) party.get("members");
                remainingCharacters.removeAll(partyMembers);
                
                log.info("파티 {} 구성 완료: {}명, 남은 캐릭터: {}명", parties.size(), partyMembers.size(), remainingCharacters.size());
            } else {
                // 파티를 구성할 수 없는 경우 중단
                log.warn("더 이상 파티를 구성할 수 없습니다. 남은 캐릭터: {}명", remainingCharacters.size());
                break;
            }
        }
        
        log.info("파티 구성 완료: 총 {}개 파티, 남은 캐릭터: {}명", parties.size(), remainingCharacters.size());
        return parties;
    }
    
    /**
     * 모험단 중복 제약 조건을 만족하는 균형잡힌 파티 구성
     * 버퍼가 있으면 다가는 기준으로 파티 구성
     */
    private Map<String, Object> createBalancedPartyWithAdventureConstraint(List<Map<String, Object>> characters, String difficulty) {
        if (characters.size() < 4) return null;
        
        // 1. 버퍼 우선 선택 (가장 높은 버프력)
        Map<String, Object> buffer = characters.stream()
            .filter(c -> isBuffer(c))
            .max(Comparator.comparing(c -> ((Number) c.get("buffPower")).longValue()))
            .orElse(null);
        
        final String bufferAdventure = buffer != null ? (String) buffer.get("adventureName") : null;
        
        // 2. 뾰족한 딜러 1명 선택 (버퍼와 다른 모험단에서, 가장 높은 딜)
        Map<String, Object> topDealer = characters.stream()
            .filter(c -> isDealer(c) && 
                        (bufferAdventure == null || !bufferAdventure.equals(c.get("adventureName"))))
            .max(Comparator.comparing(c -> ((Number) c.get("totalDamage")).longValue()))
            .orElse(null);
        
        // 버퍼와 같은 모험단에서 딜러를 찾을 수 없는 경우, 모든 딜러에서 선택
        if (topDealer == null) {
            topDealer = characters.stream()
                .filter(c -> isDealer(c))
                .max(Comparator.comparing(c -> ((Number) c.get("totalDamage")).longValue()))
                .orElse(null);
        }
        
        if (topDealer == null) return null;
        
        final String finalTopDealerAdventure = (String) topDealer.get("adventureName");
        final String finalBufferAdventure = bufferAdventure;
        final Map<String, Object> finalTopDealer = topDealer;
        final Map<String, Object> finalBuffer = buffer;
        
        // 3. 컷 딜러 2명 선택 (서로 다른 모험단에서, 이미 선택된 모험단 제외)
        Set<String> usedAdventures = new HashSet<>();
        if (finalBuffer != null) {
            usedAdventures.add(finalBufferAdventure);
        }
        usedAdventures.add(finalTopDealerAdventure);
        
        List<Map<String, Object>> cutDealers = characters.stream()
            .filter(c -> isDealer(c) && 
                        !usedAdventures.contains(c.get("adventureName")) &&
                        !c.equals(finalTopDealer))
            .sorted(Comparator.comparing(c -> ((Number) c.get("totalDamage")).longValue()))
            .limit(2)
            .collect(Collectors.toList());
        
        // 4. 파티 구성 검증 및 보완
        if (cutDealers.size() < 2) {
            // 컷 딜러가 부족한 경우, 남은 캐릭터로 보완
            List<Map<String, Object>> remainingCharacters = characters.stream()
                .filter(c -> !c.equals(finalTopDealer) && 
                            !c.equals(finalBuffer) &&
                            !cutDealers.contains(c))
                .collect(Collectors.toList());
            
            // 딜러 우선, 그 다음 다른 직업
            List<Map<String, Object>> additionalDealers = remainingCharacters.stream()
                .filter(c -> isDealer(c))
                .limit(2 - cutDealers.size())
                .collect(Collectors.toList());
            
            cutDealers.addAll(additionalDealers);
            
            // 여전히 부족한 경우, 모든 남은 캐릭터로 보완
            if (cutDealers.size() < 2) {
                List<Map<String, Object>> otherCharacters = remainingCharacters.stream()
                    .filter(c -> !cutDealers.contains(c))
                    .limit(2 - cutDealers.size())
                    .collect(Collectors.toList());
                
                cutDealers.addAll(otherCharacters);
            }
        }
        
        // 5. 파티 구성
        List<Map<String, Object>> partyMembers = new ArrayList<>();
        if (finalBuffer != null) partyMembers.add(finalBuffer); // 버퍼 우선 배치
        partyMembers.add(finalTopDealer);
        partyMembers.addAll(cutDealers);
        
        // 6. 파티 정보 구성
        Map<String, Object> party = new HashMap<>();
        party.put("adventureName", "혼합 모험단"); // 여러 모험단으로 구성
        party.put("difficulty", difficulty);
        party.put("members", partyMembers);
        party.put("partySize", partyMembers.size());
        party.put("efficiency", calculatePartyEfficiency(partyMembers));
        
        log.info("파티 구성 완료: 버퍼={}, 딜러={}, 총 {}명", 
                finalBuffer != null ? "있음" : "없음", 
                partyMembers.size() - (finalBuffer != null ? 1 : 0),
                partyMembers.size());
        
        return party;
    }
    
    /**
     * 딜러 여부 판단
     */
    private boolean isDealer(Map<String, Object> character) {
        return !isBuffer(character);
    }
    
    /**
     * 버퍼 여부 판단 (직업명 기반)
     */
    private boolean isBuffer(Map<String, Object> character) {
        String jobName = (String) character.get("jobName");
        String jobGrowName = (String) character.get("jobGrowName");
        
        return characterUtils.isBuffer(jobName, jobGrowName);

    }
    
    /**
     * 나벨 파티 통계 계산
     */
    private Map<String, Object> calculateNabelPartyStats(List<Map<String, Object>> parties) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalParties = parties.size();
        long totalMembers = parties.stream()
            .mapToLong(p -> ((List<?>) p.get("members")).size())
            .sum();
        
        double avgEfficiency = parties.stream()
            .mapToDouble(p -> ((Number) p.get("efficiency")).doubleValue())
            .average()
            .orElse(0.0);
        
        stats.put("totalParties", totalParties);
        stats.put("totalMembers", totalMembers);
        stats.put("averageEfficiency", avgEfficiency);
        
        return stats;
    }
}
