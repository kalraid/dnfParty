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
public class PartyRecommendationService {

    /**
     * 파티 구성 추천 생성
     */
    public Map<String, Object> generatePartyRecommendation(
            List<Character> characters, 
            String dungeonName, 
            int partySize,
            Map<String, Object> preferences) {
        
        try {
            log.info("파티 구성 추천 생성 시작: 던전={}, 파티크기={}, 캐릭터수={}", 
                    dungeonName, partySize, characters.size());

            // 1. 던전 요구사항 분석
            Map<String, Object> dungeonRequirements = analyzeDungeonRequirements(dungeonName);
            
            // 2. 캐릭터 분석 및 분류
            Map<String, List<Character>> categorizedCharacters = categorizeCharacters(characters, dungeonRequirements);
            
            // 3. 추천 전략 선택
            String strategy = selectRecommendationStrategy(preferences, dungeonRequirements);
            
            // 4. 전략별 파티 구성 생성
            Map<String, Object> recommendation = createPartyByStrategy(
                    categorizedCharacters, dungeonName, partySize, strategy, preferences);
            
            // 5. 추천 결과 분석 및 점수 계산
            Map<String, Object> analysis = analyzeRecommendation(recommendation, dungeonRequirements);
            
            // 6. 대안 파티 구성 생성
            List<Map<String, Object>> alternatives = generateAlternativeParties(
                    categorizedCharacters, dungeonName, partySize, strategy, preferences);
            
            // 7. 최종 추천 결과 구성
            Map<String, Object> result = new HashMap<>();
            result.put("recommendation", recommendation);
            result.put("analysis", analysis);
            result.put("alternatives", alternatives);
            result.put("strategy", strategy);
            result.put("dungeonRequirements", dungeonRequirements);
            result.put("characterAnalysis", analyzeCharacterPool(categorizedCharacters));
            result.put("generatedAt", new Date());
            
            log.info("파티 구성 추천 생성 완료: 전략={}, 점수={}", 
                    strategy, analysis.get("totalScore"));
            
            return result;
            
        } catch (Exception e) {
            log.error("파티 구성 추천 생성 실패", e);
            return createErrorResult("파티 구성 추천 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전별 추천 전략 생성
     */
    public Map<String, Object> generateDungeonSpecificRecommendations(
            List<Character> characters, 
            String dungeonName) {
        
        try {
            log.info("던전별 추천 전략 생성: 던전={}", dungeonName);
            
            Map<String, Object> dungeonRequirements = analyzeDungeonRequirements(dungeonName);
            Map<String, List<Character>> categorizedCharacters = categorizeCharacters(characters, dungeonRequirements);
            
            Map<String, Object> result = new HashMap<>();
            
            // 4인 파티 추천
            Map<String, Object> party4Recommendation = generatePartyRecommendation(
                    characters, dungeonName, 4, new HashMap<>());
            result.put("party4", party4Recommendation);
            
            // 8인 파티 추천
            Map<String, Object> party8Recommendation = generatePartyRecommendation(
                    characters, dungeonName, 8, new HashMap<>());
            result.put("party8", party8Recommendation);
            
            // 던전별 최적 전략 추천
            String optimalStrategy = recommendOptimalStrategy(dungeonName, categorizedCharacters);
            result.put("optimalStrategy", optimalStrategy);
            
            // 던전별 특화 파티 구성
            Map<String, Object> specializedParty = createSpecializedParty(
                    categorizedCharacters, dungeonName, optimalStrategy);
            result.put("specializedParty", specializedParty);
            
            result.put("dungeonRequirements", dungeonRequirements);
            result.put("characterPool", analyzeCharacterPool(categorizedCharacters));
            
            return result;
            
        } catch (Exception e) {
            log.error("던전별 추천 전략 생성 실패", e);
            return createErrorResult("던전별 추천 전략 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 개인화된 파티 추천 생성
     */
    public Map<String, Object> generatePersonalizedRecommendation(
            List<Character> characters, 
            String dungeonName, 
            int partySize,
            Map<String, Object> userPreferences,
            Map<String, Object> playHistory) {
        
        try {
            log.info("개인화된 파티 추천 생성: 던전={}, 파티크기={}", dungeonName, partySize);
            
            // 1. 사용자 선호도 분석
            Map<String, Object> analyzedPreferences = analyzeUserPreferences(userPreferences, playHistory);
            
            // 2. 플레이 히스토리 기반 패턴 분석
            Map<String, Object> playPatterns = analyzePlayPatterns(playHistory, dungeonName);
            
            // 3. 선호도 기반 캐릭터 가중치 적용
            List<Character> weightedCharacters = applyCharacterWeights(characters, analyzedPreferences);
            
            // 4. 개인화된 추천 생성
            Map<String, Object> recommendation = generatePartyRecommendation(
                    weightedCharacters, dungeonName, partySize, analyzedPreferences);
            
            // 5. 개인화 요소 추가
            recommendation.put("personalizationFactors", analyzedPreferences);
            recommendation.put("playPatterns", playPatterns);
            recommendation.put("userPreferences", userPreferences);
            
            return recommendation;
            
        } catch (Exception e) {
            log.error("개인화된 파티 추천 생성 실패", e);
            return createErrorResult("개인화된 파티 추천 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전 요구사항 분석
     */
    private Map<String, Object> analyzeDungeonRequirements(String dungeonName) {
        Map<String, Object> requirements = new HashMap<>();
        
        // 던전별 기본 요구사항
        switch (dungeonName.toLowerCase()) {
            case "발할라":
                requirements.put("minFame", 50000);
                requirements.put("requiredRoles", Arrays.asList("딜러", "딜러", "딜러", "버퍼"));
                requirements.put("preferredJobs", Arrays.asList("소드마스터", "버서커", "아수라", "크루세이더"));
                requirements.put("difficulty", "매우높음");
                break;
            case "오즈마":
                requirements.put("minFame", 45000);
                requirements.put("requiredRoles", Arrays.asList("딜러", "딜러", "딜러", "버퍼"));
                requirements.put("preferredJobs", Arrays.asList("소드마스터", "버서커", "아수라", "크루세이더"));
                requirements.put("difficulty", "높음");
                break;
            case "시로코":
                requirements.put("minFame", 40000);
                requirements.put("requiredRoles", Arrays.asList("딜러", "딜러", "딜러", "버퍼"));
                requirements.put("preferredJobs", Arrays.asList("소드마스터", "버서커", "아수라", "크루세이더"));
                requirements.put("difficulty", "보통");
                break;
            default:
                requirements.put("minFame", 30000);
                requirements.put("requiredRoles", Arrays.asList("딜러", "딜러", "딜러", "버퍼"));
                requirements.put("preferredJobs", new ArrayList<>());
                requirements.put("difficulty", "보통");
        }
        
        requirements.put("dungeonName", dungeonName);
        return requirements;
    }

    /**
     * 캐릭터 분류
     */
    private Map<String, List<Character>> categorizeCharacters(
            List<Character> characters, 
            Map<String, Object> dungeonRequirements) {
        
        Map<String, List<Character>> categorized = new HashMap<>();
        
        int minFame = (Integer) dungeonRequirements.get("minFame");
        
        // 명성 기준 필터링
        List<Character> eligibleCharacters = characters.stream()
                .filter(c -> c.getFame() != null && c.getFame() >= minFame)
                .collect(Collectors.toList());
        
        // 역할별 분류
        categorized.put("dealers", filterDealers(eligibleCharacters));
        categorized.put("buffers", filterBuffers(eligibleCharacters));
        categorized.put("updoongis", filterUpdoongis(eligibleCharacters));
        categorized.put("others", filterOthers(eligibleCharacters));
        
        return categorized;
    }

    /**
     * 추천 전략 선택
     */
    private String selectRecommendationStrategy(
            Map<String, Object> preferences, 
            Map<String, Object> dungeonRequirements) {
        
        String difficulty = (String) dungeonRequirements.get("difficulty");
        
        // 사용자 선호도가 있으면 우선 적용
        if (preferences.containsKey("strategy")) {
            return (String) preferences.get("strategy");
        }
        
        // 던전 난이도에 따른 기본 전략
        switch (difficulty) {
            case "매우높음":
                return "safety"; // 안전성 우선
            case "높음":
                return "balance"; // 밸런스 우선
            case "보통":
                return "efficiency"; // 효율성 우선
            default:
                return "hybrid"; // 하이브리드
        }
    }

    /**
     * 전략별 파티 구성 생성
     */
    private Map<String, Object> createPartyByStrategy(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize,
            String strategy,
            Map<String, Object> preferences) {
        
        switch (strategy) {
            case "efficiency":
                return createEfficiencyParty(categorizedCharacters, dungeonName, partySize);
            case "balance":
                return createBalanceParty(categorizedCharacters, dungeonName, partySize);
            case "safety":
                return createSafetyParty(categorizedCharacters, dungeonName, partySize);
            case "synergy":
                return createSynergyParty(categorizedCharacters, dungeonName, partySize);
            case "hybrid":
                return createHybridParty(categorizedCharacters, dungeonName, partySize);
            default:
                return createBalanceParty(categorizedCharacters, dungeonName, partySize);
        }
    }

    /**
     * 효율성 중심 파티 구성
     */
    private Map<String, Object> createEfficiencyParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize) {
        
        List<Character> dealers = categorizedCharacters.get("dealers");
        List<Character> buffers = categorizedCharacters.get("buffers");
        List<Character> updoongis = categorizedCharacters.get("updoongis");
        
        // 명성 순으로 정렬
        dealers.sort((a, b) -> Long.compare(b.getFame() != null ? b.getFame() : 0L, a.getFame() != null ? a.getFame() : 0L));
        buffers.sort((a, b) -> Long.compare(b.getFame() != null ? b.getFame() : 0L, a.getFame() != null ? a.getFame() : 0L));
        updoongis.sort((a, b) -> Long.compare(b.getFame() != null ? b.getFame() : 0L, a.getFame() != null ? a.getFame() : 0L));
        
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        if (partySize == 4) {
            // 4인 파티: 최고 명성 3딜러 + 최고 명성 1버퍼
            for (int i = 0; i < 3 && i < dealers.size(); i++) {
                slots.add(createSlotInfo(dealers.get(i), "딜러", i + 1));
            }
            if (buffers.size() > 0) {
                slots.add(createSlotInfo(buffers.get(0), "버퍼", 4));
            }
        } else if (partySize == 8) {
            // 8인 파티: 2개 파티로 분할
            Map<String, Object> party1 = createEfficiencyParty(categorizedCharacters, dungeonName, 4);
            Map<String, Object> party2 = createEfficiencyParty(categorizedCharacters, dungeonName, 4);
            
            party.put("party1", party1);
            party.put("party2", party2);
            party.put("totalEfficiency", calculateTotalEfficiency(party1, party2));
            return party;
        }
        
        party.put("slots", slots);
        party.put("efficiency", calculatePartyEfficiency(slots));
        party.put("strategy", "efficiency");
        
        return party;
    }

    /**
     * 밸런스 중심 파티 구성
     */
    private Map<String, Object> createBalanceParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize) {
        
        // 효율성과 안전성의 균형
        Map<String, Object> efficiencyParty = createEfficiencyParty(categorizedCharacters, dungeonName, partySize);
        Map<String, Object> safetyParty = createSafetyParty(categorizedCharacters, dungeonName, partySize);
        
        // 두 전략의 결과를 비교하여 더 균형잡힌 파티 선택
        double efficiencyScore = (Double) efficiencyParty.get("efficiency");
        double safetyScore = (Double) safetyParty.get("safety");
        
        if (Math.abs(efficiencyScore - safetyScore) < 0.1) {
            return efficiencyParty; // 비슷하면 효율성 우선
        } else if (efficiencyScore > safetyScore) {
            return efficiencyParty;
        } else {
            return safetyParty;
        }
    }

    /**
     * 안전성 중심 파티 구성
     */
    private Map<String, Object> createSafetyParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize) {
        
        List<Character> dealers = categorizedCharacters.get("dealers");
        List<Character> buffers = categorizedCharacters.get("buffers");
        List<Character> updoongis = categorizedCharacters.get("updoongis");
        
        // 안전성을 위해 업둥이 캐릭터 우선 배치
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        if (partySize == 4) {
            // 업둥이 우선 배치
            int updoongiCount = Math.min(2, updoongis.size());
            for (int i = 0; i < updoongiCount; i++) {
                slots.add(createSlotInfo(updoongis.get(i), "업둥이", i + 1));
            }
            
            // 나머지는 명성 순으로 배치
            int remainingSlots = partySize - updoongiCount;
            int dealerCount = Math.min(remainingSlots - 1, dealers.size());
            int bufferCount = Math.min(1, buffers.size());
            
            for (int i = 0; i < dealerCount; i++) {
                slots.add(createSlotInfo(dealers.get(i), "딜러", slots.size() + 1));
            }
            
            if (bufferCount > 0) {
                slots.add(createSlotInfo(buffers.get(0), "버퍼", slots.size() + 1));
            }
        }
        
        party.put("slots", slots);
        party.put("safety", calculateSafetyScore(slots));
        party.put("strategy", "safety");
        
        return party;
    }

    /**
     * 시너지 중심 파티 구성
     */
    private Map<String, Object> createSynergyParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize) {
        
        // 직업 간 시너지를 고려한 파티 구성
        Map<String, Object> party = new HashMap<>();
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 시너지가 좋은 직업 조합 우선
        List<String> synergyJobs = Arrays.asList("소드마스터", "버서커", "아수라", "크루세이더");
        
        // 시너지 직업 우선 배치
        for (String job : synergyJobs) {
            if (slots.size() >= partySize) break;
            
            Character character = findCharacterByJob(categorizedCharacters, job);
            if (character != null) {
                String role = determineRole(character);
                slots.add(createSlotInfo(character, role, slots.size() + 1));
            }
        }
        
        // 나머지 슬롯 채우기
        fillRemainingSlots(categorizedCharacters, slots, partySize);
        
        party.put("slots", slots);
        party.put("synergy", calculateSynergyScore(slots));
        party.put("strategy", "synergy");
        
        return party;
    }

    /**
     * 하이브리드 파티 구성
     */
    private Map<String, Object> createHybridParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize) {
        
        // 여러 전략의 장점을 결합
        Map<String, Object> efficiencyParty = createEfficiencyParty(categorizedCharacters, dungeonName, partySize);
        Map<String, Object> safetyParty = createSafetyParty(categorizedCharacters, dungeonName, partySize);
        Map<String, Object> synergyParty = createSynergyParty(categorizedCharacters, dungeonName, partySize);
        
        // 각 전략의 점수 계산
        double efficiencyScore = (Double) efficiencyParty.get("efficiency");
        double safetyScore = (Double) safetyParty.get("safety");
        double synergyScore = (Double) synergyParty.get("synergy");
        
        // 가중 평균으로 최적 파티 선택
        Map<String, Object> bestParty = efficiencyParty;
        double bestScore = efficiencyScore * 0.4 + safetyScore * 0.3 + synergyScore * 0.3;
        
        double currentScore = efficiencyScore * 0.4 + safetyScore * 0.3 + synergyScore * 0.3;
        if (currentScore > bestScore) {
            bestParty = efficiencyParty;
            bestScore = currentScore;
        }
        
        currentScore = efficiencyScore * 0.4 + safetyScore * 0.3 + synergyScore * 0.3;
        if (currentScore > bestScore) {
            bestParty = safetyParty;
            bestScore = currentScore;
        }
        
        bestParty.put("strategy", "hybrid");
        bestParty.put("hybridScore", bestScore);
        
        return bestParty;
    }

    /**
     * 추천 결과 분석
     */
    private Map<String, Object> analyzeRecommendation(
            Map<String, Object> recommendation, 
            Map<String, Object> dungeonRequirements) {
        
        Map<String, Object> analysis = new HashMap<>();
        
        if (recommendation.containsKey("party1") && recommendation.containsKey("party2")) {
            // 8인 파티 분석
            Map<String, Object> party1 = (Map<String, Object>) recommendation.get("party1");
            Map<String, Object> party2 = (Map<String, Object>) recommendation.get("party2");
            
            analysis.put("party1Score", party1.get("efficiency"));
            analysis.put("party2Score", party2.get("efficiency"));
            analysis.put("totalScore", recommendation.get("totalEfficiency"));
            analysis.put("balance", calculateBalanceScore(party1, party2));
        } else {
            // 4인 파티 분석
            List<Map<String, Object>> slots = (List<Map<String, Object>>) recommendation.get("slots");
            analysis.put("totalScore", recommendation.get("efficiency"));
            analysis.put("roleBalance", analyzeRoleBalance(slots));
            analysis.put("fameBalance", analyzeFameBalance(slots));
        }
        
        analysis.put("dungeonCompatibility", calculateDungeonCompatibility(recommendation, dungeonRequirements));
        analysis.put("overallRating", calculateOverallRating(analysis));
        
        return analysis;
    }

    /**
     * 대안 파티 구성 생성
     */
    private List<Map<String, Object>> generateAlternativeParties(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            int partySize,
            String primaryStrategy,
            Map<String, Object> preferences) {
        
        List<Map<String, Object>> alternatives = new ArrayList<>();
        
        // 다른 전략들로 파티 구성
        String[] strategies = {"efficiency", "balance", "safety", "synergy", "hybrid"};
        
        for (String strategy : strategies) {
            if (!strategy.equals(primaryStrategy)) {
                Map<String, Object> alternative = createPartyByStrategy(
                        categorizedCharacters, dungeonName, partySize, strategy, preferences);
                alternatives.add(alternative);
            }
        }
        
        // 점수 순으로 정렬
        alternatives.sort((a, b) -> {
            double scoreA = getPartyScore(a);
            double scoreB = getPartyScore(b);
            return Double.compare(scoreB, scoreA);
        });
        
        return alternatives;
    }

    /**
     * 캐릭터 풀 분석
     */
    private Map<String, Object> analyzeCharacterPool(Map<String, List<Character>> categorizedCharacters) {
        Map<String, Object> analysis = new HashMap<>();
        
        analysis.put("totalCharacters", categorizedCharacters.values().stream()
                .mapToInt(List::size).sum());
        analysis.put("dealers", categorizedCharacters.get("dealers").size());
        analysis.put("buffers", categorizedCharacters.get("buffers").size());
        analysis.put("updoongis", categorizedCharacters.get("updoongis").size());
        analysis.put("others", categorizedCharacters.get("others").size());
        
        // 평균 명성 계산
        double avgFame = categorizedCharacters.values().stream()
                .flatMap(List::stream)
                .mapToLong(c -> c.getFame() != null ? c.getFame() : 0L)
                .average()
                .orElse(0.0);
        analysis.put("averageFame", avgFame);
        
        return analysis;
    }

    /**
     * 사용자 선호도 분석
     */
    private Map<String, Object> analyzeUserPreferences(
            Map<String, Object> userPreferences, 
            Map<String, Object> playHistory) {
        
        Map<String, Object> analysis = new HashMap<>();
        
        // 기본값 설정
        analysis.put("strategy", userPreferences.getOrDefault("strategy", "balance"));
        analysis.put("preferUpdoongis", userPreferences.getOrDefault("preferUpdoongis", true));
        analysis.put("preferHighFame", userPreferences.getOrDefault("preferHighFame", true));
        analysis.put("preferJobSynergy", userPreferences.getOrDefault("preferJobSynergy", true));
        
        // 플레이 히스토리 기반 선호도 분석
        if (playHistory != null && playHistory.containsKey("favoriteJobs")) {
            analysis.put("favoriteJobs", playHistory.get("favoriteJobs"));
        }
        
        return analysis;
    }

    /**
     * 플레이 패턴 분석
     */
    private Map<String, Object> analyzePlayPatterns(
            Map<String, Object> playHistory, 
            String dungeonName) {
        
        Map<String, Object> patterns = new HashMap<>();
        
        if (playHistory != null) {
            patterns.put("frequentDungeons", playHistory.get("frequentDungeons"));
            patterns.put("preferredPartySize", playHistory.get("preferredPartySize"));
            patterns.put("successRate", playHistory.get("successRate"));
        }
        
        return patterns;
    }

    /**
     * 캐릭터 가중치 적용
     */
    private List<Character> applyCharacterWeights(
            List<Character> characters, 
            Map<String, Object> preferences) {
        
        // 선호도에 따른 가중치 적용
        List<Character> weightedCharacters = new ArrayList<>(characters);
        
        // 업둥이 선호도가 높으면 업둥이 캐릭터에 가중치 부여
        if ((Boolean) preferences.get("preferUpdoongis")) {
            weightedCharacters.sort((a, b) -> {
                boolean aIsUpdoongi = a.getIsFavorite() != null && a.getIsFavorite();
                boolean bIsUpdoongi = b.getIsFavorite() != null && b.getIsFavorite();
                
                if (aIsUpdoongi && !bIsUpdoongi) return -1;
                if (!aIsUpdoongi && bIsUpdoongi) return 1;
                return Long.compare(b.getFame() != null ? b.getFame() : 0L, a.getFame() != null ? a.getFame() : 0L);
            });
        }
        
        return weightedCharacters;
    }

    /**
     * 최적 전략 추천
     */
    private String recommendOptimalStrategy(String dungeonName, Map<String, List<Character>> categorizedCharacters) {
        // 던전과 캐릭터 풀을 고려한 최적 전략 추천
        int totalCharacters = categorizedCharacters.values().stream().mapToInt(List::size).sum();
        int updoongiCount = categorizedCharacters.get("updoongis").size();
        int dealerCount = categorizedCharacters.get("dealers").size();
        int bufferCount = categorizedCharacters.get("buffers").size();
        
        if (updoongiCount >= 2 && dealerCount >= 3 && bufferCount >= 1) {
            return "hybrid"; // 모든 조건이 충족되면 하이브리드
        } else if (updoongiCount >= 1) {
            return "safety"; // 업둥이가 있으면 안전성 우선
        } else if (dealerCount >= 4 && bufferCount >= 2) {
            return "efficiency"; // 딜러와 버퍼가 충분하면 효율성 우선
        } else {
            return "balance"; // 기본적으로 밸런스
        }
    }

    /**
     * 특화 파티 구성
     */
    private Map<String, Object> createSpecializedParty(
            Map<String, List<Character>> categorizedCharacters,
            String dungeonName,
            String strategy) {
        
        // 던전별 특화 파티 구성
        Map<String, Object> party = new HashMap<>();
        
        if ("발할라".equals(dungeonName)) {
            // 발할라는 최고 명성 캐릭터들로 구성
            party = createEfficiencyParty(categorizedCharacters, dungeonName, 4);
            party.put("specialization", "발할라 특화 - 최고 명성 우선");
        } else if ("오즈마".equals(dungeonName)) {
            // 오즈마는 밸런스 중심
            party = createBalanceParty(categorizedCharacters, dungeonName, 4);
            party.put("specialization", "오즈마 특화 - 밸런스 중심");
        } else {
            // 기본 파티 구성
            party = createPartyByStrategy(categorizedCharacters, dungeonName, 4, strategy, new HashMap<>());
            party.put("specialization", "일반 던전 - " + strategy + " 전략");
        }
        
        return party;
    }

    // Utility methods
    private List<Character> filterDealers(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getJobName() != null && 
                        (c.getJobName().contains("소드마스터") || 
                         c.getJobName().contains("버서커") || 
                         c.getJobName().contains("아수라")))
                .collect(Collectors.toList());
    }

    private List<Character> filterBuffers(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getJobName() != null && 
                        (c.getJobName().contains("크루세이더") || 
                         c.getJobName().contains("팔라딘")))
                .collect(Collectors.toList());
    }

    private List<Character> filterUpdoongis(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getIsFavorite() != null && c.getIsFavorite())
                .collect(Collectors.toList());
    }

    private List<Character> filterOthers(List<Character> characters) {
        return characters.stream()
                .filter(c -> c.getJobName() != null && 
                        !c.getJobName().contains("소드마스터") && 
                        !c.getJobName().contains("버서커") && 
                        !c.getJobName().contains("아수라") &&
                        !c.getJobName().contains("크루세이더") && 
                        !c.getJobName().contains("팔라딘"))
                .collect(Collectors.toList());
    }

    private Map<String, Object> createSlotInfo(Character character, String role, int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber);
        slot.put("character", character);
        slot.put("role", role);
        slot.put("fame", character.getFame());
        slot.put("job", character.getJobName());
        return slot;
    }

