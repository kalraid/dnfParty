package com.dfparty.backend.service;

import com.dfparty.backend.controller.SseController;
import com.dfparty.backend.model.RealtimeEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {
    
    private final SseController sseController;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<RealtimeEvent>> eventHistory = new ConcurrentHashMap<>();
    
    public RealtimeEventService(SseController sseController) {
        this.sseController = sseController;
    }
    
    public void sendEvent(RealtimeEvent event) {
        try {
            System.out.println("이벤트 전송 시도: " + event);
            sseController.sendEventToAll(event);
            System.out.println("이벤트 전송 성공: " + event.getId());
            
            // 이벤트 히스토리에 저장
            String userId = event.getUserId();
            eventHistory.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(event);
            
            // 히스토리 크기 제한 (최근 100개만 유지)
            if (eventHistory.get(userId).size() > 100) {
                eventHistory.get(userId).remove(0);
            }
            
        } catch (Exception e) {
            System.err.println("이벤트 전송 실패: " + event + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendEventToUser(String userId, RealtimeEvent event) {
        try {
            System.out.println("사용자별 이벤트 전송 시도: userId=" + userId + ", eventId=" + event.getId());
            sseController.sendEventToUser(userId, event);
            System.out.println("사용자별 이벤트 전송 성공: userId=" + userId + ", eventId=" + event.getId());
            
            // 이벤트 히스토리에 저장
            eventHistory.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(event);
            
            // 히스토리 크기 제한
            if (eventHistory.get(userId).size() > 100) {
                eventHistory.get(userId).remove(0);
            }
            
        } catch (Exception e) {
            System.err.println("사용자별 이벤트 전송 실패: userId=" + userId + ", eventId=" + event.getId() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendEventToTopic(String topic, RealtimeEvent event) {
        try {
            System.out.println("토픽별 이벤트 전송 시도: topic=" + topic + ", eventId=" + event.getId());
            sseController.sendEventToTopic(topic, event);
            System.out.println("토픽별 이벤트 전송 성공: topic=" + topic + ", eventId=" + event.getId());
        } catch (Exception e) {
            System.err.println("토픽별 이벤트 전송 실패: topic=" + topic + ", eventId=" + event.getId() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
        
        sendEvent(event);
    }
    
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
        
        sendEvent(event);
    }
    
    public void sendSystemNotification(String message, Map<String, Object> data) {
        RealtimeEvent event = RealtimeEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(RealtimeEvent.EventType.SYSTEM_NOTIFICATION)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .broadcast(true)
                .build();
        
        sendEvent(event);
    }
    
    public CopyOnWriteArrayList<RealtimeEvent> getUserEventHistory(String userId) {
        return eventHistory.getOrDefault(userId, new CopyOnWriteArrayList<>());
    }
    
    public void clearUserEventHistory(String userId) {
        eventHistory.remove(userId);
        System.out.println("사용자 이벤트 히스토리 삭제: userId=" + userId);
    }
}
