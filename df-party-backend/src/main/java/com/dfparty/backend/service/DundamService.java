package com.dfparty.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DundamService {

    // 새로운 크롤링 서비스들
    private final HtmlCrawlingService htmlCrawlingService;
    private final SeleniumCrawlingService seleniumCrawlingService;
    private final PlaywrightCrawlingService playwrightCrawlingService;
    
    /**
     * 메서드에 따라 던담 크롤링 방식 선택 (통합 API용)
     */
    public Map<String, Object> getCharacterInfoWithMethod(String serverId, String characterId, String method) {
        log.info("=== 던담 크롤링 시작 (메서드: {}) ===", method);
            log.info("serverId: {}, characterId: {}", serverId, characterId);
            
        if ("selenium".equalsIgnoreCase(method)) {
            // 셀레니움 크롤링 서비스 호출
            return seleniumCrawlingService.getCharacterInfoWithSelenium(serverId, characterId);
        } else if ("playwright".equalsIgnoreCase(method)) {
            // Playwright 크롤링 서비스 호출
            return playwrightCrawlingService.getCharacterInfoWithPlaywright(serverId, characterId);
        } else if ("html".equalsIgnoreCase(method)) {
            // HTML 크롤링 서비스 호출
            return htmlCrawlingService.getCharacterInfoWithHtml(serverId, characterId);
                } else {
            // 잘못된 메서드
            log.error("잘못된 크롤링 메서드: {}", method);
            Map<String, Object> invalidMethodResult = new HashMap<>();
            invalidMethodResult.put("success", false);
            invalidMethodResult.put("message", "잘못된 크롤링 메서드입니다. 'selenium', 'playwright', 또는 'html'을 지정해주세요.");
            invalidMethodResult.put("invalidMethod", true);
            invalidMethodResult.put("method", method);
            return invalidMethodResult;
        }
    }

    // 기존 코드와의 호환성을 위한 메서드들
    
    /**
     * 기본 던담 크롤링 (기본값: Playwright 사용)
     */
    public Map<String, Object> getCharacterInfo(String serverId, String characterId) {
        log.info("기본 던담 크롤링 호출 - Playwright 사용");
        return getCharacterInfoWithMethod(serverId, characterId, "playwright");
    }
    
    /**
     * 셀레니움 던담 크롤링
     */
    public Map<String, Object> getCharacterInfoWithSelenium(String serverId, String characterId) {
        log.info("셀레니움 던담 크롤링 호출");
        return getCharacterInfoWithMethod(serverId, characterId, "selenium");
    }
    
    /**
     * Playwright 던담 크롤링
     */
    public Map<String, Object> getCharacterInfoWithPlaywright(String serverId, String characterId) {
        log.info("Playwright 던담 크롤링 호출");
        return getCharacterInfoWithMethod(serverId, characterId, "playwright");
    }
    
    /**
     * Playwright 빠른 던담 크롤링
     */
    public Map<String, Object> getCharacterInfoWithPlaywrightFast(String serverId, String characterId) {
        log.info("Playwright 빠른 던담 크롤링 호출");
        return getCharacterInfoWithMethod(serverId, characterId, "playwright");
    }
    
    /**
     * WebDriver 던담 크롤링
     */
    public Map<String, Object> getCharacterInfoWithWebDriver(String serverId, String characterId) {
        log.info("WebDriver 던담 크롤링 호출 - 셀레니움 사용");
        return getCharacterInfoWithMethod(serverId, characterId, "selenium");
    }
    
    /**
     * CSS 선택자 찾기 (셀레니움 사용)
     */
    public Map<String, Object> findCssSelectorsWithSelenium(String serverId, String characterId) {
        log.info("CSS 선택자 찾기 호출 - 셀레니움 사용");
        return seleniumCrawlingService.findCssSelectorsWithSelenium(serverId, characterId);
    }

    @PreDestroy
    public void destroy() {
        // 새로운 크롤링 서비스들에서 정리 작업 수행
        log.info("DundamService 종료");
    }
}