    private double calculatePartyEfficiency(List<Map<String, Object>> slots) {
        if (slots.isEmpty()) return 0.0;
        
        double totalFame = slots.stream()
                .mapToDouble(slot -> (Integer) slot.get("fame"))
                .sum();
        
        return totalFame / slots.size();
    }

    private double calculateTotalEfficiency(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = (Double) party1.get("efficiency");
        double efficiency2 = (Double) party2.get("efficiency");
        return (efficiency1 + efficiency2) / 2.0;
    }

    private double calculateSafetyScore(List<Map<String, Object>> slots) {
        if (slots.isEmpty()) return 0.0;
        
        long updoongiCount = slots.stream()
                .filter(slot -> "업둥이".equals(slot.get("role")))
                .count();
        
        return (double) updoongiCount / slots.size();
    }

    private double calculateSynergyScore(List<Map<String, Object>> slots) {
        if (slots.size() < 2) return 1.0;
        
        // 직업 시너지 점수 계산
        Set<String> jobs = slots.stream()
                .map(slot -> (String) slot.get("job"))
                .collect(Collectors.toSet());
        
        // 시너지가 좋은 직업 조합
        Set<String> synergyJobs = new HashSet<>(Arrays.asList("소드마스터", "버서커", "아수라", "크루세이더"));
        
        long synergyCount = jobs.stream()
                .filter(synergyJobs::contains)
                .count();
        
        return (double) synergyCount / jobs.size();
    }

