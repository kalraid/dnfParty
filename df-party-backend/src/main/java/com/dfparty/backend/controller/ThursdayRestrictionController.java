package com.dfparty.backend.controller;

import com.dfparty.backend.service.ThursdayFallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/thursday-restriction")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThursdayRestrictionController {

    private final ThursdayFallbackService thursdayFallbackService;

    /**
     * 한국 표준시(KST) 기준 현재 시간 반환
     */
    private ZonedDateTime getCurrentTimeKST() {
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        return ZonedDateTime.now(kstZone);
    }

    /**
     * 목요일 API 제한 정보 조회
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getThursdayRestrictionInfo() {
        try {
            Map<String, Object> info = thursdayFallbackService.getThursdayRestrictionInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("목요일 제한 정보 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "목요일 제한 정보 조회에 실패했습니다.",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 목요일 API 제한 상태 확인
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkThursdayApiRestriction(
            @RequestParam String operation) {
        try {
            Map<String, Object> restriction = thursdayFallbackService.checkThursdayApiRestriction(operation);
            if (restriction != null) {
                return ResponseEntity.ok(restriction);
            } else {
                return ResponseEntity.ok(Map.of(
                    "thursdayRestriction", false,
                    "message", "API 사용 가능합니다."
                ));
            }
        } catch (Exception e) {
            log.error("목요일 API 제한 상태 확인 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "목요일 API 제한 상태 확인에 실패했습니다.",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 목요일 던전 클리어 초기화 상태 확인
     */
    @GetMapping("/reset-status")
    public ResponseEntity<Map<String, Object>> getDungeonResetStatus() {
        try {
            boolean isThursday = thursdayFallbackService.isThursday();
            boolean isNearResetTime = thursdayFallbackService.isNearThursdayResetTime();
            
            Map<String, Object> status = Map.of(
                "isThursday", isThursday,
                "isNearResetTime", isNearResetTime,
                "resetTime", "매주 목요일 오전 8시 (KST)",
                "nextReset", isThursday ? "이번 주 목요일" : "다음 주 목요일",
                "currentTime", getCurrentTimeKST(),
                "timezone", "Asia/Seoul (KST)"
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("던전 초기화 상태 확인 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "던전 초기화 상태 확인에 실패했습니다.",
                "message", e.getMessage()
            ));
        }
    }
}
