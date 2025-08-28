package com.dfparty.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThursdayFallbackService {

    private final DungeonClearResetService dungeonClearResetService;

    /**
     * 한국 표준시(KST) 기준 현재 시간 반환
     */
    private LocalDateTime getCurrentTimeKST() {
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        return LocalDateTime.now(kstZone);
    }

    /**
     * 목요일인지 확인 (KST 기준)
     */
    public boolean isThursday() {
        return dungeonClearResetService.isThursday();
    }

    /**
     * 목요일 오전 8시50분 전후인지 확인 (KST 기준)
     */
    public boolean isNearThursdayResetTime() {
        return dungeonClearResetService.isNearThursdayResetTime();
    }

    /**
     * 목요일 API 사용 가능 여부 확인
     * 목요일 오전 8시 ~ 10시까지만 API 사용을 제한 (KST 기준)
     */
    public boolean isApiAvailableOnThursday() {
        if (!isThursday()) {
            return true; // 목요일이 아니면 API 사용 가능
        }

        LocalTime currentTime = getCurrentTimeKST().toLocalTime();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        // 8시 ~ 10시 사이에는 API 사용 제한

        boolean isRestrictedTime = currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
        
        if (isRestrictedTime) {
            log.warn("목요일 API 제한 시간 (8:00 ~ 10:00 KST) - DB 정보만 제공");
            return false;
        }

        return true;
    }

    /**
     * 목요일 API 제한 메시지 생성
     */
    public Map<String, Object> createThursdayRestrictionMessage(String operation) {
        Map<String, Object> message = new HashMap<>();
        message.put("thursdayRestriction", true);
        message.put("operation", operation);
        message.put("message", "목요일에는 API 통신이 제한되어 DB에 저장된 정보만 제공됩니다.");
        message.put("restrictedTime", "매주 목요일 오전 8시 ~ 10시 (KST)");
        message.put("reason", "목요일에는 DFO API와 Dundam 크롤링이 불안정할 수 있어 DB 정보만 제공합니다.");
        message.put("timestamp", getCurrentTimeKST());
        
        return message;
    }

    /**
     * 목요일 API 제한 상태 확인 및 메시지 반환
     */
    public Map<String, Object> checkThursdayApiRestriction(String operation) {
        if (isApiAvailableOnThursday()) {
            return null; // API 사용 가능
        }
        
        return createThursdayRestrictionMessage(operation);
    }

    /**
     * 목요일 API 제한 시간 정보 반환
     */
    public Map<String, Object> getThursdayRestrictionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("isThursday", isThursday());
        info.put("isNearResetTime", isNearThursdayResetTime());
        info.put("isApiRestricted", !isApiAvailableOnThursday());
        info.put("restrictedTimeRange", "8:00 ~ 10:00 (KST)");
        info.put("resetTime", "8:50 (KST)");
        info.put("currentTime", getCurrentTimeKST());
        info.put("timezone", "Asia/Seoul (KST)");
        
        return info;
    }
}