    private double calculateBalanceScore(Map<String, Object> party1, Map<String, Object> party2) {
        double efficiency1 = (Double) party1.get("efficiency");
        double efficiency2 = (Double) party2.get("efficiency");
        
        return 1.0 - Math.abs(efficiency1 - efficiency2) / Math.max(efficiency1, efficiency2);
    }

    private Map<String, Object> analyzeRoleBalance(List<Map<String, Object>> slots) {
        Map<String, Object> balance = new HashMap<>();
        
        long dealerCount = slots.stream().filter(slot -> "딜러".equals(slot.get("role"))).count();
        long bufferCount = slots.stream().filter(slot -> "버퍼".equals(slot.get("role"))).count();
        long updoongiCount = slots.stream().filter(slot -> "업둥이".equals(slot.get("role"))).count();
        
        balance.put("dealers", dealerCount);
        balance.put("buffers", bufferCount);
        balance.put("updoongis", updoongiCount);
        balance.put("isBalanced", dealerCount >= 2 && bufferCount >= 1);
        
        return balance;
    }

    private Map<String, Object> analyzeFameBalance(List<Map<String, Object>> slots) {
        Map<String, Object> balance = new HashMap<>();
        
        List<Integer> fames = slots.stream()
                .map(slot -> (Integer) slot.get("fame"))
                .collect(Collectors.toList());
        
        if (fames.isEmpty()) {
            balance.put("average", 0.0);
            balance.put("variance", 0.0);
            balance.put("isBalanced", true);
            return balance;
        }
        
        double average = fames.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = fames.stream()
                .mapToDouble(fame -> Math.pow(fame - average, 2))
                .average()
                .orElse(0.0);
        
        balance.put("average", average);
        balance.put("variance", variance);
        balance.put("isBalanced", variance < 10000); // 명성 차이가 1만 이하면 균형잡힌 것으로 간주
        
        return balance;
    }

