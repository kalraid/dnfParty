<template>
  <div v-if="showProgress" class="refresh-progress">
    <div class="progress-header">
      <span class="progress-icon">🔄</span>
      <span class="progress-title">{{ progressInfo.title }}</span>
      <button @click="closeProgress" class="close-btn">✕</button>
    </div>
    
    <div class="progress-content">
      <div class="progress-bar-container">
        <div class="progress-bar" :style="{ width: progressPercentage + '%' }"></div>
      </div>
      
      <div class="progress-stats">
        <div class="stat-item">
          <span class="stat-label">진행:</span>
          <span class="stat-value">{{ progressInfo.processedCount }}/{{ progressInfo.totalCharacters }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">성공:</span>
          <span class="stat-value success">{{ progressInfo.successCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">실패:</span>
          <span class="stat-value error">{{ progressInfo.failCount }}</span>
        </div>
      </div>
      
      <div v-if="progressInfo.currentCharacter" class="current-character">
        <span class="current-label">현재 처리 중:</span>
        <span class="character-name">{{ progressInfo.currentCharacter }}</span>
      </div>
      
      <div v-if="progressInfo.message" class="progress-message">
        {{ progressInfo.message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useEventSource } from '../composables/useEventSource'

interface ProgressInfo {
  type: string
  adventureName: string
  totalCharacters: number
  processedCount: number
  successCount: number
  failCount: number
  currentCharacter?: string
  message?: string
  title?: string
}

const showProgress = ref(false)
const progressInfo = ref<ProgressInfo>({
  type: '',
  adventureName: '',
  totalCharacters: 0,
  processedCount: 0,
  successCount: 0,
  failCount: 0
})

const { connectEventSource, disconnectEventSource } = useEventSource()

// 진행률 계산
const progressPercentage = computed(() => {
  if (progressInfo.value.totalCharacters === 0) return 0
  return Math.round((progressInfo.value.processedCount / progressInfo.value.totalCharacters) * 100)
})

// 진행 상황 초기화
const initializeProgress = (adventureName: string, totalCharacters: number) => {
  progressInfo.value = {
    type: 'refresh_start',
    adventureName,
    totalCharacters,
    processedCount: 0,
    successCount: 0,
    failCount: 0,
    title: `'${adventureName}' 모험단 최신화`
  }
  showProgress.value = true
}

// SSE 이벤트 처리
const handleSSEEvent = (event: MessageEvent) => {
  try {
    const data = JSON.parse(event.data)
    console.log('🔍 SSE 이벤트 수신:', data)
    
    // RealtimeEvent 구조에서 data 필드 확인
    const eventData = data.data || data
    
    if (eventData.type === 'refresh_start' && eventData.adventureName === progressInfo.value.adventureName) {
      console.log('🚀 모험단 최신화 시작:', eventData)
      progressInfo.value = {
        ...progressInfo.value,
        ...eventData,
        title: `'${eventData.adventureName}' 모험단 최신화`
      }
      showProgress.value = true
    }
    
    if (eventData.type === 'refresh_progress' && eventData.adventureName === progressInfo.value.adventureName) {
      console.log('📊 모험단 최신화 진행:', eventData)
      progressInfo.value = {
        ...progressInfo.value,
        ...eventData,
        currentCharacter: eventData.characterName
      }
    }
    
    if (eventData.type === 'refresh_complete' && eventData.adventureName === progressInfo.value.adventureName) {
      console.log('✅ 모험단 최신화 완료:', eventData)
      progressInfo.value = {
        ...progressInfo.value,
        ...eventData,
        message: `완료! 성공: ${eventData.successCount}명, 실패: ${eventData.failCount}명`
      }
      
      // 3초 후 자동으로 닫기
      setTimeout(() => {
        showProgress.value = false
      }, 3000)
    }
    
    if (eventData.type === 'refresh_error' && eventData.adventureName === progressInfo.value.adventureName) {
      console.log('❌ 모험단 최신화 오류:', eventData)
      progressInfo.value = {
        ...progressInfo.value,
        message: `오류 발생: ${eventData.error}`
      }
      
      // 5초 후 자동으로 닫기
      setTimeout(() => {
        showProgress.value = false
      }, 5000)
    }
    
  } catch (error) {
    console.error('SSE 이벤트 파싱 오류:', error)
  }
}

// 진행 상황 닫기
const closeProgress = () => {
  showProgress.value = false
}

// 컴포넌트 마운트 시 SSE 연결
onMounted(() => {
  connectEventSource('/api/sse/events', handleSSEEvent)
})

// 컴포넌트 언마운트 시 SSE 연결 해제
onUnmounted(() => {
  disconnectEventSource()
})

// 외부에서 호출할 수 있도록 expose
defineExpose({
  initializeProgress
})
</script>

<style scoped>
.refresh-progress {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 400px;
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  animation: slideDown 0.5s ease-out;
}

@keyframes slideDown {
  from {
    transform: translateX(-50%) translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(-50%) translateY(0);
    opacity: 1;
  }
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.progress-icon {
  font-size: 20px;
  margin-right: 10px;
}

.progress-title {
  font-weight: bold;
  font-size: 16px;
  flex: 1;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.progress-bar-container {
  width: 100%;
  height: 8px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #FFD700, #FFA500);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.stat-value {
  font-weight: bold;
  font-size: 16px;
}

.stat-value.success {
  color: #90EE90;
}

.stat-value.error {
  color: #FFB6C1;
}

.current-character {
  text-align: center;
  padding: 10px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.current-label {
  font-size: 12px;
  opacity: 0.8;
  display: block;
  margin-bottom: 5px;
}

.character-name {
  font-weight: bold;
  font-size: 14px;
}

.progress-message {
  text-align: center;
  font-weight: bold;
  padding: 10px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 14px;
}
</style>
