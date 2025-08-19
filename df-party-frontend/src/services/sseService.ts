import { ref, onUnmounted } from 'vue'
import { getApiUrl } from '../config/api'

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

class SseService {
  private eventSource: EventSource | null = null
  private isConnected = ref(false)
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private eventListeners: Map<string, ((event: RealtimeEvent) => void)[]> = new Map()
  private clientId: string = 'anonymous'

  constructor() {
    this.clientId = this.generateClientId()
  }

  /**
   * SSE 연결
   */
  async connect(): Promise<void> {
    try {
      if (this.eventSource && this.eventSource.readyState === EventSource.OPEN) {
        console.log('이미 SSE에 연결되어 있습니다.')
        return
      }

      const url = `${getApiUrl('sse/connect')}?clientId=${this.clientId}`
      this.eventSource = new EventSource(url)

      this.eventSource.onopen = () => {
        console.log('SSE 연결 성공')
        this.isConnected.value = true
        this.reconnectAttempts = 0
      }

      this.eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.handleEvent(data)
        } catch (error) {
          console.error('SSE 메시지 파싱 오류:', error)
        }
      }

      this.eventSource.onerror = (error) => {
        console.error('SSE 연결 오류:', error)
        this.isConnected.value = false
        this.handleReconnection()
      }

      // 이벤트별 리스너 등록
      this.setupEventListeners()

    } catch (error) {
      console.error('SSE 연결 중 오류:', error)
      this.handleReconnection()
      throw error
    }
  }

  /**
   * 이벤트별 리스너 설정
   */
  private setupEventListeners(): void {
    if (!this.eventSource) return

    // CHARACTER_UPDATED 이벤트
    this.eventSource.addEventListener('character_updated', (event) => {
      try {
        const data: RealtimeEvent = JSON.parse(event.data)
        this.handleEvent(data)
      } catch (error) {
        console.error('CHARACTER_UPDATED 이벤트 파싱 오류:', error)
      }
    })

    // 연결 성공 이벤트
    this.eventSource.addEventListener('connect', (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('SSE 연결 확인:', data)
      } catch (error) {
        console.error('CONNECT 이벤트 파싱 오류:', error)
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
   * SSE 연결 해제
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
      this.isConnected.value = false
      console.log('SSE 연결 해제')
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
      console.log(`SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('SSE 재연결 실패:', error)
        })
      }, this.reconnectDelay)
    } else {
      console.error('SSE 최대 재연결 시도 횟수 초과')
    }
  }

  /**
   * 클라이언트 ID 생성
   */
  private generateClientId(): string {
    return 'client_' + Math.random().toString(36).substr(2, 9)
  }

  /**
   * 연결 상태 반환
   */
  getConnectionStatus() {
    return this.isConnected
  }

  /**
   * 클라이언트 정보 설정
   */
  setClientInfo(clientId: string): void {
    this.clientId = clientId
  }
}

// 싱글톤 인스턴스 생성
const sseService = new SseService()

export default sseService
export { SseService }
