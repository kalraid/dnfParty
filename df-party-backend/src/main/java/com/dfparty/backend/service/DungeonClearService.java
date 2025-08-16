package com.dfparty.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DungeonClearService {

    @Autowired
    private DfoApiService dfoApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 캐릭터의 던전 클리어 현황을 확인합니다.
     * DFO API 타임라인을 통해 실제 클리어 현황을 확인합니다.
     */
    public Map<String, Object> getDungeonClearStatus(String serverId, String characterId) {
        try {
            // DFO API 타임라인 조회 (새로운 메서드 사용)
            Map<String, Object> timelineResult = dfoApiService.getCharacterTimeline(serverId, characterId);
            
            if (!(Boolean) timelineResult.get("success")) {
                // 타임라인 조회 실패 시 기본값 반환
                Map<String, Boolean> defaultStatus = new HashMap<>();
                defaultStatus.put("nabel", false);
                defaultStatus.put("venus", false);
                defaultStatus.put("fog", false);
                defaultStatus.put("twilight", false);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("characterId", characterId);
                result.put("serverId", serverId);
                result.put("clearStatus", defaultStatus);
                result.put("message", "타임라인 조회 실패로 기본값을 반환합니다: " + timelineResult.get("error"));
                result.put("source", "Default (Timeline Failed)");
                
                return result;
            }
            
            // 타임라인에서 던전 클리어 현황 추출
            @SuppressWarnings("unchecked")
            Map<String, Boolean> clearStatus = (Map<String, Boolean>) timelineResult.get("dungeonStatus");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("characterId", characterId);
            result.put("serverId", serverId);
            result.put("clearStatus", clearStatus);
            result.put("message", "던전 클리어 현황을 성공적으로 조회했습니다.");
            result.put("source", timelineResult.get("source"));

            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "던전 클리어 현황 조회 중 오류가 발생했습니다: " + e.getMessage());
            return error;
        }
    }

    /**
     * 최근 목요일 오전 9시를 계산합니다.
     */
    private LocalDateTime getLastThursday9AM() {
        LocalDate today = LocalDate.now();
        LocalDate lastThursday = today;

        // 오늘이 목요일이 아니면 이전 목요일로 이동
        while (lastThursday.getDayOfWeek().getValue() != 4) { // 4 = 목요일
            lastThursday = lastThursday.minusDays(1);
        }

        // 오전 9시 설정
        return LocalDateTime.of(lastThursday, LocalTime.of(9, 0));
    }

    /**
     * 타임라인에서 던전 클리어 현황을 분석합니다.
     */
    private Map<String, Boolean> analyzeDungeonClear(Object timeline) {
        Map<String, Boolean> clearStatus = new HashMap<>();
        
        try {
            // 기본값 설정
            clearStatus.put("nabel", false);      // 만들어진 신 나벨
            clearStatus.put("venus", false);      // 베누스
            clearStatus.put("fog", false);        // 안개신

            if (timeline instanceof JsonNode) {
                JsonNode timelineNode = (JsonNode) timeline;
                JsonNode rows = timelineNode.path("rows");

                if (rows.isArray()) {
                    for (JsonNode row : rows) {
                        String raidName = row.path("raidName").asText("");
                        String regionName = row.path("regionName").asText("");
                        String name = row.path("name").asText("");

                        // 레기온 클리어 확인
                        if ("레기온 클리어".equals(name)) {
                            // 나벨 클리어 확인
                            if ("만들어진 신 나벨".equals(raidName)) {
                                clearStatus.put("nabel", true);
                            }
                            // 베누스 클리어 확인
                            if ("베누스".equals(regionName)) {
                                clearStatus.put("venus", true);
                            }
                            // 안개신 클리어 확인
                            if ("안개신".equals(raidName)) {
                                clearStatus.put("fog", true);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // 에러 발생 시 기본값 유지
        }

        return clearStatus;
    }

    /**
     * 여러 캐릭터의 던전 클리어 현황을 일괄 조회합니다.
     */
    public Map<String, Object> getBulkDungeonClearStatus(String serverId, String[] characterIds) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Map<String, Boolean>> bulkStatus = new HashMap<>();

        try {
            for (String characterId : characterIds) {
                Map<String, Object> status = getDungeonClearStatus(serverId, characterId);
                if ((Boolean) status.get("success")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> clearStatus = (Map<String, Boolean>) status.get("clearStatus");
                    bulkStatus.put(characterId, clearStatus);
                }
            }

            result.put("success", true);
            result.put("bulkStatus", bulkStatus);
            result.put("message", "일괄 던전 클리어 현황을 성공적으로 조회했습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "일괄 던전 클리어 현황 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }
}
