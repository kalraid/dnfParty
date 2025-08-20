<template>
  <div class="theme-toggle">
    <button 
      @click="toggleTheme" 
      :class="['theme-btn', currentTheme]"
      :title="currentTheme === 'dark' ? '라이트 테마로 전환' : '다크 테마로 전환'"
    >
      <span v-if="currentTheme === 'light'" class="icon">🌙</span>
      <span v-else class="icon">☀️</span>
      <span class="label">{{ currentTheme === 'light' ? '다크' : '라이트' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const currentTheme = ref<'light' | 'dark'>('light')

const toggleTheme = () => {
  currentTheme.value = currentTheme.value === 'light' ? 'dark' : 'light'
  applyTheme(currentTheme.value)
  localStorage.setItem('theme', currentTheme.value)
}

const applyTheme = (theme: 'light' | 'dark') => {
  const root = document.documentElement
  if (theme === 'dark') {
    root.classList.add('dark-theme')
    root.classList.remove('light-theme')
  } else {
    root.classList.add('light-theme')
    root.classList.remove('dark-theme')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  
  if (savedTheme) {
    currentTheme.value = savedTheme
  } else if (prefersDark) {
    currentTheme.value = 'dark'
  }
  
  applyTheme(currentTheme.value)
})

// 시스템 테마 변경 감지
watch(currentTheme, (newTheme) => {
  applyTheme(newTheme)
})
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
}

.theme-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 2px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-background);
  color: var(--color-text);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.theme-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: var(--color-border-hover);
}

.theme-btn.light {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.theme-btn.dark {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  color: white;
  border-color: transparent;
}

.icon {
  font-size: 16px;
}

.label {
  font-weight: 600;
}

/* ========================================
   반응형 디자인 - 글자 크기 포함 완전 최적화
   ======================================== */

/* 태블릿 (768px 이하) */
@media (max-width: 768px) {
  .theme-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
  
  .label {
    display: none;
  }
  
  .icon {
    font-size: 14px;
  }
}

/* 중형 모바일 (600px 이하) */
@media (max-width: 600px) {
  .theme-btn {
    padding: 5px 8px;
    font-size: 11px;
  }
  
  .icon {
    font-size: 12px;
  }
}

/* 소형 모바일 (480px 이하) */
@media (max-width: 480px) {
  .theme-btn {
    padding: 4px 6px;
    font-size: 10px;
  }
  
  .icon {
    font-size: 10px;
  }
}

/* 초소형 모바일 (320px 이하) */
@media (max-width: 320px) {
  .theme-btn {
    padding: 3px 5px;
    font-size: 9px;
  }
  
  .icon {
    font-size: 8px;
  }
}
</style>