    private double calculateDungeonCompatibility(
            Map<String, Object> recommendation, 
            Map<String, Object> dungeonRequirements) {
        
        // 던전 요구사항과 파티 구성의 호환성 계산
        if (recommendation.containsKey("party1") && recommendation.containsKey("party2")) {
            // 8인 파티
            Map<String, Object> party1 = (Map<String, Object>) recommendation.get("party1");
            Map<String, Object> party2 = (Map<String, Object>) recommendation.get("party2");
            
            double compatibility1 = calculateSinglePartyCompatibility(party1, dungeonRequirements);
            double compatibility2 = calculateSinglePartyCompatibility(party2, dungeonRequirements);
            
            return (compatibility1 + compatibility2) / 2.0;
        } else {
            // 4인 파티
            return calculateSinglePartyCompatibility(recommendation, dungeonRequirements);
        }
    }

    private double calculateSinglePartyCompatibility(
            Map<String, Object> party, 
            Map<String, Object> dungeonRequirements) {
        
        List<Map<String, Object>> slots = (List<Map<String, Object>>) party.get("slots");
        if (slots == null || slots.isEmpty()) return 0.0;
        
        int minFame = (Integer) dungeonRequirements.get("minFame");
        List<String> requiredRoles = (List<String>) dungeonRequirements.get("requiredRoles");
        
        // 명성 요구사항 충족도
        long eligibleCharacters = slots.stream()
                .filter(slot -> (Integer) slot.get("fame") >= minFame)
                .count();
        double fameCompatibility = (double) eligibleCharacters / slots.size();
        
        // 역할 요구사항 충족도
        Set<String> partyRoles = slots.stream()
                .map(slot -> (String) slot.get("role"))
                .collect(Collectors.toSet());
        
        long requiredRolesMet = requiredRoles.stream()
                .filter(partyRoles::contains)
                .count();
        double roleCompatibility = (double) requiredRolesMet / requiredRoles.size();
        
        return (fameCompatibility + roleCompatibility) / 2.0;
    }

