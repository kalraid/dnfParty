import { ref, onUnmounted } from 'vue'

export function useEventSource() {
  const eventSource = ref<EventSource | null>(null)
  const isConnected = ref(false)

  const connectEventSource = (url: string, onMessage: (event: MessageEvent) => void) => {
    try {
      // 기존 연결이 있다면 해제
      if (eventSource.value) {
        eventSource.value.close()
      }

      // 새로운 SSE 연결 생성
      eventSource.value = new EventSource(url)
      
      eventSource.value.onopen = () => {
        console.log('SSE 연결이 열렸습니다:', url)
        isConnected.value = true
      }

      eventSource.value.onmessage = onMessage

      eventSource.value.onerror = (error) => {
        console.error('SSE 연결 오류:', error)
        isConnected.value = false
      }

    } catch (error) {
      console.error('SSE 연결 생성 실패:', error)
      isConnected.value = false
    }
  }

  const disconnectEventSource = () => {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
      isConnected.value = false
      console.log('SSE 연결이 해제되었습니다.')
    }
  }

  // 컴포넌트 언마운트 시 자동으로 연결 해제
  onUnmounted(() => {
    disconnectEventSource()
  })

  return {
    isConnected,
    connectEventSource,
    disconnectEventSource
  }
}
