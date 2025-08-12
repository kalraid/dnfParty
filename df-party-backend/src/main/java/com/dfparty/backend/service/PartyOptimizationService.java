package com.dfparty.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PartyOptimizationService {

    @Autowired
    private DungeonClearService dungeonClearService;

    /**
     * 자동 파티 구성을 위한 최적화 알고리즘
     */
    public Map<String, Object> optimizeParty(Map<String, Object> request) {
        try {
            String dungeonType = (String) request.get("dungeonType");
            String navelMode = (String) request.get("navelMode");
            int partySize = (Integer) request.get("partySize");
            long minDamageCut = (Long) request.get("minDamageCut");
            List<Map<String, Object>> characters = (List<Map<String, Object>>) request.get("characters");

            // 던전별 요구사항에 맞는 캐릭터 필터링
            List<Map<String, Object>> validCharacters = filterCharactersByDungeon(
                characters, dungeonType, navelMode, minDamageCut
            );

            if (validCharacters.size() < partySize) {
                return createErrorResponse("파티 구성에 필요한 캐릭터가 부족합니다.");
            }

            // 파티 최적화 실행
            List<Map<String, Object>> optimizedParty = createOptimizedParty(
                validCharacters, partySize, dungeonType
            );

            // 파티 전투력 계산
            Map<String, Object> partyStats = calculatePartyStats(optimizedParty);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("party", optimizedParty);
            result.put("stats", partyStats);
            result.put("message", "파티 최적화가 완료되었습니다.");

            return result;

        } catch (Exception e) {
            return createErrorResponse("파티 최적화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전별 요구사항에 맞는 캐릭터 필터링
     */
    private List<Map<String, Object>> filterCharactersByDungeon(
            List<Map<String, Object>> characters, 
            String dungeonType, 
            String navelMode, 
            long minDamageCut) {
        
        List<Map<String, Object>> validCharacters = new ArrayList<>();

        for (Map<String, Object> character : characters) {
            long totalDamage = ((Number) character.get("totalDamage")).longValue();
            long buffPower = ((Number) character.get("buffPower")).longValue();
            long fame = ((Number) character.get("fame")).longValue();

            // 기본 딜컷 체크
            if (totalDamage < minDamageCut) continue;

            boolean isValid = false;

            switch (dungeonType) {
                case "navel":
                    if ("hard".equals(navelMode)) {
                        // 하드 나벨: 전투력 100억, 버프력 500만 이상
                        isValid = totalDamage >= 10000000000L || buffPower >= 5000000L;
                    } else {
                        // 일반 나벨: 전투력 30억, 버프력 400만 이상
                        isValid = totalDamage >= 3000000000L || buffPower >= 4000000L;
                    }
                    // 나벨: 명성 63000 이상
                    isValid = isValid && fame >= 63000;
                    break;

                case "venus":
                    // 베누스: 명성 41929 이상
                    isValid = fame >= 41929;
                    break;

                case "fog":
                    // 안개신: 명성 32253 이상
                    isValid = fame >= 32253;
                    break;

                default:
                    isValid = true;
                    break;
            }

            if (isValid) {
                validCharacters.add(character);
            }
        }

        return validCharacters;
    }

    /**
     * 최적화된 파티 구성 생성
     */
    private List<Map<String, Object>> createOptimizedParty(
            List<Map<String, Object>> characters, 
            int partySize, 
            String dungeonType) {
        
        // 버퍼와 딜러 분리
        List<Map<String, Object>> buffers = new ArrayList<>();
        List<Map<String, Object>> dealers = new ArrayList<>();

        for (Map<String, Object> character : characters) {
            long totalDamage = ((Number) character.get("totalDamage")).longValue();
            long buffPower = ((Number) character.get("buffPower")).longValue();

            if (buffPower > totalDamage) {
                buffers.add(character);
            } else {
                dealers.add(character);
            }
        }

        // 파티 구성 규칙에 따른 멤버 선택
        List<Map<String, Object>> selectedMembers = new ArrayList<>();

        // 던전별 최소 구성 요구사항 체크
        int minDealers = getMinDealers(dungeonType);
        int minBuffers = getMinBuffers(dungeonType);

        // 버퍼 선택
        int neededBuffers = Math.min(minBuffers, partySize);
        if (buffers.size() >= neededBuffers) {
            // 버프력 순으로 정렬하여 최고 버퍼 선택
            buffers.sort((a, b) -> Long.compare(
                ((Number) b.get("buffPower")).longValue(),
                ((Number) a.get("buffPower")).longValue()
            ));
            selectedMembers.addAll(buffers.subList(0, neededBuffers));
        }

        // 딜러 선택
        int neededDealers = partySize - selectedMembers.size();
        if (dealers.size() >= neededDealers) {
            // 전투력 순으로 정렬하여 최고 딜러 선택
            dealers.sort((a, b) -> Long.compare(
                ((Number) b.get("totalDamage")).longValue(),
                ((Number) a.get("totalDamage")).longValue()
            ));
            selectedMembers.addAll(dealers.subList(0, neededDealers));
        }

        return selectedMembers;
    }

    /**
     * 던전별 최소 딜러 요구사항
     */
    private int getMinDealers(String dungeonType) {
        switch (dungeonType) {
            case "fog": return 2;  // 안개신: 최소 2명 딜러
            default: return 1;     // 기본: 최소 1명 딜러
        }
    }

    /**
     * 던전별 최소 버퍼 요구사항
     */
    private int getMinBuffers(String dungeonType) {
        switch (dungeonType) {
            case "fog": return 1;  // 안개신: 최소 1명 버퍼
            default: return 1;     // 기본: 최소 1명 버퍼
        }
    }

    /**
     * 파티 전투력 계산
     */
    private Map<String, Object> calculatePartyStats(List<Map<String, Object>> party) {
        long totalDamage = 0;
        long totalBuffPower = 0;
        long partyCombatPower = 0;

        for (Map<String, Object> member : party) {
            totalDamage += ((Number) member.get("totalDamage")).longValue();
            totalBuffPower += ((Number) member.get("buffPower")).longValue();
        }

        // 파티 전투력 = (총 딜러 전투력) * (총 버프력) / 100
        partyCombatPower = (totalDamage * totalBuffPower) / 100;

        Map<String, Object> stats = new HashMap<>();
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
}
