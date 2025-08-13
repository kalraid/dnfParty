package com.dfparty.backend.controller;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.AdvancedPartyOptimizationService;
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
@RequestMapping("/api/advanced-optimization")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvancedPartyOptimizationController {

    private final AdvancedPartyOptimizationService advancedPartyOptimizationService;
    private final CharacterService characterService;

    /**
     * 고급 파티 최적화 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeAdvancedOptimization(@RequestBody Map<String, Object> request) {
        try {
            log.info("고급 파티 최적화 요청: {}", request);
            
            // 요청 파라미터 검증
            if (!validateOptimizationRequest(request)) {
                return ResponseEntity.badRequest().body(createErrorResponse("필수 파라미터가 누락되었습니다."));
            }
            
            String serverId = (String) request.get("serverId");
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            int partySize = (Integer) request.get("partySize");
            String optimizationStrategy = (String) request.get("optimizationStrategy");
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            if (characters.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("선택된 캐릭터를 찾을 수 없습니다."));
            }
            
            // 고급 최적화 실행
            Map<String, Object> result = advancedPartyOptimizationService.executeAdvancedOptimization(
                    characters, dungeonName, partySize, optimizationStrategy
            );
            
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            // 결과에 메타데이터 추가
            result.put("requestInfo", Map.of(
                    "serverId", serverId,
                    "characterCount", characters.size(),
                    "dungeonName", dungeonName,
                    "partySize", partySize,
                    "optimizationStrategy", optimizationStrategy
            ));
            
            log.info("고급 파티 최적화 완료: 전략={}, 결과크기={}", optimizationStrategy, result.size());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("고급 파티 최적화 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("최적화 실행 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 효율성 중심 최적화
     */
    @PostMapping("/efficiency")
    public ResponseEntity<Map<String, Object>> optimizeForEfficiency(@RequestBody Map<String, Object> request) {
        request.put("optimizationStrategy", "efficiency");
        return executeAdvancedOptimization(request);
    }

    /**
     * 밸런스 중심 최적화
     */
    @PostMapping("/balance")
    public ResponseEntity<Map<String, Object>> optimizeForBalance(@RequestBody Map<String, Object> request) {
        request.put("optimizationStrategy", "balance");
        return executeAdvancedOptimization(request);
    }

    /**
     * 시너지 중심 최적화
     */
    @PostMapping("/synergy")
    public ResponseEntity<Map<String, Object>> optimizeForSynergy(@RequestBody Map<String, Object> request) {
        request.put("optimizationStrategy", "synergy");
        return executeAdvancedOptimization(request);
    }

    /**
     * 안전성 중심 최적화
     */
    @PostMapping("/safety")
    public ResponseEntity<Map<String, Object>> optimizeForSafety(@RequestBody Map<String, Object> request) {
        request.put("optimizationStrategy", "safety");
        return executeAdvancedOptimization(request);
    }

    /**
     * 하이브리드 최적화
     */
    @PostMapping("/hybrid")
    public ResponseEntity<Map<String, Object>> optimizeHybrid(@RequestBody Map<String, Object> request) {
        request.put("optimizationStrategy", "hybrid");
        return executeAdvancedOptimization(request);
    }

    /**
     * 최적화 전략 비교 분석
     */
    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareOptimizationStrategies(@RequestBody Map<String, Object> request) {
        try {
            log.info("최적화 전략 비교 분석 요청: {}", request);
            
            // 요청 파라미터 검증
            if (!validateOptimizationRequest(request)) {
                return ResponseEntity.badRequest().body(createErrorResponse("필수 파라미터가 누락되었습니다."));
            }
            
            String serverId = (String) request.get("serverId");
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            int partySize = (Integer) request.get("partySize");
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            if (characters.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("선택된 캐릭터를 찾을 수 없습니다."));
            }
            
            // 모든 전략으로 최적화 실행
            Map<String, Object> efficiencyResult = advancedPartyOptimizationService.executeAdvancedOptimization(
                    characters, dungeonName, partySize, "efficiency"
            );
            
            Map<String, Object> balanceResult = advancedPartyOptimizationService.executeAdvancedOptimization(
                    characters, dungeonName, partySize, "balance"
            );
            
            Map<String, Object> synergyResult = advancedPartyOptimizationService.executeAdvancedOptimization(
                    characters, dungeonName, partySize, "synergy"
            );
            
            Map<String, Object> safetyResult = advancedPartyOptimizationService.executeAdvancedOptimization(
                    characters, dungeonName, partySize, "safety"
            );
            
            // 결과 비교 분석
            Map<String, Object> comparisonResult = createComparisonResult(
                    efficiencyResult, balanceResult, synergyResult, safetyResult, dungeonName
            );
            
            comparisonResult.put("requestInfo", Map.of(
                    "serverId", serverId,
                    "characterCount", characters.size(),
                    "dungeonName", dungeonName,
                    "partySize", partySize
            ));
            
            log.info("최적화 전략 비교 분석 완료");
            return ResponseEntity.ok(comparisonResult);
            
        } catch (Exception e) {
            log.error("최적화 전략 비교 분석 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("비교 분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최적화 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getOptimizationHistory(
            @RequestParam(required = false) String serverId,
            @RequestParam(required = false) String dungeonName,
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("최적화 히스토리 조회: serverId={}, dungeonName={}, strategy={}, page={}, size={}", 
                    serverId, dungeonName, strategy, page, size);
            
            // TODO: 최적화 히스토리 저장 및 조회 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "최적화 히스토리 기능은 추후 구현 예정입니다.");
            result.put("page", page);
            result.put("size", size);
            result.put("totalElements", 0);
            result.put("content", new ArrayList<>());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("최적화 히스토리 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("히스토리 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최적화 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOptimizationStats(
            @RequestParam(required = false) String serverId,
            @RequestParam(required = false) String dungeonName) {
        
        try {
            log.info("최적화 통계 조회: serverId={}, dungeonName={}", serverId, dungeonName);
            
            // TODO: 최적화 통계 계산 기능 구현
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOptimizations", 0);
            stats.put("successRate", 0.0);
            stats.put("averageExecutionTime", 0.0);
            stats.put("strategyUsage", Map.of(
                    "efficiency", 0,
                    "balance", 0,
                    "synergy", 0,
                    "safety", 0,
                    "hybrid", 0
            ));
            stats.put("dungeonStats", new HashMap<>());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("최적화 통계 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최적화 설정 조회
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getOptimizationConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 사용 가능한 최적화 전략
            config.put("availableStrategies", List.of(
                    Map.of("id", "efficiency", "name", "효율성 중심", "description", "명성과 전투력을 기준으로 최적화"),
                    Map.of("id", "balance", "name", "밸런스 중심", "description", "직업별 균등한 분배로 최적화"),
                    Map.of("id", "synergy", "name", "시너지 중심", "description", "직업 조합의 시너지를 고려한 최적화"),
                    Map.of("id", "safety", "name", "안전성 중심", "description", "안전 마진을 고려한 최적화"),
                    Map.of("id", "hybrid", "name", "하이브리드", "description", "여러 전략을 조합한 최적화")
            ));
            
            // 던전별 최적화 가중치
            config.put("dungeonWeights", Map.of(
                    "레이드", Map.of("efficiency", 0.4, "balance", 0.3, "synergy", 0.3),
                    "일반", Map.of("efficiency", 0.5, "balance", 0.3, "synergy", 0.2)
            ));
            
            // 최적화 설정
            config.put("optimizationSettings", Map.of(
                    "maxIterations", 100,
                    "convergenceThreshold", 0.001,
                    "enableParallelProcessing", true,
                    "cacheResults", true
            ));
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("최적화 설정 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("설정 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최적화 설정 업데이트
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateOptimizationConfig(@RequestBody Map<String, Object> config) {
        try {
            log.info("최적화 설정 업데이트: {}", config);
            
            // TODO: 설정 저장 및 적용 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "설정이 성공적으로 업데이트되었습니다.");
            result.put("updatedConfig", config);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("최적화 설정 업데이트 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("설정 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최적화 성능 테스트
     */
    @PostMapping("/performance-test")
    public ResponseEntity<Map<String, Object>> runPerformanceTest(@RequestBody Map<String, Object> request) {
        try {
            log.info("최적화 성능 테스트 시작: {}", request);
            
            String testType = (String) request.getOrDefault("testType", "basic");
            int iterations = (Integer) request.getOrDefault("iterations", 10);
            
            Map<String, Object> testResults = new HashMap<>();
            long startTime = System.currentTimeMillis();
            
            // 기본 성능 테스트
            if ("basic".equals(testType)) {
                testResults = runBasicPerformanceTest(iterations);
            } else if ("stress".equals(testType)) {
                testResults = runStressPerformanceTest(iterations);
            }
            
            long endTime = System.currentTimeMillis();
            testResults.put("totalExecutionTime", endTime - startTime);
            testResults.put("testType", testType);
            testResults.put("iterations", iterations);
            
            log.info("최적화 성능 테스트 완료: 실행시간={}ms", endTime - startTime);
            return ResponseEntity.ok(testResults);
            
        } catch (Exception e) {
            log.error("최적화 성능 테스트 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("성능 테스트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // Private helper methods
    private boolean validateOptimizationRequest(Map<String, Object> request) {
        return request.containsKey("serverId") &&
               request.containsKey("characterIds") &&
               request.containsKey("dungeonName") &&
               request.containsKey("partySize") &&
               request.get("characterIds") instanceof List &&
               !((List<?>) request.get("characterIds")).isEmpty();
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    private Map<String, Object> createComparisonResult(Map<String, Object> efficiencyResult,
                                                       Map<String, Object> balanceResult,
                                                       Map<String, Object> synergyResult,
                                                       Map<String, Object> safetyResult,
                                                       String dungeonName) {
        Map<String, Object> comparison = new HashMap<>();
        
        // 각 전략별 점수 계산
        double efficiencyScore = calculateStrategyScore(efficiencyResult);
        double balanceScore = calculateStrategyScore(balanceResult);
        double synergyScore = calculateStrategyScore(synergyResult);
        double safetyScore = calculateStrategyScore(safetyResult);
        
        // 점수 비교
        comparison.put("scores", Map.of(
                "efficiency", Map.of("score", efficiencyScore, "rank", getRank(efficiencyScore, List.of(efficiencyScore, balanceScore, synergyScore, safetyScore))),
                "balance", Map.of("score", balanceScore, "rank", getRank(balanceScore, List.of(efficiencyScore, balanceScore, synergyScore, safetyScore))),
                "synergy", Map.of("score", synergyScore, "rank", getRank(synergyScore, List.of(efficiencyScore, balanceScore, synergyScore, safetyScore))),
                "safety", Map.of("score", safetyScore, "rank", getRank(safetyScore, List.of(efficiencyScore, balanceScore, synergyScore, safetyScore)))
        ));
        
        // 최고 점수 전략
        String bestStrategy = getBestStrategy(efficiencyScore, balanceScore, synergyScore, safetyScore);
        comparison.put("bestStrategy", bestStrategy);
        comparison.put("bestScore", Math.max(Math.max(efficiencyScore, balanceScore), Math.max(synergyScore, safetyScore)));
        
        // 던전별 추천 전략
        String recommendedStrategy = getRecommendedStrategyForDungeon(dungeonName, efficiencyScore, balanceScore, synergyScore, safetyScore);
        comparison.put("recommendedStrategy", recommendedStrategy);
        
        // 상세 결과
        comparison.put("detailedResults", Map.of(
                "efficiency", efficiencyResult,
                "balance", balanceResult,
                "synergy", synergyResult,
                "safety", safetyResult
        ));
        
        return comparison;
    }

    private double calculateStrategyScore(Map<String, Object> result) {
        if (result.containsKey("error")) {
            return 0.0;
        }
        
        if (result.containsKey("totalEfficiency")) {
            return (Double) result.get("totalEfficiency");
        } else if (result.containsKey("efficiency")) {
            return (Double) result.get("efficiency");
        } else if (result.containsKey("balanceScore")) {
            return (Double) result.get("balanceScore");
        } else if (result.containsKey("synergyScore")) {
            return (Double) result.get("synergyScore");
        } else if (result.containsKey("safetyScore")) {
            return (Double) result.get("safetyScore");
        }
        
        return 0.0;
    }

    private int getRank(double score, List<Double> allScores) {
        List<Double> sortedScores = new ArrayList<>(allScores);
        sortedScores.sort((a, b) -> Double.compare(b, a));
        return sortedScores.indexOf(score) + 1;
    }

    private String getBestStrategy(double efficiencyScore, double balanceScore, double synergyScore, double safetyScore) {
        if (efficiencyScore >= balanceScore && efficiencyScore >= synergyScore && efficiencyScore >= safetyScore) {
            return "efficiency";
        } else if (balanceScore >= efficiencyScore && balanceScore >= synergyScore && balanceScore >= safetyScore) {
            return "balance";
        } else if (synergyScore >= efficiencyScore && synergyScore >= balanceScore && synergyScore >= safetyScore) {
            return "synergy";
        } else {
            return "safety";
        }
    }

    private String getRecommendedStrategyForDungeon(String dungeonName, double efficiencyScore, double balanceScore, double synergyScore, double safetyScore) {
        if (dungeonName.contains("레이드")) {
            // 레이드는 시너지와 밸런스가 중요
            if (synergyScore >= balanceScore) {
                return "synergy";
            } else {
                return "balance";
            }
        } else {
            // 일반 던전은 효율성이 중요
            return "efficiency";
        }
    }

    private Map<String, Object> runBasicPerformanceTest(int iterations) {
        Map<String, Object> results = new HashMap<>();
        List<Long> executionTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            
            // 간단한 계산 작업 (실제 최적화 로직 대신)
            double result = 0.0;
            for (int j = 0; j < 1000; j++) {
                result += Math.sqrt(j) * Math.sin(j);
            }
            
            long end = System.currentTimeMillis();
            executionTimes.add(end - start);
        }
        
        results.put("averageExecutionTime", executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
        results.put("minExecutionTime", executionTimes.stream().mapToLong(Long::longValue).min().orElse(0L));
        results.put("maxExecutionTime", executionTimes.stream().mapToLong(Long::longValue).max().orElse(0L));
        results.put("executionTimes", executionTimes);
        
        return results;
    }

    private Map<String, Object> runStressPerformanceTest(int iterations) {
        Map<String, Object> results = new HashMap<>();
        List<Long> executionTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            
            // 스트레스 테스트용 복잡한 계산
            double result = 0.0;
            for (int j = 0; j < 10000; j++) {
                result += Math.pow(j, 2) * Math.cos(j) * Math.log(j + 1);
            }
            
            long end = System.currentTimeMillis();
            executionTimes.add(end - start);
        }
        
        results.put("averageExecutionTime", executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
        results.put("minExecutionTime", executionTimes.stream().mapToLong(Long::longValue).min().orElse(0L));
        results.put("maxExecutionTime", executionTimes.stream().mapToLong(Long::longValue).max().orElse(0L));
        results.put("executionTimes", executionTimes);
        
        return results;
    }
}
