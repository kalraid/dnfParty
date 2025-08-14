package com.dfparty.backend.service;

import com.dfparty.backend.config.MockConfig;
import com.dfparty.backend.dto.ServerDto;
import com.dfparty.backend.dto.CharacterDto;
import com.dfparty.backend.dto.CharacterDetailDto;
import com.dfparty.backend.model.MockApiData;
import com.dfparty.backend.service.ThursdayFallbackService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DfoApiService {

    @Value("${df.api.base-url:https://api.neople.co.kr}")
    private String baseUrl;

    @Value("${df.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MockConfig mockConfig;
    private final MockDataService mockDataService;
    private final ThursdayFallbackService thursdayFallbackService;

    public List<ServerDto> getServers() throws Exception {
        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("서버 목록 조회");
        if (restriction != null) {
            throw new RuntimeException("목요일 API 제한: " + restriction.get("message"));
        }
        
        // Mock 모드 체크
        if (mockConfig.isEnabled()) {
            Optional<ResponseEntity<?>> mockResponse = mockDataService.getMockResponse(
                MockApiData.ApiType.DFO_SERVERS, 
                "/servers", 
                "GET"
            );
            
            if (mockResponse.isPresent()) {
                // Mock 응답을 ServerDto로 변환
                return convertMockResponseToServers(mockResponse.get());
            }
        }
        
        // 실제 API 호출
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers")
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Mock 데이터 저장
        Map<String, String> params = new HashMap<>();
        params.put("apikey", apiKey);
        mockDataService.saveMockData(
            MockApiData.ApiType.DFO_SERVERS,
            "/servers",
            "GET",
            params,
            null,
            response
        );
        
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

        return servers;
    }

    public List<CharacterDto> searchCharacters(String serverId, String characterName, 
                                            String jobId, String jobGrowId, 
                                            boolean isAllJobGrow, int limit, String wordType) throws Exception {
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
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
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

        return characters;
    }

    public CharacterDetailDto getCharacterDetail(String serverId, String characterId) throws Exception {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters/" + characterId)
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());

        CharacterDetailDto characterDetail = new CharacterDetailDto(
            serverId,
            characterId,
            root.path("characterName").asText(),
            root.path("jobId").asText(),
            root.path("jobGrowId").asText(),
            root.path("jobName").asText(),
            root.path("jobGrowName").asText(),
            root.path("level").asInt(),
            root.path("adventureName").asText(),
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

        return characterDetail;
    }

    public Object getCharacterTimeline(String serverId, String characterId, int limit, 
                                     String code, String startDate, String endDate, String next) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/servers/" + serverId + "/characters/" + characterId + "/timeline")
                .queryParam("limit", limit)
                .queryParam("apikey", apiKey);

        if (code != null && !code.isEmpty()) {
            builder.queryParam("code", code);
        }
        if (startDate != null && !startDate.isEmpty()) {
            builder.queryParam("startDate", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            builder.queryParam("endDate", endDate);
        }
        if (next != null && !next.isEmpty()) {
            builder.queryParam("next", next);
        }

        String url = builder.build().toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return objectMapper.readTree(response.getBody());
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
