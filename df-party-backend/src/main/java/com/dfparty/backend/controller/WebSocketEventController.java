package com.dfparty.backend.controller;

import com.dfparty.backend.model.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketEventController {
    
    /**
     * 사용자 연결 이벤트 처리
     */
    @MessageMapping("/user.join")
    @SendTo("/topic/user.status")
    public RealtimeEvent handleUserJoin(@Payload Map<String, Object> payload, 
                                      SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) payload.get("userId");
        String username = (String) payload.get("username");
        
        log.info("사용자 {} ({}) 연결됨", username, userId);
        
        return RealtimeEvent.builder()
                .type(RealtimeEvent.EventType.USER_JOINED)
                .userId(userId)
                .message(username + "님이 접속했습니다.")
                .broadcast(true)
                .build();
    }
    
    /**
     * 사용자 연결 해제 이벤트 처리
     */
    @MessageMapping("/user.leave")
    @SendTo("/topic/user.status")
    public RealtimeEvent handleUserLeave(@Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String username = (String) payload.get("username");
        
        log.info("사용자 {} ({}) 연결 해제됨", username, userId);
        
        return RealtimeEvent.builder()
                .type(RealtimeEvent.EventType.USER_LEFT)
                .userId(userId)
                .message(username + "님이 접속을 해제했습니다.")
                .broadcast(true)
                .build();
    }
    
    /**
     * 실시간 채팅 메시지 처리
     */
    @MessageMapping("/chat.message")
    @SendTo("/topic/chat")
    public RealtimeEvent handleChatMessage(@Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String username = (String) payload.get("username");
        String message = (String) payload.get("message");
        
        log.info("채팅 메시지: {} - {}", username, message);
        
        return RealtimeEvent.builder()
                .type(RealtimeEvent.EventType.SYSTEM_NOTIFICATION)
                .userId(userId)
                .message(username + ": " + message)
                .broadcast(true)
                .build();
    }
    
    /**
     * 파티 상태 업데이트 요청 처리
     */
    @MessageMapping("/party.status.request")
    @SendTo("/topic/party.status")
    public RealtimeEvent handlePartyStatusRequest(@Payload Map<String, Object> payload) {
        String partyId = (String) payload.get("partyId");
        String userId = (String) payload.get("userId");
        
        log.info("파티 {} 상태 업데이트 요청: {}", partyId, userId);
        
        return RealtimeEvent.builder()
                .type(RealtimeEvent.EventType.PARTY_UPDATED)
                .targetId(partyId)
                .userId(userId)
                .message("파티 상태 업데이트 요청")
                .broadcast(false)
                .build();
    }
    
    /**
     * 실시간 알림 요청 처리
     */
    @MessageMapping("/notification.request")
    public void handleNotificationRequest(@Payload Map<String, Object> payload, 
                                       SimpMessageHeaderAccessor headerAccessor) {
        String userId = (String) payload.get("userId");
        String notificationType = (String) payload.get("type");
        
        log.info("사용자 {} 알림 요청: {}", userId, notificationType);
        
        // 사용자별 알림 설정에 따라 처리
        // 실제 구현에서는 사용자 설정을 확인하고 적절한 알림을 전송
    }
}
