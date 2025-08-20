import { ref, computed } from 'vue'

interface SSEEvent {
  type: string
  data: any
  timestamp: number
}

class SSEService {
  private eventSource: EventSource | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private eventListeners: Map<string, Set<(data: any) => void>> = new Map()
  private connectionStartTime: number = 0
  private lastHeartbeat: number = 0
  
  // 상태 관리
  public isConnected = ref(false)
  public connectionStatus = ref<'disconnected' | 'connecting' | 'connected' | 'error' | 'reconnecting'>('disconnected')
  public lastError = ref<string | null>(null)
  public connectionDuration = ref(0)
  public totalReconnectAttempts = ref(0)
  public lastReconnectTime = ref<number | null>(null)

  // 연결 상태 계산
  public getConnectionStatus = computed(() => this.connectionStatus.value)
  public getIsConnected = computed(() => this.isConnected.value)

  // 연결 시작
  async connect(): Promise<void> {
    if (this.eventSource && this.eventSource.readyState !== EventSource.CLOSED) {
      console.log('🔗 SSE 이미 연결되어 있음, 기존 연결 유지')
      return
    }

    try {
      console.log('🚀 SSE 연결 시작...')
      this.connectionStatus.value = 'connecting'
      this.connectionStartTime = Date.now()
      this.lastError.value = null
      
      const clientId = this.generateClientId()
      const url = `${import.meta.env.VITE_API_BASE_URL}/api/sse/connect?clientId=${clientId}`
      
      console.log(`📍 SSE 연결 URL: ${url}`)
      console.log(`🆔 클라이언트 ID: ${clientId}`)
      
      this.eventSource = new EventSource(url)
      this.setupEventSourceHandlers()
      
      console.log('✅ SSE EventSource 생성 완료')
      
    } catch (error) {
      console.error('❌ SSE 연결 실패:', error)
      this.handleReconnection()
    }
  }

  // EventSource 이벤트 핸들러 설정
  private setupEventSourceHandlers(): void {
    if (!this.eventSource) return

    // 연결 성공
    this.eventSource.onopen = () => {
      console.log('🟢 SSE 연결 열림 (onopen)')
      console.log(`📊 EventSource 상태: ${this.getReadyStateText(this.eventSource?.readyState)}`)
      this.isConnected.value = true
      this.connectionStatus.value = 'connected'
      this.reconnectAttempts = 0
      this.totalReconnectAttempts.value = 0
      this.lastHeartbeat = Date.now()
      
      console.log('🎯 SSE 연결 성공 - 이벤트 리스너 등록 대기 중...')
    }

    // 메시지 수신
    this.eventSource.onmessage = (event) => {
      console.log('📨 SSE 메시지 수신:', event)
      this.lastHeartbeat = Date.now()
      
      try {
        const data = JSON.parse(event.data)
        console.log('📋 파싱된 메시지 데이터:', data)
        
        // 이벤트 타입별 처리
        if (data.type) {
          this.notifyListeners(data.type, data)
        } else {
          console.log('⚠️ 메시지에 type이 없음:', data)
        }
      } catch (parseError) {
        console.error('❌ 메시지 파싱 실패:', parseError)
        console.log('📄 원본 메시지:', event.data)
      }
    }

    // 에러 처리
    this.eventSource.onerror = (event) => {
      console.error('🚨 SSE 연결 오류 발생:', event)
      console.log(`📊 EventSource 상태: ${this.getReadyStateText(this.eventSource?.readyState)}`)
      console.log(`⏰ 오류 발생 시간: ${new Date().toISOString()}`)
      console.log(`🔗 연결 지속 시간: ${this.getConnectionDuration()}ms`)
      
      this.lastError.value = `SSE 연결 오류: ${event.type}`
      this.connectionStatus.value = 'error'
      
      // 연결 상태 확인
      if (this.eventSource?.readyState === EventSource.CLOSED) {
        console.log('🔒 EventSource가 닫힘 - 재연결 시도')
        this.handleReconnection()
      } else if (this.eventSource?.readyState === EventSource.CONNECTING) {
        console.log('🔄 EventSource가 재연결 중...')
        this.connectionStatus.value = 'reconnecting'
      } else {
        console.log('❓ 알 수 없는 EventSource 상태')
      }
    }
  }

  // EventSource 상태 텍스트 변환
  private getReadyStateText(readyState: number | undefined): string {
    if (readyState === undefined) return 'undefined'
    switch (readyState) {
      case EventSource.CONNECTING: return 'CONNECTING (0)'
      case EventSource.OPEN: return 'OPEN (1)'
      case EventSource.CLOSED: return 'CLOSED (2)'
      default: return `UNKNOWN (${readyState})`
    }
  }

  // 연결 지속 시간 계산
  private getConnectionDuration(): number {
    if (this.connectionStartTime === 0) return 0
    return Date.now() - this.connectionStartTime
  }

  // 재연결 처리
  private handleReconnection(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log(`🚫 SSE 최대 재연결 시도 횟수 초과 (${this.maxReconnectAttempts}회)`)
      console.log('⚠️ 자동 재연결 비활성화됨 - 수동으로만 연결 가능')
      this.connectionStatus.value = 'error'
      this.isConnected.value = false
      this.lastError.value = '최대 재연결 시도 횟수 초과'
      return
    }

