package com.dfparty.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HtmlCrawlingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ThursdayFallbackService thursdayFallbackService;

    /**
     * 일반 HTML 크롤링으로 던담에서 캐릭터 정보 가져오기
     */
    public Map<String, Object> getCharacterInfoWithHtml(String serverId, String characterId) {
        log.info("=== HTML 크롤링 던담 크롤링 시작 ===");
        log.info("serverId: {}, characterId: {}", serverId, characterId);

        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("HTML 던담 크롤링");
        if (restriction != null) {
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", restriction.get("message"));
            return fallbackResult;
        }

        try {
            // 던담.xyz 캐릭터 페이지로 HTTP 요청
            String dundamUrl = String.format("https://dundam.xyz/character/%s/%s", serverId, characterId);
            log.info("던담 URL: {}", dundamUrl);

            // HTTP GET 요청으로 HTML 내용 가져오기
            String htmlContent = restTemplate.getForObject(dundamUrl, String.class);
            
            if (htmlContent == null || htmlContent.isEmpty()) {
                log.warn("HTML 크롤링으로 던담 페이지 내용을 가져올 수 없습니다.");
                Map<String, Object> noDataResult = new HashMap<>();
                noDataResult.put("success", false);
                noDataResult.put("message", "HTML 크롤링으로 던담 페이지 내용을 가져올 수 없습니다.");
                noDataResult.put("source", "dundam.xyz (HTML)");
                return noDataResult;
            }

            log.info("HTML 크롤링으로 던담 페이지 내용 가져오기 성공 (크기: {} bytes)", htmlContent.length());

            // HTML 파싱하여 캐릭터 정보 추출 (기본 구현)
            Map<String, Object> characterInfo = parseHtmlContent(htmlContent);
            
            if (characterInfo.isEmpty()) {
                log.warn("HTML 크롤링으로 추출된 정보가 없습니다.");
                Map<String, Object> noDataResult = new HashMap<>();
                noDataResult.put("success", false);
                noDataResult.put("message", "HTML 크롤링으로 캐릭터 정보를 추출할 수 없습니다.");
                noDataResult.put("source", "dundam.xyz (HTML)");
                return noDataResult;
            }

            // 성공 결과 반환
            Map<String, Object> successResult = new HashMap<>();
            successResult.put("success", true);
            successResult.put("message", "HTML 크롤링 성공");
            successResult.put("characterInfo", characterInfo);

            log.info("=== HTML 크롤링 던담 크롤링 완료 ===");
            return successResult;

        } catch (Exception e) {
            log.error("HTML 크롤링 던담 크롤링 중 예외 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "HTML 크롤링 던담 크롤링 중 오류가 발생했습니다: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        }
    }

    /**
     * HTML 내용을 파싱하여 캐릭터 정보 추출
     */
    private Map<String, Object> parseHtmlContent(String htmlContent) {
        Map<String, Object> characterInfo = new HashMap<>();
        
        try {
            // 간단한 텍스트 검색으로 정보 추출
            if (htmlContent.contains("총딜")) {
                // 총딜 추출 로직
                log.info("HTML에서 총딜 정보 발견");
                characterInfo.put("totalDamage", 0L); // 기본값
            }
            
            if (htmlContent.contains("버프력")) {
                // 버프력 추출 로직
                log.info("HTML에서 버프력 정보 발견");
                characterInfo.put("buffPower", 0L); // 기본값
            }
            
        } catch (Exception e) {
            log.warn("HTML 파싱 중 오류: {}", e.getMessage());
        }
        
        return characterInfo;
    }
}
