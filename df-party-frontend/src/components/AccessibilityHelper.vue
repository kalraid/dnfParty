<template>
  <div class="accessibility-helper">
    <!-- 스크린 리더 전용 스킵 링크 -->
    <a href="#main-content" class="skip-link sr-only">
      메인 콘텐츠로 건너뛰기
    </a>
    
    <!-- 키보드 네비게이션 도움말 -->
    <div v-if="showKeyboardHelp" class="keyboard-help modal-overlay" @click="closeKeyboardHelp">
      <div class="keyboard-help-content" @click.stop>
        <h3>키보드 단축키</h3>
        <div class="keyboard-shortcuts">
          <div class="shortcut-item">
            <kbd>Tab</kbd>
            <span>다음 요소로 이동</span>
          </div>
          <div class="shortcut-item">
            <kbd>Shift + Tab</kbd>
            <span>이전 요소로 이동</span>
          </div>
          <div class="shortcut-item">
            <kbd>Enter</kbd>
            <span>선택/실행</span>
          </div>
          <div class="shortcut-item">
            <kbd>Space</kbd>
            <span>체크박스 토글</span>
          </div>
          <div class="shortcut-item">
            <kbd>Escape</kbd>
            <span>모달 닫기</span>
          </div>
          <div class="shortcut-item">
            <kbd>H</kbd>
            <span>홈으로 이동</span>
          </div>
          <div class="shortcut-item">
            <kbd>C</kbd>
            <span>캐릭터 검색</span>
          </div>
          <div class="shortcut-item">
            <kbd>P</kbd>
            <span>파티 구성</span>
          </div>
        </div>
        <button @click="closeKeyboardHelp" class="btn btn-primary">
          닫기
        </button>
      </div>
    </div>
    
    <!-- 접근성 설정 패널 -->
    <div v-if="showAccessibilityPanel" class="accessibility-panel">
      <div class="panel-header">
        <h4>접근성 설정</h4>
        <button @click="closeAccessibilityPanel" class="close-btn" aria-label="접근성 패널 닫기">
          ×
        </button>
      </div>
      
      <div class="panel-content">
        <div class="setting-group">
          <label class="setting-label">
            <input 
              type="checkbox" 
              v-model="settings.highContrast"
              @change="updateHighContrast"
            >
            고대비 모드
          </label>
        </div>
        
        <div class="setting-group">
          <label class="setting-label">
            <input 
              type="checkbox" 
              v-model="settings.largeText"
              @change="updateLargeText"
            >
            큰 글씨 모드
          </label>
        </div>
        
        <div class="setting-group">
          <label class="setting-label">
            <input 
              type="checkbox" 
              v-model="settings.reducedMotion"
              @change="updateReducedMotion"
            >
            애니메이션 줄이기
          </label>
        </div>
        
        <div class="setting-group">
          <label class="setting-label">
            <input 
              type="checkbox" 
              v-model="settings.focusIndicator"
              @change="updateFocusIndicator"
            >
            포커스 표시 강화
          </label>
        </div>
      </div>
    </div>
    
    <!-- 접근성 도움말 버튼 -->
    <button 
      @click="toggleAccessibilityPanel"
      class="accessibility-toggle"
      :class="{ active: showAccessibilityPanel }"
      aria-label="접근성 설정 열기"
      title="접근성 설정"
    >
      <span class="icon">♿</span>
    </button>
    
    <!-- 키보드 도움말 버튼 -->
    <button 
      @click="showKeyboardHelp = true"
      class="keyboard-help-toggle"
      aria-label="키보드 단축키 도움말"
      title="키보드 단축키"
    >
      <span class="icon">⌨️</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface AccessibilitySettings {
  highContrast: boolean
  largeText: boolean
  reducedMotion: boolean
  focusIndicator: boolean
}

const showKeyboardHelp = ref(false)
const showAccessibilityPanel = ref(false)

const settings = ref<AccessibilitySettings>({
  highContrast: false,
  largeText: false,
  reducedMotion: false,
  focusIndicator: true
})

// 키보드 단축키 처리
const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl + K: 키보드 도움말
  if (event.ctrlKey && event.key === 'k') {
    event.preventDefault()
    showKeyboardHelp.value = true
  }
  
  // Ctrl + A: 접근성 패널
  if (event.ctrlKey && event.key === 'a') {
    event.preventDefault()
    toggleAccessibilityPanel()
  }
  
  // H: 홈으로 이동
  if (event.key === 'h' && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    navigateToHome()
  }
  
  // C: 캐릭터 검색
  if (event.key === 'c' && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    navigateToCharacterSearch()
  }
  
  // P: 파티 구성
  if (event.key === 'p' && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    navigateToPartyFormation()
  }
}

