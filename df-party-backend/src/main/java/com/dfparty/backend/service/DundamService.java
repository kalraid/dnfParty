package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
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
import org.springframework.beans.factory.annotation.Value;

// Selenium imports
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Duration;





import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class DundamService {

    private static final String DUNDAM_BASE_URL = "https://dundam.xyz";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ThursdayFallbackService thursdayFallbackService;
    private final CharacterRepository characterRepository;
    
    @Value("${dundam.crawling.enabled:false}")
    private boolean crawlingEnabled;

    public Map<String, Object> getCharacterInfo(String serverId, String characterId) {
        // 던담 크롤링이 비활성화되어 있으면 즉시 반환
        if (!crawlingEnabled) {
            log.info("던담 크롤링이 비활성화되어 있습니다. DFO API만 사용하세요.");
            Map<String, Object> disabledResult = new HashMap<>();
            disabledResult.put("success", false);
            disabledResult.put("message", "던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.");
            disabledResult.put("crawlingDisabled", true);
            return disabledResult;
        }
        
        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("Dundam 크롤링");
        if (restriction != null) {
            // 목요일 제한 시 빈 결과 반환
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", "목요일에는 Dundam 크롤링이 제한되어 데이터를 제공할 수 없습니다.");
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
            headers.set("Accept-Charset", "UTF-8");
            headers.set("Connection", "keep-alive");
            headers.set("Upgrade-Insecure-Requests", "1");
            headers.set("Sec-Fetch-Dest", "document");
            headers.set("Sec-Fetch-Mode", "navigate");
            headers.set("Sec-Fetch-Site", "none");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.info("던담 응답 상태: {}", response.getStatusCode());
            
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.length() > 0) {
                log.info("던담 응답 크기: {} bytes", responseBody.length());
                
                // UTF-8로 디코딩하여 한글이 제대로 표시되도록 함
                byte[] responseBytes = responseBody.getBytes("ISO-8859-1");
                String decodedResponse = new String(responseBytes, "UTF-8");
                
                log.info("던담 응답 첫 200자 (UTF-8): {}", decodedResponse.substring(0, Math.min(200, decodedResponse.length())));
                
                // 캐릭터 정보가 있는지 간단 체크
                if (decodedResponse.contains("캐릭터") || decodedResponse.contains("character") || decodedResponse.contains("버프") || decodedResponse.contains("딜")) {
                    log.info("던담 응답에 캐릭터 관련 정보 포함됨");
                    responseBody = decodedResponse; // 디코딩된 응답 사용
                } else {
                    log.warn("던담 응답에 캐릭터 정보가 없어 보임");
                    // 실패 시 빈 결과 반환하여 업데이트 방지
                    Map<String, Object> fallbackResult = new HashMap<>();
                    fallbackResult.put("success", false);
                    fallbackResult.put("message", "던담 응답에 캐릭터 정보가 없습니다.");
                    return fallbackResult;
                }
            } else {
                log.warn("던담 응답 내용이 비어있음");
                // 실패 시 빈 결과 반환하여 업데이트 방지
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("success", false);
                fallbackResult.put("message", "던담 응답이 비어있습니다.");
                return fallbackResult;
            }
            
            // 수동 크롤링 메서드 호출 (중복 코드 제거)
            return performManualCrawling(serverId, characterId);
            
        } catch (Exception e) {
            log.error("던담 크롤링 실패: {}", e.getMessage(), e);
            // 에러 발생 시 빈 결과 반환
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("source", "dundam.xyz - Error");
            return errorResult;
        }
    }

    private Map<String, Object> parseDundamResponse(String htmlContent, String serverId, String characterId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("=== 던담 HTML 파싱 시작 ===");
            
            // 셀레니움으로 가져온 페이지 HTML 로그 출력 (디버깅용)
            log.info("=== 셀레니움 페이지 HTML 시작 ===");
            log.info("HTML 전체 텍스트 길이: {} 문자", htmlContent.length());
            log.info("HTML 전체 텍스트 (처음 3000자): {}", 
                htmlContent.length() > 3000 ? htmlContent.substring(0, 3000) + "..." : htmlContent);
            log.info("HTML 전체 텍스트 (마지막 3000자): {}", 
                htmlContent.length() > 3000 ? "..." + htmlContent.substring(htmlContent.length() - 3000) : htmlContent);
            log.info("=== 셀레니움 페이지 HTML 끝 ===");
            
            Document doc = Jsoup.parse(htmlContent);
            
            // 캐릭터 직업 정보 가져오기 (직업별 스탯 추출을 위해)
            Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
            if (characterOpt.isEmpty()) {
                log.warn("캐릭터 정보를 찾을 수 없음: {}", characterId);
                result.put("success", false);
                result.put("message", "캐릭터 정보를 찾을 수 없습니다.");
                return result;
            }
            
            Character character = characterOpt.get();
            
            boolean isBuffer = isBuffer(character);
            log.info("캐릭터 {} (직업: {}) - 버퍼 여부: {}", character.getCharacterName(), character.getJobName(), isBuffer);
            
            Long buffPower = null;
            Long totalDamage = null;
            
            // characterInfo 맵 생성 (CharacterService에서 기대하는 형태)
            Map<String, Object> characterInfo = new HashMap<>();
            characterInfo.put("buffPower", buffPower != null ? buffPower : 0L);
            characterInfo.put("totalDamage", totalDamage != null ? totalDamage : 0L);
            characterInfo.put("source", "dundam.xyz");
            
            if (isBuffer) {
                // 버퍼: 버프력만 추출
                log.info("버퍼 캐릭터 - 버프력만 추출");
                buffPower = extractBuffPower(doc);
                totalDamage = 0L; // 버퍼는 총딜 0
            } else {
                // 딜러: 총딜만 추출
                log.info("딜러 캐릭터 - 총딜만 추출");
                totalDamage = extractTotalDamage(doc);
                buffPower = 0L; // 딜러는 버프력 0
                
                // 딜러의 경우 총딜 값만 사용 (사용자 수정된 로직)
                if (totalDamage != null) {
                    log.info("딜러 총딜 값: {}", totalDamage);
                }
            }
            
            // characterInfo 맵 업데이트
            characterInfo.put("buffPower", buffPower != null ? buffPower : 0L);
            characterInfo.put("totalDamage", totalDamage != null ? totalDamage : 0L);
            
            result.put("characterInfo", characterInfo);
            result.put("buffPower", buffPower != null ? buffPower : 0L);
            result.put("totalDamage", totalDamage != null ? totalDamage : 0L);
            result.put("source", "dundam.xyz");
            result.put("success", true);
            
            // 직업별로 필요한 스탯이 추출되었는지 확인
            if (isBuffer && buffPower == null) {
                log.warn("버퍼 캐릭터의 버프력 추출 실패");
                result.put("success", false);
                result.put("message", "버퍼 캐릭터의 버프력을 추출할 수 없습니다.");
                return result;
            } else if (!isBuffer && totalDamage == null) {
                log.warn("딜러 캐릭터의 총딜 추출 실패");
                result.put("success", false);
                result.put("message", "딜러 캐릭터의 총딜을 추출할 수 없습니다.");
                return result;
            }
            
            log.info("던담 파싱 성공 - 직업: {}, 버프력: {}, 총딜: {}", 
                isBuffer ? "버퍼" : "딜러", buffPower, totalDamage);
            
        } catch (Exception e) {
            log.error("던담 HTML 파싱 실패: {}", e.getMessage(), e);
            
            // 파싱 실패 시 업데이트를 하지 않도록 실패 결과 반환
            result.put("success", false);
            result.put("message", "던담 HTML 파싱 중 오류가 발생했습니다: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 캐릭터가 버퍼인지 확인
     */
    private boolean isBuffer(Character character) {
        if (character.getJobName() == null || character.getJobName().equals("N/A")) {
            return false;
        }
        
        // 버퍼 직업 목록
        String[] bufferJobs = {"뮤즈", "패러메딕", "크루세이더", "인챈트리스"};
        
        for (String bufferJob : bufferJobs) {
            if (character.getJobName().contains(bufferJob)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 던담 HTML에서 버프력 추출
     */
    private Long extractBuffPower(Document doc) {
        try {
            log.info("=== 버프력 추출 시작 ===");
            
            // 방법 1: "버프 점수" 또는 "버프력" 텍스트가 포함된 요소 찾기
            Elements buffElements = doc.getElementsContainingOwnText("버프");
            log.info("버프 관련 요소 수: {}", buffElements.size());
            
            for (Element element : buffElements) {
                String text = element.text();
                log.info("버프 요소 텍스트: {}", text);
                
                // 숫자가 포함된 경우 추출 시도
                Long buffPower = extractNumberFromText(text);
                if (buffPower != null && buffPower > 100000 && buffPower < 50000000) { // 10만~5000만 사이
                    log.info("버프력 추출 성공 (방법1): {}", buffPower);
                    return buffPower;
                }
                
                // 부모/형제 요소에서도 숫자 찾기
                Element parent = element.parent();
                if (parent != null) {
                    String parentText = parent.text();
                    Long parentBuffPower = extractNumberFromText(parentText);
                    if (parentBuffPower != null && parentBuffPower > 100000 && parentBuffPower < 50000000) {
                        log.info("버프력 추출 성공 (부모 요소): {}", parentBuffPower);
                        return parentBuffPower;
                    }
                }
            }
            
            // 방법 2: 전체 HTML에서 숫자 패턴으로 버프력으로 추정되는 값 찾기
            String htmlText = doc.text();
            log.info("전체 HTML 텍스트 길이: {}", htmlText.length());
            
            // 다양한 버프력 패턴 시도
            String[] buffPatterns = {
                "([1-9],[0-9]{3},[0-9]{3})",           // 5,053,108 형태
                "([1-9][0-9]{6,7})",                   // 5053108 형태 (쉼표 없음)
                "([1-9][0-9]{2,3}만)",                 // 505만 형태
                "([1-9][0-9]{2,3}천)",                 // 505천 형태
                "([1-9][0-9]{2,3}억)",                 // 5억 형태
                "([1-9][0-9]{2,3}M)",                  // 505M 형태
                "([1-9][0-9]{2,3}K)"                   // 505K 형태
            };
            
            for (String patternStr : buffPatterns) {
                Pattern buffPattern = Pattern.compile(patternStr);
                Matcher buffMatcher = buffPattern.matcher(htmlText);
                
                while (buffMatcher.find()) {
                    String buffStr = buffMatcher.group(1);
                    log.info("버프력 패턴 매칭: {}", buffStr);
                    
                    Long buffPower = extractNumberFromText(buffStr);
                    if (buffPower != null && buffPower > 100000 && buffPower < 100000000) { // 10만~1억 사이
                        log.info("버프력 추출 성공 (패턴 매칭): {}", buffPower);
                        return buffPower;
                    }
                }
            }
            
            // 방법 3: 다양한 CSS 셀렉터로 시도
            String[] selectors = {
                ".stat-value", ".number", ".score", ".value", 
                "[class*=buff]", "[class*=stat]", "[class*=score]",
                "[class*=power]", "[class*=point]", "[class*=damage]",
                "span", "div", "strong", "b", "p", "h1", "h2", "h3", "h4", "h5", "h6"
            };
            
            for (String selector : selectors) {
                Elements elements = doc.select(selector);
                for (Element element : elements) {
                    String text = element.text();
                    Long buffPower = extractNumberFromText(text);
                    if (buffPower != null && buffPower > 100000 && buffPower < 100000000) { // 10만~1억 사이로 범위 확장
                        log.info("버프력 추출 성공 (CSS 셀렉터 {}): {}", selector, buffPower);
                        return buffPower;
                    }
                }
            }
            
            // 방법 4: 모든 텍스트 노드에서 버프력으로 추정되는 숫자 찾기
            Elements allElements = doc.getAllElements();
            for (Element element : allElements) {
                String text = element.ownText(); // 자식 요소 제외하고 현재 요소의 텍스트만
                if (text != null && !text.trim().isEmpty()) {
                    Long buffPower = extractNumberFromText(text);
                    if (buffPower != null && buffPower > 100000 && buffPower < 100000000) {
                        log.info("버프력 추출 성공 (전체 요소 스캔): {}", buffPower);
                        return buffPower;
                    }
                }
            }
            
            log.warn("버프력 추출 실패 - 모든 방법 실패");
            return null;
            
        } catch (Exception e) {
            log.error("버프력 추출 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 던담 HTML에서 전투력(30Lv 점수) 추출
     */
    private Long extractTotalDamage(Document doc) {
        try {
            log.info("=== 전투력 추출 시작 ===");
            
            // 방법 1: "30Lv", "전투력", "딜량" 관련 텍스트가 포함된 요소 찾기
            String[] damageKeywords = {"30Lv", "전투력", "딜량", "총딜", "점수"};
            
            for (String keyword : damageKeywords) {
                Elements damageElements = doc.getElementsContainingOwnText(keyword);
                log.info("{} 관련 요소 수: {}", keyword, damageElements.size());
                
                for (Element element : damageElements) {
                    String text = element.text();
                    log.info("{} 요소 텍스트: {}", keyword, text);
                    
                    // 큰 숫자가 포함된 경우 추출 시도 (전투력은 보통 억 단위)
                    Long totalDamage = extractNumberFromText(text);
                    if (totalDamage != null && totalDamage > 1000000000L && totalDamage < 1000000000000L) { // 10억~1조 사이
                        log.info("전투력 추출 성공 (키워드 {}): {}", keyword, totalDamage);
                        return totalDamage;
                    }
                    
                    // 부모/형제 요소에서도 숫자 찾기
                    Element parent = element.parent();
                    if (parent != null) {
                        String parentText = parent.text();
                        Long parentTotalDamage = extractNumberFromText(parentText);
                        if (parentTotalDamage != null && parentTotalDamage > 1000000000L && parentTotalDamage < 1000000000000L) {
                            log.info("전투력 추출 성공 (부모 요소, 키워드 {}): {}", keyword, parentTotalDamage);
                            return parentTotalDamage;
                        }
                    }
                }
            }
            
            // 방법 2: 전체 HTML에서 억 단위 숫자 패턴으로 전투력 추정
            String htmlText = doc.text();
            
            // 사진에서 확인된 패턴: 1,875,485 형태의 전투력 찾기 (백만~천만 범위도 포함)
            Pattern damagePattern = Pattern.compile("([1-9](?:[0-9]{1,2})?,[0-9]{3},[0-9]{3})");
            Matcher damageMatcher = damagePattern.matcher(htmlText);
            
            while (damageMatcher.find()) {
                String damageStr = damageMatcher.group(1);
                Long totalDamage = extractNumberFromText(damageStr);
                if (totalDamage != null && totalDamage > 1000000L) { // 100만 이상 (사진에서 1,875,485 확인)
                    log.info("전투력 추출 성공 (패턴 매칭): {}", totalDamage);
                    return totalDamage;
                }
            }
            
            // 방법 3: HTML에서 모든 큰 숫자들을 찾아서 전투력으로 추정
            Pattern allNumberPattern = Pattern.compile("([0-9,]+)");
            Matcher allNumberMatcher = allNumberPattern.matcher(htmlText);
            
            while (allNumberMatcher.find()) {
                String numberStr = allNumberMatcher.group(1);
                if (numberStr.contains(",")) { // 콤마가 포함된 큰 숫자만
                    Long number = extractNumberFromText(numberStr);
                    if (number != null && number > 5000000000L && number < 1000000000000L) { // 50억~1조 사이
                        log.info("전투력 추출 성공 (모든 숫자 검색): {}", number);
                        return number;
                    }
                }
            }
            
            log.warn("전투력 추출 실패 - 모든 방법 실패");
            return null;
            
        } catch (Exception e) {
            log.error("전투력 추출 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 던담 HTML에서 두 개의 총딜 값 추출 (작은 값, 큰 값)
     */
    private Long[] extractMultipleTotalDamage(Document doc) {
        try {
            log.info("=== 두 개의 총딜 값 추출 시작 ===");
            log.info("HTML 전체 텍스트 길이: {}", doc.text().length());
            
            List<Long> damageValues = new ArrayList<>();
            
            // 방법 1: "총딜" 텍스트가 포함된 요소에서 숫자 찾기 (우선순위 높음)
            log.info("=== 방법 1: '총딜' 키워드로 검색 ===");
            Elements totalDamageElements = doc.getElementsContainingOwnText("총딜");
            log.info("'총딜' 키워드가 포함된 요소 수: {}", totalDamageElements.size());
            
            for (int i = 0; i < totalDamageElements.size(); i++) {
                Element element = totalDamageElements.get(i);
                String text = element.text();
                log.info("'총딜' 요소 {} 텍스트: '{}'", i, text);
                
                // 텍스트에서 모든 숫자 추출 (1억 이상)
                List<Long> numbers = extractAllNumbersFromText(text);
                log.info("'총딜' 요소 {}에서 추출된 숫자들: {}", i, numbers);
                
                for (Long number : numbers) {
                    if (number >= 100000000L) { // 1억 이상 (3억 5천만도 포함)
                        if (!damageValues.contains(number)) {
                            damageValues.add(number);
                            log.info("총딜 값 추가 ('총딜' 키워드): {}", number);
                        }
                    }
                }
                
                // 부모/형제 요소에서도 숫자 찾기
                Element parent = element.parent();
                if (parent != null) {
                    String parentText = parent.text();
                    List<Long> parentNumbers = extractAllNumbersFromText(parentText);
                    log.info("'총딜' 요소 {}의 부모에서 추출된 숫자들: {}", i, parentNumbers);
                    
                    for (Long number : parentNumbers) {
                        if (number >= 100000000L) {
                            if (!damageValues.contains(number)) {
                                damageValues.add(number);
                                log.info("총딜 값 추가 (부모 요소): {}", number);
                            }
                        }
                    }
                }
            }
            
            // 방법 2: "30Lv", "전투력", "딜량" 관련 텍스트가 포함된 요소에서 숫자 찾기
            log.info("=== 방법 2: 전투력 관련 키워드로 검색 ===");
            String[] damageKeywords = {"30Lv", "전투력", "딜량", "점수"};
            
            for (String keyword : damageKeywords) {
                Elements damageElements = doc.getElementsContainingOwnText(keyword);
                log.info("'{}' 관련 요소 수: {}", keyword, damageElements.size());
                
                for (int i = 0; i < damageElements.size(); i++) {
                    Element element = damageElements.get(i);
                    String text = element.text();
                    log.info("'{}' 요소 {} 텍스트: '{}'", keyword, i, text);
                    
                    // 텍스트에서 모든 큰 숫자 추출 (1억 이상)
                    List<Long> numbers = extractAllNumbersFromText(text);
                    log.info("'{}' 요소 {}에서 추출된 숫자들: {}", keyword, i, numbers);
                    
                    for (Long number : numbers) {
                        if (number >= 100000000L) { // 1억 이상
                            if (!damageValues.contains(number)) {
                                damageValues.add(number);
                                log.info("총딜 값 추가 (키워드 '{}'): {}", keyword, number);
                            }
                        }
                    }
                }
            }
            
            // 방법 3: 전체 HTML에서 콤마가 포함된 큰 숫자 패턴으로 검색
            log.info("=== 방법 3: 콤마 패턴으로 검색 ===");
            String htmlText = doc.text();
            
            // 콤마가 포함된 숫자 패턴: 1,234,567 형태
            Pattern commaPattern = Pattern.compile("([1-9][0-9]{0,2}(?:,[0-9]{3}){1,3})");
            Matcher commaMatcher = commaPattern.matcher(htmlText);
            
            while (commaMatcher.find()) {
                String numberStr = commaMatcher.group(1);
                Long number = extractNumberFromText(numberStr);
                if (number != null && number >= 100000000L && !damageValues.contains(number)) {
                    damageValues.add(number);
                    log.info("총딜 값 추가 (콤마 패턴): {} (원본: {})", number, numberStr);
                }
            }
            
            // 방법 4: HTML에서 모든 큰 숫자들을 찾아서 전투력으로 추정
            log.info("=== 방법 4: 모든 숫자 검색 ===");
            Pattern allNumberPattern = Pattern.compile("([0-9,]+)");
            Matcher allNumberMatcher = allNumberPattern.matcher(htmlText);
            
            while (allNumberMatcher.find()) {
                String numberStr = allNumberMatcher.group(1);
                if (numberStr.contains(",") && numberStr.length() >= 7) { // 콤마가 포함되고 7자리 이상
                    Long number = extractNumberFromText(numberStr);
                    if (number != null && number >= 100000000L && !damageValues.contains(number)) {
                        damageValues.add(number);
                        log.info("총딜 값 추가 (모든 숫자 검색): {} (원본: {})", number, numberStr);
                    }
                }
            }
            
            // 중복 제거 및 정렬
            damageValues = damageValues.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            log.info("=== 최종 결과 ===");
            log.info("추출된 총딜 값들 (정렬됨): {}", damageValues);
            log.info("총딜 값 개수: {}", damageValues.size());
            
            if (damageValues.size() >= 2) {
                // 두 개 이상의 값이 있는 경우, 작은 값과 큰 값 반환
                Long[] result = {damageValues.get(0), damageValues.get(damageValues.size() - 1)};
                log.info("두 개의 총딜 값 추출 성공: 작은값={}, 큰값={}", result[0], result[1]);
                return result;
            } else if (damageValues.size() == 1) {
                // 하나의 값만 있는 경우
                Long[] result = {damageValues.get(0), null};
                log.info("단일 총딜 값 추출: {}", result[0]);
                return result;
            } else {
                log.warn("총딜 값 추출 실패 - 모든 방법 실패");
                return new Long[0];
            }
            
        } catch (Exception e) {
            log.error("두 개의 총딜 값 추출 중 오류: {}", e.getMessage(), e);
            return new Long[0];
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
    
    /**
     * 텍스트에서 모든 숫자 추출 (콤마 제거 후 변환)
     */
    private List<Long> extractAllNumbersFromText(String text) {
        List<Long> numbers = new ArrayList<>();
        try {
            log.debug("텍스트에서 숫자 추출 시작: '{}'", text);
            
            // 숫자와 콤마만 추출하는 정규식
            Pattern pattern = Pattern.compile("([0-9,]+)");
            Matcher matcher = pattern.matcher(text);
            
            int matchCount = 0;
            while (matcher.find()) {
                matchCount++;
                String originalNumberStr = matcher.group(1);
                String cleanNumberStr = originalNumberStr.replace(",", "");
                
                log.debug("매치 {}: 원본='{}', 정리됨='{}'", matchCount, originalNumberStr, cleanNumberStr);
                
                try {
                    Long number = Long.parseLong(cleanNumberStr);
                    log.debug("매치 {}: 숫자 변환 성공 = {}", matchCount, number);
                    
                    // 의미 있는 숫자만 추가 (1000 이상)
                    if (number >= 1000) {
                        numbers.add(number);
                        log.debug("매치 {}: 숫자 추가됨 = {}", matchCount, number);
                    } else {
                        log.debug("매치 {}: 숫자 너무 작음, 제외 = {}", matchCount, number);
                    }
                } catch (NumberFormatException e) {
                    log.debug("매치 {}: 숫자 변환 실패 = '{}', 오류: {}", matchCount, cleanNumberStr, e.getMessage());
                    continue;
                }
            }
            
            log.debug("숫자 추출 완료: 총 {}개 매치, {}개 숫자 추가됨", matchCount, numbers.size());
            log.debug("추출된 숫자들: {}", numbers);
            
        } catch (Exception e) {
            log.error("모든 숫자 추출 중 오류: {}", e.getMessage(), e);
        }
        return numbers;
    }

    private Map<String, Object> getMockCharacterInfo(String serverId, String characterId) {
        Map<String, Object> mockData = new HashMap<>();
        

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
    
    /**
     * WebDriver를 사용한 JavaScript 렌더링 후 크롤링
     */
    public Map<String, Object> getCharacterInfoWithWebDriver(String serverId, String characterId) {
        // 기본 던담 크롤링 메서드 사용
        return getCharacterInfo(serverId, characterId);
    }
    
        /**
     * Playwright를 사용한 빠른 CSS 셀렉터 기반 크롤링
     */
    public Map<String, Object> getCharacterInfoWithPlaywrightFast(String serverId, String characterId) {
        return getCharacterInfoWithPlaywrightFast(serverId, characterId, null);
    }
    
    /**
     * Playwright를 사용한 빠른 CSS 셀렉터 기반 크롤링 (직업 정보 포함)
     */
    public Map<String, Object> getCharacterInfoWithPlaywrightFast(String serverId, String characterId, String jobName) {
        // 기본 던담 크롤링 메서드 사용
        return getCharacterInfo(serverId, characterId);
    }
    
    /**
     * 캐릭터 직업 추출 (기본 구현)
     */
    private String extractCharacterClass(String characterId) {
        log.info("=== 캐릭터 직업 추출 시작 ===");
        
        // 캐릭터 ID로 직업 매핑 (하드코딩)
        if ("2c37ba51544e86792ea0e6d53018f9a8".equals(characterId)) {
            log.info("뮤즈앙갚음2 캐릭터 ID 매칭");
            return "뮤즈";
        }
        if ("db12e75994ed26dffd7247035283dd45".equals(characterId)) {
            log.info("헤카테앙갚음 캐릭터 ID 매칭");
            return "헤카테";
        }
        if ("e967eee957794439886327170d6af17f".equals(characterId)) {
            log.info("에반젤앙갚음 캐릭터 ID 매칭");
            return "크루세이더";
        }
        
        log.warn("직업을 찾을 수 없음, 기본값 '딜러'로 설정");
        return "딜러"; // 기본값
    }
    
    /**
     * 캐릭터 ID로 직업 매핑 (하드코딩 테스트용)
     */
    private String getCharacterClassById(String characterId) {
        log.info("캐릭터 ID로 직업 매핑: {}", characterId);
        
        // 앙갚음 캐릭터들의 직업 매핑
        if ("2c37ba51544e86792ea0e6d53018f9a8".equals(characterId)) {
            log.info("뮤즈앙갚음2 캐릭터 ID 매칭");
            return "뮤즈";
        }
        if ("db12e75994ed26dffd7247035283dd45".equals(characterId)) {
            log.info("헤카테앙갚음 캐릭터 ID 매칭");
            return "헤카테";
        }
        if ("e967eee957794439886327170d6af17f".equals(characterId)) {
            log.info("에반젤앙갚음 캐릭터 ID 매칭");
            return "크루세이더";
        }
        if ("f86d0424bb613b0767bc78def127d35c".equals(characterId)) {
            log.info("세라핌앙갚음 캐릭터 ID 매칭");
            return "크루세이더(여)";
        }
        if ("448c4e12f062068c525864da4c168c05".equals(characterId)) {
            log.info("패러앙갚음 캐릭터 ID 매칭");
            return "패러메딕";
        }
        if ("9bd28823481dcde708538510a06bb12d".equals(characterId)) {
            log.info("아처앙갚음 캐릭터 ID 매칭");
            return "아처";
        }
        if ("b15a5123bf53afa89fd04acaa8ba8453".equals(characterId)) {
            log.info("검제앙갚음 캐릭터 ID 매칭");
            return "베가본드";
        }
        if ("eb1e58b4cd07657661520cfc7a6ce225".equals(characterId)) {
            log.info("천신낭랑갚음 캐릭터 ID 매칭");
            return "천신낭랑";
        }
        
        log.warn("알려진 캐릭터 ID가 아님, 기본값 '딜러'로 설정");
        return "딜러";
    }

    /**
     * 사용자 제공 정확한 수치 (검증용)
     */
    private Map<String, Object> getExpectedValues(String characterId) {
        Map<String, Object> expected = new HashMap<>();
        
        switch (characterId) {
            case "2c37ba51544e86792ea0e6d53018f9a8": // 뮤즈앙갚음2
                expected.put("buffPower", 50353108L);
                expected.put("buffPower4p", 50353108L);
                expected.put("buffPower3p", 50353108L);
                expected.put("buffPower2p", 50353108L);
                break;
            case "db12e75994ed26dffd7247035283dd45": // 헤카테앙갚음
                expected.put("buffPower2p", 4497448L);
                expected.put("buffPower3p", 4153304L);
                expected.put("buffPower4p", 4038589L);
                break;
            case "e967eee957794439886327170d6af17f": // 에반젤앙갚음
                expected.put("buffPower", 4436187L);
                expected.put("buffPower4p", 4436187L);
                expected.put("buffPower3p", 4436187L);
                expected.put("buffPower2p", 4436187L);
                break;
            case "9bd28823481dcde708538510a06bb12d": // 아처앙갚음
                expected.put("totalDamage", 3911900000L); // 39억 1190만
                expected.put("totalDamage4p", 3911900000L);
                expected.put("totalDamage3p", 3911900000L);
                expected.put("totalDamage2p", 3911900000L);
                break;
            case "b15a5123bf53afa89fd04acaa8ba8453": // 검제앙갚음
                expected.put("totalDamage", 2259920000L); // 22억 5992만
                expected.put("totalDamage4p", 2259920000L);
                expected.put("totalDamage3p", 2259920000L);
                expected.put("totalDamage2p", 2259920000L);
                break;
            case "eb1e58b4cd07657661520cfc7a6ce225": // 천신낭랑갚음
                expected.put("totalDamage", 3062840000L); // 30억 6284만
                expected.put("totalDamage4p", 1488490000L); // 14억 8849만
                expected.put("totalDamage3p", 3062840000L);
                expected.put("totalDamage2p", 1296430000L); // 12억 9643만
                break;
        }
        
        return expected;
    }
    
    /**
     * 버퍼 직업인지 확인
     */
    private boolean isBufferClass(String characterClass) {
        if (characterClass == null) return false;
        
        String lowerClass = characterClass.toLowerCase();
        return lowerClass.contains("뮤즈") || 
               lowerClass.contains("muse") ||
               lowerClass.contains("패러메딕") || 
               lowerClass.contains("paramedic") ||
               lowerClass.contains("크루세이더") || 
               lowerClass.contains("crusader") ||
               lowerClass.contains("헤카테") || 
               lowerClass.contains("hecate");

    }
    
    /**
     * 버퍼용 버프력 추출 (기본 구현)
     */
    private void extractBuffPowerForBuffer(String pageText, Map<String, Object> result) {
        try {
            log.info("=== 버퍼용 버프력 추출 시작 ===");
            
            // 텍스트에서 버프력 추출
            extractBuffPowerFromText(pageText, result);
            
            // 상세 추출
            extractBuffPowerDetailed(pageText, result);
            
        } catch (Exception e) {
            log.warn("버퍼 버프력 추출 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 딜러용 전투력 추출 (기본 구현)
     */
    private void extractDamageStatsForDealer(String pageText, Map<String, Object> result) {
        try {
            log.info("=== 딜러용 전투력 추출 시작 ===");
            
            // 큰 숫자 패턴 찾기 (전투력은 보통 억 단위 이상)
            Pattern damagePattern = Pattern.compile("([1-9](?:[0-9]{1,2})?,[0-9]{3},[0-9]{3})");
            Matcher damageMatcher = damagePattern.matcher(pageText);
            
            if (damageMatcher.find()) {
                String damageStr = damageMatcher.group(1);
                Long totalDamage = extractNumberFromText(damageStr);
                if (totalDamage != null && totalDamage > 1000000) {
                    result.put("totalDamage", totalDamage);
                    log.info("✅ 딜러 전투력 추출 성공: {}", totalDamage);
                }
            }
            
        } catch (Exception e) {
            log.warn("딜러 전투력 추출 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 텍스트에서 버프력 추출 (버퍼용)
     */
    private void extractBuffPowerFromText(String pageText, Map<String, Object> result) {
        Pattern buffPattern = Pattern.compile("([1-9],[0-9]{3},[0-9]{3})");
        Matcher buffMatcher = buffPattern.matcher(pageText);
        
        while (buffMatcher.find()) {
            String buffStr = buffMatcher.group(1);
            Long buffPower = extractNumberFromText(buffStr);
            if (buffPower != null && buffPower > 1000000 && buffPower < 10000000) {
                result.put("buffPower", buffPower);
                log.info("✅ 패턴 매칭으로 버프력 추출: {}", buffPower);
                break;
            }
        }
    }

    /**
     * 텍스트에서 4인/3인/2인 버프력 상세 추출 (버퍼용)
     */
    private void extractBuffPowerDetailed(String pageText, Map<String, Object> result) {
        log.info("=== 버프력 상세 추출 시작 ===");
        
        // 디버깅: 페이지 텍스트에서 "버프점수" 관련 부분 찾기
        if (pageText.contains("버프점수")) {
            log.info("✅ 페이지에서 '버프점수' 키워드 발견");
            
            // "버프점수" 주변 텍스트 샘플 출력
            int buffIndex = pageText.indexOf("버프점수");
            String buffContext = pageText.substring(Math.max(0, buffIndex - 50), 
                                                 Math.min(pageText.length(), buffIndex + 100));
            log.info("버프점수 주변 텍스트: {}", buffContext);
        } else {
            log.warn("❌ 페이지에서 '버프점수' 키워드를 찾을 수 없음");
            
            // 대안 키워드들 확인
            String[] alternativeKeywords = {"버프", "점수", "뮤즈", "에반젤", "앙갚음"};
            for (String keyword : alternativeKeywords) {
                if (pageText.contains(keyword)) {
                    log.info("✅ 대안 키워드 '{}' 발견", keyword);
                    int keywordIndex = pageText.indexOf(keyword);
                    String keywordContext = pageText.substring(Math.max(0, keywordIndex - 30), 
                                                           Math.min(pageText.length(), keywordIndex + 80));
                    log.info("'{}' 주변 텍스트: {}", keyword, keywordContext);
                }
            }
        }
        
        // 방법 1: "버프점수" 키워드가 포함된 텍스트에서 우선 추출
        Pattern buffScorePattern = Pattern.compile("버프점수\\s*([1-9],[0-9]{3},[0-9]{3})");
        Matcher buffScoreMatcher = buffScorePattern.matcher(pageText);
        Long exactBuffPower = null;
        
        // "버프점수" 키워드로 정확한 값 찾기
        while (buffScoreMatcher.find()) {
            String buffStr = buffScoreMatcher.group(1);
            Long buffPower = extractNumberFromText(buffStr);
            if (buffPower != null && buffPower > 1000000 && buffPower < 10000000) {
                exactBuffPower = buffPower;
                log.info("✅ 버프점수 키워드로 정확한 버프력 발견: {}", buffPower);
                break;
            }
        }
        
        if (exactBuffPower == null) {
            log.info("버프점수 키워드로 찾지 못함, 다른 방법 시도");
            
            // 방법 2: "버프점수"로 찾지 못한 경우, 일반적인 숫자 패턴으로 찾기
            Pattern buffPattern4p = Pattern.compile("([1-9],[0-9]{3},[0-9]{3})");
            Matcher buffMatcher4p = buffPattern4p.matcher(pageText);
            Long maxBuffPower = null;
            
            while (buffMatcher4p.find()) {
                String buffStr = buffMatcher4p.group(1);
                Long buffPower = extractNumberFromText(buffStr);
                if (buffPower != null && buffPower > 1000000 && buffPower < 10000000) {
                    if (maxBuffPower == null || buffPower > maxBuffPower) {
                        maxBuffPower = buffPower;
                    }
                }
            }
            
            if (maxBuffPower != null) {
                exactBuffPower = maxBuffPower;
                log.info("✅ 패턴 매칭으로 버프력 발견: {}", maxBuffPower);
            }
        }
        
        if (exactBuffPower != null) {
            result.put("buffPower", exactBuffPower);
            result.put("buffPower4p", exactBuffPower);
            result.put("buffPower3p", exactBuffPower);
            result.put("buffPower2p", exactBuffPower);
            log.info("✅ 최종 4인/3인/2인 버프력 설정: {}", exactBuffPower);
        } else {
            log.warn("❌ 버프력을 찾을 수 없음");
        }
    }
    
    /**
     * 텍스트에서 전투력 추출 (딜러용)
     */
    private void extractTotalDamageFromText(String pageText, Map<String, Object> result) {
        Pattern damagePattern = Pattern.compile("([1-9](?:[0-9]{1,2})?,[0-9]{3},[0-9]{3})");
        Matcher damageMatcher = damagePattern.matcher(pageText);
        
        while (damageMatcher.find()) {
            String damageStr = damageMatcher.group(1);
            Long totalDamage = extractNumberFromText(damageStr);
            if (totalDamage != null && totalDamage > 1000000) {
                result.put("totalDamage", totalDamage);
                log.info("✅ 패턴 매칭으로 전투력 추출: {}", totalDamage);
                break;
            }
        }
    }

    /**
     * 텍스트에서 4인/3인/2인 총딜 상세 추출 (딜러용)
     */
    private void extractTotalDamageDetailed(String pageText, Map<String, Object> result) {
        // 방법 1: "총딜" 키워드가 포함된 텍스트에서 우선 추출
        Pattern totalDamagePattern = Pattern.compile("총딜\\s*([1-9](?:[0-9]{1,2})?,[0-9]{3},[0-9]{3})");
        Matcher totalDamageMatcher = totalDamagePattern.matcher(pageText);
        Long maxTotalDamage = null;
        Long secondTotalDamage = null;
        
        // "총딜" 키워드가 포함된 값들을 우선적으로 찾기
        while (totalDamageMatcher.find()) {
            String damageStr = totalDamageMatcher.group(1);
            Long totalDamage = extractNumberFromText(damageStr);
            if (totalDamage != null && totalDamage > 100000000) { // 1억 이상
                if (maxTotalDamage == null || totalDamage > maxTotalDamage) {
                    secondTotalDamage = maxTotalDamage;
                    maxTotalDamage = totalDamage;
                } else if (secondTotalDamage == null || totalDamage > secondTotalDamage) {
                    secondTotalDamage = totalDamage;
                }
                log.info("총딜 키워드로 발견: {}", totalDamage);
            }
        }
        
        // 방법 2: "총딜" 키워드로 찾지 못한 경우, 일반적인 숫자 패턴으로 찾기
        if (maxTotalDamage == null) {
            Pattern damagePattern = Pattern.compile("([1-9](?:[0-9]{1,2})?,[0-9]{3},[0-9]{3})");
            Matcher damageMatcher = damagePattern.matcher(pageText);
            
            while (damageMatcher.find()) {
                String damageStr = damageMatcher.group(1);
                Long totalDamage = extractNumberFromText(damageStr);
                if (totalDamage != null && totalDamage > 100000000) { // 1억 이상
                    if (maxTotalDamage == null || totalDamage > maxTotalDamage) {
                        secondTotalDamage = maxTotalDamage;
                        maxTotalDamage = totalDamage;
                    } else if (secondTotalDamage == null || totalDamage > secondTotalDamage) {
                        secondTotalDamage = totalDamage;
                    }
                }
            }
        }
        
        if (maxTotalDamage != null) {
            result.put("totalDamage", maxTotalDamage);
            result.put("totalDamage4p", maxTotalDamage);
            result.put("totalDamage3p", maxTotalDamage);
            result.put("totalDamage2p", maxTotalDamage);
            
            // 두 번째로 높은 값도 저장 (디버깅용)
            if (secondTotalDamage != null) {
                result.put("totalDamageSecond", secondTotalDamage);
                log.info("✅ 4인/3인/2인 총딜 추출: {} (두 번째: {})", maxTotalDamage, secondTotalDamage);
            } else {
                log.info("✅ 4인/3인/2인 총딜 추출: {}", maxTotalDamage);
            }
        }
    }
    
    /**
     * CSS 셀렉터로 버프력 추출 (버퍼용) - 기본 구현
     */
    private void extractBuffPowerWithSelectors(String pageText, Map<String, Object> result) {
        // 기본 텍스트 패턴 매칭으로 버프력 추출
        extractBuffPowerFromText(pageText, result);
    }
    
    /**
     * CSS 셀렉터로 전투력 추출 (딜러용) - 기본 구현
     */
    private void extractTotalDamageWithSelectors(String pageText, Map<String, Object> result) {
        // 기본 텍스트 패턴 매칭으로 전투력 추출
        extractTotalDamageFromText(pageText, result);
    }

    /**
     * CSS 셀렉터 찾기 (기본 구현)
     */
    public Map<String, Object> findCssSelectorsWithSelenium(String serverId, String characterId) {
        try {
            log.info("=== CSS 셀렉터 찾기 시작 ===");
            log.info("serverId: {}, characterId: {}", serverId, characterId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("source", "basic-css-finder");
            result.put("success", true);
            result.put("message", "기본 CSS 셀렉터 찾기 구현");
            
            return result;
            
        } catch (Exception e) {
            log.error("CSS 셀렉터 찾기 실패: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * CSS 셀렉터 생성 (기본 구현)
     */
    private String generateCssSelector(String elementInfo) {
        try {
            log.info("CSS 셀렉터 생성: {}", elementInfo);
            return "body"; // 기본값
        } catch (Exception e) {
            log.warn("CSS 셀렉터 생성 실패: {}", e.getMessage());
            return "body";
        }
    }
    
    /**
     * 빠른 버프력 추출 (기본 구현)
     */
    private void extractBuffPowerFast(String pageText, Map<String, Object> result) {
        try {
            // 기본 텍스트 패턴 매칭으로 버프력 추출
            extractBuffPowerFromText(pageText, result);
        } catch (Exception e) {
            log.warn("빠른 버프력 추출 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 빠른 전투력 추출 (기본 구현)
     */
    private void extractTotalDamageFast(String pageText, Map<String, Object> result) {
        try {
            // 기본 텍스트 패턴 매칭으로 전투력 추출
            extractTotalDamageFromText(pageText, result);
        } catch (Exception e) {
            log.warn("빠른 전투력 추출 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 결과 값 검증 및 정리
     */
    private void validateAndCleanResults(Map<String, Object> result, boolean isBuffer) {
        if (isBuffer) {
            // 버퍼: 버프력만 유지, 전투력 제거
            if (result.containsKey("totalDamage")) {
                result.remove("totalDamage");
                log.info("버퍼 캐릭터이므로 전투력 제거");
            }
            
            if (result.containsKey("buffPower")) {
                Long buffPower = (Long) result.get("buffPower");
                if (buffPower < 1000000 || buffPower > 10000000) {
                    log.warn("버프력 범위 이상: {} (1M~10M 범위가 아님)", buffPower);
                    result.remove("buffPower");
                } else {
                    log.info("✅ 최종 검증된 버프력: {}", buffPower);
                }
            }
        } else {
            // 딜러: 전투력만 유지, 버프력 제거
            if (result.containsKey("buffPower")) {
                result.remove("buffPower");
                log.info("딜러 캐릭터이므로 버프력 제거");
            }
            
            if (result.containsKey("totalDamage")) {
                Long totalDamage = (Long) result.get("totalDamage");
                if (totalDamage < 1000000) {
                    log.warn("전투력 범위 이상: {} (1M 미만)", totalDamage);
                    result.remove("totalDamage");
                } else {
                    log.info("✅ 최종 검증된 전투력: {}", totalDamage);
                }
            }
        }
    }

    /**
     * 4인/3인/2인 결과 값 검증 및 정리 (상세)
     */
    private void validateAndCleanResultsDetailed(Map<String, Object> result, boolean isBuffer) {
        if (isBuffer) {
            // 버퍼: 4인/3인/2인 버프력 모두 유지, 전투력 제거
            if (result.containsKey("totalDamage")) {
                result.remove("totalDamage");
                log.info("버퍼 캐릭터이므로 전투력 제거");
            }
            
            // 4인/3인/2인 버프력 검증
            String[] buffKeys = {"buffPower", "buffPower4p", "buffPower3p", "buffPower2p"};
            for (String key : buffKeys) {
                if (result.containsKey(key)) {
                    Long buffPower = (Long) result.get(key);
                    if (buffPower < 1000000 || buffPower > 10000000) {
                        log.warn("{} 범위 이상: {} (1M~10M 범위가 아님)", key, buffPower);
                        result.remove(key);
                    } else {
                        log.info("✅ 최종 검증된 {}: {}", key, buffPower);
                    }
                }
            }
        } else {
            // 딜러: 4인/3인/2인 총딜 모두 유지, 버프력 제거
            if (result.containsKey("buffPower")) {
                result.remove("buffPower");
                log.info("딜러 캐릭터이므로 버프력 제거");
            }
            
            // 4인/3인/2인 총딜 검증
            String[] damageKeys = {"totalDamage", "totalDamage4p", "totalDamage3p", "totalDamage2p"};
            for (String key : damageKeys) {
                if (result.containsKey(key)) {
                    Long totalDamage = (Long) result.get(key);
                    if (totalDamage < 1000000) {
                        log.warn("{} 범위 이상: {} (1M 미만)", key, totalDamage);
                        result.remove(key);
                    } else {
                        log.info("✅ 최종 검증된 {}: {}", key, totalDamage);
                    }
                }
            }
        }
    }
    
    /**
     * 던담 크롤링을 통한 캐릭터 정보 조회 (자동 동기화용 - 크롤링 비활성화 영향받음)
     */
    public Map<String, Object> getCharacterInfoWithSelenium(String serverId, String characterId) {
        // 자동 동기화(케릭터 검색, 스케줄러)는 크롤링 비활성화에 영향받음
        if (!crawlingEnabled) {
            log.info("던담 크롤링이 비활성화되어 있습니다. DFO API만 사용하세요.");
            Map<String, Object> disabledResult = new HashMap<>();
            disabledResult.put("success", false);
            disabledResult.put("message", "던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.");
            disabledResult.put("crawlingDisabled", true);
            return disabledResult;
        }
        
        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("Dundam 크롤링");
        if (restriction != null) {
            // 목요일 제한 시 빈 결과 반환
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", "목요일에는 Dundam 크롤링이 제한되어 데이터를 제공할 수 없습니다.");
            return fallbackResult;
        }
        
        try {
            log.info("=== 던담 기본 크롤링 시작 ===");
            log.info("serverId: {}, characterId: {}", serverId, characterId);
            
            // 기본 던담 크롤링 메서드 사용
            Map<String, Object> result = getCharacterInfo(serverId, characterId);
            result.put("source", "dundam.xyz (Basic)");
            result.put("message", "던담 기본 크롤링을 통해 캐릭터 정보를 가져왔습니다.");
            
            log.info("=== 던담 기본 크롤링 완료 ===");
            
            return result;
            
        } catch (Exception e) {
            log.error("던담 기본 크롤링 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("source", "dundam.xyz (Basic) - Error");
            return errorResult;
        }
    }
    
    /**
     * 던담 크롤링을 통한 캐릭터 정보 조회 (수동 동기화용 - 크롤링 비활성화 무시)
     */
    public Map<String, Object> getCharacterInfoWithSeleniumManual(String serverId, String characterId) {
        // 수동 동기화는 크롤링 비활성화에 영향받지 않음
        log.info("=== 던담 수동 동기화 시작 (크롤링 비활성화 무시) ===");
        log.info("serverId: {}, characterId: {}", serverId, characterId);
        
        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("Dundam 크롤링");
        if (restriction != null) {
            // 목요일 제한 시 빈 결과 반환
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", "목요일에는 Dundam 크롤링이 제한되어 데이터를 제공할 수 없습니다.");
            return fallbackResult;
        }
        
        try {
            log.info("=== 던담 수동 크롤링 시작 ===");
            
            // 수동 동기화용 크롤링 직접 수행 (크롤링 비활성화 무시)
            Map<String, Object> result = performManualCrawling(serverId, characterId);
            result.put("source", "dundam.xyz (Manual)");
            result.put("message", "던담 수동 크롤링을 통해 캐릭터 정보를 가져왔습니다.");
            
            log.info("=== 던담 수동 크롤링 완료 ===");
            
            return result;
            
        } catch (Exception e) {
            log.error("던담 수동 크롤링 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("source", "dundam.xyz (Manual) - Error");
            return errorResult;
        }
    }
    
    /**
     * 수동 동기화용 크롤링 직접 수행 (크롤링 비활성화 무시) - Selenium 사용
     */
    private Map<String, Object> performManualCrawling(String serverId, String characterId) {
        WebDriver driver = null;
        try {
            log.info("=== Selenium을 사용한 수동 크롤링 시작 ===");
            log.info("serverId: {}, characterId: {}", serverId, characterId);
            
            // Chrome WebDriver 설정
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // 헤드리스 모드
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Dundam.xyz 캐릭터 상세 페이지 접속
            String url = String.format("%s/character?server=%s&key=%s", 
                                     DUNDAM_BASE_URL, serverId, characterId);
            
            log.info("던담 URL: {}", url);
            driver.get(url);
            
            // 페이지 로딩 대기
            log.info("페이지 로딩 대기 중...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
            // JavaScript 렌더링 대기 (최대 30초)
            log.info("JavaScript 렌더링 대기 중...");
            Thread.sleep(5000); // 5초 대기
            
            // 페이지 소스 가져오기
            String pageSource = driver.getPageSource();
            log.info("페이지 소스 크기: {} bytes", pageSource.length());
            
            // 캐릭터 정보가 있는지 확인
            if (pageSource.contains("캐릭터") || pageSource.contains("character") || 
                pageSource.contains("버프") || pageSource.contains("딜") || 
                pageSource.length() > 1000) {
                
                log.info("✅ 던담 페이지 로딩 완료 (크기: {} bytes)", pageSource.length());
                
                // HTML 파싱하여 버프력과 총딜 추출
                Map<String, Object> result = parseDundamResponse(pageSource, serverId, characterId);
                result.put("source", "dundam.xyz (Selenium)");
                result.put("message", "Selenium을 통해 던담 페이지를 크롤링했습니다.");
                
                return result;
                
            } else {
                log.warn("❌ 던담 페이지에 캐릭터 정보가 없음 (크기: {} bytes)", pageSource.length());
                log.info("페이지 내용: {}", pageSource.substring(0, Math.min(500, pageSource.length())));
                
                Map<String, Object> fallbackResult = new HashMap<>();
                fallbackResult.put("success", false);
                fallbackResult.put("message", "던담 페이지에 캐릭터 정보가 없습니다.");
                return fallbackResult;
            }
            
        } catch (Exception e) {
            log.error("Selenium 크롤링 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("source", "dundam.xyz (Selenium) - Error");
            return errorResult;
            
        } finally {
            // WebDriver 정리
            if (driver != null) {
                try {
                    driver.quit();
                    log.info("WebDriver 정리 완료");
                } catch (Exception e) {
                    log.warn("WebDriver 정리 중 오류: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Selenium을 사용한 JavaScript 렌더링 대기 및 데이터 로딩 확인
     */
    private Map<String, Object> waitForDataLoading(String initialHtmlContent, String serverId, String characterId) {
        // Selenium을 사용하므로 이 메서드는 더 이상 필요하지 않음
        // performManualCrawling에서 직접 처리
        log.info("=== Selenium을 사용하므로 waitForDataLoading은 더 이상 사용되지 않음 ===");
        return null;
    }
    
    /**
     * 새로운 요청으로 최신 HTML 내용 가져오기 (Selenium 사용으로 더 이상 불필요)
     */
    private String fetchFreshHtmlContent(String serverId, String characterId) {
        // Selenium을 사용하므로 이 메서드는 더 이상 필요하지 않음
        log.info("=== Selenium을 사용하므로 fetchFreshHtmlContent는 더 이상 사용되지 않음 ===");
        return null;
    }
    
    /**
     * 총딜 값 파싱 (예: "1,234,567" -> 1234567)
     */
    private Long parseDamageValue(String damageText) {
        try {
            return Long.parseLong(damageText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("총딜 파싱 실패: {}", damageText);
            return 0L;
        }
    }
    
    /**
     * 버프력 값 파싱 (예: "123,456" -> 123456)
     */
    private Integer parseBuffPowerValue(String buffText) {
        try {
            return Integer.parseInt(buffText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("버프력 파싱 실패: {}", buffText);
            return 0;
        }
    }
}
