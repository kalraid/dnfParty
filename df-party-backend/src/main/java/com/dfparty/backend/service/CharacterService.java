package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import com.dfparty.backend.repository.AdventureRepository;
import com.dfparty.backend.repository.JobTypeRepository;
import com.dfparty.backend.service.DfoApiService;
import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.dto.ManualStatsUpdateDto;
import com.dfparty.backend.service.DundamService;
import com.dfparty.backend.service.CachingService;
import com.dfparty.backend.service.DungeonClearService;
import com.dfparty.backend.service.RealtimeEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import com.dfparty.backend.utils.CharacterUtils;


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
import java.util.UUID;
import com.dfparty.backend.model.RealtimeEvent;

import com.dfparty.backend.entity.Adventure;
import com.dfparty.backend.entity.JobType;
import com.dfparty.backend.entity.Server;
import com.dfparty.backend.repository.ServerRepository;
import com.dfparty.backend.dto.CharacterDetailDto;
import com.dfparty.backend.repository.NabelDifficultySelectionRepository;
import com.dfparty.backend.entity.NabelDifficultySelection;

@Slf4j
@Service
public class CharacterService {

    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private AdventureRepository adventureRepository;
    
    @Autowired
    private JobTypeRepository jobTypeRepository;
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private DfoApiService dfoApiService;
    
    @Autowired
    private DundamService dundamService;
    
    @Autowired
    private CachingService cachingService;
    
    @Autowired
    private DungeonClearService dungeonClearService;
    
    @Autowired
    private ThursdayFallbackService thursdayFallbackService;
    
    @Autowired
    private RealtimeEventService realtimeEventService;
    
