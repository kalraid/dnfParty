package com.dfparty.backend.service;

import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeleniumCrawlingService {

    private final ObjectMapper objectMapper;
    private final ThursdayFallbackService thursdayFallbackService;
    private final CharacterRepository characterRepository;

    private static final String DUNDAM_BASE_URL = "https://dundam.xyz";

    /**
     * 셀레니움 크롤링으로 던담에서 캐릭터 정보 가져오기
     */
    public Map<String, Object> getCharacterInfoWithSelenium(String serverId, String characterId) {
        log.info("=== 셀레니움 던담 크롤링 시작 ===");
        log.info("serverId: {}, characterId: {}", serverId, characterId);

        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("셀레니움 던담 크롤링");
        if (restriction != null) {
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", restriction.get("message"));
            return fallbackResult;
        }

        WebDriver driver = null;
        try {
            log.info("=== Selenium을 사용한 크롤링 시작 ===");
            
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
     * CSS 선택자 찾기 (셀레니움 사용)
     */
    public Map<String, Object> findCssSelectorsWithSelenium(String serverId, String characterId) {
        log.info("=== CSS 선택자 찾기 시작 (셀레니움) ===");
        log.info("serverId: {}, characterId: {}", serverId, characterId);

        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("CSS 선택자 찾기");
        if (restriction != null) {
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", restriction.get("message"));
            return fallbackResult;
        }

        WebDriver driver = null;
        try {
            log.info("=== CSS 선택자 찾기 시작 ===");
            
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
            
            // CSS 선택자 찾기 로직
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "CSS 선택자 찾기 완료");
            result.put("pageSource", pageSource);
            result.put("source", "dundam.xyz (Selenium CSS 선택자 찾기)");
            
            return result;
            
        } catch (Exception e) {
            log.error("CSS 선택자 찾기 중 예외 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "CSS 선택자 찾기 중 오류가 발생했습니다: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    log.info("WebDriver 종료 완료");
                } catch (Exception e) {
                    log.warn("WebDriver 종료 중 오류: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 던담 HTML 응답 파싱
     */
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
        String[] bufferJobs = {"뮤즈", "패러메딕", "크루세이더", "인챈트리스", "헤카테"};
        
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
}