    private double calculateOverallRating(Map<String, Object> analysis) {
        double totalScore = (Double) analysis.get("totalScore");
        double dungeonCompatibility = (Double) analysis.get("dungeonCompatibility");
        
        // 가중 평균으로 전체 등급 계산
        return totalScore * 0.7 + dungeonCompatibility * 0.3;
    }

    private double getPartyScore(Map<String, Object> party) {
        if (party.containsKey("efficiency")) {
            return (Double) party.get("efficiency");
        } else if (party.containsKey("totalEfficiency")) {
            return (Double) party.get("totalEfficiency");
        }
        return 0.0;
    }

    private Character findCharacterByJob(Map<String, List<Character>> categorizedCharacters, String job) {
        return categorizedCharacters.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getJobName() != null && c.getJobName().contains(job))
                .findFirst()
                .orElse(null);
    }

    private String determineRole(Character character) {
        if (character.getJobName() == null) return "기타";
        
        if (character.getJobName().contains("소드마스터") || 
            character.getJobName().contains("버서커") || 
            character.getJobName().contains("아수라")) {
            return "딜러";
        } else if (character.getJobName().contains("크루세이더") || 
                   character.getJobName().contains("팔라딘")) {
            return "버퍼";
        } else {
            return "기타";
        }
    }

    private void fillRemainingSlots(
            Map<String, List<Character>> categorizedCharacters,
            List<Map<String, Object>> slots,
            int partySize) {
        
        // 남은 슬롯을 딜러, 버퍼, 기타 순으로 채우기
        List<Character> dealers = new ArrayList<>(categorizedCharacters.get("dealers"));
        List<Character> buffers = new ArrayList<>(categorizedCharacters.get("buffers"));
        List<Character> others = new ArrayList<>(categorizedCharacters.get("others"));
        
        // 이미 배치된 캐릭터 제거
        Set<String> placedCharacterIds = slots.stream()
                .map(slot -> ((Character) slot.get("character")).getCharacterId())
                .collect(Collectors.toSet());
        
        dealers.removeIf(c -> placedCharacterIds.contains(c.getCharacterId()));
        buffers.removeIf(c -> placedCharacterIds.contains(c.getCharacterId()));
        others.removeIf(c -> placedCharacterIds.contains(c.getCharacterId()));
        
        // 남은 슬롯 채우기
        while (slots.size() < partySize) {
            if (!dealers.isEmpty()) {
                Character dealer = dealers.remove(0);
                slots.add(createSlotInfo(dealer, "딜러", slots.size() + 1));
            } else if (!buffers.isEmpty()) {
                Character buffer = buffers.remove(0);
                slots.add(createSlotInfo(buffer, "버퍼", slots.size() + 1));
            } else if (!others.isEmpty()) {
                Character other = others.remove(0);
                slots.add(createSlotInfo(other, "기타", slots.size() + 1));
            } else {
                // 캐릭터가 부족하면 빈 슬롯 생성
                slots.add(createEmptySlot(slots.size() + 1));
            }
        }
    }

    private Map<String, Object> createEmptySlot(int slotNumber) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("slotNumber", slotNumber);
        slot.put("character", null);
        slot.put("role", "빈슬롯");
        slot.put("fame", 0);
        slot.put("job", "없음");
        return slot;
    }

    private Map<String, Object> createErrorResult(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", new Date());
        return error;
    }
}
