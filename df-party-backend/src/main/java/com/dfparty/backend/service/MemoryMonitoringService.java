package com.dfparty.backend.service;

import com.dfparty.backend.controller.SseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Slf4j
@Service
public class MemoryMonitoringService {
    
    private final PlaywrightCrawlingService playwrightService;
    private final SseController sseController;
    
    @Autowired
    public MemoryMonitoringService(PlaywrightCrawlingService playwrightService, SseController sseController) {
        this.playwrightService = playwrightService;
        this.sseController = sseController;
    }
    
    /**
     * 5분마다 메모리 사용량 모니터링
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void monitorMemoryUsage() {
        try {
            log.info("=== 메모리 사용량 모니터링 시작 ===");
            
            // JVM 메모리 정보
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
            
            // 힙 메모리 정보
            long heapUsed = heapMemoryUsage.getUsed();
            long heapMax = heapMemoryUsage.getMax();
            long heapCommitted = heapMemoryUsage.getCommitted();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            // 비힙 메모리 정보
            long nonHeapUsed = nonHeapMemoryUsage.getUsed();
            long nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
            
            log.info("=== JVM 메모리 현황 ===");
            log.info("힙 메모리 사용량: {} MB / {} MB ({}%)", 
                heapUsed / (1024 * 1024), 
                heapMax / (1024 * 1024), 
                String.format("%.2f", heapUsagePercent));
            log.info("힙 메모리 커밋: {} MB", heapCommitted / (1024 * 1024));
            log.info("비힙 메모리 사용량: {} MB", nonHeapUsed / (1024 * 1024));
            log.info("비힙 메모리 커밋: {} MB", nonHeapCommitted / (1024 * 1024));
            
            // OOM 위험 감지
            if (heapUsagePercent > 80) {
                log.warn("⚠️ 메모리 사용률이 80%를 초과했습니다: {}%", String.format("%.2f", heapUsagePercent));
                
                if (heapUsagePercent > 90) {
                    log.error("🚨 메모리 사용률이 90%를 초과했습니다: {}% - OOM 위험!", String.format("%.2f", heapUsagePercent));
                    // 강제 가비지 컬렉션 실행
                    System.gc();
                    log.info("강제 가비지 컬렉션 실행 완료");
                }
            }
            
            // Playwright 서비스 메모리 모니터링
            if (playwrightService != null) {
                playwrightService.logMemoryUsage();
            }
            
            // SSE 컨트롤러 메모리 모니터링
            if (sseController != null) {
                sseController.logMemoryUsage();
            }
            
            log.info("=== 메모리 사용량 모니터링 완료 ===");
            
        } catch (Exception e) {
            log.error("메모리 모니터링 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 1분마다 메모리 사용량 체크 (긴급 상황)
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void checkMemoryUrgency() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            
            long heapUsed = heapMemoryUsage.getUsed();
            long heapMax = heapMemoryUsage.getMax();
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            
            // 95% 이상이면 긴급 상황
            if (heapUsagePercent > 95) {
                log.error("🚨🚨 긴급: 메모리 사용률이 95%를 초과했습니다: {}%", String.format("%.2f", heapUsagePercent));
                log.error("🚨🚨 시스템 재시작을 고려하세요!");
                
                // 긴급 가비지 컬렉션
                System.gc();
                log.info("긴급 가비지 컬렉션 실행 완료");
            }
        } catch (Exception e) {
            log.error("긴급 메모리 체크 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * 현재 메모리 상태를 Map으로 반환
     */
    public java.util.Map<String, Object> getCurrentMemoryStatus() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
            
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("heapUsed", heapMemoryUsage.getUsed() / (1024 * 1024)); // MB
            status.put("heapMax", heapMemoryUsage.getMax() / (1024 * 1024)); // MB
            status.put("heapCommitted", heapMemoryUsage.getCommitted() / (1024 * 1024)); // MB
            status.put("heapUsagePercent", (double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax() * 100);
            status.put("nonHeapUsed", nonHeapMemoryUsage.getUsed() / (1024 * 1024)); // MB
            status.put("nonHeapCommitted", nonHeapMemoryUsage.getCommitted() / (1024 * 1024)); // MB
            status.put("timestamp", System.currentTimeMillis());
            
            return status;
        } catch (Exception e) {
            log.error("메모리 상태 조회 중 오류 발생: {}", e.getMessage());
            return java.util.Map.of("error", e.getMessage());
        }
    }
}
