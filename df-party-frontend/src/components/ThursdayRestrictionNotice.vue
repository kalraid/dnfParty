<template>
  <div v-if="showNotice" class="thursday-restriction-notice">
    <div class="notice-header">
      <span class="notice-icon">⚠️</span>
      <span class="notice-title">목요일 API 제한 안내</span>
    </div>
    <div class="notice-content">
      <p>{{ restrictionInfo?.message }}</p>
      <p class="restriction-time">
        <strong>제한 시간:</strong> {{ restrictionInfo?.restrictedTime }}
      </p>
      <p class="reset-time">
        <strong>던전 초기화:</strong> {{ restrictionStatus?.resetTime || '확인 중...' }}
      </p>
      <p class="reason">{{ restrictionInfo?.reason }}</p>
    </div>
    <div class="notice-actions">
      <button @click="checkStatus" class="status-btn">상태 확인</button>
      <button @click="dismissNotice" class="dismiss-btn">닫기</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { apiFetch } from '../config/api'

interface RestrictionInfo {
  thursdayRestriction: boolean
  operation: string
  message: string
  restrictedTime: string
  reason: string
  timestamp: string
}

interface RestrictionStatus {
  isThursday: boolean
  isNearResetTime: boolean
  isApiRestricted: boolean
  restrictedTimeRange: string
  resetTime: string
  currentTime: string
}

const restrictionInfo = ref<RestrictionInfo | null>(null)
const restrictionStatus = ref<RestrictionStatus | null>(null)
const showNotice = ref(false)

// 목요일 제한 정보 조회
const fetchRestrictionInfo = async () => {
  try {
    const response = await apiFetch('/thursday-restriction/info')
    if (response.ok) {
      const data = await response.json()
      if (data.isApiRestricted) {
        restrictionInfo.value = {
          thursdayRestriction: true,
          operation: '일반',
          message: '목요일에는 API 통신이 제한되어 DB에 저장된 정보만 제공됩니다.',
          restrictedTime: data.restrictedTimeRange,
          reason: '목요일에는 DFO API와 Dundam 크롤링이 불안정할 수 있어 DB 정보만 제공합니다.',
          timestamp: data.currentTime
        }
        showNotice.value = true
      }
    }
  } catch (error) {
    console.error('목요일 제한 정보 조회 실패:', error)
  }
}

// 던전 초기화 상태 확인
const checkStatus = async () => {
  try {
    const response = await apiFetch('/thursday-restriction/reset-status')
    if (response.ok) {
      restrictionStatus.value = await response.json()
    }
  } catch (error) {
    console.error('던전 초기화 상태 확인 실패:', error)
  }
}

// 알림 닫기
const dismissNotice = () => {
  showNotice.value = false
}

// 컴포넌트 마운트 시 목요일 제한 정보 확인
onMounted(() => {
  fetchRestrictionInfo()
  
  // 1분마다 상태 확인
  setInterval(() => {
    fetchRestrictionInfo()
  }, 60000)
})
</script>

<style scoped>
.thursday-restriction-notice {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 350px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notice-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  font-weight: bold;
  font-size: 16px;
}

.notice-icon {
  font-size: 20px;
  margin-right: 10px;
}

.notice-title {
  font-size: 18px;
}

.notice-content {
  margin-bottom: 20px;
}

.notice-content p {
  margin: 8px 0;
  line-height: 1.4;
}

.restriction-time,
.reset-time {
  font-size: 14px;
  opacity: 0.9;
}

.reason {
  font-size: 13px;
  opacity: 0.8;
  font-style: italic;
}

.notice-actions {
  display: flex;
  gap: 10px;
}

.status-btn,
.dismiss-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.status-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.status-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.dismiss-btn {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.dismiss-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 768px) {
  .thursday-restriction-notice {
    position: fixed;
    top: 10px;
    left: 10px;
    right: 10px;
    width: auto;
    max-width: none;
  }
}
</style>