    this.reconnectAttempts++
    this.totalReconnectAttempts.value++
    this.lastReconnectTime.value = Date.now()
    
    console.log(`🔄 SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
    console.log(`⏰ 재연결 시도 시간: ${new Date().toISOString()}`)
    console.log(`📊 총 재연결 시도 횟수: ${this.totalReconnectAttempts.value}`)
    
    this.connectionStatus.value = 'reconnecting'
    
    setTimeout(() => {
      console.log(`🔄 ${this.reconnectDelay}ms 후 재연결 시도...`)
      this.connect()
    }, this.reconnectDelay)
  }

  // 연결 해제
  disconnect(): void {
    console.log('🔌 SSE 연결 해제 시작...')
    
    if (this.eventSource) {
      console.log(`📊 연결 해제 전 EventSource 상태: ${this.getReadyStateText(this.eventSource.readyState)}`)
      
      // 이벤트 핸들러 제거
      this.eventSource.onopen = null
      this.eventSource.onmessage = null
      this.eventSource.onerror = null
      
      // 연결 종료
      this.eventSource.close()
      console.log('✅ EventSource 연결 종료 완료')
      
      this.eventSource = null
    }
    
    this.isConnected.value = false
    this.connectionStatus.value = 'disconnected'
    this.reconnectAttempts = 0
    this.connectionStartTime = 0
    this.lastHeartbeat = 0
    
    console.log('🔌 SSE 연결 해제 완료')
  }

  // 이벤트 리스너 등록
  addEventListener(eventType: string, callback: (data: any) => void): void {
    console.log(`👂 SSE 이벤트 리스너 등록: ${eventType}`)
    
    if (!this.eventListeners.has(eventType)) {
      this.eventListeners.set(eventType, new Set())
    }
    
    this.eventListeners.get(eventType)!.add(callback)
    console.log(`✅ 이벤트 리스너 등록 완료: ${eventType}`)
    console.log(`📊 현재 등록된 리스너 수: ${this.eventListeners.get(eventType)!.size}`)
  }

  // 이벤트 리스너 제거
  removeEventListener(eventType: string, callback: (data: any) => void): void {
    console.log(`🗑️ SSE 이벤트 리스너 제거: ${eventType}`)
    
    const listeners = this.eventListeners.get(eventType)
    if (listeners) {
      listeners.delete(callback)
      console.log(`✅ 이벤트 리스너 제거 완료: ${eventType}`)
      console.log(`📊 남은 리스너 수: ${listeners.size}`)
      
      if (listeners.size === 0) {
        this.eventListeners.delete(eventType)
        console.log(`🗑️ 빈 이벤트 타입 제거: ${eventType}`)
      }
    }
  }

  // 이벤트 리스너들에게 알림
  private notifyListeners(eventType: string, data: any): void {
    const listeners = this.eventListeners.get(eventType)
    if (listeners && listeners.size > 0) {
      console.log(`📢 이벤트 알림 전송: ${eventType} (${listeners.size}개 리스너)`)
      listeners.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`❌ 이벤트 리스너 실행 오류 (${eventType}):`, error)
        }
      })
    } else {
      console.log(`⚠️ 이벤트 타입 '${eventType}'에 등록된 리스너가 없음`)
    }
  }

  // 클라이언트 ID 생성
  private generateClientId(): string {
    return `client_${Math.random().toString(36).substr(2, 9)}`
  }

  // 연결 상태 정보 반환
  getConnectionInfo(): any {
    return {
      isConnected: this.isConnected.value,
      connectionStatus: this.connectionStatus.value,
      lastError: this.lastError.value,
      connectionDuration: this.getConnectionDuration(),
      totalReconnectAttempts: this.totalReconnectAttempts.value,
      lastReconnectTime: this.lastReconnectTime.value,
      lastHeartbeat: this.lastHeartbeat,
      eventSourceReadyState: this.eventSource ? this.getReadyStateText(this.eventSource.readyState) : 'N/A',
      registeredEventTypes: Array.from(this.eventListeners.keys()),
      totalEventListeners: Array.from(this.eventListeners.values()).reduce((sum, set) => sum + set.size, 0)
    }
  }

  // 연결 상태 로그 출력
  logConnectionStatus(): void {
    const info = this.getConnectionInfo()
    console.log('📊 === SSE 연결 상태 정보 ===')
    console.log('🔗 연결 상태:', info.connectionStatus)
    console.log('✅ 연결됨:', info.isConnected)
    console.log('⏱️ 연결 지속 시간:', info.connectionDuration + 'ms')
    console.log('🔄 총 재연결 시도:', info.totalReconnectAttempts)
    console.log('📡 EventSource 상태:', info.eventSourceReadyState)
    console.log('👂 등록된 이벤트 타입:', info.registeredEventTypes)
    console.log('📊 총 이벤트 리스너:', info.totalEventListeners)
    if (info.lastError) {
      console.log('❌ 마지막 오류:', info.lastError)
    }
    if (info.lastReconnectTime) {
      console.log('🔄 마지막 재연결 시간:', new Date(info.lastReconnectTime).toISOString())
    }
    console.log('================================')
  }
}

// 싱글톤 인스턴스 생성
const sseService = new SSEService()

export default sseService