    @Autowired
    private NabelDifficultySelectionRepository nabelDifficultySelectionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CharacterUtils characterUtils;
    /**
     * 통합 캐릭터 정보 조회 (DFO API + DB + 타임라인)
     * 1. DFO API 캐릭터 조회 (캐릭터 ID 획득)
     * 2. 캐릭터 상세조회 (모험단 확인)
     * 3. 전투력 DB 조회 (있으면 반환)
     * 4. 타임라인 API 조회 (던전 클리어 여부 확인)
     * 5. 통합 response 반환
     */
    public Map<String, Object> getCompleteCharacterInfo(String serverId, String characterName) {
        try {
            log.info("캐릭터 통합 정보 조회 시작: serverId={}, characterName={}", serverId, characterName);
            
            // 1. DFO API에서 캐릭터 검색 (캐릭터 ID 획득 목적)
            log.info("1단계: DFO API 캐릭터 검색 시작");
            Object searchResult = dfoApiService.searchCharacters(serverId, characterName, null, null, true, 1, "match");
            if (searchResult == null) {
                log.warn("DFO API 캐릭터 검색 실패: characterName={}", characterName);
                return createErrorResponse("DFO API에서 캐릭터를 찾을 수 없습니다.");
            }

            // 2. 검색 결과에서 캐릭터 ID 추출
            String characterId = extractCharacterId(searchResult);
            if (characterId == null) {
                log.warn("캐릭터 ID 추출 실패: searchResult={}", searchResult);
                return createErrorResponse("캐릭터 ID를 추출할 수 없습니다.");
            }
            log.info("캐릭터 ID 획득: characterId={}", characterId);

            // 3. DFO API에서 캐릭터 상세조회 (모험단 확인)
            log.info("2단계: DFO API 캐릭터 상세조회 시작");
            Object characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            if (characterDetail == null) {
                log.warn("캐릭터 상세조회 실패: characterId={}", characterId);
                return createErrorResponse("캐릭터 상세 정보를 조회할 수 없습니다.");
            }
            log.info("캐릭터 상세조회 완료: characterId={}", characterId);

            // 4. DB에서 기존 정보 확인 (전투력 등)
            log.info("3단계: DB에서 전투력 정보 조회");
            Optional<Character> existingChar = characterRepository.findByCharacterId(characterId);
            Character character;
            
            if (existingChar.isPresent()) {
                // DB에 있으면 기존 정보 사용
                character = existingChar.get();
                log.info("DB에서 기존 캐릭터 정보 조회: characterId={}", characterId);
                
                // DFO API 정보로 업데이트 (모험단명 등 최신 정보)
                updateCharacterFromDfoApi(character, characterDetail);
                characterRepository.save(character);
                log.info("DFO API 정보로 캐릭터 업데이트 완료");
            } else {
                // DB에 없으면 새로 생성
                log.info("새 캐릭터 생성: characterId={}", characterId);
                character = createCharacterFromDfoApi(characterDetail, serverId);
                characterRepository.save(character);
                log.info("새 캐릭터 생성 및 저장 완료");
            }

            // 5. 타임라인 API 조회 (던전 클리어 여부 확인)
            log.info("4단계: 타임라인 API 조회 시작 (던전 클리어 여부)");
            Map<String, Object> dungeonClearInfo = dungeonClearService.getDungeonClearStatus(serverId, characterId);
            
            // 6. 통합 response 구성
            Map<String, Object> response = new HashMap<>();
            response.put("character", convertToDto(character));
            response.put("dungeonClearInfo", dungeonClearInfo);
            response.put("dataSource", "DFO API + DB + Timeline");
            response.put("lastUpdated", LocalDateTime.now());
            
            log.info("캐릭터 통합 정보 조회 완료: characterId={}, dungeonClearStatus={}", 
                    characterId, dungeonClearInfo.get("clearStatus"));
            
            return createSuccessResponse("캐릭터 통합 정보를 성공적으로 조회했습니다.", response);

        } catch (Exception e) {
            log.error("캐릭터 통합 정보 조회 중 오류 발생: serverId={}, characterName={}", serverId, characterName, e);
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
            
            // 목요일 API 제한 확인
            Map<String, Object> thursdayRestriction = thursdayFallbackService.checkThursdayApiRestriction("캐릭터 검색");
            if (thursdayRestriction != null) {
                log.warn("목요일 API 제한으로 DB 정보만 제공: characterName={}", characterName);
                return searchCharactersFromDB(characterName, serverId, thursdayRestriction);
            }
            
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
                    
                    // 모험단 정보가 없거나 "모험단 정보 없음"이면 DFO API에서 상세 정보 조회
                    String currentAdventureName = (String) character.get("adventureName");
                    log.info("=== searchCharacters 모험단 정보 체크 ===");
                    log.info("characterId: {}, currentAdventureName: '{}'", charId, currentAdventureName);
                    if (currentAdventureName == null || currentAdventureName.trim().isEmpty() || "모험단 정보 없음".equals(currentAdventureName)) {
                        log.info("조건 만족: DFO API 상세 정보 조회 실행");
                        try {
                            // 캐릭터 검색 결과에서 실제 서버 ID 추출
                            String actualServerId = (String) character.get("serverId");
                            log.info("캐릭터 상세 정보 조회용 실제 서버 ID: '{}' (원본: '{}')", actualServerId, serverIdChar);
                            
                            log.debug("DFO API에서 캐릭터 상세 정보 조회 시작: characterId={}, serverId={}", charId, actualServerId);
                            Object characterDetail = dfoApiService.getCharacterDetail(actualServerId, charId);
                            if (characterDetail != null) {
                                // characterDetail에서 모험단 정보 추출
                                String adventureName = null;
                                log.info("characterDetail 타입: {}", characterDetail.getClass().getSimpleName());
                                
                                if (characterDetail instanceof CharacterDetailDto) {
                                    CharacterDetailDto dto = (CharacterDetailDto) characterDetail;
                                    adventureName = dto.getAdventureName();
                                    log.info("CharacterDetailDto에서 모험단 정보 추출: '{}'", adventureName);
                                } else if (characterDetail instanceof JsonNode) {
                                    JsonNode root = (JsonNode) characterDetail;
                                    adventureName = root.path("adventureName").asText();
                                    log.info("JsonNode에서 모험단 정보 추출: '{}'", adventureName);
                                } else if (characterDetail instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> detail = (Map<String, Object>) characterDetail;
                                    adventureName = (String) detail.get("adventureName");
                                    log.info("Map에서 모험단 정보 추출: '{}'", adventureName);
                                } else {
                                    log.warn("알 수 없는 characterDetail 타입: {}", characterDetail.getClass().getName());
                                }
                                
                                log.info("최종 추출된 모험단 정보: '{}'", adventureName);
                                log.info("조건 검사: null?={}, empty?={}, 'null'?={}", 
                                    (adventureName == null), 
                                    (adventureName != null && adventureName.trim().isEmpty()), 
                                    "null".equals(adventureName));
                                
                                if (adventureName != null && !adventureName.trim().isEmpty() && !"null".equals(adventureName)) {
                                    character.put("adventureName", adventureName);
                                                                        log.info("조건 만족! 모험단 DB 저장 시작: '{}'", adventureName);
                                    // 모험단 정보를 DB에 자동 저장
                                    try {
                                        saveAdventureToDB(adventureName, serverIdChar);
                                        log.info("✅ 모험단 DB 저장 성공, 캐릭터 관계 설정 시작");
                                        
                                        // 캐릭터와 모험단 관계 설정 (모험단 저장 성공 후에만)
                                        Optional<Character> dbCharOpt = characterRepository.findByCharacterId(charId);
                                        if (dbCharOpt.isPresent()) {
                                            Character dbCharacter = dbCharOpt.get();
                                            setAdventureForCharacter(dbCharacter, adventureName);
                                            characterRepository.save(dbCharacter);
                                            log.info("✅ 캐릭터-모험단 관계 설정 완료: characterId={}, adventureName={}", charId, adventureName);
                                        } else {
                                            log.warn("⚠️ 캐릭터를 찾을 수 없어 관계 설정 건너뜀: characterId={}", charId);
                                        }
        } catch (Exception e) {
                                        log.error("❌ 모험단 저장 실패, 캐릭터 관계 설정 건너뜀: adventureName={}, error={}", adventureName, e.getMessage());
                                        // 모험단 저장 실패 시 관계 설정을 건너뛰고 기본값 유지
                                        character.put("adventureName", "모험단 정보 없음");
                                    }
                                    
                                    log.info("모험단 정보 업데이트: characterId={}, adventureName={}", charId, adventureName);
                                } else {
                                    log.warn("조건 불만족! 모험단 정보를 '모험단 정보 없음'으로 설정: '{}'", adventureName);
                                    character.put("adventureName", "모험단 정보 없음");
                                }
                                
                                // 이미지 URL 정보도 함께 추출
                                Map<String, String> imageUrls = extractImageUrlsFromDetail(characterDetail);
                                if (imageUrls.get("characterImageUrl") != null) {
                                    character.put("characterImageUrl", imageUrls.get("characterImageUrl"));
                                    log.info("캐릭터 이미지 URL 업데이트: characterId={}, imageUrl={}", charId, imageUrls.get("characterImageUrl"));
                                }
                                if (imageUrls.get("avatarImageUrl") != null) {
                                    character.put("avatarImageUrl", imageUrls.get("avatarImageUrl"));
                                    log.info("아바타 이미지 URL 업데이트: characterId={}, avatarUrl={}", charId, imageUrls.get("avatarImageUrl"));
                                }
                            } else {
                                character.put("adventureName", "모험단 정보 없음");
                            }
                        } catch (Exception e) {
                            log.warn("캐릭터 상세 정보 조회 실패: characterId={}, error={}", charId, e.getMessage());
                            character.put("adventureName", "모험단 정보 없음");
                        }
                    } else {
                        log.info("조건 불만족: 기존 모험단 정보 사용 - '{}'", currentAdventureName);
                    }
                    
                    // 던담에서 버프력/전투력 정보 조회는 프론트엔드에서 별도로 처리
                    // (검색 완료 후 사용자가 확인할 수 있도록)
                    log.info("검색 완료: characterId={}, 기본 정보만 반환 (던담 동기화는 프론트엔드에서 처리)", charId);
                    
                    // DFO API 타임라인에서 던전 클리어 정보 조회 (항상 최신화)
                    try {
                        log.info("DFO API에서 던전 클리어 현황 조회 시작: characterId={}", charId);
                        Map<String, Object> clearStatusInfo = dungeonClearService.getDungeonClearStatus(serverIdChar, charId);
                        
                        if (clearStatusInfo != null && Boolean.TRUE.equals(clearStatusInfo.get("success"))) {
                            @SuppressWarnings("unchecked")
                            Map<String, Boolean> clearStatus = (Map<String, Boolean>) clearStatusInfo.get("clearStatus");
                            
                            if (clearStatus != null) {
                                character.put("dungeonClearNabel", clearStatus.getOrDefault("nabel", false));
                                character.put("dungeonClearVenus", clearStatus.getOrDefault("venus", false));
                                character.put("dungeonClearFog", clearStatus.getOrDefault("fog", false));
                                character.put("dungeonClearTwilight", clearStatus.getOrDefault("twilight", false));
                                
                                log.info("던전 클리어 현황 업데이트: characterId={}, nabel={}, venus={}, fog={}, twilight={}", 
                                    charId, clearStatus.get("nabel"), clearStatus.get("venus"), clearStatus.get("fog"), clearStatus.get("twilight"));
                            }
                        } else {
                            log.warn("던전 클리어 현황 조회 실패: characterId={}, response={}", charId, clearStatusInfo);
                        }
                    } catch (Exception e) {
                        log.warn("던전 클리어 현황 조회 실패: characterId={}, error={}", charId, e.getMessage());
                    }
                    
                    // DB에 캐릭터 정보 저장/업데이트
                    try {
                        saveOrUpdateCharacterToDb(character);
                        log.info("캐릭터 정보 DB 저장 완료: characterId={}", charId);
                    } catch (Exception e) {
                        log.warn("캐릭터 정보 DB 저장 실패: characterId={}, error={}", charId, e.getMessage());
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
                    character.put("adventureName", "모험단 정보 없음");
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
                if (!character.containsKey("dungeonClearTwilight")) {
                    character.put("dungeonClearTwilight", false);
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
                CharacterDetailDto characterDetailDto = convertMapToCharacterDetailDto(characterData);
                updateCharacterFromData(character, characterDetailDto);
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
     * 캐릭터 정보 새로고침
     */
    public Map<String, Object> refreshCharacter(String serverId, String characterId) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다.");
            }

            Character character = characterOpt.get();
            
            log.info("=== 캐릭터 새로고침 시작: {} ({}) ===", character.getCharacterName(), characterId);
            
            // 1. DFO API에서 최신 정보 조회 (총딜/버프력은 제외)
            Object characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            if (characterDetail != null) {
                updateCharacterFromDfoApiExcludingStats(character, characterDetail);
                log.info("캐릭터 '{}' DFO API 정보 업데이트 완료", character.getCharacterName());
            }

            // 2. Dundam 정보 업데이트
            Map<String, Object> dundamResult = updateDundamInfo(character);
            log.info("캐릭터 '{}' 던담 정보 업데이트 완료", character.getCharacterName());
            
            // 3. DB에 저장
            characterRepository.save(character);
            
            // 4. SSE로 실시간 업데이트 전송
            try {
                Map<String, Object> wsData = new HashMap<>();
                wsData.put("characterId", characterId);
                wsData.put("serverId", serverId);
                wsData.put("updateType", "refresh_api");
                wsData.put("characterName", character.getCharacterName());
                wsData.put("adventureName", character.getAdventureName());
                wsData.put("buffPower", character.getBuffPower());
                wsData.put("totalDamage", character.getTotalDamage());
                wsData.put("dundamResult", dundamResult);
                wsData.put("timestamp", LocalDateTime.now());
                
                realtimeEventService.sendEventToTopic("character-updates", 
                    RealtimeEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .type(RealtimeEvent.EventType.CHARACTER_UPDATED)
                        .targetId(characterId)
                        .data(wsData)
                        .timestamp(LocalDateTime.now())
                        .message("캐릭터 새로고침이 완료되었습니다.")
                        .broadcast(true)
                        .build()
                );
                
                log.info("SSE로 실시간 업데이트 전송 완료");
            } catch (Exception e) {
                log.warn("SSE 전송 실패: {}", e.getMessage());
            }
            
            log.info("=== 캐릭터 새로고침 완료: {} ===", character.getCharacterName());
            
            return createSuccessResponse("캐릭터 정보가 새로고침되었습니다.", convertToDto(character));

        } catch (Exception e) {
            log.error("캐릭터 새로고침 실패: {}", e.getMessage(), e);
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
            Object timeline = dfoApiService.getCharacterTimeline(serverId, characterId);
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
            if (dundamInfo != null && dundamInfo.get("success") == Boolean.TRUE) {
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
                                     
                    // 나벨 적격성 업데이트
                    updateNabelEligibility(character);
                    
                    characterRepository.save(character);
                }
                
                return createSuccessResponse("Dundam에서 스펙 정보를 조회했습니다.", Map.of("dundamInfo", dundamInfo));
            } else if (dundamInfo != null) {
                // 던담 크롤링이 비활성화되어 있는 경우
                if (dundamInfo.get("crawlingDisabled") == Boolean.TRUE) {
                    return createErrorResponse("던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.");
                }
                // 실패한 경우 실패 메시지 반환
                String message = (String) dundamInfo.get("message");
                log.warn("Dundam 크롤링 실패로 인한 업데이트 건너뜀: {}", message);
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



    /**
     * 서버 정보를 DB에 자동 저장
     */
    private void saveServerToDB(String serverId) {
        try {
            // 이미 존재하는지 확인
            if (serverRepository.findByServerId(serverId).isEmpty()) {
                // DFO API에서 서버 정보 가져오기 (서버명을 위해)
                String serverName = getServerNameById(serverId);
                
                // 새로운 서버 생성 및 저장
                Server server = Server.builder()
                    .serverId(serverId)
                    .serverName(serverName != null ? serverName : serverId)
                    .build();
                serverRepository.save(server);
                log.info("새로운 서버가 DB에 저장되었습니다: {} ({})", serverId, serverName);
            } else {
                log.debug("서버가 이미 존재합니다: {}", serverId);
            }
        } catch (Exception e) {
            log.warn("서버 DB 저장 실패: {}, error: {}", serverId, e.getMessage());
        }
    }
    
    /**
     * 모험단 정보를 DB에 자동 저장
     */
    private void saveAdventureToDB(String adventureName, String serverId) {
        try {
            log.info("=== 모험단 DB 저장 시작 ===");
            log.info("저장할 모험단명: '{}', 서버 ID: '{}'", adventureName, serverId);
            
            // 이미 존재하는지 확인
            Optional<Adventure> existingAdventure = adventureRepository.findByAdventureName(adventureName);
            if (existingAdventure.isEmpty()) {
                log.info("모험단이 DB에 없음. 새로 생성합니다: {}", adventureName);
                
                // 새로운 모험단 생성 및 저장
                Adventure adventure = Adventure.builder()
                    .adventureName(adventureName)
                    .serverId(serverId)  // serverId 추가
                    .build();
                Adventure savedAdventure = adventureRepository.save(adventure);
                
                log.info("✅ 새로운 모험단 DB 저장 성공!");
                log.info("   - 모험단명: {}", savedAdventure.getAdventureName());
                log.info("   - 서버 ID: {}", savedAdventure.getServerId());
                log.info("   - 모험단 ID: {}", savedAdventure.getId());
                log.info("   - 생성 시간: {}", savedAdventure.getCreatedAt());
            } else {
                Adventure existingAdv = existingAdventure.get();
                log.info("모험단이 이미 존재합니다");
                log.info("   - 모험단명: {}", existingAdv.getAdventureName());
                log.info("   - 서버 ID: {}", existingAdv.getServerId());
                log.info("   - 모험단 ID: {}", existingAdv.getId());
                log.info("   - 생성 시간: {}", existingAdv.getCreatedAt());
            }
            log.info("=== 모험단 DB 저장 완료 ===");
        } catch (Exception e) {
            log.error("❌ 모험단 DB 저장 실패: {}", adventureName);
            log.error("   - 에러: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 서버 ID로 서버명 조회
     */
    private String getServerNameById(String serverId) {
        // 일반적인 서버 ID -> 서버명 매핑 (DFO API 스타일)
        switch (serverId.toLowerCase()) {
            case "cain": return "카인";
            case "diregie": return "디레지에";
            case "siroco": return "시로코";
            case "prey": return "프레이";
            case "casillas": return "카시야스";
            case "hilder": return "힐더";
            case "anton": return "안톤";
            case "bakal": return "바칼";
            default: return serverId; // 알 수 없는 서버는 ID 그대로 사용
        }
    }

    /**
     * CharacterDetail에서 이미지 URL 정보 추출
     */
    private Map<String, String> extractImageUrlsFromDetail(Object characterDetail) {
        Map<String, String> imageUrls = new HashMap<>();
        imageUrls.put("characterImageUrl", null);
        imageUrls.put("avatarImageUrl", null);

        try {
            if (characterDetail instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detail = (Map<String, Object>) characterDetail;
                imageUrls.put("characterImageUrl", (String) detail.get("characterImageUrl"));
                imageUrls.put("avatarImageUrl", (String) detail.get("avatarImageUrl"));
            } else if (characterDetail instanceof JsonNode) {
                JsonNode root = (JsonNode) characterDetail;
                
                // 캐릭터 이미지 URL 추출
                String characterImageUrl = null;
                if (root.has("characterImageUrl")) {
                    characterImageUrl = root.path("characterImageUrl").asText();
                } else if (root.has("image")) {
                    JsonNode imageNode = root.path("image");
                    if (imageNode.has("characterImage")) {
                        characterImageUrl = imageNode.path("characterImage").asText();
                    }
                }
                imageUrls.put("characterImageUrl", characterImageUrl);
                
                // 아바타 이미지 URL 추출
                String avatarImageUrl = null;
                if (root.has("avatarImageUrl")) {
                    avatarImageUrl = root.path("avatarImageUrl").asText();
                } else if (root.has("avatar")) {
                    JsonNode avatarNode = root.path("avatar");
                    if (avatarNode.has("imageUrl")) {
                        avatarImageUrl = avatarNode.path("imageUrl").asText();
                    }
                } else if (root.has("jobGrowId")) {
                    // 기본 직업 이미지 URL 생성
                    String jobGrowId = root.path("jobGrowId").asText();
                    avatarImageUrl = String.format("https://img.neople.co.kr/img/df/portrait/%s.png", jobGrowId);
                }
                imageUrls.put("avatarImageUrl", avatarImageUrl);
            }
        } catch (Exception e) {
            log.debug("이미지 URL 추출 실패: {}", e.getMessage());
        }
        
        return imageUrls;
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
                    character.put("adventureName", dto.getAdventureName() != null ? dto.getAdventureName() : "모험단 정보 없음");
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
                            adventureName = "모험단 정보 없음";
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
        try {
            String characterId = null;
            String characterName = null;
            String jobName = null;
            String adventureName = null;
            Long fame = null;
            Integer level = null;
            
            if (characterDetail instanceof JsonNode) {
                JsonNode root = (JsonNode) characterDetail;
                characterId = root.path("characterId").asText();
                characterName = root.path("characterName").asText();
                jobName = root.path("jobName").asText();
                adventureName = root.path("adventureName").asText();
                
                if (root.has("fame")) {
                    fame = root.path("fame").asLong();
                }
                if (root.has("level")) {
                    level = root.path("level").asInt();
                }
            } else if (characterDetail instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detail = (Map<String, Object>) characterDetail;
                characterId = (String) detail.get("characterId");
                characterName = (String) detail.get("characterName");
                jobName = (String) detail.get("jobName");
                adventureName = (String) detail.get("adventureName");
                fame = detail.get("fame") != null ? ((Number) detail.get("fame")).longValue() : null;
                level = detail.get("level") != null ? ((Number) detail.get("level")).intValue() : null;
            }
            
            if (characterId == null || characterName == null) {
                throw new IllegalArgumentException("필수 캐릭터 정보가 누락되었습니다.");
            }
            
            Character character = new Character(characterId, characterName, serverId);
            character.setJobName(jobName);
            setAdventureForCharacter(character, adventureName);
            if (fame != null) character.setFame(fame);
            if (level != null) character.setLevel(level);
            
            log.info("✅ DFO API 데이터로 캐릭터 생성: ID={}, 이름={}, 직업={}", characterId, characterName, jobName);
            return character;
            
        } catch (Exception e) {
            log.error("DFO API 데이터로 캐릭터 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("캐릭터 생성 실패", e);
        }
    }

    private void updateCharacterFromDfoApi(Character character, Object characterDetail) {
        try {
            if (characterDetail instanceof JsonNode) {
                JsonNode root = (JsonNode) characterDetail;
                character.setJobName(root.path("jobName").asText());
                setAdventureForCharacter(character, root.path("adventureName").asText());
                if (root.has("fame")) {
                    character.setFame(root.path("fame").asLong());
                }
                if (root.has("level")) {
                    character.setLevel(root.path("level").asInt());
                }
            } else if (characterDetail instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detail = (Map<String, Object>) characterDetail;
                character.setJobName((String) detail.get("jobName"));
                setAdventureForCharacter(character, (String) detail.get("adventureName"));
                if (detail.get("fame") != null) {
                    character.setFame(((Number) detail.get("fame")).longValue());
                }
                if (detail.get("level") != null) {
                    character.setLevel(((Number) detail.get("level")).intValue());
                }
            }
        } catch (Exception e) {
            log.warn("DFO API 데이터로 캐릭터 업데이트 실패: {}", e.getMessage());
        }
    }

    private Map<String, Object> updateDundamInfo(Character character) {
        // Dundam에서 캐릭터 스펙 정보 조회 및 업데이트
        try {
            log.info("=== 던담 크롤링 시작 (캐릭터: {}) ===", character.getCharacterName());
            
            // DFO API에서 가져온 직업 정보를 함께 전달
            Map<String, Object> dundamInfo = dundamService.getCharacterInfoWithMethod(
                character.getServerId(), 
                character.getCharacterId(),
                "playwright"
            );
            
            log.info("던담 크롤링 결과: {}", dundamInfo);
            
            if (dundamInfo != null && dundamInfo.get("success") == Boolean.TRUE) {
                // 던담에서 가져온 스탯 값 확인 (characterInfo 객체에서 추출)
                Object characterInfoObj = dundamInfo.get("characterInfo");
                Object buffPowerObj = null;
                Object totalDamageObj = null;
                
                if (characterInfoObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> characterInfo = (Map<String, Object>) characterInfoObj;
                    buffPowerObj = characterInfo.get("buffPower");
                    totalDamageObj = characterInfo.get("totalDamage");
                    log.info("characterInfo에서 스탯 값 추출 - 버프력: {}, 총딜: {}", buffPowerObj, totalDamageObj);
                } else {
                    // 기존 방식으로도 시도
                    buffPowerObj = dundamInfo.get("buffPower");
                    totalDamageObj = dundamInfo.get("totalDamage");
                    log.info("기존 방식으로 스탯 값 추출 - 버프력: {}, 총딜: {}", buffPowerObj, totalDamageObj);
                }
                
                Long buffPower = null;
                Long totalDamage = null;
                
                // 타입 변환 처리
                if (buffPowerObj instanceof Integer) {
                    buffPower = ((Integer) buffPowerObj).longValue();
                } else if (buffPowerObj instanceof Long) {
                    buffPower = (Long) buffPowerObj;
                }
                
                if (totalDamageObj instanceof Integer) {
                    totalDamage = ((Integer) totalDamageObj).longValue();
                } else if (totalDamageObj instanceof Long) {
                    totalDamage = (Long) totalDamageObj;
                }
                
                log.info("던담 크롤링 스탯 값 - 버프력: {} (타입: {}), 총딜: {} (타입: {})", 
                    buffPower, buffPowerObj != null ? buffPowerObj.getClass().getSimpleName() : "null",
                    totalDamage, totalDamageObj != null ? totalDamageObj.getClass().getSimpleName() : "null");
                
                // 0이거나 null이면 업데이트하지 않음
                if (buffPower != null && buffPower > 0 && totalDamage != null && totalDamage > 0) {
                    log.info("던담 크롤링 성공 - 버프력: {}, 총딜: {}", buffPower, totalDamage);
                    
                    // 기본 스탯 업데이트
                    character.updateStats(buffPower, totalDamage, "dundam.xyz");
                    
                    // 나벨 적격성 업데이트
                    updateNabelEligibility(character);
                    
                    log.info("=== 던담 크롤링 완료 (캐릭터: {}) ===", character.getCharacterName());
                } else {
                    log.warn("던담 크롤링 결과가 0이거나 null이므로 업데이트 건너뜀 - 버프력: {}, 총딜: {}", buffPower, totalDamage);
                }
                
            } else if (dundamInfo != null) {
                // 실패한 경우 로그만 남기고 업데이트하지 않음
                String message = (String) dundamInfo.get("message");
                log.warn("Dundam 크롤링 실패로 인한 업데이트 건너뜀: {}", message);
            } else {
                log.warn("Dundam 크롤링 결과가 null입니다.");
            }
            
            return dundamInfo != null ? dundamInfo : Map.of();
        } catch (Exception e) {
            log.error("Dundam 정보 업데이트 실패: {}", e.getMessage(), e);
            return Map.of();
        }
    }

    private Character createCharacterFromData(Map<String, Object> data) {
        Character character = new Character(
            (String) data.get("characterId"),
            (String) data.get("characterName"),
            (String) data.get("serverId")
        );
        
        CharacterDetailDto characterDetailDto = convertMapToCharacterDetailDto(data);
        updateCharacterFromData(character, characterDetailDto);
        return character;
    }

    private void updateCharacterFromData(Character character, CharacterDetailDto data) {
        if (data.getAdventureName() != null && !data.getAdventureName().trim().isEmpty() && !"모험단 정보 없음".equals(data.getAdventureName())) {
            // 모험단 정보를 DB에서 조회하고 관계 설정
            setAdventureForCharacter(character, data.getAdventureName());
        }
        // fame과 level은 primitive 타입이므로 항상 설정
        character.setFame(data.getFame());
        character.setLevel(data.getLevel());
        
        // 총딜/버프력은 DFO API 호출 시에는 업데이트하지 않음 (수동 입력이나 던담 동기화 시에만)
        // DFO API에서 가져온 데이터는 기본적으로 0이므로 업데이트하지 않음
        if (data.getBuffPower() != null && data.getBuffPower() > 0) {
            character.setBuffPower(data.getBuffPower());
        }
        if (data.getTotalDamage() != null && data.getTotalDamage() > 0) {
            character.setTotalDamage(data.getTotalDamage());
        }
        if (data.getJobName() != null) {
            character.setJobName(data.getJobName());
        }
        if (data.getJobGrowName() != null) {
            character.setJobGrowName(data.getJobGrowName());
        }
        if (data.getDungeonClearNabel() != null) {
            character.setDungeonClearNabel(data.getDungeonClearNabel());
        }
        if (data.getDungeonClearVenus() != null) {
            character.setDungeonClearVenus(data.getDungeonClearVenus());
        }
        if (data.getDungeonClearFog() != null) {
            character.setDungeonClearFog(data.getDungeonClearFog());
        }
    }

    /**
     * DFO API 데이터로 캐릭터 업데이트 (총딜/버프력 제외)
     * refresh API에서 사용하여 던담 크롤링 결과가 0으로 덮어쓰이지 않도록 함
     */
    private void updateCharacterFromDfoApiExcludingStats(Character character, Object characterDetail) {
        try {
            CharacterDetailDto characterDetailDto = convertMapToCharacterDetailDto((Map<String, Object>) characterDetail);
            
            // 모험단 정보 설정
            if (characterDetailDto.getAdventureName() != null && !characterDetailDto.getAdventureName().trim().isEmpty() && !"모험단 정보 없음".equals(characterDetailDto.getAdventureName())) {
                setAdventureForCharacter(character, characterDetailDto.getAdventureName());
            }
            
            // 기본 정보 업데이트 (총딜/버프력 제외)
            character.setFame(characterDetailDto.getFame());
            character.setLevel(characterDetailDto.getLevel());
            
            if (characterDetailDto.getJobName() != null) {
                character.setJobName(characterDetailDto.getJobName());
            }
            if (characterDetailDto.getJobGrowName() != null) {
                character.setJobGrowName(characterDetailDto.getJobGrowName());
            }
            if (characterDetailDto.getDungeonClearNabel() != null) {
                character.setDungeonClearNabel(characterDetailDto.getDungeonClearNabel());
            }
            if (characterDetailDto.getDungeonClearVenus() != null) {
                character.setDungeonClearVenus(characterDetailDto.getDungeonClearVenus());
            }
            if (characterDetailDto.getDungeonClearFog() != null) {
                character.setDungeonClearFog(characterDetailDto.getDungeonClearFog());
            }
            
            log.info("DFO API 데이터로 캐릭터 업데이트 완료 (총딜/버프력 제외): {}", character.getCharacterName());
            
        } catch (Exception e) {
            log.warn("DFO API 데이터로 캐릭터 업데이트 실패: {}", e.getMessage());
        }
    }

    /**
     * 캐릭터에 모험단 관계 설정
     */
    private void setAdventureForCharacter(Character character, String adventureName) {
        try {
            log.info("모험단 관계 설정 시작: character={}, adventureName={}", 
                character.getCharacterName(), adventureName);
            
            // 모험단을 DB에서 조회 (서버별로 조회)
            Optional<Adventure> adventureOpt = adventureRepository.findByAdventureNameAndServerId(adventureName, character.getServerId());
            if (adventureOpt.isPresent()) {
                Adventure adventure = adventureOpt.get();
                character.setAdventure(adventure);
                log.info("캐릭터 {}에 기존 모험단 {} (ID: {}) 관계 설정 완료", 
                    character.getCharacterName(), adventureName, adventure.getId());
            } else {
                log.info("모험단 {}이 DB에 없으므로 새로 생성합니다", adventureName);
                
                // 모험단이 없으면 새로 생성 (serverId 포함)
                Adventure newAdventure = Adventure.builder()
                    .adventureName(adventureName)
                    .serverId(character.getServerId())
                    .build();
                Adventure savedAdventure = adventureRepository.save(newAdventure);
                character.setAdventure(savedAdventure);
                
                log.info("새로운 모험단 {} (ID: {}) 생성 및 캐릭터 {}에 관계 설정 완료", 
                    adventureName, savedAdventure.getId(), character.getCharacterName());
            }
            
            // 설정 후 확인
            if (character.getAdventure() != null) {
                log.info("모험단 관계 설정 확인: 캐릭터 {} -> 모험단 {} (ID: {})", 
                    character.getCharacterName(), 
                    character.getAdventure().getAdventureName(),
                    character.getAdventure().getId());
            } else {
                log.warn("모험단 관계 설정 실패: 캐릭터 {}의 adventure가 null", character.getCharacterName());
            }
            
        } catch (Exception e) {
            log.error("모험단 관계 설정 실패: character={}, adventure={}, error={}", 
                character.getCharacterName(), adventureName, e.getMessage(), e);
        }
    }

    private Map<String, Object> convertToDto(Character character) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("characterId", character.getCharacterId());
        dto.put("characterName", character.getCharacterName());
        dto.put("serverId", character.getServerId());
        
        // 모험단 정보 로깅
        String adventureName = character.getAdventureName();
        log.info("convertToDto - 캐릭터: {}, 모험단 정보: '{}'", character.getCharacterName(), adventureName);
        
        // adventureName이 null이면 "모험단 정보 없음"으로 설정 (N/A 대신)
        if (adventureName == null || adventureName.trim().isEmpty()) {
            adventureName = "모험단 정보 없음";
            log.warn("캐릭터 {}의 모험단 정보가 없음. '모험단 정보 없음'으로 설정", character.getCharacterName());
        }
        dto.put("adventureName", adventureName);
        
        dto.put("fame", character.getFame());
        dto.put("buffPower", character.getBuffPower());
        dto.put("totalDamage", character.getTotalDamage());
        dto.put("level", character.getLevel());
        dto.put("jobName", character.getDisplayJobName());
        dto.put("jobGrowName", character.getJobGrowName());
        dto.put("dungeonClearNabel", character.getDungeonClearNabel());
        dto.put("dungeonClearVenus", character.getDungeonClearVenus());
        dto.put("dungeonClearFog", character.getDungeonClearFog());
        dto.put("dungeonClearTwilight", character.getDungeonClearTwilight());
        dto.put("isFavorite", character.getIsFavorite());
        dto.put("excludedDungeons", parseExcludedDungeons(character.getExcludedDungeons()));
        dto.put("createdAt", character.getCreatedAt());
        dto.put("updatedAt", character.getUpdatedAt());
        
        // 수동 입력 값들도 포함
        dto.put("manualBuffPower", character.getManualBuffPower());
        dto.put("manualTotalDamage", character.getManualTotalDamage());

        
        // 안감/업둥 상태 추가
        dto.put("isExcludedNabel", character.getIsExcludedNabel());
        dto.put("isExcludedVenus", character.getIsExcludedVenus());
        dto.put("isExcludedFog", character.getIsExcludedFog());
        dto.put("isSkipNabel", character.getIsSkipNabel());
        dto.put("isSkipVenus", character.getIsSkipVenus());
        dto.put("isSkipFog", character.getIsSkipFog());
        
        // 하드 나벨 대상자 여부 추가
        dto.put("isHardNabelEligible", character.getIsHardNabelEligible());
        
        // 일반 나벨 대상자 여부 추가
        dto.put("isNormalNabelEligible", character.getIsNormalNabelEligible());
        
        // 매칭 나벨 대상자 여부 추가
        dto.put("isMatchingNabelEligible", character.getIsMatchingNabelEligible());
        
        // 이내 황혼전 대상자 여부 추가
        dto.put("isTwilightEligible", character.getIsTwilightEligible());
        

        // 선택된 나벨 난이도 추가
        String selectedDifficulty = getNabelDifficultySelection(character.getCharacterId());
        if (selectedDifficulty != null) {
            dto.put("selectedNabelDifficulty", selectedDifficulty);
        }
        
        log.info("convertToDto 완료 - DTO에 포함된 모험단 정보: '{}'", dto.get("adventureName"));
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
     * 나벨 난이도 선택 저장
     */
    @Transactional
    public Map<String, Object> saveNabelDifficultySelection(String characterId, String difficulty) {
        try {
            log.info("나벨 난이도 선택 저장: characterId={}, difficulty={}", characterId, difficulty);
            
            // 기존 선택 삭제
            nabelDifficultySelectionRepository.deleteByCharacterId(characterId);
            
            // 새로운 선택 저장
            NabelDifficultySelection.NabelDifficulty selectedDifficulty = 
                NabelDifficultySelection.NabelDifficulty.valueOf(difficulty.toUpperCase());
            
            NabelDifficultySelection selection = NabelDifficultySelection.builder()
                .characterId(characterId)
                .selectedDifficulty(selectedDifficulty)
                .build();
            
            nabelDifficultySelectionRepository.save(selection);
            
            log.info("나벨 난이도 선택 저장 완료: characterId={}, difficulty={}", characterId, difficulty);
            
            return createSuccessResponse(
                String.format("나벨 난이도 선택이 저장되었습니다: %s", difficulty),
                Map.of(
                    "characterId", characterId,
                    "selectedDifficulty", difficulty
                )
            );
            
        } catch (Exception e) {
            log.error("나벨 난이도 선택 저장 실패: characterId={}, difficulty={}", characterId, difficulty, e);
            return createErrorResponse("나벨 난이도 선택 저장에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 나벨 난이도 선택 조회
     */
    @Transactional(readOnly = true)
    public String getNabelDifficultySelection(String characterId) {
        try {
            Optional<NabelDifficultySelection> selection = 
                nabelDifficultySelectionRepository.findByCharacterId(characterId);
            
            if (selection.isPresent()) {
                return selection.get().getSelectedDifficulty().name().toLowerCase();
            }
            
            return null; // 선택된 난이도가 없음
            
        } catch (Exception e) {
            log.error("나벨 난이도 선택 조회 실패: characterId={}", characterId, e);
            return null;
        }
    }
    
    /**
     * server_id가 'all'인 캐릭터들을 'bakal'로 변경
     */
    @Transactional
    public Map<String, Object> fixServerIdForAllCharacters() {
        try {
            log.info("=== server_id가 'all'인 캐릭터들을 'bakal'로 변경 시작 ===");
            
            // server_id가 'all'인 캐릭터들 조회
            List<Character> allCharacters = characterRepository.findByServerId("all");
            log.info("server_id가 'all'인 캐릭터 {}개 발견", allCharacters.size());
            
            if (allCharacters.isEmpty()) {
                return createSuccessResponse("변경할 캐릭터가 없습니다.", null);
            }
            
            int updatedCount = 0;
            for (Character character : allCharacters) {
                try {
                    log.info("캐릭터 {}의 server_id를 'all'에서 'bakal'로 변경", character.getCharacterName());
                    character.setServerId("bakal");
                    characterRepository.save(character);
                    updatedCount++;
                } catch (Exception e) {
                    log.error("캐릭터 {} server_id 변경 실패: {}", character.getCharacterName(), e.getMessage());
                }
            }
            
            log.info("=== server_id 변경 완료: {}개 캐릭터 업데이트됨 ===", updatedCount);
            
            return createSuccessResponse(
                String.format("server_id 변경 완료: %d개 캐릭터가 'bakal'로 변경되었습니다.", updatedCount),
                Map.of(
                    "updatedCount", updatedCount,
                    "characters", allCharacters.stream()
                        .map(Character::getCharacterName)
                        .collect(java.util.stream.Collectors.toList())
                )
            );
            
        } catch (Exception e) {
            log.error("server_id 변경 실패: {}", e.getMessage(), e);
            return createErrorResponse("server_id 변경 중 오류가 발생했습니다: " + e.getMessage());
        }
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

    /**
     * 캐릭터 정보를 DB에 저장 또는 업데이트
     */
    private void saveOrUpdateCharacterToDb(Map<String, Object> characterData) {
        try {
            String charId = (String) characterData.get("characterId");
            if (charId == null || charId.trim().isEmpty()) {
                log.warn("characterId가 없어서 DB 저장을 건너뜀");
                return;
            }

            // 서버 정보 자동 저장 (실제 서버 ID 사용)
            String serverId = (String) characterData.get("serverId");
            if (serverId != null && !serverId.trim().isEmpty() && !"all".equals(serverId)) {
                saveServerToDB(serverId);
                log.info("서버 정보 DB 저장 완료: serverId={}", serverId);
            }

            // 모험단 정보 자동 저장 (Adventure 엔티티 먼저 저장)
            String adventureName = (String) characterData.get("adventureName");
            String adventureServerId = (String) characterData.get("serverId");
            if (adventureName != null && !adventureName.equals("모험단 정보 없음") && !adventureName.trim().isEmpty()) {
                saveAdventureToDB(adventureName, adventureServerId);
                log.info("모험단 정보 DB 저장 완료: adventureName={}, serverId={}", adventureName, adventureServerId);
            }

            // 기존 캐릭터 조회
            Optional<Character> existingCharOpt = characterRepository.findByCharacterId(charId);
            Character character;

            if (existingCharOpt.isPresent()) {
                // 기존 캐릭터 업데이트
                character = existingCharOpt.get();
                updateCharacterFromMap(character, characterData);
                log.debug("기존 캐릭터 업데이트: characterId={}", charId);
            } else {
                // 새 캐릭터 생성
                character = createCharacterFromMap(characterData);
                log.debug("새 캐릭터 생성: characterId={}", charId);
            }

            // DB에 저장
            characterRepository.save(character);
            log.info("캐릭터 DB 저장 성공: characterId={}, name={}", charId, character.getCharacterName());

        } catch (Exception e) {
            log.error("캐릭터 DB 저장 중 예외 발생", e);
            throw e;
        }
    }

    /**
     * Map 데이터로부터 새 Character 엔티티 생성
     */
    private Character createCharacterFromMap(Map<String, Object> data) {
        Character character = new Character();
        
        // 기본 정보
        character.setCharacterId(getStringValue(data, "characterId"));
        character.setCharacterName(getStringValue(data, "characterName"));
        character.setServerId(getStringValue(data, "serverId"));
        character.setJobId(getStringValue(data, "jobId"));
        character.setJobGrowId(getStringValue(data, "jobGrowId"));
        character.setJobName(getStringValue(data, "jobName"));
        character.setJobGrowName(getStringValue(data, "jobGrowName"));
        
        // 레벨과 명성
        character.setLevel(getIntegerValue(data, "level"));
        character.setFame(getLongValue(data, "fame"));
        
        // 모험단 정보 설정 (Adventure 엔티티 참조)
        String adventureName = getStringValue(data, "adventureName");
        if (adventureName != null && !adventureName.equals("모험단 정보 없음") && !adventureName.trim().isEmpty()) {
            // Adventure 엔티티 조회 및 설정
            Optional<Adventure> adventureOpt = adventureRepository.findByAdventureName(adventureName);
            if (adventureOpt.isPresent()) {
                character.setAdventure(adventureOpt.get());
                log.debug("Adventure 엔티티 설정: adventureName={}, adventureId={}", adventureName, adventureOpt.get().getId());
            } else {
                log.warn("Adventure 엔티티를 찾을 수 없음: adventureName={}", adventureName);
            }
        }
        
        // 던담 정보 (DFO API 호출 시에는 0으로 설정, 수동 입력이나 던담 동기화 시에만 실제 값 설정)
        Long buffPower = getLongValue(data, "buffPower");
        Long totalDamage = getLongValue(data, "totalDamage");
        
        // 수동 입력이나 던담 동기화로 가져온 데이터인 경우에만 설정
        if (buffPower != null && buffPower > 0) {
            character.setBuffPower(buffPower);
            character.setDundamSource("dundam.xyz");
            character.setLastStatsUpdate(LocalDateTime.now());
        } else {
            character.setBuffPower(0L); // 기본값 0
        }
        
        if (totalDamage != null && totalDamage > 0) {
            character.setTotalDamage(totalDamage);
            character.setDundamSource("dundam.xyz");
            character.setLastStatsUpdate(LocalDateTime.now());
        } else {
            character.setTotalDamage(0L); // 기본값 0
        }
        
        // 이미지 URL 정보
        character.setCharacterImageUrl(getStringValue(data, "characterImageUrl"));
        character.setAvatarImageUrl(getStringValue(data, "avatarImageUrl"));
        
        // 던전 클리어 정보
        character.setDungeonClearNabel(getBooleanValue(data, "dungeonClearNabel"));
        character.setDungeonClearVenus(getBooleanValue(data, "dungeonClearVenus"));
        character.setDungeonClearFog(getBooleanValue(data, "dungeonClearFog"));
        character.setLastDungeonCheck(LocalDateTime.now());
        
        return character;
    }

    /**
     * 기존 Character 엔티티를 Map 데이터로 업데이트
     */
    private void updateCharacterFromMap(Character character, Map<String, Object> data) {
        // 레벨과 명성 업데이트 (동적 정보)
        Integer level = getIntegerValue(data, "level");
        if (level != null) {
            character.setLevel(level);
        }
        
        Long fame = getLongValue(data, "fame");
        if (fame != null) {
            character.setFame(fame);
        }
        
        // 모험단 정보 업데이트 (Adventure 엔티티 참조)
        String adventureName = getStringValue(data, "adventureName");
        if (adventureName != null && !adventureName.equals("모험단 정보 없음") && !adventureName.trim().isEmpty()) {
            // Adventure 엔티티 조회 및 설정
            Optional<Adventure> adventureOpt = adventureRepository.findByAdventureName(adventureName);
            if (adventureOpt.isPresent()) {
                character.setAdventure(adventureOpt.get());
                log.debug("Adventure 엔티티 업데이트: adventureName={}, adventureId={}", adventureName, adventureOpt.get().getId());
            } else {
                log.warn("Adventure 엔티티를 찾을 수 없음: adventureName={}", adventureName);
            }
        }
        
        // 던담 정보 업데이트 (DFO API 호출 시에는 업데이트하지 않음, 수동 입력이나 던담 동기화 시에만)
        // DFO API에서 가져온 데이터는 buffPower와 totalDamage가 0이므로 업데이트하지 않음
        Long buffPower = getLongValue(data, "buffPower");
        Long totalDamage = getLongValue(data, "totalDamage");
        
        // 수동 입력이나 던담 동기화로 가져온 데이터인 경우에만 업데이트
        // (DFO API 데이터는 기본적으로 0이므로 업데이트하지 않음)
        if (buffPower != null && buffPower > 0) {
            character.setBuffPower(buffPower);
            character.setDundamSource("dundam.xyz");
            character.setLastStatsUpdate(LocalDateTime.now());
        }
        if (totalDamage != null && totalDamage > 0) {
            character.setTotalDamage(totalDamage);
            character.setDundamSource("dundam.xyz");
            character.setLastStatsUpdate(LocalDateTime.now());
        }
        
        // 던전 클리어 정보 업데이트 (있으면)
        Boolean nabel = getBooleanValue(data, "dungeonClearNabel");
        Boolean venus = getBooleanValue(data, "dungeonClearVenus");
        Boolean fog = getBooleanValue(data, "dungeonClearFog");
        if (nabel != null || venus != null || fog != null) {
            character.setDungeonClearNabel(nabel != null ? nabel : character.getDungeonClearNabel());
            character.setDungeonClearVenus(venus != null ? venus : character.getDungeonClearVenus());
            character.setDungeonClearFog(fog != null ? fog : character.getDungeonClearFog());
            character.setLastDungeonCheck(LocalDateTime.now());
        }
    }

    // Helper 메서드들
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * 모험단별 캐릭터 조회
     */
    public Map<String, Object> getCharactersByAdventure(String adventureName) {
        try {
            log.info("=== 던전 클리어 현황: 모험단 검색 시작 ===");
            log.info("검색할 모험단명: '{}'", adventureName);
            
            // 1. 모험단이 DB에 존재하는지 확인
            Optional<Adventure> adventure = adventureRepository.findByAdventureName(adventureName);
            if (adventure.isEmpty()) {
                log.warn("❌ 모험단을 찾을 수 없음: '{}'", adventureName);
                return createErrorResponse("모험단 '" + adventureName + "'을 찾을 수 없습니다.");
            }
            
            Adventure foundAdventure = adventure.get();
            log.info("✅ 모험단 찾음:");
            log.info("   - 모험단명: {}", foundAdventure.getAdventureName());
            log.info("   - 모험단 ID: {}", foundAdventure.getId());
            log.info("   - 생성일: {}", foundAdventure.getCreatedAt());
            
            // 2. 해당 모험단의 캐릭터들 조회
            log.info("해당 모험단의 캐릭터 조회 시작...");
            List<Character> characters = characterRepository.findByAdventure_AdventureName(adventureName);
            log.info("조회된 캐릭터 수: {}개", characters.size());
            
            // 3. 각 캐릭터 정보 로깅
            for (int i = 0; i < characters.size(); i++) {
                Character character = characters.get(i);
                log.info("   {}. 캐릭터명: {}, 서버: {}, 레벨: {}, 명성: {}", 
                    (i + 1), character.getCharacterName(), character.getServerId(), 
                    character.getLevel(), character.getFame());
            }
            
            // 4. 각 캐릭터의 던전 클리어 현황 조회 및 업데이트
            log.info("각 캐릭터의 던전 클리어 현황 조회 시작...");
            for (Character character : characters) {
                try {
                    log.info("캐릭터 {}의 던전 클리어 현황 조회 중...", character.getCharacterName());
                    Map<String, Object> clearStatusInfo = dungeonClearService.getDungeonClearStatus(
                        character.getServerId(), 
                        character.getCharacterId()
                    );
                    
                    if (clearStatusInfo != null && Boolean.TRUE.equals(clearStatusInfo.get("success"))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Boolean> clearStatus = (Map<String, Boolean>) clearStatusInfo.get("clearStatus");
                        
                        // 던전 클리어 상태 업데이트
                        character.setDungeonClearNabel(clearStatus.getOrDefault("nabel", false));
                        character.setDungeonClearVenus(clearStatus.getOrDefault("venus", false));
                        character.setDungeonClearFog(clearStatus.getOrDefault("fog", false));
                        character.setDungeonClearTwilight(clearStatus.getOrDefault("twilight", false));
                        
                        log.info("캐릭터 {} 던전 클리어 현황 업데이트: nabel={}, venus={}, fog={}, twilight={}", 
                            character.getCharacterName(), clearStatus.get("nabel"), clearStatus.get("venus"), 
                            clearStatus.get("fog"), clearStatus.get("twilight"));
                    } else {
                        log.warn("캐릭터 {}의 던전 클리어 현황 조회 실패: {}", character.getCharacterName(), clearStatusInfo);
                    }
                } catch (Exception e) {
                    log.warn("캐릭터 {}의 던전 클리어 현황 조회 중 오류: {}", character.getCharacterName(), e.getMessage());
                }
            }
            
            // 5. DTO 변환
            log.info("캐릭터 정보를 DTO로 변환 중...");
            List<Map<String, Object>> characterDtos = characters.stream()
                .map(this::convertToDto)
                .collect(java.util.stream.Collectors.toList());
            log.info("DTO 변환 완료: {}개", characterDtos.size());
            
            Map<String, Object> result = createSuccessResponse(
                "모험단 '" + adventureName + "'의 캐릭터 " + characters.size() + "개를 조회했습니다.",
                Map.of(
                    "adventureName", adventureName,
                    "characters", characterDtos,
                    "count", characters.size()
                )
            );
            
            log.info("=== 던전 클리어 현황: 모험단 검색 완료 ===");
            return result;
            
        } catch (Exception e) {
            log.error("❌ 모험단별 캐릭터 조회 실패: adventureName={}", adventureName);
            log.error("   - 에러 메시지: {}", e.getMessage());
            log.error("   - 스택 트레이스:", e);
            return createErrorResponse("모험단별 캐릭터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모든 모험단 목록 조회
     */
    public Map<String, Object> getAllAdventures() {
        try {
            List<String> adventures = characterRepository.findDistinctAdventureNames();
            
            // N/A나 null 제거
            List<String> validAdventures = adventures.stream()
                .filter(name -> name != null && !name.trim().isEmpty() && !"모험단 정보 없음".equals(name))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            return createSuccessResponse(
                validAdventures.size() + "개의 모험단을 조회했습니다.",
                Map.of(
                    "adventures", validAdventures,
                    "count", validAdventures.size()
                )
            );
            
        } catch (Exception e) {
            log.error("모험단 목록 조회 실패", e);
            return createErrorResponse("모험단 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모험단별 캐릭터 수 통계
     */
    public Map<String, Object> getAdventureStatistics() {
        try {
            List<Character> allCharacters = characterRepository.findAll();
            
            Map<String, Long> adventureStats = allCharacters.stream()
                .filter(c -> c.getAdventureName() != null && !c.getAdventureName().trim().isEmpty() && !"모험단 정보 없음".equals(c.getAdventureName()))
                .collect(java.util.stream.Collectors.groupingBy(
                    Character::getAdventureName,
                    java.util.stream.Collectors.counting()
                ));
            
            return createSuccessResponse(
                "모험단별 캐릭터 수 통계를 조회했습니다.",
                Map.of(
                    "statistics", adventureStats,
                    "totalAdventures", adventureStats.size(),
                    "totalCharacters", allCharacters.size()
                )
            );
            
        } catch (Exception e) {
            log.error("모험단 통계 조회 실패", e);
            return createErrorResponse("모험단 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 목요일 DB 전용 캐릭터 검색
     */
    private Map<String, Object> searchCharactersFromDB(String characterName, String serverId, Map<String, Object> thursdayRestriction) {
        try {
            log.info("DB에서 캐릭터 검색: characterName={}, serverId={}", characterName, serverId);
            
            List<Character> characters;
            
            if ("all".equals(serverId)) {
                // 전체 서버에서 캐릭터명으로 검색
                characters = characterRepository.findAll().stream()
                    .filter(c -> c.getCharacterName().toLowerCase().contains(characterName.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            } else {
                // 특정 서버에서 캐릭터명으로 검색
                characters = characterRepository.findByServerId(serverId).stream()
                    .filter(c -> c.getCharacterName().toLowerCase().contains(characterName.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (characters.isEmpty()) {
                Map<String, Object> response = createErrorResponse("DB에서 캐릭터를 찾을 수 없습니다.");
                response.put("thursdayRestriction", thursdayRestriction);
                return response;
            }
            
            // Character 엔티티를 Map으로 변환
            List<Map<String, Object>> characterMaps = characters.stream()
                .map(this::convertToDto)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = createSuccessResponse(
                "DB에서 " + characters.size() + "개의 캐릭터를 찾았습니다. (목요일 제한 모드)",
                Map.of("characters", characterMaps)
            );
            
            // 목요일 제한 정보 추가
            response.put("thursdayRestriction", thursdayRestriction);
            response.put("dataSource", "database");
            
            return response;
            
        } catch (Exception e) {
            log.error("DB 캐릭터 검색 실패", e);
            Map<String, Object> response = createErrorResponse("DB 검색 중 오류가 발생했습니다: " + e.getMessage());
            response.put("thursdayRestriction", thursdayRestriction);
            return response;
        }
    }

    /**
     * 던전별 업둥이 설정
     */
    public Map<String, Object> setDungeonFavorite(String characterId, String dungeonType, boolean isFavorite) {
        try {
            Character character = characterRepository.findByCharacterId(characterId)
                .orElseThrow(() -> new RuntimeException("캐릭터를 찾을 수 없습니다: " + characterId));

            // 던전 타입에 따라 해당 필드 업데이트
            switch (dungeonType.toLowerCase()) {
                case "nabel":
                case "navel":
                    character.setIsFavoriteNabel(isFavorite);
                    break;
                case "venus":
                    character.setIsFavoriteVenus(isFavorite);
                    break;
                case "fog":
                    character.setIsFavoriteFog(isFavorite);
                    break;
                case "twilight":
                    character.setIsFavoriteTwilight(isFavorite);
                    break;
                default:
                    throw new RuntimeException("지원하지 않는 던전 타입입니다: " + dungeonType);
            }

            character.setUpdatedAt(java.time.LocalDateTime.now());
            characterRepository.save(character);

            return createSuccessResponse(
                String.format("%s 던전에서 %s의 업둥이 상태가 %s로 변경되었습니다.",
                    getDungeonDisplayName(dungeonType),
                    character.getCharacterName(),
                    isFavorite ? "설정" : "해제"),
                Map.of(
                    "characterId", characterId,
                    "dungeonType", dungeonType,
                    "isFavorite", isFavorite
                )
            );

        } catch (Exception e) {
            log.error("던전별 업둥이 설정 실패: characterId={}, dungeonType={}", characterId, dungeonType, e);
            return createErrorResponse("업둥이 설정에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터의 던전별 업둥이 상태 조회
     */
    public Map<String, Object> getDungeonFavorites(String characterId) {
        try {
            Character character = characterRepository.findByCharacterId(characterId)
                .orElseThrow(() -> new RuntimeException("캐릭터를 찾을 수 없습니다: " + characterId));

            Map<String, Boolean> favorites = Map.of(
                "nabel", Boolean.TRUE.equals(character.getIsFavoriteNabel()),
                "venus", Boolean.TRUE.equals(character.getIsFavoriteVenus()),
                "fog", Boolean.TRUE.equals(character.getIsFavoriteFog()),
                "twilight", Boolean.TRUE.equals(character.getIsFavoriteTwilight())
            );

            return createSuccessResponse(
                "던전별 업둥이 상태 조회 완료",
                Map.of(
                    "characterId", characterId,
                    "characterName", character.getCharacterName(),
                    "favorites", favorites
                )
            );

        } catch (Exception e) {
            log.error("던전별 업둥이 상태 조회 실패: characterId={}", characterId, e);
            return createErrorResponse("업둥이 상태 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전 표시명 반환
     */
    private String getDungeonDisplayName(String dungeonType) {
        switch (dungeonType.toLowerCase()) {
            case "nabel":
            case "navel":
                return "나벨";
            case "venus":
                return "베누스";
            case "fog":
                return "여신전";
            case "twilight":
                return "이내 황혼전";
            default:
                return dungeonType;
        }
    }

    /**
     * 수동 스탯 입력
     */
    public Map<String, Object> updateManualStats(String characterId, ManualStatsUpdateDto manualStatsDto) {
        try {
            Character character = characterRepository.findByCharacterId(characterId)
                .orElseThrow(() -> new RuntimeException("캐릭터를 찾을 수 없습니다: " + characterId));

            // 수동 입력 값 업데이트
            if (manualStatsDto.getBuffPower() != null || manualStatsDto.getTotalDamage() != null) {
                character.updateManualStats(
                    manualStatsDto.getBuffPower(),
                    manualStatsDto.getTotalDamage(),
                    manualStatsDto.getUpdatedBy() != null ? manualStatsDto.getUpdatedBy() : "사용자"
                );
                
                // 메인 스탯 컬럼도 함께 업데이트 (프론트엔드 표시용)
                if (manualStatsDto.getBuffPower() != null) {
                    character.setBuffPower(manualStatsDto.getBuffPower());
                }
                if (manualStatsDto.getTotalDamage() != null) {
                    character.setTotalDamage(manualStatsDto.getTotalDamage());
                }
            }

            // 4인/3인/2인 수동 입력 값 업데이트
            // 나벨 적격성 업데이트
            updateNabelEligibility(character);

            characterRepository.save(character);

            // 실시간 이벤트 발송 (선택적)
            try {
                if (realtimeEventService != null) {
                    // realtimeEventService.sendCharacterUpdate(characterId, "manual_stats_updated");
                }
            } catch (Exception e) {
                log.warn("실시간 이벤트 발송 실패: {}", e.getMessage());
            }

            // null 값 처리를 위해 HashMap 사용
            Map<String, Object> manualStats = new HashMap<>();
            manualStats.put("buffPower", character.getManualBuffPower());
            manualStats.put("totalDamage", character.getManualTotalDamage());
            manualStats.put("updatedAt", character.getManualUpdatedAt());
            manualStats.put("updatedBy", character.getManualUpdatedBy());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("characterId", characterId);
            responseData.put("characterName", character.getCharacterName());
            responseData.put("manualStats", manualStats);

            return createSuccessResponse(
                "수동 스탯이 성공적으로 업데이트되었습니다.",
                responseData
            );

        } catch (Exception e) {
            log.error("수동 스탯 입력 실패: characterId={}", characterId, e);
            return createErrorResponse("수동 스탯 입력에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐릭터의 수동/동기화 스탯 조회
     */
    public Map<String, Object> getCharacterStats(String characterId) {
        try {
            Character character = characterRepository.findByCharacterId(characterId)
                .orElseThrow(() -> new RuntimeException("캐릭터를 찾을 수 없습니다: " + characterId));

            // null 값 처리를 위해 HashMap 사용
            Map<String, Object> manualStats = new HashMap<>();
            manualStats.put("buffPower", character.getManualBuffPower());
            manualStats.put("totalDamage", character.getManualTotalDamage());
            manualStats.put("updatedAt", character.getManualUpdatedAt());
            manualStats.put("updatedBy", character.getManualUpdatedBy());

            Map<String, Object> syncedStats = new HashMap<>();
            syncedStats.put("buffPower", character.getBuffPower());
            syncedStats.put("totalDamage", character.getTotalDamage());
            syncedStats.put("lastUpdate", character.getLastStatsUpdate());
            syncedStats.put("source", character.getDundamSource());

            Map<String, Object> effectiveStats = new HashMap<>();
            effectiveStats.put("buffPower", character.getEffectiveBuffPower());
            effectiveStats.put("totalDamage", character.getEffectiveTotalDamage());

            Map<String, Object> stats = new HashMap<>();
            stats.put("characterId", characterId);
            stats.put("characterName", character.getCharacterName());
            stats.put("manual", manualStats);
            stats.put("synced", syncedStats);
            stats.put("effective", effectiveStats);

            return createSuccessResponse("캐릭터 스탯 조회 완료", stats);

        } catch (Exception e) {
            log.error("캐릭터 스탯 조회 실패: characterId={}", characterId, e);
            return createErrorResponse("캐릭터 스탯 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 직업이 버퍼인지 확인 (DB 기반)
     */
    public boolean isBuffer(String jobName, String jobGrowName) {
        return characterUtils.isBuffer(jobName, jobGrowName);
    }
    
    /**
     * 직업이 딜러인지 확인 (DB 기반)
     */
    public boolean isDealer(String jobName, String jobGrowName) {
        return characterUtils.isDealer(jobName, jobGrowName);
    }
    
    /**
     * 하드 나벨 대상자 여부 확인
     */
    public boolean isHardNabelEligible(Character character) {
        if (character == null) return false;
        
        // 명성 47,684 이상 조건 확인
        if (character.getFame() == null || character.getFame() < 47684L) {
            return false;
        }
        
        boolean isBuffer = isBuffer(character.getJobName(), character.getJobGrowName());
        
        if (isBuffer) {
            // 버퍼: 버프력 500만 이상
            Long buffPower = character.getEffectiveBuffPower();
            return buffPower != null && buffPower >= 5000000;
        } else {
            // 딜러: 총딜 100억 이상
            Long totalDamage = character.getEffectiveTotalDamage();
            return totalDamage != null && totalDamage >= 10000000000L;
        }
    }
    
    /**
     * 일반 나벨 대상자 여부 확인
     */
    public boolean isNormalNabelEligible(Character character) {
        if (character == null) return false;
        
        // 명성 47,684 이상 조건 확인
        if (character.getFame() == null || character.getFame() < 47684L) {
            return false;
        }
        
        boolean isBuffer = isBuffer(character.getJobName(), character.getJobGrowName());
        
        if (isBuffer) {
            // 버퍼: 버프력 400만 이상
            Long buffPower = character.getEffectiveBuffPower();
            return buffPower != null && buffPower >= 4000000;
        } else {
            // 딜러: 총딜 30억 이상
            Long totalDamage = character.getEffectiveTotalDamage();
            return totalDamage != null && totalDamage >= 3000000000L;
        }
    }
    
    /**
     * 매칭 나벨 대상자 여부 확인 (명성만 충족하면 가능)
     */
    public boolean isMatchingNabelEligible(Character character) {
        if (character == null) return false;
        
        // 명성 47,684 이상 조건만 확인 (스펙컷 없음)
        return character.getFame() != null && character.getFame() >= 47684L;
    }
    
    /**
     * 베누스 대상자 여부 확인 (명성만 충족하면 가능)
     */
    public boolean isVenusEligible(Character character) {
        if (character == null) return false;
        
        // 명성 41,929 이상 조건만 확인 (스펙컷 없음)
        return character.getFame() != null && character.getFame() >= 41929L;
    }
    
    /**
     * 안개신 대상자 여부 확인 (명성만 충족하면 가능)
     */
    public boolean isFogEligible(Character character) {
        if (character == null) return false;
        
        // 명성 30,135 이상 조건만 확인 (스펙컷 없음)
        return character.getFame() != null && character.getFame() >= 30135L;
    }
    
       /**
     * 이내 황혼전 대상자 여부 확인
     */
    public boolean isTwilightEligible(Character character) {
        if (character == null) return false;
        
        // 명성 72,688 이상 조건 확인
        if (character.getFame() == null || character.getFame() < 72688L) {
            return false;
        }
        
        boolean isBuffer = isBuffer(character.getJobName(), character.getJobGrowName());
        
        if (isBuffer) {
            // 버퍼: 버프력 520만 이상 (황혼전 전용)
            Long buffPower = character.getEffectiveBuffPower();
            return buffPower != null && buffPower >= 5200000;
        } else {
            // 딜러: 총딜 100억 이상 (하드 나벨과 동일)
            Long totalDamage = character.getEffectiveTotalDamage();
            return totalDamage != null && totalDamage >= 10000000000L;
        }
    }
    

    /**
     * 하드 나벨 대상자 여부 업데이트
     */
    public Map<String, Object> updateHardNabelEligibility(String characterId) {
        try {
            log.info("하드 나벨 대상자 여부 업데이트 시작: characterId={}", characterId);
            
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                log.warn("캐릭터를 찾을 수 없음: characterId={}", characterId);
                return Map.of("success", false, "message", "캐릭터를 찾을 수 없습니다.");
            }
            
            Character character = characterOpt.get();
            boolean isEligible = isHardNabelEligible(character);
            
            // DB에 하드 나벨 대상자 여부 저장
            character.setIsHardNabelEligible(isEligible);
            
            // 일반 나벨 대상자 여부도 함께 업데이트
            boolean isNormalEligible = isNormalNabelEligible(character);
            character.setIsNormalNabelEligible(isNormalEligible);
            
            characterRepository.save(character);
            
            log.info("하드 나벨 대상자 여부 업데이트 완료: characterId={}, isEligible={}", characterId, isEligible);
            
                            // SSE로 실시간 업데이트 알림
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("characterId", characterId);
            eventData.put("isHardNabelEligible", isEligible);
            realtimeEventService.notifyCharacterUpdated(characterId, "HARD_NABEL_ELIGIBILITY_UPDATED", eventData);
            
            return Map.of(
                "success", true,
                "isHardNabelEligible", isEligible,
                "message", "하드 나벨 대상자 여부가 업데이트되었습니다."
            );
            
        } catch (Exception e) {
            log.error("하드 나벨 대상자 여부 업데이트 실패: characterId={}", characterId, e);
            return Map.of("success", false, "message", "업데이트에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 이내 황혼전 대상자 여부 업데이트
     */
    public Map<String, Object> updateTwilightEligibility(String characterId) {
        try {
            log.info("이내 황혼전 대상자 여부 업데이트 시작: characterId={}", characterId);
            
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                log.warn("캐릭터를 찾을 수 없음: characterId={}", characterId);
                return Map.of("success", false, "message", "캐릭터를 찾을 수 없습니다.");
            }
            
            Character character = characterOpt.get();
            boolean isEligible = isTwilightEligible(character);
            
            // DB에 이내 황혼전 대상자 여부 저장
            character.setIsTwilightEligible(isEligible);
            characterRepository.save(character);
            
            log.info("이내 황혼전 대상자 여부 업데이트 완료: characterId={}, isEligible={}", characterId, isEligible);
            
            // SSE로 실시간 업데이트 알림
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("characterId", characterId);
            eventData.put("isTwilightEligible", isEligible);
            realtimeEventService.notifyCharacterUpdated(characterId, "TWILIGHT_ELIGIBILITY_UPDATED", eventData);
            
            return Map.of(
                "success", true,
                "isTwilightEligible", isEligible,
                "message", "이내 황혼전 대상자 여부가 업데이트되었습니다."
            );
            
        } catch (Exception e) {
            log.error("이내 황혼전 대상자 여부 업데이트 실패: characterId={}", characterId, e);
            return Map.of("success", false, "message", "업데이트에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 나벨 적격성 업데이트 (매칭, 일반, 하드)
     */
    public void updateNabelEligibility(Character character) {
        if (character == null) return;
            // 매칭 나벨 적격성 (명성만 확인)
            boolean isMatchingEligible = character.getFame() != null && character.getFame() >= 47684L;
            character.setIsMatchingNabelEligible(isMatchingEligible);
            if (isMatchingEligible) {
            // 일반 나벨 적격성
            boolean isNormalEligible = isNormalNabelEligible(character);
            character.setIsNormalNabelEligible(isNormalEligible);
            
            if (isNormalEligible) {
                // 하드 나벨 적격성
                boolean isHardEligible = isHardNabelEligible(character);
                character.setIsHardNabelEligible(isHardEligible);
            } else {
                character.setIsHardNabelEligible(false);
            }
        } else {
            character.setIsNormalNabelEligible(false);
            character.setIsHardNabelEligible(false);
        }
    }

    public Map<String, Object> searchCharacter(String serverId, String characterName) {
        try {
            log.info("캐릭터 검색 시작: serverId={}, characterName={}", serverId, characterName);
            
            // 1. DFO API로 캐릭터 검색
            List<CharacterDto> searchResults = dfoApiService.searchCharacters(serverId, characterName, null, null, true, 10, "match");
            if (searchResults == null || searchResults.isEmpty()) {
                log.warn("DFO API 캐릭터 검색 실패: serverId={}, characterName={}", serverId, characterName);
                return Map.of("error", "캐릭터를 찾을 수 없습니다.");
            }
            
            // 2. 검색 결과에서 캐릭터 ID 추출
            String characterId = searchResults.get(0).getCharacterId();
            if (characterId == null) {
                log.warn("캐릭터 ID 추출 실패: searchResults={}", searchResults);
                return Map.of("error", "캐릭터 ID를 찾을 수 없습니다.");
            }
            
            // 3. DFO API로 캐릭터 상세 정보 조회 (여기서 adventureName 획득)
            CharacterDetailDto characterDetail = dfoApiService.getCharacterDetail(serverId, characterId);
            if (characterDetail == null) {
                log.warn("DFO API 캐릭터 상세 정보 조회 실패: serverId={}, characterId={}", serverId, characterId);
                return Map.of("error", "캐릭터 상세 정보를 가져올 수 없습니다.");
            }
            
            // 서버 정보가 'all'인 경우 캐릭터 상세 정보에서 실제 서버 정보로 업데이트
            if ("all".equals(serverId)) {
                String actualServerId = characterDetail.getServerId();
                if (actualServerId != null && !actualServerId.trim().isEmpty() && !"all".equals(actualServerId)) {
                    serverId = actualServerId;
                    log.info("서버 정보 업데이트: 'all' -> '{}'", serverId);
                } else {
                    log.warn("캐릭터 상세 정보에서도 실제 서버 정보를 찾을 수 없음. serverId를 'all'로 유지");
                }
            }
            
            // 4. 서버 정보 자동 저장
            saveServerToDB(serverId);
            
            // 5. 모험단 정보 자동 조회 및 저장
            String adventureName = characterDetail.getAdventureName();
            log.info("캐릭터 기본정보에서 추출된 모험단 정보: '{}'", adventureName);
            
            if (adventureName != null && !adventureName.trim().isEmpty() && !"null".equals(adventureName)) {
                saveAdventureToDB(adventureName, serverId);
                log.info("모험단 정보 자동 저장 완료: {}", adventureName);
            } else {
                log.warn("모험단 정보가 없거나 비어있음: adventureName='{}'", adventureName);
            }
            
            // 6. DFO API로 캐릭터 타임라인 정보 조회 (던전 클리어 상태)
            Object timelineInfo = dfoApiService.getCharacterTimeline(serverId, characterId);
            Map<String, Boolean> dungeonClearStatus = extractDungeonClearStatus(timelineInfo);
            
            // 7. 캐릭터 정보를 DB에 저장 또는 업데이트
            Character character = saveOrUpdateCharacter(serverId, characterId, characterName, characterDetail, dungeonClearStatus);
            
            // 7-1. 모험단 관계 설정 (adventureName이 유효한 경우에만)
            if (adventureName != null && !adventureName.trim().isEmpty() && !"null".equals(adventureName)) {
                setAdventureForCharacter(character, adventureName);
                // 모험단 관계 설정 후 다시 저장
                character = characterRepository.save(character);
                log.info("모험단 관계 설정 후 캐릭터 재저장 완료: {}", character.getCharacterName());
            }
            
            // 8. DTO로 변환하여 반환
            Map<String, Object> result = convertToDto(character);
            result.put("message", "캐릭터 검색 완료");
            
            log.info("캐릭터 검색 완료: characterId={}, characterName={}, adventureName={}", 
                characterId, characterName, adventureName);
            
            return result;
            
        } catch (Exception e) {
            log.error("캐릭터 검색 중 오류 발생: serverId={}, characterName={}, error={}", 
                serverId, characterName, e.getMessage(), e);
            return Map.of("error", "캐릭터 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    
    /**
     * 던전 클리어 상태 추출
     */
    private Map<String, Boolean> extractDungeonClearStatus(Object timelineInfo) {
        Map<String, Boolean> status = new HashMap<>();
        status.put("nabel", false);
        status.put("venus", false);
        status.put("fog", false);
        status.put("twilight", false);
        
        try {
            if (timelineInfo instanceof JsonNode) {
                JsonNode root = (JsonNode) timelineInfo;
                JsonNode timeline = root.path("timeline");
                if (timeline.isArray()) {
                    for (JsonNode item : timeline) {
                        String dungeonName = item.path("dungeonName").asText();
                        if (dungeonName.contains("나벨") || dungeonName.contains("Nabel")) {
                            status.put("nabel", true);
                        } else if (dungeonName.contains("베누스") || dungeonName.contains("Venus")) {
                            status.put("venus", true);
                        } else if (dungeonName.contains("여신전") || dungeonName.contains("Fog")) {
                            status.put("fog", true);
                        } else if (dungeonName.contains("황혼전") || dungeonName.contains("twilight")) {
                            status.put("twilight", true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("던전 클리어 상태 추출 실패: {}", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 캐릭터 정보를 DB에 저장 또는 업데이트
     */
    private Character saveOrUpdateCharacter(String serverId, String characterId, String characterName, 
                                         CharacterDetailDto characterDetail, Map<String, Boolean> dungeonClearStatus) {
        try {
            // 기존 캐릭터 조회
            Optional<Character> existingCharacter = characterRepository.findByCharacterId(characterId);
            Character character;
            
            if (existingCharacter.isPresent()) {
                character = existingCharacter.get();
                log.info("기존 캐릭터 업데이트: {}", characterName);
            } else {
                character = new Character(characterId, characterName, serverId);
                log.info("새 캐릭터 생성: {}", characterName);
            }
            
            // 캐릭터 정보 업데이트
            updateCharacterFromData(character, characterDetail);
            
            // 던전 클리어 상태 업데이트 (이내 황혼전 포함)
            character.updateDungeonClearStatusWithTwilight(
                dungeonClearStatus.get("nabel"),
                dungeonClearStatus.get("venus"),
                dungeonClearStatus.get("fog"),
                dungeonClearStatus.get("twilight")
            );
            
            // DB에 저장
            Character savedCharacter = characterRepository.save(character);
            log.info("캐릭터 저장 완료: {}", savedCharacter.getCharacterName());
            
            return savedCharacter;
            
        } catch (Exception e) {
            log.error("캐릭터 저장 실패: characterName={}, error={}", characterName, e.getMessage(), e);
            throw new RuntimeException("캐릭터 저장 실패", e);
        }
    }

    /**
     * Map<String, Object>를 CharacterDetailDto로 변환
     */
    private CharacterDetailDto convertMapToCharacterDetailDto(Map<String, Object> data) {
        CharacterDetailDto dto = new CharacterDetailDto();
        
        if (data.containsKey("serverId")) {
            dto.setServerId((String) data.get("serverId"));
        }
        if (data.containsKey("characterId")) {
            dto.setCharacterId((String) data.get("characterId"));
        }
        if (data.containsKey("characterName")) {
            dto.setCharacterName((String) data.get("characterName"));
        }
        if (data.containsKey("jobId")) {
            dto.setJobId((String) data.get("jobId"));
        }
        if (data.containsKey("jobGrowId")) {
            dto.setJobGrowId((String) data.get("jobGrowId"));
        }
        if (data.containsKey("jobName")) {
            // jobName을 jobGrowName으로 설정하여 실제 직업명 표시
            String jobName = (String) data.get("jobName");
            String jobGrowName = (String) data.get("jobGrowName");
            
            if (jobGrowName != null && !jobGrowName.trim().isEmpty()) {
                // jobGrowName에서 괄호와 특수문자 제거
                String cleanJobName = jobGrowName
                    .replaceAll("[()]", "") // 괄호 제거
                    .replaceAll("眞", "")   // 眞 제거
                    .replaceAll("\\s+", "") // 공백 제거
                    .trim();
                
                if (!cleanJobName.isEmpty()) {
                    dto.setJobName(cleanJobName);
                } else {
                    dto.setJobName(jobName);
                }
            } else {
                dto.setJobName(jobName);
            }
        }
        if (data.containsKey("jobGrowName")) {
            dto.setJobGrowName((String) data.get("jobGrowName"));
        }
        if (data.containsKey("level")) {
            Object levelObj = data.get("level");
            if (levelObj instanceof Number) {
                dto.setLevel(((Number) levelObj).intValue());
            }
        }
        if (data.containsKey("adventureName")) {
            dto.setAdventureName((String) data.get("adventureName"));
        }
        if (data.containsKey("fame")) {
            Object fameObj = data.get("fame");
            if (fameObj instanceof Number) {
                dto.setFame(((Number) fameObj).longValue());
            }
        }
        if (data.containsKey("buffPower")) {
            Object buffPowerObj = data.get("buffPower");
            if (buffPowerObj instanceof Number) {
                dto.setBuffPower(((Number) buffPowerObj).longValue());
            }
        }
        if (data.containsKey("totalDamage")) {
            Object totalDamageObj = data.get("totalDamage");
            if (totalDamageObj instanceof Number) {
                dto.setTotalDamage(((Number) totalDamageObj).longValue());
            }
        }
        if (data.containsKey("dungeonClearNabel")) {
            dto.setDungeonClearNabel((Boolean) data.get("dungeonClearNabel"));
        }
        if (data.containsKey("dungeonClearVenus")) {
            dto.setDungeonClearVenus((Boolean) data.get("dungeonClearVenus"));
        }
        if (data.containsKey("dungeonClearFog")) {
            dto.setDungeonClearFog((Boolean) data.get("dungeonClearFog"));
        }
        
        return dto;
    }

    /**
     * 모험단 정보를 DB에 자동 저장
     */

    /**
     * 던전별 안감 상태 업데이트
     */
    public Map<String, Object> updateDungeonExcludeStatus(String characterId, String dungeonType, Boolean isExcluded) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다: " + characterId);
            }
            
            Character character = characterOpt.get();
            
            // 던전 타입에 따라 안감 상태 업데이트
            switch (dungeonType.toLowerCase()) {
                case "nabel":
                    character.setIsExcludedNabel(isExcluded);
                    break;
                case "venus":
                    character.setIsExcludedVenus(isExcluded);
                    break;
                case "fog":
                    character.setIsExcludedFog(isExcluded);
                    break;
                default:
                    return createErrorResponse("잘못된 던전 타입: " + dungeonType);
            }
            
            characterRepository.save(character);
            log.info("던전 안감 상태 업데이트: characterId={}, dungeonType={}, isExcluded={}", 
                characterId, dungeonType, isExcluded);
            
            return createSuccessResponse(
                "던전 안감 상태가 업데이트되었습니다.",
                Map.of(
                    "characterId", characterId,
                    "dungeonType", dungeonType,
                    "isExcluded", isExcluded
                )
            );
            
        } catch (Exception e) {
            log.error("던전 안감 상태 업데이트 실패: characterId={}, dungeonType={}, error={}", 
                characterId, dungeonType, e.getMessage());
            return createErrorResponse("던전 안감 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 던전별 업둥 상태 업데이트
     */
    public Map<String, Object> updateDungeonSkipStatus(String characterId, String dungeonType, Boolean isSkip) {
        try {
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                return createErrorResponse("캐릭터를 찾을 수 없습니다: " + characterId);
            }
            
            Character character = characterOpt.get();
            
            // 던전 타입에 따라 업둥 상태 업데이트
            switch (dungeonType.toLowerCase()) {
                case "nabel":
                    character.setIsSkipNabel(isSkip);
                    break;
                case "venus":
                    character.setIsSkipVenus(isSkip);
                    break;
                case "fog":
                    character.setIsSkipFog(isSkip);
                    break;
                default:
                    return createErrorResponse("잘못된 던전 타입: " + dungeonType);
            }
            
            characterRepository.save(character);
            log.info("던전 업둥 상태 업데이트: characterId={}, dungeonType={}, isSkip={}", 
                characterId, dungeonType, isSkip);
            
            return createSuccessResponse(
                "던전 업둥 상태가 업데이트되었습니다.",
                Map.of(
                    "characterId", characterId,
                    "dungeonType", dungeonType,
                    "isSkip", isSkip
                )
            );
            
        } catch (Exception e) {
            log.error("던전 업둥 상태 업데이트 실패: characterId={}, dungeonType={}, error={}", 
                characterId, dungeonType, e.getMessage());
            return createErrorResponse("던전 업둥 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모험단 전체 캐릭터 최신화 (비동기 처리 + SSE 진행 상황 제공)
     */
    @Async
    public CompletableFuture<Map<String, Object>> refreshAdventureCharactersAsync(String adventureName) {
        try {
            log.info("=== 모험단 '{}' 전체 캐릭터 비동기 최신화 시작 ===", adventureName);
            
            // 해당 모험단의 캐릭터들 조회
            List<Character> characters = characterRepository.findByAdventure_AdventureName(adventureName);
            
            if (characters.isEmpty()) {
                return CompletableFuture.completedFuture(createErrorResponse("해당 모험단의 캐릭터를 찾을 수 없습니다."));
            }
            
            int totalCharacters = characters.size();
            int processedCount = 0;
            int successCount = 0;
            int failCount = 0;
            
            log.info("모험단 '{}'에서 {}명의 캐릭터를 찾았습니다.", adventureName, totalCharacters);
            
            // SSE를 통해 진행 상황 전송
            log.info("=== SSE refresh_start 이벤트 전송 시작 ===");
            Map<String, Object> startData = Map.of(
                "type", "refresh_start",
                "adventureName", adventureName,
                "totalCharacters", totalCharacters,
                "processedCount", 0,
                "successCount", 0,
                "failCount", 0
            );
            log.info("refresh_start 데이터: {}", startData);
            
            realtimeEventService.sendSystemNotification(
                String.format("'%s' 모험단 캐릭터 정보 업데이트를 시작합니다. (총 %d명)", adventureName, totalCharacters),
                startData
            );
            log.info("=== SSE refresh_start 이벤트 전송 완료 ===");
            
            // 각 캐릭터를 순차적으로 처리
            for (Character character : characters) {
                try {
                    log.info("캐릭터 '{}' 처리 중... ({}/{})", character.getCharacterName(), processedCount + 1, totalCharacters);
                    
                    // SSE 진행 상황 업데이트
                    realtimeEventService.sendSystemNotification(
                        String.format("'%s' 캐릭터 처리 중... (%d/%d)", character.getCharacterName(), processedCount + 1, totalCharacters),
                        Map.of(
                            "type", "refresh_progress",
                            "adventureName", adventureName,
                            "characterName", character.getCharacterName(),
                            "totalCharacters", totalCharacters,
                            "processedCount", processedCount + 1,
                            "successCount", successCount,
                            "failCount", failCount
                        )
                    );
                    
                    // 1. DFO API에서 최신 정보 조회
                    try {
                        Object characterDetail = dfoApiService.getCharacterDetail(character.getServerId(), character.getCharacterId());
                        if (characterDetail != null) {
                            updateCharacterFromDfoApi(character, characterDetail);
                            log.info("캐릭터 '{}' DFO API 정보 업데이트 완료", character.getCharacterName());
                        }
                    } catch (Exception e) {
                        log.warn("캐릭터 '{}' DFO API 정보 업데이트 실패: {}", character.getCharacterName(), e.getMessage());
                    }
                    
                    // 2. Dundam 정보 업데이트
                    try {
                        updateDundamInfo(character);
                        log.info("캐릭터 '{}' Dundam 정보 업데이트 완료", character.getCharacterName());
                    } catch (Exception e) {
                        log.warn("캐릭터 '{}' Dundam 정보 업데이트 실패: {}", character.getCharacterName(), e.getMessage());
                    }
                    
                    // 3. DB에 저장
                    characterRepository.save(character);
                    successCount++;
                    log.info("캐릭터 '{}' 처리 완료 (성공)", character.getCharacterName());
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("캐릭터 '{}' 처리 실패: {}", character.getCharacterName(), e.getMessage());
                }
                
                processedCount++;
                
                // 2초 대기 (API 제한 고려)
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 최종 결과를 SSE로 전송
            log.info("=== SSE refresh_complete 이벤트 전송 시작 ===");
            Map<String, Object> finalResult = Map.of(
                "type", "refresh_complete",
                "adventureName", adventureName,
                "totalCharacters", totalCharacters,
                "processedCount", processedCount,
                "successCount", successCount,
                "failCount", failCount
            );
            log.info("refresh_complete 데이터: {}", finalResult);
            
            realtimeEventService.sendSystemNotification(
                String.format("'%s' 모험단 캐릭터 정보 업데이트가 완료되었습니다. (성공: %d, 실패: %d)", 
                    adventureName, successCount, failCount),
                finalResult
            );
            log.info("=== SSE refresh_complete 이벤트 전송 완료 ===");
            
            log.info("=== 모험단 '{}' 전체 캐릭터 비동기 최신화 완료 ===", adventureName);
            log.info("총 처리: {}명, 성공: {}명, 실패: {}명", processedCount, successCount, failCount);
            
            return CompletableFuture.completedFuture(createSuccessResponse(
                "모험단 전체 최신화가 완료되었습니다.",
                finalResult
            ));
            
        } catch (Exception e) {
            log.error("모험단 '{}' 전체 캐릭터 비동기 최신화 중 오류 발생: {}", adventureName, e.getMessage(), e);
            
            // 오류 발생 시에도 SSE로 알림
            realtimeEventService.sendSystemNotification(
                String.format("'%s' 모험단 캐릭터 정보 업데이트 중 오류가 발생했습니다.", adventureName),
                Map.of(
                    "type", "refresh_error",
                    "adventureName", adventureName,
                    "error", e.getMessage()
                )
            );
            
            return CompletableFuture.completedFuture(createErrorResponse("모험단 전체 최신화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 던담에서 가져온 정보로 캐릭터 업데이트
     */
    public Map<String, Object> updateCharacterFromDundam(String serverId, String characterId, Map<String, Object> dundamInfo) {
        try {
            log.info("=== 던담 정보로 캐릭터 업데이트 시작 ===");
            log.info("서버: {}, 캐릭터 ID: {}, 던담 정보: {}", serverId, characterId, dundamInfo);
            
            // 캐릭터 ID로 DB에서 캐릭터 찾기
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                log.warn("캐릭터를 찾을 수 없음: characterId={}", characterId);
                return Map.of(
                    "success", false,
                    "message", "캐릭터를 찾을 수 없습니다."
                );
            }
            
            Character character = characterOpt.get();
            boolean updated = false;
            
            // 총딜 업데이트
            if (dundamInfo.containsKey("totalDamage")) {
                Long totalDamage = (Long) dundamInfo.get("totalDamage");
                if (totalDamage != null && totalDamage > 0) {
                    character.setTotalDamage(totalDamage);
                    updated = true;
                    log.info("총딜 업데이트: {} -> {}", character.getTotalDamage(), totalDamage);
                }
            }
            
            // 버프력 업데이트
            if (dundamInfo.containsKey("buffPower")) {
                Long buffPower = (Long) dundamInfo.get("buffPower");
                if (buffPower != null && buffPower > 0) {
                    character.setBuffPower(buffPower);
                    updated = true;
                    log.info("버프력 업데이트: {} -> {}", character.getBuffPower(), buffPower);
                }
            }
            
            // 레벨 업데이트
            if (dundamInfo.containsKey("level")) {
                Object levelObj = dundamInfo.get("level");
                if (levelObj != null) {
                    Integer level = null;
                    if (levelObj instanceof Integer) {
                        level = (Integer) levelObj;
                    } else if (levelObj instanceof Long) {
                        level = ((Long) levelObj).intValue();
                    }
                    if (level != null && level > 0) {
                        character.setLevel(level);
                        updated = true;
                        log.info("레벨 업데이트: {} -> {}", character.getLevel(), level);
                    }
                }
            }
            
            // 명성 업데이트
            if (dundamInfo.containsKey("fame")) {
                Object fameObj = dundamInfo.get("fame");
                if (fameObj != null) {
                    Long fame = null;
                    if (fameObj instanceof Integer) {
                        fame = ((Integer) fameObj).longValue();
                    } else if (fameObj instanceof Long) {
                        fame = (Long) fameObj;
                    }
                    if (fame != null && fame > 0) {
                        character.setFame(fame);
                        updated = true;
                        log.info("명성 업데이트: {} -> {}", character.getFame(), fame);
                    }
                }
            }
            
            // 던담에서 가져온 총딜 값 처리 (사용자 수정된 로직 사용)
            if (dundamInfo.containsKey("totalDamage")) {
                Long totalDamage = (Long) dundamInfo.get("totalDamage");
                if (totalDamage != null && totalDamage > 0) {
                    character.setTotalDamage(totalDamage);
                    updated = true;
                    log.info("총딜 업데이트: {} -> {}", character.getTotalDamage(), totalDamage);
                }
            }
            
            if (updated) {
                // 던전 자격 자동 업데이트 (총딜/버프력 변경 후)
                boolean isHardEligible = isHardNabelEligible(character);
                boolean isNormalEligible = isNormalNabelEligible(character);
                boolean isMatchingEligible = isMatchingNabelEligible(character);
                boolean isTwilightEligible = isTwilightEligible(character);
                boolean isVenusEligible = isVenusEligible(character);
                boolean isFogEligible = isFogEligible(character);
                
                character.setIsHardNabelEligible(isHardEligible);
                character.setIsNormalNabelEligible(isNormalEligible);
                character.setIsMatchingNabelEligible(isMatchingEligible);
                character.setIsTwilightEligible(isTwilightEligible);
                character.setIsVenusEligible(isVenusEligible);
                character.setIsFogEligible(isFogEligible);
                
                log.info("던전 자격 업데이트 완료: 나벨(하드={}, 일반={}, 매칭={}), 황혼전={}, 베누스={}, 안개신={}", 
                    isHardEligible, isNormalEligible, isMatchingEligible, isTwilightEligible, isVenusEligible, isFogEligible);
                
                character.setLastStatsUpdate(LocalDateTime.now());
                characterRepository.save(character);
                log.info("캐릭터 정보 업데이트 완료");
                
                return Map.of(
                    "success", true,
                    "message", "캐릭터 정보가 성공적으로 업데이트되었습니다.",
                    "characterName", character.getCharacterName(),
                    "updatedFields", dundamInfo.keySet(),
                    "nabelEligibility", Map.of(
                        "isHardNabelEligible", character.getIsHardNabelEligible(),
                        "isNormalNabelEligible", character.getIsNormalNabelEligible(),
                        "isMatchingNabelEligible", character.getIsMatchingNabelEligible()
                    )
                );
            } else {
                log.info("업데이트할 정보가 없음");
                return Map.of(
                    "success", true,
                    "message", "업데이트할 정보가 없습니다.",
                    "characterName", character.getCharacterName()
                );
            }
            
        } catch (Exception e) {
            log.error("던담 정보로 캐릭터 업데이트 실패: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "캐릭터 업데이트 중 오류가 발생했습니다."
            );
        }
    }
    
    /**
     * 모험단 전체 캐릭터 던담 동기화 (셀레니움 - 비활성화됨)
     */
    public Map<String, Object> syncAdventureFromDundam(String adventureName) {
        log.warn("셀레니움 모험단 동기화는 K8s 환경에서 실패하여 비활성화되었습니다. Playwright 동기화를 사용하세요.");
        
        return Map.of(
            "success", false,
            "message", "셀레니움 모험단 동기화는 K8s 환경에서 실패하여 비활성화되었습니다. Playwright 동기화를 사용하세요.",
            "seleniumDisabled", true,
            "reason", "selenium_failed_in_k8s"
        );
    }

    /**
     * 모험단 전체 캐릭터 던담 동기화 (Playwright)
     */
    public Map<String, Object> syncAdventureFromDundamPlaywright(String adventureName) {
        try {
            log.info("=== Playwright 모험단 던담 동기화 시작 ===");
            log.info("모험단: {}", adventureName);
            
            // 모험단 찾기
            Optional<Adventure> adventureOpt = adventureRepository.findByAdventureName(adventureName);
            if (adventureOpt.isEmpty()) {
                log.warn("모험단을 찾을 수 없음: {}", adventureName);
                return Map.of(
                    "success", false,
                    "message", "모험단을 찾을 수 없습니다."
                );
            }
            
            Adventure adventure = adventureOpt.get();
            
            // 모험단의 모든 캐릭터 조회
            List<Character> characters = characterRepository.findByAdventure_AdventureName(adventureName);
            log.info("모험단 '{}'의 캐릭터 수: {}개", adventureName, characters.size());
            
            int successCount = 0;
            int failCount = 0;
            List<String> successList = new ArrayList<>();
            List<String> failList = new ArrayList<>();
            
            for (Character character : characters) {
                try {
                    // Playwright로 던담에서 캐릭터 정보 가져오기
                    Map<String, Object> dundamResult = dundamService.getCharacterInfoWithPlaywright(
                        character.getServerId(), 
                        character.getCharacterId()
                    );
                    
                    if ((Boolean) dundamResult.get("success")) {
                        Map<String, Object> characterInfo = (Map<String, Object>) dundamResult.get("characterInfo");
                        Map<String, Object> updateResult = updateCharacterFromDundam(
                            character.getServerId(), 
                            character.getCharacterId(), 
                            characterInfo
                        );
                        
                        if ((Boolean) updateResult.get("success")) {
                            successCount++;
                            successList.add(character.getCharacterName());
                            log.info("캐릭터 '{}' Playwright 던담 동기화 성공", character.getCharacterName());
                        } else {
                            failCount++;
                            failList.add(character.getCharacterName() + " (업데이트 실패)");
                            log.warn("캐릭터 '{}' 업데이트 실패: {}", 
                                character.getCharacterName(), updateResult.get("message"));
                        }
                    } else {
                        failCount++;
                        failList.add(character.getCharacterName() + " (Playwright 던담 크롤링 실패)");
                        log.warn("캐릭터 '{}' Playwright 던담 크롤링 실패: {}", 
                            character.getCharacterName(), dundamResult.get("message"));
                    }
                    
                    // API 호출 간격 조절 (서버 부하 방지)
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    failCount++;
                    failList.add(character.getCharacterName() + " (오류: " + e.getMessage() + ")");
                    log.error("캐릭터 '{}' Playwright 던담 동기화 중 오류: {}", character.getCharacterName(), e.getMessage());
                }
            }
            
            log.info("=== Playwright 모험단 던담 동기화 완료 ===");
            log.info("성공: {}개, 실패: {}개", successCount, failCount);
            
            return Map.of(
                "success", true,
                "message", String.format("Playwright 모험단 '%s' 동기화 완료 (성공: %d개, 실패: %d개)", 
                    adventureName, successCount, failCount),
                "adventureName", adventureName,
                "totalCharacters", characters.size(),
                "successCount", successCount,
                "failCount", failCount,
                "successList", successList,
                "failList", failList
            );
            
        } catch (Exception e) {
            log.error("Playwright 모험단 던담 동기화 실패: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Playwright 모험단 던담 동기화 중 오류가 발생했습니다."
            );
        }
    }

    /**
     * 메서드에 따라 모험단 전체 캐릭터 던담 동기화 (통합 API용)
     */
    public Map<String, Object> syncAdventureFromDundamWithMethod(String adventureName, String method) {
        log.info("=== 모험단 던담 동기화 시작 (메서드: {}) ===", method);
        log.info("모험단: {}", adventureName);
        
        if ("selenium".equalsIgnoreCase(method)) {
            // 셀레니움은 K8s 환경에서 실패하여 비활성화됨
            log.warn("셀레니움 모험단 동기화는 K8s 환경에서 실패하여 비활성화되었습니다.");
            return Map.of(
                "success", false,
                "message", "셀레니움 모험단 동기화는 K8s 환경에서 실패하여 비활성화되었습니다. Playwright 동기화를 사용하세요.",
                "seleniumDisabled", true,
                "reason", "selenium_failed_in_k8s"
            );
        } else if ("playwright".equalsIgnoreCase(method)) {
            // Playwright 사용
            return syncAdventureFromDundamPlaywright(adventureName);
        } else {
            // 잘못된 메서드
            log.error("잘못된 동기화 메서드: {}", method);
            return Map.of(
                "success", false,
                "message", "잘못된 동기화 메서드입니다. 'selenium' 또는 'playwright'를 지정해주세요.",
                "invalidMethod", true,
                "method", method
            );
        }
    }
}

