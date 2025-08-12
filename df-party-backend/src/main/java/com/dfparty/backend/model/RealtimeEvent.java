package com.dfparty.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent {
    
    public enum EventType {
        CHARACTER_UPDATED,
        CHARACTER_DELETED,
        PARTY_CREATED,
        PARTY_UPDATED,
        PARTY_DELETED,
        PARTY_OPTIMIZED,
        RECOMMENDATION_GENERATED,
        USER_JOINED,
        USER_LEFT,
        SYSTEM_NOTIFICATION
    }
    
    private String id;
    private EventType type;
    private String targetId; // 대상 ID (캐릭터 ID, 파티 ID 등)
    private String userId; // 이벤트를 발생시킨 사용자 ID
    private Map<String, Object> data; // 이벤트 관련 데이터
    private LocalDateTime timestamp;
    private String message;
    private boolean broadcast; // 모든 사용자에게 브로드캐스트할지 여부
}
