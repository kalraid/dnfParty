package com.dfparty.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.dfparty.backend.entity.Character;
import com.dfparty.backend.repository.CharacterRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightCrawlingService {

    private final ThursdayFallbackService thursdayFallbackService;
    private final CharacterRepository characterRepository;

    // 브라우저 인스턴스 공유를 위한 정적 변수
    private static Playwright sharedPlaywright = null;
    private static Browser sharedBrowser = null;
    private static final Object browserLock = new Object();
    private static volatile boolean isInitializing = false;

    /**
     * 공유 브라우저 인스턴스 초기화
     */
    private void initializeSharedBrowser() {
        if (sharedBrowser != null && sharedBrowser.isConnected()) {
            log.info("=== 공유 브라우저 사용 중 (구간 1,2,3 생략) ===");
            return;
        }

        synchronized (browserLock) {
            if (isInitializing) {
                log.info("다른 스레드에서 브라우저 초기화 중, 대기...");
                while (isInitializing) {
                    try {
                        browserLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (sharedBrowser != null && sharedBrowser.isConnected()) {
                    log.info("다른 스레드에서 초기화 완료, 공유 브라우저 사용");
                    return;
                }
            }

            isInitializing = true;
            try {
                log.info("=== 공유 브라우저 인스턴스 초기화 시작 ===");

                // Playwright 인스턴스 생성
                log.info("=== 구간 1: Playwright 인스턴스 생성 시작 ===");
                sharedPlaywright = Playwright.create();
                log.info("=== 구간 1: Playwright 인스턴스 생성 완료 ===");

                // Chromium 브라우저 시작
                log.info("=== 구간 2: Chromium 브라우저 시작 중 ===");
                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-blink-features=AutomationControlled",
                        "--disable-features=IsolateOrigins,site-per-process",
                        "--lang=ko-KR,ko",
                        "--window-size=1366,768"
                    ));
                

                
                sharedBrowser = sharedPlaywright.chromium().launch(launchOptions);
                log.info("=== 구간 2: Chromium 브라우저 시작 완료 ===");

                log.info("=== 공유 브라우저 인스턴스 초기화 완료 ===");

            } catch (Exception e) {
                log.error("공유 브라우저 초기화 실패: {}", e.getMessage(), e);
                cleanupSharedBrowser();
                throw e;
            } finally {
                isInitializing = false;
                browserLock.notifyAll();
            }
        }
    }

    /**
     * 공유 브라우저 상태 확인
     */
    public boolean isSharedBrowserAvailable() {
        return sharedBrowser != null && sharedBrowser.isConnected();
    }

    /**
     * 공유 브라우저 정리
     */
    private void cleanupSharedBrowser() {
        if (sharedBrowser != null) {
            try {
                sharedBrowser.close();
            } catch (Exception e) {
                log.warn("브라우저 정리 중 오류: {}", e.getMessage());
            }
            sharedBrowser = null;
        }
        if (sharedPlaywright != null) {
            try {
                sharedPlaywright.close();
            } catch (Exception e) {
                log.warn("Playwright 정리 중 오류: {}", e.getMessage());
            }
            sharedPlaywright = null;
        }
    }






    private Map<String, Object> extractBuffPower(Page page) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("버프력 추출 시작 - 다양한 방법으로 시도");
            
            // 1차: React 전용 선택자로 시도
                        String[] reactSelectors = {
                            ".dvtit.buff.secend:has-text('4인 점수') + .dval.secend",
                            ".dvtit.buff.secend:has-text('3인 점수') + .dval.secend",
                            ".dvtit.buff:has-text('2인 점수') + .dval",
                            ".dvtit.buff:has-text('버프점수') + .dval",
                "span[title^='버프력']",
                "[class*='buff'] [class*='value']"
            };
            
                        String buffPower = null;
            for (String selector : reactSelectors) {
                            try {
                                buffPower = page.textContent(selector);
                                if (buffPower != null && !buffPower.trim().isEmpty()) {
                                    log.info("✅ React 선택자로 버프력 추출 성공: {} (선택자: {})", buffPower, selector);
                                    break;
                                }
                            } catch (Exception e) {
                                log.debug("선택자 실패: {} - {}", selector, e.getMessage());
                            }
                        }
                        
                        // 2차: 전체 페이지 텍스트에서 패턴 검색
                        if (buffPower == null || buffPower.trim().isEmpty()) {
                            log.info("2차: 전체 페이지 텍스트에서 버프력 패턴 검색");
                            String fullPageText = page.textContent("body");
                            
                            String[] buffPatterns = {
                                "4인 점수\\s*([0-9,]+)",
                                "3인 점수\\s*([0-9,]+)", 
                                "2인 점수\\s*([0-9,]+)",
                                "버프점수\\s*([0-9,]+)",
                                "버프력\\s*([0-9,]+)"
                            };
                            
                            for (String pattern : buffPatterns) {
                                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
                                java.util.regex.Matcher matcher = regex.matcher(fullPageText);
                                
                                if (matcher.find()) {
                                    buffPower = matcher.group(1);
                                    log.info("✅ 정규식으로 버프력 추출 성공: {} (패턴: {})", buffPower, pattern);
                                    break;
                                }
                            }
                        }
                        
                        // 결과 검증 및 반환
                        if (buffPower != null && !buffPower.trim().isEmpty()) {
                            String cleanBuffPower = buffPower.replaceAll("[^0-9]", "");
                            if (!cleanBuffPower.isEmpty()) {
                    long buffValue = Long.parseLong(cleanBuffPower);
                    result.put("buffPower", buffValue);
                    log.info("🎯 최종 버프력 추출 완료: {} (원본: {})", buffValue, buffPower);
                } else {
                    log.warn("❌ 버프력 숫자 추출 실패");
                    result.put("buffPower", 0L);
                }
            } else {
                log.warn("❌ 모든 방법으로 버프력 추출 실패");
                result.put("buffPower", 0L);
            }
                    } catch (Exception e) {
                        log.error("버프력 추출 중 예외 발생: {}", e.getMessage(), e);
            result.put("buffPower", 0L);
        }
        return result;
    }

    private Map<String, Object> extractTotalDamage(Page page) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("총딜 추출 시작 - 다양한 방법으로 시도");
            
            // 모든 총딜 값을 찾아서 가장 큰 값(최신값) 선택
            Set<Long> totalDamageValues = new HashSet<>(); // 중복 제거를 위해 Set 사용
            
            // 1. CSS 셀렉터로 총딜 값들 찾기 (우선순위 높음)
            try {
                String[] primarySelectors = {
                        ".dvtit:has-text('총딜') + .dval",
                    ".dvtit:has-text('총딜') ~ .dval",
                    "div.row > div:nth-child(2) .ng-binding",
                    "[class*='damage']",
                    "[class*='total']"
                };
                
                for (String selector : primarySelectors) {
                    if (totalDamageValues.size() >= 2) break; // 2개 이상이면 충분
                    
                    try {
                        List<ElementHandle> elements = page.querySelectorAll(selector);
                        for (ElementHandle element : elements) {
                            try {
                                String text = element.textContent();
                                if (text != null && text.matches(".*[0-9,]+.*")) {
                                    String cleanText = text.replaceAll("[^0-9]", "");
                                    if (!cleanText.isEmpty() && cleanText.length() >= 4) {
                                        long value = Long.parseLong(cleanText);
                                        totalDamageValues.add(value);
                                        log.info("✅ CSS 셀렉터로 총딜 발견: {} = {} (선택자: {})", text, value, selector);
                                        if (totalDamageValues.size() >= 2) break;
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("CSS 요소 텍스트 파싱 실패: {}", e.getMessage());
                            }
                            }
                        } catch (Exception e) {
                        log.debug("CSS 셀렉터 {} 실패: {}", selector, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("CSS 셀렉터 총딜 검색 실패: {}", e.getMessage());
            }
            
            // 2. XPath로 총딜 값들 찾기 (백업)
            if (totalDamageValues.size() < 2) {
                try {
                    String[] xpathSelectors = {
                        "//*[contains(text(),'총딜')]/following-sibling::*[contains(@class,'dval') or contains(@class,'value')]",
                        "//*[contains(text(),'총딜')]",
                        "//*[contains(@class,'damage')]//text()"
                    };
                    
                    for (String xpath : xpathSelectors) {
                        if (totalDamageValues.size() >= 2) break;
                        
                        try {
                            List<ElementHandle> xpathElements = page.querySelectorAll("xpath=" + xpath);
                            for (ElementHandle element : xpathElements) {
                                if (totalDamageValues.size() >= 2) break;
                                
                                try {
                                    String text = element.textContent();
                                    if (text != null && text.matches(".*[0-9,]+.*")) {
                                        String cleanText = text.replaceAll("[^0-9]", "");
                                        if (!cleanText.isEmpty() && cleanText.length() >= 4) {
                                            long value = Long.parseLong(cleanText);
                                            totalDamageValues.add(value);
                                            log.info("✅ XPath로 총딜 발견: {} = {} (XPath: {})", text, value, xpath);
                                            if (totalDamageValues.size() >= 2) break;
                                        }
                                    }
                                } catch (Exception e) {
                                    log.debug("XPath 요소 텍스트 파싱 실패: {}", e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.debug("XPath {} 실패: {}", xpath, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.debug("XPath 총딜 검색 실패: {}", e.getMessage());
                }
            }
            
            // 3. 텍스트 패턴으로 총딜 값들 찾기 (최후 수단)
            if (totalDamageValues.isEmpty()) {
                try {
                    String fullPageText = page.textContent("body");
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("총딜\\s*:?\\s*([0-9,]+)");
                    java.util.regex.Matcher matcher = pattern.matcher(fullPageText);
                    
                    while (matcher.find()) {
                        String match = matcher.group(1);
                        String cleanText = match.replaceAll("[^0-9]", "");
                        if (!cleanText.isEmpty()) {
                            long value = Long.parseLong(cleanText);
                            totalDamageValues.add(value);
                            log.info("✅ 텍스트 패턴으로 총딜 발견: {} = {}", match, value);
                        }
                    }
                } catch (Exception e) {
                    log.debug("텍스트 패턴 총딜 검색 실패: {}", e.getMessage());
                }
            }
            
            // 4. 최종 총딜 값 선택 (가장 큰 값 = 최신값)
            if (!totalDamageValues.isEmpty()) {
                long maxTotalDamage = totalDamageValues.stream().mapToLong(Long::longValue).max().orElse(0);
                result.put("totalDamage", maxTotalDamage);
                
                if (totalDamageValues.size() > 1) {
                    log.info("⚠️ 여러 총딜 값 발견: {} (가장 큰 값 {} 선택)", totalDamageValues, maxTotalDamage);
                } else {
                    log.info("✅ 최종 총딜 추출 완료: {} (원본: {})", maxTotalDamage, totalDamageValues.iterator().next());
                }
            } else {
                log.warn("❌ 총딜 값을 찾을 수 없음");
                result.put("totalDamage", 0L);
            }
            
        } catch (Exception e) {
            log.error("총딜 추출 중 예외 발생: {}", e.getMessage(), e);
            result.put("totalDamage", 0L);
        }
        return result;
    }


    private Map<String, Object> buildErrorResult(String message, String errorType) {
        return Map.of(
            "success", false,
            "errorMessage", message,
            "errorType", errorType,
            "source", "던담",
            "crawlingMethod", "Playwright"
        );
    }



    /**
     * Playwright를 사용한 던담 크롤링 (GPT 기반 단순화 + 최적화)
     */
    public Map<String, Object> getCharacterInfoWithPlaywright(String serverId, String characterId) {
        log.info("=== Playwright 던담 크롤링 시작 (GPT 기반 단순화 + 최적화) ===");
        log.info("serverId: {}, characterId: {}", serverId, characterId);

        // 목요일 API 제한 체크
        Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction("Playwright 던담 크롤링");
        if (restriction != null) {
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("success", false);
            fallbackResult.put("thursdayRestriction", true);
            fallbackResult.put("message", "목요일에는 Playwright 던담 크롤링이 제한되어 있습니다.");
            return fallbackResult;
        }

        BrowserContext context = null;
        Page page = null;
        
        // 공유 브라우저 초기화
        initializeSharedBrowser();

        if (sharedBrowser == null || !sharedBrowser.isConnected()) {
            log.error("공유 브라우저를 사용할 수 없습니다.");
            return buildErrorResult("공유 브라우저를 사용할 수 없습니다.", "Browser Error");
        }

        // 캐릭터 직업 정보 미리 가져오기
        log.info("=== 구간 0: 캐릭터 직업 정보 조회 ===");
        Optional<Character> characterOpt = characterRepository.findByCharacterId(characterId);
        if (characterOpt.isEmpty()) {
            log.warn("캐릭터 정보를 찾을 수 없음: {}", characterId);
            return buildErrorResult("캐릭터 정보를 찾을 수 없습니다.", "Character Not Found");
        }

        Character character = characterOpt.get();
        boolean isBuffer = isBuffer(character);
        log.info("캐릭터 {} (직업: {}) - 버퍼 여부: {}", character.getCharacterName(), character.getJobName(), isBuffer);

        // 새 브라우저 컨텍스트 및 페이지 생성
        log.info("=== 구간 1: 새 브라우저 컨텍스트 및 페이지 생성 ===");
        context = sharedBrowser.newContext(new Browser.NewContextOptions()
            .setLocale("ko-KR")
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            .setViewportSize(1366, 768)
        );
        page = context.newPage();
        page.setExtraHTTPHeaders(new HashMap<String, String>() {{
            put("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
            put("Cache-Control", "no-cache");
            put("Pragma", "no-cache");
            put("Upgrade-Insecure-Requests", "1");
        }});
        log.info("=== 구간 1: 새 브라우저 컨텍스트 및 페이지 생성 완료 ===");

        try {
            // 던담.xyz 캐릭터 페이지로 이동
            log.info("=== 구간 2: 던담 URL로 이동 시작 ===");
            String dundamUrl = String.format("https://dundam.xyz/character?server=%s&key=%s", serverId, characterId);
            log.info("던담 URL: {}", dundamUrl);

            // 페이지 로딩 타임아웃 설정
            page.setDefaultTimeout(60000); // 1분으로 단축
            
            try {
                page.navigate(dundamUrl);
                log.info("✅ 던담 URL 네비게이션 성공");
            } catch (Exception navErr) {
                log.error("던담 URL 네비게이션 실패: {}", navErr.getMessage());
                return buildErrorResult("페이지 이동에 실패했습니다: " + navErr.getMessage(), "Navigation Failed");
            }
            log.info("=== 구간 2: 페이지 이동 완료 ===");

            // 페이지 로딩 대기 (단순화된 버전)
            log.info("=== 구간 3: 페이지 로딩 대기 시작 ===");
            
            // 1. DOM 로드 완료 대기 (30초로 단축)
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
                log.info("✅ DOM 콘텐츠 로드 완료");
            } catch (Exception e) {
                log.warn("DOM 로드 대기 실패, 계속 진행: {}", e.getMessage());
            }
            
            // 2. JavaScript 실행 완료 대기 (30초로 단축)
            try {
                page.waitForFunction("() => document.readyState === 'complete'", null, new Page.WaitForFunctionOptions().setTimeout(30000));
                log.info("✅ Document readyState 완료");
            } catch (Exception e) {
                log.warn("Document readyState 대기 실패, 계속 진행: {}", e.getMessage());
            }
            
            // 3. React 앱 마운트 확인 (10초로 단축)
            try {
                page.waitForFunction("() => window.React !== undefined || document.querySelector('[data-reactroot]') !== null", null, new Page.WaitForFunctionOptions().setTimeout(10000));
                log.info("✅ React 앱 마운트 확인 완료");
                } catch (Exception e) {
                log.warn("React 앱 마운트 확인 실패, 계속 진행: {}", e.getMessage());
            }
            
            // 4. 안정화 대기 (2초)
            page.waitForTimeout(2000);
            log.info("✅ 안정화 대기 완료");
            
            log.info("=== 구간 3: 페이지 로딩 대기 완료 ===");

            // 페이지 내용 분석
            log.info("=== 구간 4: 페이지 내용 분석 시작 ===");
            String pageContent = page.content();
            log.info("=== 페이지 전체 HTML 크기: {} bytes ===", pageContent.length());

            // HTML 파일로 저장
            saveHtmlToFile(pageContent);
            log.info("✅ HTML 파일 저장 완료");

            // 페이지 텍스트 내용도 확인
            String pageText = page.textContent("body");
            log.info("=== 페이지 텍스트 내용 크기: {} 문자 ===", pageText != null ? pageText.length() : 0);
            log.info("=== 구간 4: 페이지 내용 분석 완료 ===");

            // 캐릭터 정보 추출
            log.info("=== 구간 5: 캐릭터 정보 추출 시작 ===");
            Map<String, Object> characterInfo = new HashMap<>();

            if (isBuffer) {
                // 버퍼: 버프력만 추출
                log.info("=== 구간 5-1: 버퍼용 버프력 추출 시작 ===");
                Map<String, Object> buffResult = extractBuffPower(page);
                characterInfo.putAll(buffResult);
                characterInfo.put("totalDamage", 0L); // 버퍼는 총딜 0
                log.info("=== 구간 5-1: 버퍼용 버프력 추출 완료 ===");
            } else {
                // 딜러: 총딜만 추출
                log.info("=== 구간 5-1: 딜러용 총딜 추출 시작 ===");
                Map<String, Object> damageResult = extractTotalDamage(page);
                characterInfo.putAll(damageResult);
                characterInfo.put("buffPower", 0L); // 딜러는 버프력 0
                log.info("=== 구간 5-1: 딜러용 총딜 추출 완료 ===");
            }

            log.info("=== 구간 5: 캐릭터 정보 추출 완료 ===");
            log.info("추출된 정보: {}", characterInfo);

            // 크롤링 결과 확인
            if (isBuffer && characterInfo.get("buffPower") == null) {
                log.warn("버퍼 캐릭터의 버프력을 추출하지 못했습니다.");
                return buildErrorResult("버퍼 캐릭터의 버프력을 추출할 수 없습니다.", "Buff Power Extraction Failed");
            } else if (!isBuffer && characterInfo.get("totalDamage") == null) {
                log.warn("딜러 캐릭터의 총딜을 추출하지 못했습니다.");
                return buildErrorResult("딜러 캐릭터의 총딜을 추출할 수 없습니다.", "Total Damage Extraction Failed");
            }

            // 성공 결과 반환
            log.info("=== 구간 6: 결과 정리 및 반환 ===");
            Map<String, Object> successResult = new HashMap<>();
            successResult.put("success", true);
            successResult.put("message", "던담 크롤링 성공");
            successResult.put("characterInfo", characterInfo);

            log.info("=== Playwright 던담 크롤링 완료 ===");
            log.info("최종 결과: {}", successResult);

            return successResult;

        } catch (Exception e) {
            log.error("=== Playwright 크롤링 중 오류 발생 ===");
            log.error("오류 타입: {}", e.getClass().getSimpleName());
            log.error("오류 메시지: {}", e.getMessage());
            log.error("스택 트레이스:", e);
            
            return buildErrorResult("Playwright 크롤링 중 오류가 발생했습니다: " + e.getMessage(), e.getClass().getSimpleName());
        } finally {
            log.info("=== 구간 7: 컨텍스트 정리 (브라우저는 공유 유지) ===");
            if (context != null) {
            context.close();
            log.info("컨텍스트 정리 완료");
        }
    }
}

/**
 * HTML 내용을 파일로 저장 (backend 프로젝트 폴더 내에 playwright_crawl.html로 저장)
 */
private void saveHtmlToFile(String htmlContent) {
    try {
        // backend 프로젝트 폴더 내에 저장
        String fileName = "playwright_crawl.html";
        String filePath = Paths.get(fileName).toAbsolutePath().normalize().toString();
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(htmlContent);
            log.info("✅ HTML 파일 저장 완료: {}", filePath);
        }
    } catch (IOException e) {
        log.warn("HTML 파일 저장 실패: {}", e.getMessage());
    }
}

/**
 * 버퍼 직업인지 확인
 */
private boolean isBuffer(Character character) {
    if (character == null || character.getJobName() == null) {
        return false;
    }

    String jobName = character.getJobName().toLowerCase();
    
    // 버퍼 직업 목록
    return jobName.contains("크루세이더") || 
           jobName.contains("뮤즈") || 
           jobName.contains("패러메딕") ||
           jobName.contains("헤카테");
}
}
