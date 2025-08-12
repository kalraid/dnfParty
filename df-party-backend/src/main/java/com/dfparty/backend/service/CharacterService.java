package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import com.dfparty.backend.repository.AdventureRepository;
import com.dfparty.backend.service.DfoApiService;
import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.CachingService;
import com.dfparty.backend.service.DungeonClearService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 통합 캐릭터 정보 조회 (DFO API + Dundam 정보)
     */
    public Map<String, Object> getCompleteCharacterInfo(String serverId, String characterName) {
        try {
            // 1. DFO API에서 캐릭터 검색
            Object searchResult = dfoApiService.searchCharacters(serverId, characterName, 1);
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
     * 캐릭터 검색 (이름으로 검색)
     */
    public Map<String, Object> searchCharacters(String characterName, String serverId) {
        try {
            // 1. DFO API에서 캐릭터 검색
            Object searchResult = dfoApiService.searchCharacters(serverId, characterName, 10);
            if (searchResult == null) {
                return createErrorResponse("캐릭터 검색에 실패했습니다.");
            }

            // 2. 검색 결과 파싱
            List<Map<String, Object>> characters = parseSearchResults(searchResult);
            if (characters.isEmpty()) {
                return createErrorResponse("검색 조건에 맞는 캐릭터가 없습니다.");
            }

            // 3. 각 캐릭터에 대해 DB 정보 확인 및 Dundam 정보 추가
            for (Map<String, Object> character : characters) {
                String charId = (String) character.get("characterId");
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
                    }
                }
            }

            return createSuccessResponse("캐릭터 검색이 완료되었습니다.", Map.of("characters", characters));

        } catch (Exception e) {
            return createErrorResponse("캐릭터 검색 중 오류가 발생했습니다: " + e.getMessage());
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
            Object timeline = dfoApiService.getCharacterTimeline(serverId, characterId, limit, startDate, endDate, null);
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

    private List<Map<String, Object>> parseSearchResults(Object searchResult) {
        // DFO API 검색 결과를 파싱하여 캐릭터 목록 반환
        // 실제 구현에서는 JSON 파싱 로직 필요
        return List.of(); // 임시 반환
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
        dto.put("createdAt", character.getCreatedAt());
        dto.put("updatedAt", character.getUpdatedAt());
        return dto;
    }

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.putAll((Map<String, Object>) data);
        }
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}

