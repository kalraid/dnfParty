package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import com.dfparty.backend.repository.AdventureRepository;
import com.dfparty.backend.service.DfoApiService;
import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.CachingService;
import com.dfparty.backend.service.DungeonClearService;
import com.dfparty.backend.service.RealtimeEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CharacterService {

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private AdventureRepository adventureRepository;
    
    @Autowired
    private DfoApiService dfoApiService;
    
    @Autowired
    private DundamService dundamService;
    
    @Autowired
    private CachingService cachingService;
    
    @Autowired
    private DungeonClearService dungeonClearService;
    
    @Autowired
    private RealtimeEventService realtimeEventService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 통합 캐릭터 정보 조회 (DFO API + Dundam 정보)
     */
    public Map<String, Object> getCompleteCharacterInfo(String serverId, String characterName) {
        try {
            // 1. DFO API에서 캐릭터 검색
            Object searchResult = dfoApiService.searchCharacters(serverId, characterName, null, null, true, 1, "match");
            if (searchResult == null) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }

            // 2. 검색 결과에서 캐릭터 ID 추출
            String characterId = extractCharacterId(searchResult);
            if (characterId == null) {
                return createErrorResponse("캐릭터 ID를 추출할 수 없습니다.");
            }

            // 3. DB에서 기존 정보 확인
            Optional<Character> existingChar = characterRepository.findByCharacterId(characterId);
            if (existingChar.isPresent()) {
                // DB에 있으면 DB 데이터 반환
                Character character = existingChar.get();
                return createSuccessResponse("DB에서 캐릭터 정보를 조회했습니다.", convertToDto(character));
            }

            // 4. DFO API에서 상세 정보 조회
            Object characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            if (characterDetail == null) {
                return createErrorResponse("캐릭터 상세 정보를 조회할 수 없습니다.");
            }

            // 5. 새 캐릭터 엔티티 생성 및 저장
            Character newCharacter = createCharacterFromDfoApi(characterDetail, serverId);
            characterRepository.save(newCharacter);

            // 6. Dundam 정보 업데이트
            Map<String, Object> dundamInfo = updateDundamInfo(newCharacter);
            newCharacter = characterRepository.save(newCharacter);

            // 7. 결과 반환
            Map<String, Object> result = convertToDto(newCharacter);
            result.put("dundamInfo", dundamInfo);
            
            return createSuccessResponse("새로운 캐릭터 정보를 조회하고 저장했습니다.", result);

        } catch (Exception e) {
            return createErrorResponse("캐릭터 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * ID 목록으로 캐릭터 조회
     */
    public List<Character> getCharactersByIds(List<String> characterIds) {
        try {
            return characterRepository.findAllByCharacterIdIn(characterIds);
        } catch (Exception e) {
            log.error("ID 목록으로 캐릭터 조회 실패", e);
            return List.of();
        }
    }
    
    /**
     * 캐릭터 검색 (이름으로 검색)
     */
    public Map<String, Object> searchCharacters(String characterName, String serverId) {
        try {
            log.info("=== CharacterService.searchCharacters 시작 ===");
            log.info("characterName: {}, serverId: {}", characterName, serverId);
            
            // 1. DFO API에서 캐릭터 검색
            Object searchResult = dfoApiService.searchCharacters(serverId, characterName, null, null, true, 10, "match");
            log.info("DFO API 검색 결과 타입: {}", searchResult != null ? searchResult.getClass().getSimpleName() : "null");
            
            if (searchResult == null) {
                log.warn("DFO API 검색 결과가 null입니다.");
                return createErrorResponse("캐릭터 검색에 실패했습니다.");
            }

            // 2. 검색 결과 파싱
            List<Map<String, Object>> characters = parseSearchResults(searchResult);
            log.info("파싱된 캐릭터 수: {}", characters.size());
            
            if (characters.isEmpty()) {
                log.warn("파싱 결과 캐릭터가 없습니다.");
                return createErrorResponse("검색 조건에 맞는 캐릭터가 없습니다.");
            }

            // 3. 각 캐릭터에 대해 DB 정보 확인 및 추가 정보 조회
            for (Map<String, Object> character : characters) {
                String charId = (String) character.get("characterId");
                String serverIdChar = (String) character.get("serverId");
                
                if (charId != null) {
                    // DB에서 기존 정보 확인
                    Optional<Character> existingChar = characterRepository.findByCharacterId(charId);
                    if (existingChar.isPresent()) {
                        Character dbChar = existingChar.get();
                        character.put("buffPower", dbChar.getBuffPower());
                        character.put("totalDamage", dbChar.getTotalDamage());
                        character.put("dungeonClearNabel", dbChar.getDungeonClearNabel());
                        character.put("dungeonClearVenus", dbChar.getDungeonClearVenus());
                        character.put("dungeonClearFog", dbChar.getDungeonClearFog());
                        
                        // DB에 모험단 정보가 있으면 사용
                        if (dbChar.getAdventureName() != null && !dbChar.getAdventureName().trim().isEmpty()) {
                            character.put("adventureName", dbChar.getAdventureName());
                        }
                    }
                    
                    // 모험단 정보가 없거나 "N/A"이면 DFO API에서 상세 정보 조회
                    String currentAdventureName = (String) character.get("adventureName");
                    if (currentAdventureName == null || currentAdventureName.trim().isEmpty() || "N/A".equals(currentAdventureName)) {
                        try {
                            log.debug("DFO API에서 캐릭터 상세 정보 조회 시작: characterId={}", charId);
                            Object characterDetail = dfoApiService.getCharacterDetail(serverIdChar, charId);
                            if (characterDetail != null) {
                                // characterDetail에서 모험단 정보 추출
                                String adventureName = extractAdventureNameFromDetail(characterDetail);
                                if (adventureName != null && !adventureName.trim().isEmpty()) {
                                    character.put("adventureName", adventureName);
                                    log.info("모험단 정보 업데이트: characterId={}, adventureName={}", charId, adventureName);
                                } else {
                                    character.put("adventureName", "N/A");
                                }
                            } else {
                                character.put("adventureName", "N/A");
                            }
                        } catch (Exception e) {
                            log.warn("캐릭터 상세 정보 조회 실패: characterId={}, error={}", charId, e.getMessage());
                            character.put("adventureName", "N/A");
                        }
                    }
                    
                    // 던담에서 버프력/전투력 정보 조회 (DB에 없거나 0인 경우)
                    Long currentBuffPower = (Long) character.get("buffPower");
                    Long currentTotalDamage = (Long) character.get("totalDamage");
                    
                    if ((currentBuffPower == null || currentBuffPower == 0L) || 
                        (currentTotalDamage == null || currentTotalDamage == 0L)) {
                        try {
                            log.info("던담에서 버프력/전투력 조회 시작: characterId={}", charId);
                            Map<String, Object> dundamInfo = dundamService.getCharacterInfo(serverIdChar, charId);
                            
                            if (dundamInfo != null && Boolean.TRUE.equals(dundamInfo.get("success"))) {
                                Long buffPower = (Long) dundamInfo.get("buffPower");
                                Long totalDamage = (Long) dundamInfo.get("totalDamage");
                                
                                if (buffPower != null && buffPower > 0) {
                                    character.put("buffPower", buffPower);
                                    log.info("던담에서 버프력 업데이트: characterId={}, buffPower={}", charId, buffPower);
                                }
                                
                                if (totalDamage != null && totalDamage > 0) {
                                    character.put("totalDamage", totalDamage);
                                    log.info("던담에서 전투력 업데이트: characterId={}, totalDamage={}", charId, totalDamage);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("던담 정보 조회 실패: characterId={}, error={}", charId, e.getMessage());
                        }
                    }
                }
            }

            // 강제로 필드 추가 (테스트용)
            for (Map<String, Object> character : characters) {
                if (!character.containsKey("buffPower")) {
                    character.put("buffPower", 0L);
                }
                if (!character.containsKey("totalDamage")) {
                    character.put("totalDamage", 0L);
                }
                if (!character.containsKey("adventureName") || "".equals(character.get("adventureName"))) {
                    character.put("adventureName", "N/A");
                }
                if (!character.containsKey("dungeonClearNabel")) {
                    character.put("dungeonClearNabel", false);
                }
                if (!character.containsKey("dungeonClearVenus")) {
                    character.put("dungeonClearVenus", false);
                }
                if (!character.containsKey("dungeonClearFog")) {
                    character.put("dungeonClearFog", false);
                }
            }
            
            Map<String, Object> responseData = Map.of("characters", characters);
            log.info("성공 응답 생성: characters 수 = {}", characters.size());
            return createSuccessResponse("캐릭터 검색이 완료되었습니다.", responseData);

        } catch (Exception e) {
            // 서버 로그에 상세한 에러 정보 기록
            log.error("캐릭터 검색 실패 - 상세 정보:", e);
            log.error("검색 파라미터 - characterName: {}, serverId: {}", characterName, serverId);
            
            // 클라이언트에는 사용자 친화적인 메시지 반환
            return createErrorResponse("캐릭터 검색에 실패했습니다.");
        }
    }

    /**
     * 캐릭터 정보 저장
     */
    public Map<String, Object> saveCharacter(Map<String, Object> characterData) {
        try {
            String characterId = (String) characterData.get("characterId");
            if (characterId == null) {
                return createErrorResponse("캐릭터 ID가 필요합니다.");
            }

            // 기존 캐릭터 확인
            Optional<Character> existingChar = characterRepository.findByCharacterId(characterId);
            if (existingChar.isPresent()) {
                // 기존 캐릭터 업데이트
                Character character = existingChar.get();
                updateCharacterFromData(character, characterData);
                characterRepository.save(character);
                return createSuccessResponse("캐릭터 정보가 업데이트되었습니다.", convertToDto(character));
            } else {
                // 새 캐릭터 생성
                Character newCharacter = createCharacterFromData(characterData);
                characterRepository.save(newCharacter);
                return createSuccessResponse("새로운 캐릭터가 저장되었습니다.", convertToDto(newCharacter));
            }

        } catch (Exception e) {
            return createErrorResponse("캐릭터 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 개별 캐릭터 정보 조회
     */
    public Map<String, Object> getCharacter(String serverId, String characterId) {
        try {
            Optional<Character> character = characterRepository.findByCharacterId(characterId);
            if (character.isPresent()) {
                return createSuccessResponse("캐릭터 정보를 조회했습니다.", convertToDto(character.get()));
            } else {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return createErrorResponse("캐릭터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모험단별 캐릭터 목록 조회
     */
    public Map<String, Object> getCharactersByAdventure(String adventureName) {
        try {
            List<Character> characters = characterRepository.findByAdventureName(adventureName);
            List<Map<String, Object>> characterDtos = characters.stream()
                .map(this::convertToDto)
                .toList();
            
            return createSuccessResponse("모험단별 캐릭터 목록을 조회했습니다.", Map.of("characters", characterDtos));
        } catch (Exception e) {
            return createErrorResponse("모험단별 캐릭터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터 정보 새로고침
     */
    public Map<String, Object> refreshCharacter(String serverId, String characterId) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }

            Character character = characterOpt.get();
            
            // 1. DFO API에서 최신 정보 조회
            Object characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            if (characterDetail != null) {
                updateCharacterFromDfoApi(character, characterDetail);
            }

            // 2. Dundam 정보 업데이트
            updateDundamInfo(character);
            
            // 3. DB에 저장
            characterRepository.save(character);
            
            return createSuccessResponse("캐릭터 정보가 새로고침되었습니다.", convertToDto(character));

        } catch (Exception e) {
            return createErrorResponse("캐릭터 새로고침 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터 삭제
     */
    public Map<String, Object> deleteCharacter(String characterId) {
        try {
            Optional<Character> character = characterRepository.findByCharacterId(characterId);
            if (character.isPresent()) {
                characterRepository.delete(character.get());
                return createSuccessResponse("캐릭터가 삭제되었습니다.", null);
            } else {
                return createErrorResponse("삭제할 캐릭터를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return createErrorResponse("캐릭터 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터 타임라인 조회 (캐싱 적용)
     */
    public Map<String, Object> getCharacterTimeline(String serverId, String characterId, int limit, String startDate, String endDate) {
        try {
            // 캐시 키 생성
            String cacheKey = CachingService.getTimelineKey(serverId, characterId);
            
            // 캐시에서 확인
            Object cachedTimeline = cachingService.get(cacheKey);
            if (cachedTimeline != null) {
                return createSuccessResponse("캐시에서 타임라인을 조회했습니다.", Map.of("timeline", cachedTimeline));
            }

            // DFO API에서 타임라인 조회
            Object timeline = dfoApiService.getCharacterTimeline(serverId, characterId, limit, null, startDate, endDate, null);
            if (timeline != null) {
                // 1분간 캐싱
                cachingService.put(cacheKey, timeline, CachingService.CacheType.TIMELINE);
                return createSuccessResponse("DFO API에서 타임라인을 조회했습니다.", Map.of("timeline", timeline));
            }

            return createErrorResponse("타임라인을 조회할 수 없습니다.");

        } catch (Exception e) {
            return createErrorResponse("타임라인 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터 스펙 정보 업데이트 (Dundam, 캐싱 적용)
     */
    public Map<String, Object> updateCharacterStats(String serverId, String characterId) {
        try {
            // 캐시 키 생성
            String cacheKey = CachingService.getDundamStatsKey(serverId, characterId);
            
            // 캐시에서 확인
            Object cachedStats = cachingService.get(cacheKey);
            if (cachedStats != null) {
                return createSuccessResponse("캐시에서 스펙 정보를 조회했습니다.", Map.of("dundamInfo", cachedStats));
            }

            // Dundam에서 정보 조회
            Map<String, Object> dundamInfo = dundamService.getCharacterInfo(serverId, characterId);
            if (dundamInfo != null) {
                // 3분간 캐싱
                cachingService.put(cacheKey, dundamInfo, CachingService.CacheType.DUNDAM_STATS);
                
                // DB 업데이트
                Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
                if (characterOpt.isPresent()) {
                    Character character = characterOpt.get();
                    character.updateStats(
                        (Long) dundamInfo.get("buffPower"),
                        (Long) dundamInfo.get("totalDamage"),
                        "dundam.xyz"
                    );
                    characterRepository.save(character);
                }
                
                return createSuccessResponse("Dundam에서 스펙 정보를 조회했습니다.", Map.of("dundamInfo", dundamInfo));
            }

            return createErrorResponse("스펙 정보를 조회할 수 없습니다.");

        } catch (Exception e) {
            return createErrorResponse("스펙 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 헬퍼 메서드들
    private String extractCharacterId(Object searchResult) {
        try {
            if (searchResult instanceof JsonNode) {
                JsonNode root = (JsonNode) searchResult;
                JsonNode rows = root.path("rows");
                if (rows.isArray() && rows.size() > 0) {
                    return rows.get(0).path("characterId").asText();
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
    }

    private String extractCharacterName(Object searchResult) {
        try {
            if (searchResult instanceof JsonNode) {
                JsonNode root = (JsonNode) searchResult;
                JsonNode rows = root.path("rows");
                if (rows.isArray() && rows.size() > 0) {
                    return rows.get(0).path("characterName").asText();
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
    }

    private String extractAdventureNameFromDetail(Object characterDetail) {
        try {
            if (characterDetail instanceof JsonNode) {
                JsonNode root = (JsonNode) characterDetail;
                String adventureName = root.path("adventureName").asText();
                if (adventureName != null && !adventureName.trim().isEmpty() && !"null".equals(adventureName)) {
                    return adventureName;
                }
            }
        } catch (Exception e) {
            log.debug("모험단 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> parseSearchResults(Object searchResult) {
        try {
            List<Map<String, Object>> characters = new ArrayList<>();
            
            if (searchResult instanceof List) {
                // 이미 List<CharacterDto> 형태로 반환된 경우
                @SuppressWarnings("unchecked")
                List<CharacterDto> characterDtos = (List<CharacterDto>) searchResult;
                
                for (CharacterDto dto : characterDtos) {
                    Map<String, Object> character = new HashMap<>();
                    character.put("characterId", dto.getCharacterId());
                    character.put("characterName", dto.getCharacterName());
                    character.put("serverId", dto.getServerId());
                    character.put("jobId", dto.getJobId());
                    character.put("jobGrowId", dto.getJobGrowId());
                    character.put("jobName", dto.getJobName());
                    character.put("jobGrowName", dto.getJobGrowName());
                    character.put("level", dto.getLevel());
                    character.put("adventureName", dto.getAdventureName() != null ? dto.getAdventureName() : "N/A");
                    character.put("fame", dto.getFame());
                    
                    // 기본값 설정 (DB에서 조회되지 않은 경우)
                    character.put("buffPower", 0L);
                    character.put("totalDamage", 0L);
                    character.put("dungeonClearNabel", false);
                    character.put("dungeonClearVenus", false);
                    character.put("dungeonClearFog", false);
                    
                    characters.add(character);
                }
            } else if (searchResult instanceof JsonNode) {
                // JsonNode 형태로 반환된 경우
                JsonNode root = (JsonNode) searchResult;
                JsonNode rows = root.path("rows");
                
                if (rows.isArray()) {
                    for (JsonNode row : rows) {
                        Map<String, Object> character = new HashMap<>();
                        character.put("characterId", row.path("characterId").asText());
                        character.put("characterName", row.path("characterName").asText());
                        character.put("serverId", row.path("serverId").asText());
                        character.put("jobId", row.path("jobId").asText());
                        character.put("jobGrowId", row.path("jobGrowId").asText());
                        character.put("jobName", row.path("jobName").asText());
                        character.put("jobGrowName", row.path("jobGrowName").asText());
                        character.put("level", row.path("level").asInt());
                        
                        // adventureName이 없거나 빈 값인 경우 처리
                        String adventureName = row.path("adventureName").asText();
                        if (adventureName == null || adventureName.trim().isEmpty()) {
                            adventureName = "N/A";
                        }
                        character.put("adventureName", adventureName);
                        character.put("fame", row.path("fame").asLong());
                        
                        // 기본값 설정 (DB에서 조회되지 않은 경우)
                        character.put("buffPower", 0L);
                        character.put("totalDamage", 0L);
                        character.put("dungeonClearNabel", false);
                        character.put("dungeonClearVenus", false);
                        character.put("dungeonClearFog", false);
                        
                        characters.add(character);
                    }
                }
            }
            
            log.info("검색 결과 파싱 완료: {}개 캐릭터", characters.size());
            
            // 디버깅: 첫 번째 캐릭터 정보 출력
            if (!characters.isEmpty()) {
                Map<String, Object> firstChar = characters.get(0);
                log.info("첫 번째 캐릭터 정보: {}", firstChar);
            }
            
            return characters;
            
        } catch (Exception e) {
            log.error("검색 결과 파싱 실패", e);
            return List.of();
        }
    }

    private Character createCharacterFromDfoApi(Object characterDetail, String serverId) {
        // DFO API 응답에서 Character 엔티티 생성
        // 실제 구현에서는 JSON 파싱 로직 필요
        return new Character("temp_id", "temp_name", serverId);
    }

    private void updateCharacterFromDfoApi(Character character, Object characterDetail) {
        // DFO API 응답으로 Character 엔티티 업데이트
        // 실제 구현에서는 JSON 파싱 로직 필요
    }

    private Map<String, Object> updateDundamInfo(Character character) {
        // Dundam에서 캐릭터 스펙 정보 조회 및 업데이트
        try {
            Map<String, Object> dundamInfo = dundamService.getCharacterInfo(
                character.getServerId(), 
                character.getCharacterId()
            );
            
            if (dundamInfo != null) {
                character.updateStats(
                    (Long) dundamInfo.get("buffPower"),
                    (Long) dundamInfo.get("totalDamage"),
                    "dundam.xyz"
                );
            }
            
            return dundamInfo != null ? dundamInfo : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Character createCharacterFromData(Map<String, Object> data) {
        Character character = new Character(
            (String) data.get("characterId"),
            (String) data.get("characterName"),
            (String) data.get("serverId")
        );
        
        updateCharacterFromData(character, data);
        return character;
    }

    private void updateCharacterFromData(Character character, Map<String, Object> data) {
        if (data.containsKey("adventureName")) {
            character.setAdventureName((String) data.get("adventureName"));
        }
        if (data.containsKey("fame")) {
            character.setFame(((Number) data.get("fame")).longValue());
        }
        if (data.containsKey("buffPower")) {
            character.setBuffPower(((Number) data.get("buffPower")).longValue());
        }
        if (data.containsKey("totalDamage")) {
            character.setTotalDamage(((Number) data.get("totalDamage")).longValue());
        }
        if (data.containsKey("level")) {
            character.setLevel(((Number) data.get("level")).intValue());
        }
    }

    private Map<String, Object> convertToDto(Character character) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("characterId", character.getCharacterId());
        dto.put("characterName", character.getCharacterName());
        dto.put("serverId", character.getServerId());
        dto.put("adventureName", character.getAdventureName());
        dto.put("fame", character.getFame());
        dto.put("buffPower", character.getBuffPower());
        dto.put("totalDamage", character.getTotalDamage());
        dto.put("level", character.getLevel());
        dto.put("jobName", character.getJobName());
        dto.put("dungeonClearNabel", character.getDungeonClearNabel());
        dto.put("dungeonClearVenus", character.getDungeonClearVenus());
        dto.put("dungeonClearFog", character.getDungeonClearFog());
        dto.put("isFavorite", character.getIsFavorite());
        dto.put("excludedDungeons", parseExcludedDungeons(character.getExcludedDungeons()));
        dto.put("createdAt", character.getCreatedAt());
        dto.put("updatedAt", character.getUpdatedAt());
        return dto;
    }

    /**
     * 제외 던전 JSON 문자열을 List로 파싱
     */
    private List<String> parseExcludedDungeons(String excludedDungeonsJson) {
        if (excludedDungeonsJson == null || excludedDungeonsJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(excludedDungeonsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            if (data instanceof Map) {
                response.putAll((Map<String, Object>) data);
            } else {
                response.put("data", data);
            }
        }
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    /**
     * 업둥이 캐릭터 설정/해제
     */
    public Map<String, Object> toggleFavorite(String serverId, String characterId, Boolean isFavorite) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }

            Character character = characterOpt.get();
            character.setIsFavorite(isFavorite);
            characterRepository.save(character);

            // 실시간 이벤트 전송
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("characterId", characterId);
            eventData.put("serverId", serverId);
            eventData.put("isFavorite", isFavorite);
            eventData.put("characterName", character.getCharacterName());
            realtimeEventService.notifyCharacterUpdated(characterId, "system", eventData);

            return createSuccessResponse(
                isFavorite ? "업둥이 캐릭터로 설정되었습니다." : "업둥이 설정이 해제되었습니다.",
                convertToDto(character)
            );

        } catch (Exception e) {
            return createErrorResponse("업둥이 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전 제외 설정
     */
    public Map<String, Object> setDungeonExclusion(String serverId, String characterId, List<String> excludedDungeons) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }

            Character character = characterOpt.get();
            
            // JSON 형태로 저장
            try {
                String excludedJson = objectMapper.writeValueAsString(excludedDungeons);
                character.setExcludedDungeons(excludedJson);
            } catch (Exception e) {
                return createErrorResponse("제외 던전 정보 저장 중 오류가 발생했습니다.");
            }

            characterRepository.save(character);

            // 실시간 이벤트 전송
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("characterId", characterId);
            eventData.put("serverId", serverId);
            eventData.put("excludedDungeons", excludedDungeons);
            eventData.put("characterName", character.getCharacterName());
            realtimeEventService.notifyCharacterUpdated(characterId, "system", eventData);

            return createSuccessResponse(
                "던전 제외 설정이 업데이트되었습니다.",
                convertToDto(character)
            );

        } catch (Exception e) {
            return createErrorResponse("던전 제외 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