// 접근성 설정 적용
const updateHighContrast = () => {
  document.documentElement.classList.toggle('high-contrast', settings.value.highContrast)
  localStorage.setItem('accessibility-high-contrast', settings.value.highContrast.toString())
}

const updateLargeText = () => {
  document.documentElement.classList.toggle('large-text', settings.value.largeText)
  localStorage.setItem('accessibility-large-text', settings.value.largeText.toString())
}

const updateReducedMotion = () => {
  document.documentElement.classList.toggle('reduced-motion', settings.value.reducedMotion)
  localStorage.setItem('accessibility-reduced-motion', settings.value.reducedMotion.toString())
}

const updateFocusIndicator = () => {
  document.documentElement.classList.toggle('enhanced-focus', settings.value.focusIndicator)
  localStorage.setItem('accessibility-focus-indicator', settings.value.focusIndicator.toString())
}

// 패널 토글
const toggleAccessibilityPanel = () => {
  showAccessibilityPanel.value = !showAccessibilityPanel.value
}

const closeAccessibilityPanel = () => {
  showAccessibilityPanel.value = false
}

const closeKeyboardHelp = () => {
  showKeyboardHelp.value = false
}

// 네비게이션 함수들
const navigateToHome = () => {
  // Vue Router를 사용하여 홈으로 이동
  if (window.location.pathname !== '/') {
    window.location.href = '/'
  }
}

const navigateToCharacterSearch = () => {
  if (window.location.pathname !== '/character-search') {
    window.location.href = '/character-search'
  }
}

const navigateToPartyFormation = () => {
  if (window.location.pathname !== '/party-formation') {
    window.location.href = '/party-formation'
  }
}

// 설정 로드
const loadSettings = () => {
  const highContrast = localStorage.getItem('accessibility-high-contrast') === 'true'
  const largeText = localStorage.getItem('accessibility-large-text') === 'true'
  const reducedMotion = localStorage.getItem('accessibility-reduced-motion') === 'true'
  const focusIndicator = localStorage.getItem('accessibility-focus-indicator') !== 'false'
  
  settings.value = { highContrast, largeText, reducedMotion, focusIndicator }
  
  // 설정 적용
  updateHighContrast()
  updateLargeText()
  updateReducedMotion()
  updateFocusIndicator()
}

onMounted(() => {
  loadSettings()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.accessibility-helper {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.accessibility-toggle,
.keyboard-help-toggle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: none;
  background: var(--color-primary);
  color: white;
  cursor: pointer;
  margin: 5px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.accessibility-toggle:hover,
.keyboard-help-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.accessibility-toggle.active {
  background: var(--color-secondary);
}

.skip-link {
  position: absolute;
  top: -40px;
  left: 6px;
  background: var(--color-primary);
  color: white;
  padding: 8px;
  text-decoration: none;
  border-radius: 4px;
  z-index: 1001;
}

.skip-link:focus {
  top: 6px;
}

.keyboard-help,
.accessibility-panel {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1002;
}

.keyboard-help-content,
.accessibility-panel {
  background: var(--color-background);
  border-radius: 8px;
  padding: 2rem;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.keyboard-shortcuts {
  margin: 1rem 0;
}

.shortcut-item {
  display: flex;
  align-items: center;
  margin: 0.5rem 0;
  gap: 1rem;
}

.shortcut-item kbd {
  background: var(--color-border);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  min-width: 80px;
  text-align: center;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--color-text);
}

.setting-group {
  margin: 1rem 0;
}

.setting-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.setting-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
}

/* 접근성 개선을 위한 추가 스타일 */
@media (prefers-reduced-motion: reduce) {
  .accessibility-toggle,
  .keyboard-help-toggle {
    transition: none;
  }
  
  .accessibility-toggle:hover,
  .keyboard-help-toggle:hover {
    transform: none;
  }
}

@media (max-width: 768px) {
  .accessibility-helper {
    bottom: 10px;
    right: 10px;
  }
  
  .accessibility-toggle,
  .keyboard-help-toggle {
    width: 45px;
    height: 45px;
    font-size: 18px;
  }
}
</style>
