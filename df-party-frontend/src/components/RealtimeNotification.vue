<template>
  <div class="realtime-notification">
    <!-- 연결 상태 표시 -->
    <div class="connection-status" :class="{ connected: isConnected }">
      <span class="status-dot"></span>
      {{ isConnected ? '실시간 연결됨' : '연결 중...' }}
    </div>

    <!-- 실시간 알림 목록 -->
    <div class="notification-list" v-if="notifications.length > 0">
      <div 
        v-for="notification in notifications" 
        :key="notification.id"
        class="notification-item"
        :class="getNotificationClass(notification.type)"
        @click="removeNotification(notification.id)"
      >
        <div class="notification-header">
          <span class="notification-type">{{ getNotificationTypeText(notification.type) }}</span>
          <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
        </div>
        <div class="notification-message">{{ notification.message }}</div>
        <div class="notification-data" v-if="notification.data">
          <pre>{{ JSON.stringify(notification.data, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- 실시간 채팅 -->
    <div class="chat-section" v-if="showChat">
      <div class="chat-header">
        <h4>실시간 채팅</h4>
        <button @click="showChat = false" class="close-btn">×</button>
      </div>
      <div class="chat-messages" ref="chatMessages">
        <div 
          v-for="message in chatMessages" 
          :key="message.id"
          class="chat-message"
          :class="{ 'own-message': message.userId === currentUserId }"
        >
          <span class="username">{{ message.username }}</span>
          <span class="message">{{ message.message }}</span>
          <span class="time">{{ formatTime(message.timestamp) }}</span>
        </div>
      </div>
      <div class="chat-input">
        <input 
          v-model="chatInput" 
          @keyup.enter="sendChatMessage"
          placeholder="메시지를 입력하세요..."
          class="chat-input-field"
        />
        <button @click="sendChatMessage" class="send-btn">전송</button>
      </div>
    </div>

    <!-- 채팅 토글 버튼 -->
    <button @click="toggleChat" class="chat-toggle-btn">
      {{ showChat ? '채팅 숨기기' : '채팅 보기' }}
    </button>

    <!-- 알림 설정 -->
    <div class="notification-settings">
      <h4>알림 설정</h4>
      <label>
        <input type="checkbox" v-model="settings.characterUpdates" />
        캐릭터 업데이트 알림
      </label>
      <label>
        <input type="checkbox" v-model="settings.partyUpdates" />
        파티 업데이트 알림
      </label>
      <label>
        <input type="checkbox" v-model="settings.recommendations" />
        추천 생성 알림
      </label>
      <label>
        <input type="checkbox" v-model="settings.userStatus" />
        사용자 상태 알림
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import websocketService, { type RealtimeEvent } from '@/services/websocketService'

interface Notification extends RealtimeEvent {
  timestamp: string
}

interface ChatMessage {
  id: string
  userId: string
  username: string
  message: string
  timestamp: string
}

interface NotificationSettings {
  characterUpdates: boolean
  partyUpdates: boolean
  recommendations: boolean
  userStatus: boolean
}

const isConnected = ref(false)
const notifications = ref<Notification[]>([])
const chatMessages = ref<ChatMessage[]>([])
const showChat = ref(false)
const chatInput = ref('')
const currentUserId = ref('')
const chatMessages = ref<ChatMessage[]>([])
const chatMessagesRef = ref<HTMLElement>()

const settings = ref<NotificationSettings>({
  characterUpdates: true,
  partyUpdates: true,
  recommendations: true,
  userStatus: true
})

// WebSocket 연결
const connectWebSocket = async () => {
  try {
    await websocketService.connect()
    isConnected.value = true
    
    // 이벤트 리스너 등록
    websocketService.addEventListener('*', handleRealtimeEvent)
    
    // 연결 상태 모니터링
    const status = websocketService.getConnectionStatus()
    isConnected.value = status.value
  } catch (error) {
    console.error('WebSocket 연결 실패:', error)
  }
}

// 실시간 이벤트 처리
const handleRealtimeEvent = (event: RealtimeEvent) => {
  // 알림 설정에 따라 필터링
  if (!shouldShowNotification(event)) {
    return
  }

  // 알림 목록에 추가
  const notification: Notification = {
    ...event,
    timestamp: new Date().toISOString()
  }
  
  notifications.value.unshift(notification)
  
  // 최대 50개까지만 유지
  if (notifications.value.length > 50) {
    notifications.value = notifications.value.slice(0, 50)
  }

  // 채팅 메시지인 경우 채팅 목록에도 추가
  if (event.type === 'SYSTEM_NOTIFICATION' && event.message.includes(':')) {
    const chatMessage: ChatMessage = {
      id: event.id,
      userId: event.userId,
      username: event.message.split(':')[0],
      message: event.message.split(':')[1]?.trim() || '',
      timestamp: new Date().toISOString()
    }
    
    chatMessages.value.push(chatMessage)
    
    // 최대 100개까지만 유지
    if (chatMessages.value.length > 100) {
      chatMessages.value = chatMessages.value.slice(-100)
    }
    
    // 채팅 스크롤을 맨 아래로
    nextTick(() => {
      if (chatMessagesRef.value) {
        chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
      }
    })
  }
}

// 알림 표시 여부 결정
const shouldShowNotification = (event: RealtimeEvent): boolean => {
  switch (event.type) {
    case 'CHARACTER_UPDATED':
    case 'CHARACTER_DELETED':
      return settings.value.characterUpdates
    case 'PARTY_CREATED':
    case 'PARTY_UPDATED':
    case 'PARTY_DELETED':
    case 'PARTY_OPTIMIZED':
      return settings.value.partyUpdates
    case 'RECOMMENDATION_GENERATED':
      return settings.value.recommendations
    case 'USER_JOINED':
    case 'USER_LEFT':
      return settings.value.userStatus
    default:
      return true
  }
}

// 알림 제거
const removeNotification = (id: string) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 알림 타입별 CSS 클래스
const getNotificationClass = (type: string): string => {
  switch (type) {
    case 'CHARACTER_UPDATED':
      return 'character-update'
    case 'PARTY_CREATED':
    case 'PARTY_UPDATED':
      return 'party-update'
    case 'RECOMMENDATION_GENERATED':
      return 'recommendation'
    case 'USER_JOINED':
      return 'user-joined'
    case 'USER_LEFT':
      return 'user-left'
    default:
      return 'system'
  }
}

// 알림 타입 텍스트
const getNotificationTypeText = (type: string): string => {
  switch (type) {
    case 'CHARACTER_UPDATED':
      return '캐릭터 업데이트'
    case 'PARTY_CREATED':
      return '파티 생성'
    case 'PARTY_UPDATED':
      return '파티 업데이트'
    case 'RECOMMENDATION_GENERATED':
      return '추천 생성'
    case 'USER_JOINED':
      return '사용자 접속'
    case 'USER_LEFT':
      return '사용자 접속 해제'
    default:
      return '시스템 알림'
  }
}

// 시간 포맷팅
const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) { // 1분 이내
    return '방금 전'
  } else if (diff < 3600000) { // 1시간 이내
    return `${Math.floor(diff / 60000)}분 전`
  } else if (diff < 86400000) { // 1일 이내
    return `${Math.floor(diff / 3600000)}시간 전`
  } else {
    return date.toLocaleDateString()
  }
}

// 채팅 토글
const toggleChat = () => {
  showChat.value = !showChat.value
}

// 채팅 메시지 전송
const sendChatMessage = () => {
  if (chatInput.value.trim()) {
    websocketService.sendChatMessage(chatInput.value.trim())
    chatInput.value = ''
  }
}

// 컴포넌트 마운트 시 WebSocket 연결
onMounted(() => {
  connectWebSocket()
})

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  websocketService.removeEventListener('*', handleRealtimeEvent)
  websocketService.disconnect()
})
</script>

