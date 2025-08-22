package com.dfparty.backend.service;

import com.dfparty.backend.dto.ServerDto;
import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.dto.CharacterDetailDto;
import com.dfparty.backend.service.ThursdayFallbackService;
import com.dfparty.backend.entity.Server;
import com.dfparty.backend.repository.ServerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import com.dfparty.backend.service.PlaywrightCrawlingService;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class DfoApiService {

    @Value("${df.api.base-url:https://api.neople.co.kr/df}")
    private String baseUrl;

    @Value("${df.api.key:}")
    private String apiKey;

    @PostConstruct
    public void logApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) {
            // API 키의 첫 8자리만 로그에 출력 (보안상)
            String maskedKey = apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : apiKey;
            System.out.println("=== DFO API Key Loaded ===");
            System.out.println("API Key: " + maskedKey);
            System.out.println("Base URL: " + baseUrl);
            System.out.println("==========================");
        } else {
            System.err.println("=== WARNING: DFO API Key is not set! ===");
            System.err.println("Please check your config.env file or environment variables");
            System.err.println("==========================================");
        }
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ThursdayFallbackService thursdayFallbackService;
    private final ServerRepository serverRepository;

    /**
     * 가장 가까운 목요일 오전 9시를 기준으로 시작날짜 계산 (DFO API 형식)
     */
    private String calculateStartDate() {
        LocalDate today = LocalDate.now();
        LocalDate currentThursday = today;
        
        // 오늘이 목요일이 아니면 이번 주 목요일 찾기
        while (currentThursday.getDayOfWeek() != DayOfWeek.THURSDAY) {
            currentThursday = currentThursday.minusDays(1);
        }
        
        // 목요일 오전 9시 기준으로 설정
        LocalDateTime thursday9AM = currentThursday.atTime(9, 0);
        
        // 현재 시간이 목요일 오전 9시보다 이전이면 이전 주 목요일 사용
        if (LocalDateTime.now().isBefore(thursday9AM)) {
            currentThursday = currentThursday.minusWeeks(1);
        }
        
        // DFO API 권장 형식: YYYYMMDDTHHMM (예: 202508140900)
        return thursday9AM.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    }

    /**
     * 현재 시간을 기준으로 종료날짜 계산 (DFO API 형식)
     */
    private String calculateEndDate() {
        // 현재 시간을 KST(한국 표준시) 기준으로 계산
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        LocalDateTime nowKST = LocalDateTime.now(kstZone);
        
        // DFO API 권장 형식으로 반환: YYYYMMDDTHHMM
        return nowKST.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    }

    /**
     * 시작날짜와 종료날짜 간의 기간이 90일 이내인지 검증
     */
    private boolean isValidDateRange(String startDate, String endDate) {
        try {
            // DFO API 형식 파싱: YYYYMMDDTHHMM
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            
            // 90일 이내인지 확인
            long daysBetween = java.time.Duration.between(start, end).toDays();
            boolean isValid = daysBetween <= 90;
            
            if (!isValid) {
                System.out.println("⚠️ 경고: 검색 기간이 90일을 초과합니다. (시작: " + startDate + ", 종료: " + endDate + ", 기간: " + daysBetween + "일)");
            }
            
            return isValid;
        } catch (Exception e) {
            System.err.println("❌ 날짜 형식 검증 실패: " + e.getMessage());
            return false;
        }
    }

    public List<ServerDto> getServers() throws Exception {
        log.info("=== 서버 목록 조회 시작 ===");
        
        // 1차: DB에서 서버 목록 조회 시도
        try {
            log.info("1차: DB에서 서버 목록 조회 시도");
            List<ServerDto> dbServers = serverRepository.findAll().stream()
                .map(server -> new ServerDto(server.getServerId(), server.getServerName()))
                .collect(Collectors.toList());
            
            if (!dbServers.isEmpty()) {
                log.info("DB에서 서버 목록 조회 성공: {}개 서버", dbServers.size());
                return dbServers;
            } else {
                log.info("DB에 서버 데이터가 없음, API 호출로 진행");
            }
        } catch (Exception e) {
            log.warn("DB 조회 실패, API 호출로 진행: {}", e.getMessage());
        }
        
        // 2차: 목요일 API 제한 체크
        log.info("2차: 목요일 API 제한 체크");
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("서버 목록 조회");
        if (restriction != null) {
            log.error("목요일 API 제한으로 인해 서버 목록 조회 불가: {}", restriction.get("message"));
            throw new RuntimeException("목요일 API 제한: " + restriction.get("message"));
        }
        
        // 3차: DFO API 호출
        log.info("3차: DFO API 호출 시작");
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers")
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        log.info("API URL: {}", url);
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("API 응답 상태: {}", response.getStatusCode());
        
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode rows = root.path("rows");

        List<ServerDto> servers = new ArrayList<>();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                ServerDto server = new ServerDto(
                    row.path("serverId").asText(),
                    row.path("serverName").asText()
                );
                servers.add(server);
            }
        }

        log.info("API에서 서버 목록 조회 성공: {}개 서버", servers.size());
        
        // 4차: DB에 서버 정보 저장
        try {
            log.info("4차: DB에 서버 정보 저장 시도");
            for (ServerDto server : servers) {
                Server serverEntity = new Server();
                serverEntity.setServerId(server.getServerId());
                serverEntity.setServerName(server.getServerName());
                serverRepository.save(serverEntity);
            }
            log.info("DB에 서버 정보 저장 완료");
        } catch (Exception e) {
            log.warn("DB 저장 실패 (무시하고 진행): {}", e.getMessage());
        }
        
        log.info("=== 서버 목록 조회 완료 ===");
        return servers;
    }

    public List<CharacterDto> searchCharacters(String serverId, String characterName, 
                                            String jobId, String jobGrowId, 
                                            boolean isAllJobGrow, int limit, String wordType) throws Exception {
        log.info("=== 캐릭터 검색 시작 ===");
        log.info("검색 파라미터: serverId={}, characterName={}, jobId={}, jobGrowId={}, isAllJobGrow={}, limit={}, wordType={}", 
                serverId, characterName, jobId, jobGrowId, isAllJobGrow, limit, wordType);
        
        try {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters")
                .queryParam("characterName", characterName)
                .queryParam("isAllJobGrow", isAllJobGrow)
                .queryParam("limit", limit)
                .queryParam("wordType", wordType)
                .queryParam("apikey", apiKey);

        if (jobId != null && !jobId.isEmpty()) {
            builder.queryParam("jobId", jobId);
        }
        if (jobGrowId != null && !jobGrowId.isEmpty()) {
            builder.queryParam("jobGrowId", jobGrowId);
        }

        String url = builder.build().toUriString();
            log.info("DFO API URL: {}", url);
            log.info("API 호출 시작: serverId={}, characterName={}", serverId, characterName);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("API 응답 상태: {}", response.getStatusCode());
            log.debug("API 응답 본문: {}", response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode rows = root.path("rows");

        List<CharacterDto> characters = new ArrayList<>();
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                CharacterDto character = new CharacterDto(
                    serverId,
                    row.path("characterId").asText(),
                    row.path("characterName").asText(),
                    row.path("jobId").asText(),
                    row.path("jobGrowId").asText(),
                    row.path("jobName").asText(),
                    row.path("jobGrowName").asText(),
                    row.path("level").asInt(),
                    row.path("adventureName").asText(),
                    row.path("fame").asLong()
                );
                characters.add(character);
            }
        }

            log.info("캐릭터 검색 완료: {}개 캐릭터 발견", characters.size());
            log.debug("검색된 캐릭터들: {}", characters.stream().map(CharacterDto::getCharacterName).collect(Collectors.toList()));

        return characters;
            
        } catch (Exception e) {
            System.err.println("=== DFO API 검색 에러 ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            System.err.println("Stack Trace: " + e.getStackTrace()[0]);
            System.err.println("Full Stack Trace:");
            e.printStackTrace();
            System.err.println("=======================");
            throw e;
        }
    }

    public CharacterDetailDto getCharacterDetail(String serverId, String characterId) throws Exception {
        log.info("=== 캐릭터 상세 정보 조회 시작 ===");
        log.info("조회 파라미터: serverId={}, characterId={}", serverId, characterId);
        
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters/" + characterId)
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();
        
        log.info("DFO API URL: {}", url);
        log.info("API 호출 시작: serverId={}, characterId={}", serverId, characterId);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        log.info("API 응답 상태: {}", response.getStatusCode());
        log.debug("API 응답 본문: {}", response.getBody());
        
        JsonNode root = objectMapper.readTree(response.getBody());

        String adventureName = root.path("adventureName").asText();
        log.info("=== DFO API에서 추출된 모험단 정보 ===");
        log.info("Character Name: {}", root.path("characterName").asText());
        log.info("Adventure Name: '{}'", adventureName);
        log.info("Adventure Name isEmpty: {}", adventureName.isEmpty());
        log.info("Adventure Name isNull: {}", (adventureName == null));

        CharacterDetailDto characterDetail = new CharacterDetailDto(
            serverId,
            characterId,
            root.path("characterName").asText(),
            root.path("jobId").asText(),
            root.path("jobGrowId").asText(),
            root.path("jobGrowName").asText(), // jobName 대신 jobGrowName 사용
            root.path("jobGrowName").asText(),
            root.path("level").asInt(),
            adventureName,
            root.path("fame").asLong()
        );

        // 추가 정보 설정
        if (root.has("status")) {
            characterDetail.setStatus(objectMapper.convertValue(root.get("status"), Map.class));
        }
        if (root.has("equipment")) {
            characterDetail.setEquipment(objectMapper.convertValue(root.get("equipment"), List.class));
        }
        if (root.has("buffSkill")) {
            characterDetail.setBuffSkill(objectMapper.convertValue(root.get("buffSkill"), Map.class));
        }
        
        // 캐릭터 이미지 URL 추출
        String characterImageUrl = extractCharacterImageUrl(root);
        String avatarImageUrl = extractAvatarImageUrl(root);
        
        characterDetail.setCharacterImageUrl(characterImageUrl);
        characterDetail.setAvatarImageUrl(avatarImageUrl);
        
        log.info("=== 추출된 이미지 URL ===");
        log.info("Character Image URL: {}", characterImageUrl);
        log.info("Avatar Image URL: {}", avatarImageUrl);
        
        log.info("=== 캐릭터 상세 정보 조회 완료 ===");
        return characterDetail;
    }

    /**
     * 캐릭터 타임라인 조회 (던전 클리어 현황 확인용)
     */
    public Map<String, Object> getCharacterTimeline(String serverId, String characterId) throws Exception {
        try {
            System.out.println("=== 타임라인 API 호출 시작 ===");
            System.out.println("서버 ID: " + serverId);
            System.out.println("캐릭터 ID: " + characterId);
            
            // 목요일 API 제한 체크
            System.out.println("1단계: 목요일 API 제한 체크 시작");
            Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("캐릭터 타임라인 조회");
            if (restriction != null) {
                System.out.println("❌ 목요일 API 제한 발생: " + restriction.get("message"));
                throw new RuntimeException("목요일 API 제한: " + restriction.get("message"));
            }
            System.out.println("✅ 목요일 API 제한 체크 통과");
            

            
            // 실제 API 호출 - URL 수정: /servers/ 추가, 시작날짜와 종료날짜 포함
            System.out.println("3단계: DFO API URL 구성");
            String startDate = calculateStartDate();
            String endDate = calculateEndDate();
            System.out.println("시작날짜: " + startDate + " (가장 가까운 목요일 오전 9시 기준)");
            System.out.println("종료날짜: " + endDate + " (현재 시간 기준)");
            
            // 날짜 범위 검증 (90일 이내)
            if (!isValidDateRange(startDate, endDate)) {
                System.out.println("⚠️ 경고: 검색 기간이 90일을 초과하여 API 호출을 진행합니다.");
            }
            
            String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters/" + characterId + "/timeline")
                    .queryParam("apikey", apiKey)
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .queryParam("limit", 100) // 최대 100개 결과
                    .build()
                    .toUriString();

            System.out.println("=== DFO 타임라인 API 호출 ===");
            System.out.println("URL: " + url);
            System.out.println("===========================");

            System.out.println("4단계: DFO API 실제 호출 시작");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            System.out.println("=== DFO 타임라인 API 응답 ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("===========================");
            

            
            // 타임라인 데이터 파싱하여 던전 클리어 현황 추출
            System.out.println("6단계: 타임라인 데이터 파싱 시작");
            Map<String, Object> result = parseTimelineForDungeonStatus(response.getBody());
            System.out.println("✅ 타임라인 파싱 완료: " + result);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 타임라인 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 오류 발생 시 기본 응답 반환
            Map<String, Object> errorResult = new HashMap<>();
            Map<String, Boolean> dungeonStatus = new HashMap<>();
            dungeonStatus.put("nabel", false);
            dungeonStatus.put("venus", false);
            dungeonStatus.put("fog", false);
            dungeonStatus.put("twilight", false);
            
            errorResult.put("success", false);
            errorResult.put("dungeonStatus", dungeonStatus);
            errorResult.put("source", "Error: " + e.getMessage());
            errorResult.put("error", e.getMessage());
            
            return errorResult;
        }
    }
    
    /**
     * 타임라인 데이터에서 던전 클리어 현황 파싱
     */
    private Map<String, Object> parseTimelineForDungeonStatus(String timelineJson) throws Exception {
        System.out.println("=== 타임라인 파싱 시작 ===");
        System.out.println("입력 JSON 길이: " + (timelineJson != null ? timelineJson.length() : "null"));
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Boolean> dungeonStatus = new HashMap<>();
        
        try {
            System.out.println("1단계: JSON 데이터 유효성 검사");
            if (timelineJson == null || timelineJson.trim().isEmpty()) {
                System.out.println("❌ 타임라인 데이터가 비어있습니다.");
                throw new Exception("타임라인 데이터가 비어있습니다.");
            }
            System.out.println("✅ JSON 데이터 유효성 검사 통과");
            
            System.out.println("2단계: JSON 파싱 시작");
            JsonNode root = objectMapper.readTree(timelineJson);
            System.out.println("✅ JSON 파싱 완료");
            
            // 기본값 설정 (클리어되지 않은 던전은 false)
            System.out.println("3단계: 기본 던전 상태 설정");
            dungeonStatus.put("nabel", false);
            dungeonStatus.put("venus", false);
            dungeonStatus.put("fog", false);
            dungeonStatus.put("twilight", false);
            System.out.println("✅ 기본 던전 상태 설정 완료: " + dungeonStatus);
            
            // 타임라인 데이터가 있는지 확인
            System.out.println("4단계: 타임라인 구조 확인");
            if (!root.has("timeline")) {
                System.out.println("❌ 타임라인 데이터가 없습니다. 기본값 반환.");
                result.put("success", true);
                result.put("dungeonStatus", dungeonStatus);
                result.put("source", "DFO API Timeline (No Data)");
                return result;
            }
            System.out.println("✅ 타임라인 구조 확인 완료");
            
            JsonNode timeline = root.path("timeline");
            System.out.println("5단계: timeline 노드 분석");
            System.out.println("timeline 노드 타입: " + timeline.getNodeType());
            System.out.println("timeline이 배열인가: " + timeline.isArray());
            
            if (timeline.isArray()) {
                System.out.println("timeline 배열 크기: " + timeline.size());
                for (JsonNode event : timeline) {
                    try {
                        String date = event.path("date").asText();
                        JsonNode data = event.path("data");
                        
                        // 던전 클리어 이벤트 확인
                        if (data.has("dungeon")) {
                            String dungeonName = data.path("dungeon").path("name").asText();
                            String dungeonType = data.path("dungeon").path("type").asText();
                            
                            System.out.println("던전 이벤트 발견: " + dungeonName + " (" + dungeonType + ")");
                            
                            // 던전 타입별로 클리어 상태 설정
                            if (dungeonName.contains("나벨") || dungeonType.contains("nabel")) {
                                dungeonStatus.put("nabel", true);
                            } else if (dungeonName.contains("베누스") || dungeonType.contains("venus")) {
                                dungeonStatus.put("venus", true);
                            } else if (dungeonName.contains("안개신") || dungeonType.contains("fog")) {
                                dungeonStatus.put("fog", true);
                            } else if (dungeonName.contains("황혼전") || dungeonType.contains("twilight")) {
                                dungeonStatus.put("twilight", true);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("개별 이벤트 파싱 오류: " + e.getMessage());
                        // 개별 이벤트 오류는 무시하고 계속 진행
                        continue;
                    }
                }
            }
            
            // rows 배열에서 던전 클리어 이벤트 확인 (실제 DFO API 구조)
            System.out.println("6단계: rows 배열 분석 시작");
                    
            // 올바른 경로: timeline.rows
            if (timeline.has("rows")) {
                JsonNode rows = timeline.path("rows");
                System.out.println("rows 노드 타입: " + rows.getNodeType());
                System.out.println("rows가 배열인가: " + rows.isArray());
                
                if (rows.isArray()) {
                    System.out.println("rows 배열 크기: " + rows.size());
                    int processedRows = 0;
                    
                    for (JsonNode row : rows) {
                        try {
                            processedRows++;
                            int code = row.path("code").asInt();
                            String name = row.path("name").asText();
                            String date = row.path("date").asText();
                            JsonNode data = row.path("data");
                            
                            System.out.println("Row " + processedRows + " 분석: code=" + code + ", name=" + name + ", date=" + date);
                            
                            // 레기온 클리어 이벤트 (code: 209)
                            if (code == 209 && data.has("regionName")) {
                                String regionName = data.path("regionName").asText();
                                System.out.println("🎯 레기온 클리어 발견: " + regionName);
                                
                                // 지역명에 따른 던전 클리어 상태 설정
                                if (regionName.contains("나벨") || regionName.equals("nabel")) {
                                    dungeonStatus.put("nabel", true);
                                    System.out.println("✅ 나벨 클리어 상태 설정됨");
                                } else if (regionName.contains("베누스") || regionName.equals("venus")) {
                                    dungeonStatus.put("venus", true);
                                    System.out.println("✅ 베누스 클리어 상태 설정됨");
                                } else if (regionName.contains("안개신") || regionName.equals("fog")) {
                                    dungeonStatus.put("fog", true);
                                    System.out.println("✅ 안개신 클리어 상태 설정됨");
                                } else if (regionName.contains("황혼전") || regionName.equals("twilight")) {
                                    dungeonStatus.put("twilight", true);
                                    System.out.println("✅ 이내 황혼전 클리어 상태 설정됨");
                                }
                            }
                            
                            // 레이드 클리어 이벤트 (code: 201) - 나벨, 안개신 등
                            if (code == 201 && data.has("raidName")) {
                                String raidName = data.path("raidName").asText();
                                System.out.println("🎯 레이드 클리어 발견: " + raidName);
                                
                                // 레이드명에 따른 던전 클리어 상태 설정
                                if (raidName.contains("나벨") || raidName.contains("nabel")) {
                                    dungeonStatus.put("nabel", true);
                                    System.out.println("✅ 나벨 클리어 상태 설정됨 (레이드)");
                                } else if (raidName.contains("베누스") || raidName.contains("venus")) {
                                    dungeonStatus.put("venus", true);
                                    System.out.println("✅ 베누스 클리어 상태 설정됨 (레이드)");
                                } else if (raidName.contains("안개신") || raidName.contains("fog") || raidName.contains("아스라한")) {
                                    dungeonStatus.put("fog", true);
                                    System.out.println("✅ 안개신 클리어 상태 설정됨 (레이드)");
                                } else if (raidName.contains("황혼전") || raidName.contains("twilight")) {
                                    dungeonStatus.put("twilight", true);
                                    System.out.println("✅ 이내 황혼전 클리어 상태 설정됨 (레이드)");
                                }
                            }
                            
                            // 던전 클리어 이벤트 (dungeonName)
                            if (data.has("dungeonName")) {
                                String dungeonName = data.path("dungeonName").asText();
                                System.out.println("🎯 던전 이벤트 발견: " + dungeonName);
                                
                                // 던전명에 따른 클리어 상태 설정
                                if (dungeonName.contains("나벨") || dungeonName.contains("nabel")) {
                                    dungeonStatus.put("nabel", true);
                                    System.out.println("✅ 나벨 클리어 상태 설정됨");
                                } else if (dungeonName.contains("베누스") || dungeonName.contains("venus")) {
                                    dungeonStatus.put("venus", true);
                                    System.out.println("✅ 베누스 클리어 상태 설정됨");
                                } else if (dungeonName.contains("안개신") || dungeonName.contains("fog")) {
                                    dungeonStatus.put("fog", true);
                                    System.out.println("✅ 안개신 클리어 상태 설정됨");
                                } else if (dungeonName.contains("황혼전") || dungeonName.contains("twilight")) {
                                    dungeonStatus.put("twilight", true);
                                    System.out.println("✅ 이내 황혼전 클리어 상태 설정됨");
                                }
                            }
                            
                        } catch (Exception e) {
                            System.err.println("개별 row 파싱 오류: " + e.getMessage());
                            continue;
                        }
                    }
                    System.out.println("✅ rows 배열 분석 완료: " + processedRows + "개 처리됨");
                }
            } else {
                System.out.println("❌ timeline.rows 노드가 없습니다.");
            }
            
            System.out.println("7단계: 최종 결과 구성");
            result.put("success", true);
            result.put("dungeonStatus", dungeonStatus);
            result.put("source", "DFO API Timeline");
            result.put("totalEvents", timeline.isArray() ? timeline.size() : 0);
            result.put("totalRows", root.has("rows") && root.path("rows").isArray() ? root.path("rows").size() : 0);
            
            System.out.println("=== 타임라인 파싱 완료 ===");
            System.out.println("최종 던전 상태: " + dungeonStatus);
            System.out.println("totalEvents: " + result.get("totalEvents"));
            System.out.println("totalRows: " + result.get("totalRows"));
            System.out.println("==========================");
            
        } catch (Exception e) {
            System.err.println("❌ 타임라인 파싱 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 파싱 실패 시 기본값 반환
            dungeonStatus.put("nabel", false);
            dungeonStatus.put("venus", false);
            dungeonStatus.put("fog", false);
            dungeonStatus.put("twilight", false);
            
            result.put("success", false);
            result.put("dungeonStatus", dungeonStatus);
            result.put("source", "DFO API Timeline (Parse Error)");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Mock 응답을 타임라인 데이터로 변환
     */
    private Map<String, Object> convertMockResponseToTimeline(ResponseEntity<?> mockResponse) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Boolean> dungeonStatus = new HashMap<>();
        
        // Mock 데이터에서 던전 클리어 현황 추출
        // 실제 구현에서는 Mock 응답의 구조에 따라 파싱
        dungeonStatus.put("nabel", false);
        dungeonStatus.put("venus", false);
        dungeonStatus.put("fog", false);
        dungeonStatus.put("twilight", false);
        
        result.put("success", true);
        result.put("dungeonStatus", dungeonStatus);
        result.put("source", "Mock Data");
        
        return result;
    }

    /**
     * DFO API 응답에서 캐릭터 이미지 URL 추출
     */
    private String extractCharacterImageUrl(JsonNode root) {
        try {
            // DFO API는 여러 가지 방식으로 이미지 URL을 제공할 수 있음
            
            // 1. 직접적인 캐릭터 이미지 URL 필드 확인
            if (root.has("characterImageUrl")) {
                return root.path("characterImageUrl").asText();
            }
            
            // 2. 이미지 정보 객체에서 확인
            if (root.has("image")) {
                JsonNode imageNode = root.path("image");
                if (imageNode.has("characterImage")) {
                    return imageNode.path("characterImage").asText();
                }
                if (imageNode.has("url")) {
                    return imageNode.path("url").asText();
                }
            }
            
            // 3. 장착 아바타 정보에서 추출
            if (root.has("avatar")) {
                JsonNode avatarNode = root.path("avatar");
                if (avatarNode.has("imageUrl")) {
                    return avatarNode.path("imageUrl").asText();
                }
            }
            
            System.out.println("캐릭터 이미지 URL을 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            System.err.println("캐릭터 이미지 URL 추출 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * DFO API 응답에서 아바타 이미지 URL 추출
     */
    private String extractAvatarImageUrl(JsonNode root) {
        try {
            // 1. 아바타 정보에서 확인
            if (root.has("avatar")) {
                JsonNode avatarNode = root.path("avatar");
                if (avatarNode.has("imageUrl")) {
                    return avatarNode.path("imageUrl").asText();
                }
                if (avatarNode.has("avatarImage")) {
                    return avatarNode.path("avatarImage").asText();
                }
            }
            
            // 2. 장착 정보에서 아바타 이미지 확인
            if (root.has("equipment")) {
                JsonNode equipmentNode = root.path("equipment");
                if (equipmentNode.isArray()) {
                    for (JsonNode item : equipmentNode) {
                        if (item.has("slotName") && "아바타".equals(item.path("slotName").asText())) {
                            if (item.has("imageUrl")) {
                                return item.path("imageUrl").asText();
                            }
                        }
                    }
                }
            }
            
            // 3. 대안: jobGrowId 기반으로 기본 직업 이미지 생성
            if (root.has("jobGrowId")) {
                String jobGrowId = root.path("jobGrowId").asText();
                return generateDefaultCharacterImageUrl(jobGrowId);
            }
            
            System.out.println("아바타 이미지 URL을 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            System.err.println("아바타 이미지 URL 추출 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 직업 ID 기반으로 기본 캐릭터 이미지 URL 생성
     */
    private String generateDefaultCharacterImageUrl(String jobGrowId) {
        // DFO 공식 이미지 URL 패턴 (예상)
        // 실제 DFO에서 제공하는 이미지 URL 패턴에 맞춰 수정 필요
        return String.format("https://img.neople.co.kr/img/df/portrait/%s.png", jobGrowId);
    }

    /**
     * 캐릭터 아바타 장착 정보 조회 (별도 API 엔드포인트)
     */
    public Object getCharacterAvatar(String serverId, String characterId) throws Exception {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters/" + characterId + "/equip/avatar")
                    .queryParam("apikey", apiKey)
                    .build()
                    .toUriString();

            System.out.println("=== DFO 아바타 API 호출 ===");
            System.out.println("URL: " + url);
            System.out.println("===========================");

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            System.out.println("=== DFO 아바타 API 응답 ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("===========================");
            
        return objectMapper.readTree(response.getBody());
            
        } catch (Exception e) {
            System.err.println("아바타 정보 조회 실패: " + e.getMessage());
            throw e;
        }
    }
    
    // Mock 응답을 ServerDto로 변환하는 메서드
    private List<ServerDto> convertMockResponseToServers(ResponseEntity<?> mockResponse) {
        try {
            // Mock 응답을 JSON으로 변환하여 파싱
            String responseBody = objectMapper.writeValueAsString(mockResponse.getBody());
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode rows = root.path("rows");
            
            List<ServerDto> servers = new ArrayList<>();
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    ServerDto server = new ServerDto(
                        row.path("serverId").asText(),
                        row.path("serverName").asText()
                    );
                    servers.add(server);
                }
            }
            return servers;
            
        } catch (Exception e) {
            // Mock 응답 변환 실패 시 빈 리스트 반환
            return new ArrayList<>();
        }
    }
}
