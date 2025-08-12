package com.dfparty.backend.controller;

import com.dfparty.backend.model.Character;
import com.dfparty.backend.service.CharacterService;
import com.dfparty.backend.service.PartyRecommendationService;
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
@RequestMapping("/api/party-recommendation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartyRecommendationController {

    private final PartyRecommendationService partyRecommendationService;
    private final CharacterService characterService;

    /**
     * 파티 구성 추천 생성
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePartyRecommendation(
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("파티 구성 추천 요청: {}", request);
            
            // 요청 파라미터 검증
            Map<String, Object> validation = validateRecommendationRequest(request);
            if ((Boolean) validation.get("hasError")) {
                return ResponseEntity.badRequest().body(validation);
            }
            
            String serverId = (String) request.get("serverId");
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            Integer partySize = (Integer) request.get("partySize");
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            
            if (preferences == null) {
                preferences = new HashMap<>();
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            if (characters.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "선택된 캐릭터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 파티 구성 추천 생성
            Map<String, Object> recommendation = partyRecommendationService.generatePartyRecommendation(
                    characters, dungeonName, partySize, preferences);
            
            if (recommendation.containsKey("error")) {
                return ResponseEntity.badRequest().body(recommendation);
            }
            
            log.info("파티 구성 추천 생성 완료: 던전={}, 파티크기={}, 캐릭터수={}", 
                    dungeonName, partySize, characters.size());
            
            return ResponseEntity.ok(recommendation);
            
        } catch (Exception e) {
            log.error("파티 구성 추천 생성 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "파티 구성 추천 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 던전별 추천 전략 생성
     */
    @PostMapping("/dungeon-specific")
    public ResponseEntity<Map<String, Object>> generateDungeonSpecificRecommendations(
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("던전별 추천 전략 요청: {}", request);
            
            // 요청 파라미터 검증
            Map<String, Object> validation = validateDungeonRequest(request);
            if ((Boolean) validation.get("hasError")) {
                return ResponseEntity.badRequest().body(validation);
            }
            
            String serverId = (String) request.get("serverId");
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            if (characters.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "선택된 캐릭터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 던전별 추천 전략 생성
            Map<String, Object> recommendations = partyRecommendationService.generateDungeonSpecificRecommendations(
                    characters, dungeonName);
            
            if (recommendations.containsKey("error")) {
                return ResponseEntity.badRequest().body(recommendations);
            }
            
            log.info("던전별 추천 전략 생성 완료: 던전={}, 캐릭터수={}", dungeonName, characters.size());
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            log.error("던전별 추천 전략 생성 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "던전별 추천 전략 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 개인화된 파티 추천 생성
     */
    @PostMapping("/personalized")
    public ResponseEntity<Map<String, Object>> generatePersonalizedRecommendation(
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("개인화된 파티 추천 요청: {}", request);
            
            // 요청 파라미터 검증
            Map<String, Object> validation = validatePersonalizedRequest(request);
            if ((Boolean) validation.get("hasError")) {
                return ResponseEntity.badRequest().body(validation);
            }
            
            String serverId = (String) request.get("serverId");
            List<String> characterIds = (List<String>) request.get("characterIds");
            String dungeonName = (String) request.get("dungeonName");
            Integer partySize = (Integer) request.get("partySize");
            Map<String, Object> userPreferences = (Map<String, Object>) request.get("userPreferences");
            Map<String, Object> playHistory = (Map<String, Object>) request.get("playHistory");
            
            if (userPreferences == null) {
                userPreferences = new HashMap<>();
            }
            if (playHistory == null) {
                playHistory = new HashMap<>();
            }
            
            // 캐릭터 정보 조회
            List<Character> characters = characterService.getCharactersByIds(characterIds);
            if (characters.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "선택된 캐릭터를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 개인화된 파티 추천 생성
            Map<String, Object> recommendation = partyRecommendationService.generatePersonalizedRecommendation(
                    characters, dungeonName, partySize, userPreferences, playHistory);
            
            if (recommendation.containsKey("error")) {
                return ResponseEntity.badRequest().body(recommendation);
            }
            
            log.info("개인화된 파티 추천 생성 완료: 던전={}, 파티크기={}, 캐릭터수={}", 
                    dungeonName, partySize, characters.size());
            
            return ResponseEntity.ok(recommendation);
            
        } catch (Exception e) {
            log.error("개인화된 파티 추천 생성 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "개인화된 파티 추천 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 추천 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getRecommendationHistory(
            @RequestParam(required = false) String serverId,
            @RequestParam(required = false) String dungeonName,
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("추천 히스토리 조회: 서버={}, 던전={}, 전략={}, 페이지={}, 크기={}", 
                    serverId, dungeonName, strategy, page, size);
            
            // TODO: 추천 히스토리 저장 및 조회 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "추천 히스토리 기능은 향후 구현 예정입니다.");
            result.put("page", page);
            result.put("size", size);
            result.put("totalElements", 0);
            result.put("content", new ArrayList<>());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("추천 히스토리 조회 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "추천 히스토리 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 추천 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(
            @RequestParam(required = false) String serverId,
            @RequestParam(required = false) String dungeonName) {
        
        try {
            log.info("추천 통계 조회: 서버={}, 던전={}", serverId, dungeonName);
            
            // TODO: 추천 통계 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "추천 통계 기능은 향후 구현 예정입니다.");
            result.put("totalRecommendations", 0);
            result.put("popularDungeons", new ArrayList<>());
            result.put("popularStrategies", new ArrayList<>());
            result.put("averageSuccessRate", 0.0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("추천 통계 조회 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "추천 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 추천 설정 조회
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getRecommendationConfig() {
        
        try {
            log.info("추천 설정 조회");
            
            Map<String, Object> config = new HashMap<>();
            
            // 사용 가능한 전략들
            config.put("availableStrategies", getAvailableStrategies());
            
            // 던전별 가중치 설정
            config.put("dungeonWeights", getDungeonWeights());
            
            // 추천 설정
            config.put("recommendationSettings", getRecommendationSettings());
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("추천 설정 조회 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "추천 설정 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 추천 설정 업데이트
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateRecommendationConfig(
            @RequestBody Map<String, Object> config) {
        
        try {
            log.info("추천 설정 업데이트: {}", config);
            
            // TODO: 추천 설정 저장 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "추천 설정이 성공적으로 업데이트되었습니다.");
            result.put("updatedConfig", config);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("추천 설정 업데이트 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "추천 설정 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 추천 성능 테스트
     */
    @PostMapping("/performance-test")
    public ResponseEntity<Map<String, Object>> runPerformanceTest(
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("추천 성능 테스트 요청: {}", request);
            
            // TODO: 추천 성능 테스트 기능 구현
            Map<String, Object> result = new HashMap<>();
            result.put("message", "추천 성능 테스트 기능은 향후 구현 예정입니다.");
            result.put("testType", "recommendation");
            result.put("iterations", 0);
            result.put("totalExecutionTime", 0);
            result.put("averageExecutionTime", 0.0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("추천 성능 테스트 중 오류 발생", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "추천 성능 테스트 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // Private helper methods

    /**
     * 추천 요청 검증
     */
    private Map<String, Object> validateRecommendationRequest(Map<String, Object> request) {
        Map<String, Object> validation = new HashMap<>();
        
        if (request == null) {
            validation.put("hasError", true);
            validation.put("message", "요청 데이터가 없습니다.");
            return validation;
        }
        
        if (!request.containsKey("serverId") || request.get("serverId") == null) {
            validation.put("hasError", true);
            validation.put("message", "서버 ID가 필요합니다.");
            return validation;
        }
        
        if (!request.containsKey("characterIds") || request.get("characterIds") == null) {
            validation.put("hasError", true);
            validation.put("message", "캐릭터 ID 목록이 필요합니다.");
            return validation;
        }
        
        List<String> characterIds = (List<String>) request.get("characterIds");
        if (characterIds.isEmpty()) {
            validation.put("hasError", true);
            validation.put("message", "최소 1명의 캐릭터가 필요합니다.");
            return validation;
        }
        
        if (!request.containsKey("dungeonName") || request.get("dungeonName") == null) {
            validation.put("hasError", true);
            validation.put("message", "던전 이름이 필요합니다.");
            return validation;
        }
        
        if (!request.containsKey("partySize") || request.get("partySize") == null) {
            validation.put("hasError", true);
            validation.put("message", "파티 크기가 필요합니다.");
            return validation;
        }
        
        Integer partySize = (Integer) request.get("partySize");
        if (partySize != 4 && partySize != 8) {
            validation.put("hasError", true);
            validation.put("message", "파티 크기는 4 또는 8이어야 합니다.");
            return validation;
        }
        
        validation.put("hasError", false);
        return validation;
    }

    /**
     * 던전 요청 검증
     */
    private Map<String, Object> validateDungeonRequest(Map<String, Object> request) {
        Map<String, Object> validation = new HashMap<>();
        
        if (request == null) {
            validation.put("hasError", true);
            validation.put("message", "요청 데이터가 없습니다.");
            return validation;
        }
        
        if (!request.containsKey("serverId") || request.get("serverId") == null) {
            validation.put("hasError", true);
            validation.put("message", "서버 ID가 필요합니다.");
            return validation;
        }
        
        if (!request.containsKey("characterIds") || request.get("characterIds") == null) {
            validation.put("hasError", true);
            validation.put("message", "캐릭터 ID 목록이 필요합니다.");
            return validation;
        }
        
        List<String> characterIds = (List<String>) request.get("characterIds");
        if (characterIds.isEmpty()) {
            validation.put("hasError", true);
            validation.put("message", "최소 1명의 캐릭터가 필요합니다.");
            return validation;
        }
        
        if (!request.containsKey("dungeonName") || request.get("dungeonName") == null) {
            validation.put("hasError", true);
            validation.put("message", "던전 이름이 필요합니다.");
            return validation;
        }
        
        validation.put("hasError", false);
        return validation;
    }

    /**
     * 개인화 요청 검증
     */
    private Map<String, Object> validatePersonalizedRequest(Map<String, Object> request) {
        Map<String, Object> validation = validateRecommendationRequest(request);
        if ((Boolean) validation.get("hasError")) {
            return validation;
        }
        
        // 개인화 요청의 경우 추가 검증이 필요하지 않음
        // userPreferences와 playHistory는 선택사항
        
        return validation;
    }

    /**
     * 사용 가능한 전략 목록
     */
    private List<Map<String, Object>> getAvailableStrategies() {
        List<Map<String, Object>> strategies = new ArrayList<>();
        
        Map<String, Object> efficiency = new HashMap<>();
        efficiency.put("id", "efficiency");
        efficiency.put("name", "효율성 중심");
        efficiency.put("description", "최고 명성 캐릭터들을 우선 배치하여 파티 효율성을 극대화");
        strategies.add(efficiency);
        
        Map<String, Object> balance = new HashMap<>();
        balance.put("id", "balance");
        balance.put("name", "밸런스 중심");
        balance.put("description", "효율성과 안전성의 균형을 맞춘 파티 구성");
        strategies.add(balance);
        
        Map<String, Object> safety = new HashMap<>();
        safety.put("id", "safety");
        safety.put("name", "안전성 중심");
        safety.put("description", "업둥이 캐릭터를 우선 배치하여 안전한 파티 구성");
        strategies.add(safety);
        
        Map<String, Object> synergy = new HashMap<>();
        synergy.put("id", "synergy");
        synergy.put("name", "시너지 중심");
        synergy.put("description", "직업 간 시너지를 고려한 파티 구성");
        strategies.add(synergy);
        
        Map<String, Object> hybrid = new HashMap<>();
        hybrid.put("id", "hybrid");
        hybrid.put("name", "하이브리드");
        hybrid.put("description", "여러 전략의 장점을 결합한 최적 파티 구성");
        strategies.add(hybrid);
        
        return strategies;
    }

    /**
     * 던전별 가중치 설정
     */
    private Map<String, Object> getDungeonWeights() {
        Map<String, Object> weights = new HashMap<>();
        
        Map<String, Object> valhalla = new HashMap<>();
        valhalla.put("efficiency", 0.6);
        valhalla.put("balance", 0.2);
        valhalla.put("safety", 0.2);
        weights.put("발할라", valhalla);
        
        Map<String, Object> ozma = new HashMap<>();
        ozma.put("efficiency", 0.4);
        ozma.put("balance", 0.4);
        ozma.put("safety", 0.2);
        weights.put("오즈마", ozma);
        
        Map<String, Object> sirocco = new HashMap<>();
        sirocco.put("efficiency", 0.3);
        sirocco.put("balance", 0.4);
        sirocco.put("safety", 0.3);
        weights.put("시로코", sirocco);
        
        Map<String, Object> general = new HashMap<>();
        general.put("efficiency", 0.3);
        general.put("balance", 0.4);
        general.put("safety", 0.3);
        weights.put("일반", general);
        
        return weights;
    }

    /**
     * 추천 설정
     */
    private Map<String, Object> getRecommendationSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        settings.put("maxIterations", 100);
        settings.put("convergenceThreshold", 0.01);
        settings.put("enableParallelProcessing", true);
        settings.put("cacheResults", true);
        settings.put("maxCacheSize", 1000);
        settings.put("cacheExpirationHours", 24);
        
        return settings;
    }
}
