import { ref, onUnmounted } from 'vue'
import { getWsUrl } from '../config/api'
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

export interface RealtimeEvent {
  id: string
  type: 'CHARACTER_UPDATED' | 'CHARACTER_DELETED' | 'PARTY_CREATED' | 'PARTY_UPDATED' | 'PARTY_DELETED' | 'PARTY_OPTIMIZED' | 'RECOMMENDATION_GENERATED' | 'USER_JOINED' | 'USER_LEFT' | 'SYSTEM_NOTIFICATION'
  targetId?: string
  userId: string
  data?: Record<string, any>
  timestamp: string
  message: string
  broadcast: boolean
}

class WebSocketService {
  private stompClient: any = null
  private isConnected = ref(false)
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private eventListeners: Map<string, ((event: RealtimeEvent) => void)[]> = new Map()
  private userId: string = 'anonymous'
  private username: string = '사용자'

  constructor() {
    this.userId = this.generateUserId()
    this.username = `사용자_${this.userId.slice(-4)}`
  }

  /**
   * WebSocket 연결
   */
  async connect(): Promise<void> {
    try {
      if (this.stompClient && this.stompClient.connected) {
        console.log('이미 연결되어 있습니다.')
        return
      }

      let wsUrl = `${getWsUrl('')}/ws`
      
      // 프로덕션에서 HTTP를 WSS로 변환
      if (!import.meta.env.DEV && wsUrl.startsWith('http://')) {
        wsUrl = wsUrl.replace('http://', 'wss://')
        console.log('🔄 프로덕션 환경에서 WSS로 변환됨')
      } else if (!import.meta.env.DEV && wsUrl.startsWith('https://')) {
        wsUrl = wsUrl.replace('https://', 'wss://')
        console.log('🔄 프로덕션 환경에서 WSS로 변환됨')
      }
      
      const socket = new SockJS(wsUrl, null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling']
      })
      this.stompClient = Stomp.over(socket)
      
      // STOMP 디버그 비활성화
      this.stompClient.debug = undefined

      return new Promise((resolve, reject) => {
        this.stompClient.connect(
          {},
          () => {
            console.log('WebSocket 연결 성공')
            this.isConnected.value = true
            this.reconnectAttempts = 0
            this.subscribeToTopics()
            this.notifyUserJoin()
            resolve()
          },
          (error: any) => {
            console.error('WebSocket 연결 실패:', error)
            this.isConnected.value = false
            this.handleReconnection()
            reject(error)
          }
        )
      })
    } catch (error) {
      console.error('WebSocket 연결 중 오류:', error)
      this.handleReconnection()
      throw error
    }
  }

  /**
   * WebSocket 연결 해제
   */
  disconnect(): void {
    if (this.stompClient) {
      this.notifyUserLeave()
      this.stompClient.disconnect()
      this.stompClient = null
      this.isConnected.value = false
      console.log('WebSocket 연결 해제')
    }
  }

  /**
   * 토픽 구독
   */
  private subscribeToTopics(): void {
    // 일반 이벤트 구독
    this.stompClient.subscribe('/topic/events', (message: any) => {
      try {
        const event: RealtimeEvent = JSON.parse(message.body)
        this.handleEvent(event)
      } catch (error) {
        console.error('이벤트 파싱 오류:', error)
      }
    })

    // 사용자 상태 구독
    this.stompClient.subscribe('/topic/user.status', (message: any) => {
      try {
        const event: RealtimeEvent = JSON.parse(message.body)
        this.handleEvent(event)
      } catch (error) {
        console.error('사용자 상태 이벤트 파싱 오류:', error)
      }
    })

    // 파티 상태 구독
    this.stompClient.subscribe('/topic/party.status', (message: any) => {
      try {
        const event: RealtimeEvent = JSON.parse(message.body)
        this.handleEvent(event)
      } catch (error) {
        console.error('파티 상태 이벤트 파싱 오류:', error)
      }
    })

    // 채팅 구독
    this.stompClient.subscribe('/topic/chat', (message: any) => {
      try {
        const event: RealtimeEvent = JSON.parse(message.body)
        this.handleEvent(event)
      } catch (error) {
        console.error('채팅 이벤트 파싱 오류:', error)
      }
    })

    // 개인 이벤트 구독
    this.stompClient.subscribe(`/user/${this.userId}/queue/events`, (message: any) => {
      try {
        const event: RealtimeEvent = JSON.parse(message.body)
        this.handleEvent(event)
      } catch (error) {
        console.error('개인 이벤트 파싱 오류:', error)
      }
    })
  }

  /**
   * 이벤트 처리
   */
  private handleEvent(event: RealtimeEvent): void {
    console.log('실시간 이벤트 수신:', event)
    
    // 이벤트 타입별 리스너 호출
    const listeners = this.eventListeners.get(event.type)
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(event)
        } catch (error) {
          console.error('이벤트 리스너 실행 오류:', error)
        }
      })
    }

    // 모든 이벤트 리스너 호출
    const allListeners = this.eventListeners.get('*')
    if (allListeners) {
      allListeners.forEach(listener => {
        try {
          listener(event)
        } catch (error) {
          console.error('전체 이벤트 리스너 실행 오류:', error)
        }
      })
    }
  }

  /**
   * 사용자 접속 알림
   */
  private notifyUserJoin(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/user.join', {}, JSON.stringify({
        userId: this.userId,
        username: this.username
      }))
    }
  }

  /**
   * 사용자 접속 해제 알림
   */
  private notifyUserLeave(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/user.leave', {}, JSON.stringify({
        userId: this.userId,
        username: this.username
      }))
    }
  }

  /**
   * 채팅 메시지 전송
   */
  sendChatMessage(message: string): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/chat.message', {}, JSON.stringify({
        userId: this.userId,
        username: this.username,
        message: message
      }))
    }
  }

  /**
   * 파티 상태 업데이트 요청
   */
  requestPartyStatusUpdate(partyId: string): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/party.status.request', {}, JSON.stringify({
        partyId: partyId,
        userId: this.userId
      }))
    }
  }

  /**
   * 알림 요청
   */
  requestNotification(type: string): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send('/app/notification.request', {}, JSON.stringify({
        userId: this.userId,
        type: type
      }))
    }
  }

  /**
   * 이벤트 리스너 등록
   */
  addEventListener(eventType: string, listener: (event: RealtimeEvent) => void): void {
    if (!this.eventListeners.has(eventType)) {
      this.eventListeners.set(eventType, [])
    }
    this.eventListeners.get(eventType)!.push(listener)
  }

  /**
   * 이벤트 리스너 제거
   */
  removeEventListener(eventType: string, listener: (event: RealtimeEvent) => void): void {
    const listeners = this.eventListeners.get(eventType)
    if (listeners) {
      const index = listeners.indexOf(listener)
      if (index > -1) {
        listeners.splice(index, 1)
      }
    }
  }

  /**
   * 모든 이벤트 리스너 제거
   */
  removeAllEventListeners(): void {
    this.eventListeners.clear()
  }

  /**
   * 재연결 처리
   */
  private handleReconnection(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('재연결 실패:', error)
        })
      }, this.reconnectDelay)
    } else {
      console.error('최대 재연결 시도 횟수 초과')
    }
  }

  /**
   * 사용자 ID 생성
   */
  private generateUserId(): string {
    return 'user_' + Math.random().toString(36).substr(2, 9)
  }

  /**
   * 연결 상태 반환
   */
  getConnectionStatus() {
    return this.isConnected
  }

  /**
   * 사용자 정보 설정
   */
  setUserInfo(userId: string, username: string): void {
    this.userId = userId
    this.username = username
  }
}

// 싱글톤 인스턴스 생성
const websocketService = new WebSocketService()

export default websocketService
export { WebSocketService }
