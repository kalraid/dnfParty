package com.dfparty.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DundamService {

    private static final String DUNDAM_BASE_URL = "https://dundam.xyz";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ThursdayFallbackService thursdayFallbackService;

    public Map<String, Object> getCharacterInfo(String serverId, String characterId) {
        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("Dundam 크롤링");
        if (restriction != null) {
            // 목요일 제한 시 Mock 데이터 반환
            Map<String, Object> fallbackResult = getMockCharacterInfo(serverId, characterId);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", "목요일에는 Dundam 크롤링이 제한되어 Mock 데이터를 제공합니다.");
            return fallbackResult;
        }
        
        try {
            log.info("=== 던담 크롤링 시작 ===");
            log.info("serverId: {}, characterId: {}", serverId, characterId);
            
            // Dundam.xyz 캐릭터 상세 페이지 크롤링
            String url = String.format("%s/character?server=%s&key=%s", 
                                     DUNDAM_BASE_URL, serverId, characterId);
            
            log.info("던담 URL: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.set("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Connection", "keep-alive");
            headers.set("Upgrade-Insecure-Requests", "1");
            headers.set("Sec-Fetch-Dest", "document");
            headers.set("Sec-Fetch-Mode", "navigate");
            headers.set("Sec-Fetch-Site", "none");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.info("던담 응답 상태: {}", response.getStatusCode());
            log.debug("던담 응답 내용: {}", response.getBody());
            
            // HTML 파싱하여 버프력과 총딜 추출
            return parseDundamResponse(response.getBody(), serverId, characterId);
            
        } catch (Exception e) {
            log.error("던담 크롤링 실패: {}", e.getMessage(), e);
            // 에러 발생 시 Mock 데이터 반환
            return getMockCharacterInfo(serverId, characterId);
        }
    }

    private Map<String, Object> parseDundamResponse(String htmlContent, String serverId, String characterId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 던담 HTML 파싱 시작 ===");
            
            Document doc = Jsoup.parse(htmlContent);
            
            // 버프력 추출 (버프 점수 부분에서)
            Long buffPower = extractBuffPower(doc);
            
            // 전투력 추출 (30Lv 점수 부분에서)
            Long totalDamage = extractTotalDamage(doc);
            
            result.put("buffPower", buffPower != null ? buffPower : 0L);
            result.put("totalDamage", totalDamage != null ? totalDamage : 0L);
            result.put("source", "dundam.xyz");
            result.put("success", true);
            
            log.info("던담 파싱 성공 - 버프력: {}, 전투력: {}", buffPower, totalDamage);
            
        } catch (Exception e) {
            log.error("던담 HTML 파싱 실패: {}", e.getMessage(), e);
            
            // 파싱 실패 시 Mock 데이터 반환
            result.put("buffPower", 4000000L); // 400만
            result.put("totalDamage", 25000000000L); // 250억
            result.put("source", "mock");
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 던담 HTML에서 버프력 추출
     */
    private Long extractBuffPower(Document doc) {
        try {
            // 방법 1: "버프 점수" 라벨 다음의 숫자 찾기
            Elements buffElements = doc.getElementsContainingOwnText("버프 점수");
            if (!buffElements.isEmpty()) {
                Element buffElement = buffElements.first();
                Element parent = buffElement.parent();
                if (parent != null) {
                    String text = parent.text();
                    Long buffPower = extractNumberFromText(text);
                    if (buffPower != null) {
                        log.info("버프력 추출 성공 (방법1): {}", buffPower);
                        return buffPower;
                    }
                }
            }
            
            // 방법 2: 클래스 또는 ID로 찾기 (실제 HTML 구조에 맞게 조정 필요)
            Elements buffScoreElements = doc.select(".buff-score, .stat-value, [class*=buff]");
            for (Element element : buffScoreElements) {
                String text = element.text();
                if (text.contains("4,") || text.contains("5,") || text.contains("6,")) { // 버프력은 보통 400만~600만대
                    Long buffPower = extractNumberFromText(text);
                    if (buffPower != null && buffPower > 1000000 && buffPower < 10000000) { // 100만~1000만 사이
                        log.info("버프력 추출 성공 (방법2): {}", buffPower);
                        return buffPower;
                    }
                }
            }
            
            log.warn("버프력 추출 실패 - HTML에서 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            log.error("버프력 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 던담 HTML에서 전투력(30Lv 점수) 추출
     */
    private Long extractTotalDamage(Document doc) {
        try {
            // 방법 1: "30Lv 점수" 라벨 다음의 숫자 찾기
            Elements damageElements = doc.getElementsContainingOwnText("30Lv 점수");
            if (!damageElements.isEmpty()) {
                Element damageElement = damageElements.first();
                Element parent = damageElement.parent();
                if (parent != null) {
                    String text = parent.text();
                    Long totalDamage = extractNumberFromText(text);
                    if (totalDamage != null) {
                        log.info("전투력 추출 성공 (방법1): {}", totalDamage);
                        return totalDamage;
                    }
                }
            }
            
            // 방법 2: 큰 숫자 찾기 (전투력은 보통 억 단위)
            Elements damageScoreElements = doc.select(".damage-score, .stat-value, [class*=damage], [class*=total]");
            for (Element element : damageScoreElements) {
                String text = element.text();
                Long totalDamage = extractNumberFromText(text);
                if (totalDamage != null && totalDamage > 1000000000L) { // 10억 이상
                    log.info("전투력 추출 성공 (방법2): {}", totalDamage);
                    return totalDamage;
                }
            }
            
            log.warn("전투력 추출 실패 - HTML에서 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            log.error("전투력 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 텍스트에서 숫자 추출 (콤마 제거 후 변환)
     */
    private Long extractNumberFromText(String text) {
        try {
            // 숫자와 콤마만 추출하는 정규식
            Pattern pattern = Pattern.compile("([0-9,]+)");
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                String numberStr = matcher.group(1).replace(",", "");
                try {
                    Long number = Long.parseLong(numberStr);
                    // 의미 있는 숫자만 반환 (1000 이상)
                    if (number >= 1000) {
                        return number;
                    }
                } catch (NumberFormatException e) {
                    // 숫자 변환 실패, 다음 매치 시도
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("숫자 추출 중 오류: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> getMockCharacterInfo(String serverId, String characterId) {
        Map<String, Object> mockData = new HashMap<>();
        
        // 서버별로 다른 Mock 데이터 생성
        if ("bakal".equals(serverId)) {
            mockData.put("buffPower", 5500000); // 550만
            mockData.put("totalDamage", 35000000000L); // 350억
        } else if ("cain".equals(serverId)) {
            mockData.put("buffPower", 4800000); // 480만
            mockData.put("totalDamage", 28000000000L); // 280억
        } else {
            mockData.put("buffPower", 4500000); // 450만
            mockData.put("totalDamage", 25000000000L); // 250억
        }
        
        mockData.put("source", "mock");
        return mockData;
    }

    public Map<String, Object> updateCharacterStats(String serverId, String characterId) {
        return getCharacterInfo(serverId, characterId);
    }
}
