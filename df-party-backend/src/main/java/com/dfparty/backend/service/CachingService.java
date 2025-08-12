package com.dfparty.backend.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CachingService {

    // 메모리 기반 캐시 (실제 운영에서는 Redis 사용 권장)
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 캐시 엔트리 클래스
    private static class CacheEntry {
        private final Object data;
        private final LocalDateTime expiryTime;
        private final CacheType type;

        public CacheEntry(Object data, LocalDateTime expiryTime, CacheType type) {
            this.data = data;
            this.expiryTime = expiryTime;
            this.type = type;
        }

        public Object getData() { return data; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public CacheType getType() { return type; }
        public boolean isExpired() { return LocalDateTime.now().isAfter(expiryTime); }
    }

    // 캐시 타입 정의 (사용자 요구사항에 맞게 수정)
    public enum CacheType {
        SERVER_LIST(null, null),                    // 서버 목록: 초기화 시 1회만, DB 저장 후 재호출 안함
        CHARACTER_BASIC_INFO(null, null),           // 캐릭터 기본 정보: DB에 있으면 재호출 안함
        TIMELINE(1, ChronoUnit.MINUTES),            // 타임라인: 1분 캐싱
        DUNDAM_STATS(3, ChronoUnit.MINUTES);       // 던담 크롤링: 3분 캐싱

        private final Integer duration;
        private final ChronoUnit unit;

        CacheType(Integer duration, ChronoUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }

        public LocalDateTime calculateExpiryTime() {
            if (duration == null || unit == null) {
                return null; // 무제한 또는 DB 저장
            }
            return LocalDateTime.now().plus(duration, unit);
        }
    }

    /**
     * 데이터를 캐시에 저장
     */
    public void put(String key, Object data, CacheType type) {
        LocalDateTime expiryTime = type.calculateExpiryTime();
        if (expiryTime != null) { // 캐시 가능한 경우에만 저장
            cache.put(key, new CacheEntry(data, expiryTime, type));
        }
    }

    /**
     * 캐시에서 데이터 조회
     */
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // 만료된 캐시 데이터 제거
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.getData();
    }

    /**
     * 캐시에서 데이터 제거
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * 특정 타입의 캐시 데이터 모두 제거
     */
    public void removeByType(CacheType type) {
        cache.entrySet().removeIf(entry -> entry.getValue().getType() == type);
    }

    /**
     * 만료된 캐시 데이터 정리
     */
    public void cleanupExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 캐시 통계 정보
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalEntries", cache.size());
        
        // 타입별 엔트리 수 계산
        for (CacheType type : CacheType.values()) {
            long count = cache.values().stream()
                .filter(entry -> entry.getType() == type)
                .count();
            stats.put(type.name() + "Count", count);
        }

        // 만료된 엔트리 수
        long expiredCount = cache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        stats.put("expiredCount", expiredCount);

        return stats;
    }

    /**
     * 캐시 키 생성 헬퍼 메서드
     */
    public static String createKey(String prefix, String... params) {
        StringBuilder key = new StringBuilder(prefix);
        for (String param : params) {
            key.append(":").append(param);
        }
        return key.toString();
    }

    // 캐시 키 생성 메서드들
    public static String getServerListKey() {
        return "servers:list";
    }

    public static String getCharacterBasicInfoKey(String serverId, String characterId) {
        return createKey("character:basic", serverId, characterId);
    }

    public static String getTimelineKey(String serverId, String characterId) {
        return createKey("timeline", serverId, characterId);
    }

    public static String getDundamStatsKey(String serverId, String characterId) {
        return createKey("dundam:stats", serverId, characterId);
    }

    /**
     * 캐시 만료 시간 확인 (디버깅용)
     */
    public LocalDateTime getExpiryTime(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null ? entry.getExpiryTime() : null;
    }

    /**
     * 캐시 타입 확인 (디버깅용)
     */
    public CacheType getCacheType(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null ? entry.getType() : null;
    }
}