<style scoped>
.realtime-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 350px;
  max-height: 80vh;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.connection-status {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  font-size: 14px;
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dc3545;
  margin-right: 8px;
  transition: background-color 0.3s;
}

.status-dot.connected {
  background: #28a745;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
}

.notification-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid #007bff;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.2s;
}

.notification-item:hover {
  background: #e9ecef;
  transform: translateX(2px);
}

.notification-item.character-update {
  border-left-color: #28a745;
}

.notification-item.party-update {
  border-left-color: #ffc107;
}

.notification-item.recommendation {
  border-left-color: #17a2b8;
}

.notification-item.user-joined {
  border-left-color: #28a745;
}

.notification-item.user-left {
  border-left-color: #dc3545;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notification-type {
  font-size: 12px;
  font-weight: 600;
  color: #495057;
}

.notification-time {
  font-size: 11px;
  color: #6c757d;
}

.notification-message {
  font-size: 14px;
  color: #212529;
  margin-bottom: 8px;
}

.notification-data {
  font-size: 11px;
  color: #6c757d;
  background: #e9ecef;
  padding: 8px;
  border-radius: 4px;
  max-height: 100px;
  overflow-y: auto;
}

.notification-data pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.chat-section {
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
}

.chat-header h4 {
  margin: 0;
  font-size: 16px;
  color: #495057;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #6c757d;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #495057;
}

.chat-messages {
  height: 200px;
  overflow-y: auto;
  padding: 12px;
}

.chat-message {
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: white;
  max-width: 80%;
}

.chat-message.own-message {
  background: #007bff;
  color: white;
  margin-left: auto;
}

.username {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
  display: block;
}

.message {
  font-size: 14px;
  word-break: break-word;
}

.time {
  font-size: 10px;
  opacity: 0.7;
  margin-top: 4px;
  display: block;
}

.chat-input {
  display: flex;
  padding: 12px;
  border-top: 1px solid #e9ecef;
}

.chat-input-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  margin-right: 8px;
}

.chat-input-field:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.send-btn {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.send-btn:hover {
  background: #0056b3;
}

.chat-toggle-btn {
  width: 100%;
  padding: 12px;
  background: #6c757d;
  color: white;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.chat-toggle-btn:hover {
  background: #545b62;
}

.notification-settings {
  padding: 16px;
  border-top: 1px solid #e9ecef;
  background: white;
}

.notification-settings h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #495057;
}

.notification-settings label {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #495057;
  cursor: pointer;
}

.notification-settings input[type="checkbox"] {
  margin-right: 8px;
}

/* 스크롤바 스타일링 */
.notification-list::-webkit-scrollbar,
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.notification-list::-webkit-scrollbar-track,
.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb,
.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb:hover,
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
