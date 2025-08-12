package com.dfparty.backend.service;

import com.dfparty.backend.model.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 특정 사용자에게 이벤트 전송
     */
    public void sendEventToUser(String userId, RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/events", event);
            log.info("이벤트를 사용자 {}에게 전송: {}", userId, event.getType());
        } catch (Exception e) {
            log.error("사용자 {}에게 이벤트 전송 실패: {}", userId, e.getMessage());
        }
    }
    
    /**
     * 모든 사용자에게 브로드캐스트
     */
    public void broadcastEvent(RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/events", event);
            log.info("이벤트 브로드캐스트: {}", event.getType());
        } catch (Exception e) {
            log.error("이벤트 브로드캐스트 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 토픽으로 이벤트 전송
     */
    public void sendEventToTopic(String topic, RealtimeEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, event);
            log.info("토픽 {}으로 이벤트 전송: {}", topic, event.getType());
        } catch (Exception e) {
            log.error("토픽 {}으로 이벤트 전송 실패: {}", topic, e.getMessage());
        }
    }
    
    /**
     * 캐릭터 업데이트 이벤트 생성 및 전송
     */
    public void notifyCharacterUpdated(String characterId, String userId, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.CHARACTER_UPDATED)
                .targetId(characterId)
                .userId(userId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .message("캐릭터 정보가 업데이트되었습니다.")
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
    
    /**
     * 파티 생성 이벤트 생성 및 전송
     */
    public void notifyPartyCreated(String partyId, String userId, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.PARTY_CREATED)
                .targetId(partyId)
                .userId(userId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .message("새로운 파티가 생성되었습니다.")
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
    
    /**
     * 파티 업데이트 이벤트 생성 및 전송
     */
    public void notifyPartyUpdated(String partyId, String userId, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.PARTY_UPDATED)
                .targetId(partyId)
                .userId(userId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .message("파티 정보가 업데이트되었습니다.")
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
    
    /**
     * 파티 최적화 이벤트 생성 및 전송
     */
    public void notifyPartyOptimized(String partyId, String userId, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.PARTY_OPTIMIZED)
                .targetId(partyId)
                .userId(userId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .message("파티가 최적화되었습니다.")
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
    
    /**
     * 추천 생성 이벤트 생성 및 전송
     */
    public void notifyRecommendationGenerated(String recommendationId, String userId, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.RECOMMENDATION_GENERATED)
                .targetId(recommendationId)
                .userId(userId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .message("새로운 파티 추천이 생성되었습니다.")
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
    
    /**
     * 시스템 알림 전송
     */
    public void sendSystemNotification(String message, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.SYSTEM_NOTIFICATION)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .broadcast(true)
                .build();
        
        broadcastEvent(event);
    }
}
