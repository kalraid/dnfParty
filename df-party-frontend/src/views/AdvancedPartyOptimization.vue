<template>
  <div class="advanced-party-optimization">
    <div class="header">
      <h1>🎯 고급 파티 최적화</h1>
      <p class="subtitle">다양한 최적화 전략으로 최적의 파티 구성을 찾아보세요</p>
    </div>

    <!-- 설정 섹션 -->
    <div class="configuration-section">
      <div class="config-card">
        <h3>⚙️ 파티 구성 설정</h3>
        
        <div class="form-row">
          <div class="form-group">
            <label for="server-select">서버 선택</label>
            <select id="server-select" v-model="selectedServer" @change="onServerChange">
              <option value="">서버를 선택하세요</option>
              <option v-for="server in servers" :key="server.serverId" :value="server.serverId">
                {{ server.serverName }}
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="dungeon-select">던전 선택</label>
            <select id="dungeon-select" v-model="selectedDungeon">
              <option value="">던전을 선택하세요</option>
              <option v-for="dungeon in dungeons" :key="dungeon.id" :value="dungeon.name">
                {{ dungeon.name }} ({{ dungeon.minFame }} 명성)
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="party-size">파티 크기</label>
            <select id="party-size" v-model="partySize">
              <option value="4">4인 파티</option>
              <option value="8">8인 파티</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="strategy-select">최적화 전략</label>
            <select id="strategy-select" v-model="selectedStrategy">
              <option value="">전략을 선택하세요</option>
              <option value="efficiency">효율성 중심</option>
              <option value="balance">밸런스 중심</option>
              <option value="synergy">시너지 중심</option>
              <option value="safety">안전성 중심</option>
              <option value="hybrid">하이브리드</option>
            </select>
          </div>
          
          <div class="form-group">
            <label for="party-count">파티 수</label>
            <select id="party-count" v-model="partyCount">
              <option value="1">1개 파티</option>
              <option value="2">2개 파티</option>
              <option value="3">3개 파티</option>
              <option value="4">4개 파티</option>
            </select>
          </div>
        </div>

        <div class="strategy-info" v-if="selectedStrategy">
          <h4>📋 {{ getStrategyInfo(selectedStrategy).name }}</h4>
          <p>{{ getStrategyInfo(selectedStrategy).description }}</p>
        </div>
      </div>
    </div>

    <!-- 파티 구성 섹션 -->
    <div class="party-formation-section">
      <div class="formation-card">
        <h3>🎯 파티 구성</h3>
        
        <div class="party-list">
          <div 
            v-for="(party, partyIndex) in parties" 
            :key="partyIndex"
            class="party-container"
          >
            <div class="party-header">
              <h4>{{ partyIndex + 1 }}파티</h4>
              <button @click="removeParty(partyIndex)" class="remove-party-btn" v-if="parties.length > 1">삭제</button>
            </div>
            
            <div class="party-slots">
              <div 
                v-for="slotIndex in parseInt(partySize)" 
                :key="slotIndex"
                class="party-slot"
                :class="{ 'empty': !party.slots[slotIndex - 1]?.characterId }"
                @click="openCharacterSelector(partyIndex, slotIndex - 1)"
              >
                <div v-if="party.slots[slotIndex - 1]?.characterId" class="slot-content">
                  <div class="character-name">{{ party.slots[slotIndex - 1].characterName }}</div>
                  <div class="character-job">{{ party.slots[slotIndex - 1].job || 'Unknown' }}</div>
                  <div class="character-stats">
                    <span class="fame">{{ party.slots[slotIndex - 1].fame?.toLocaleString() || 'N/A' }}</span>
                  </div>
                  <button @click.stop="removeCharacterFromSlot(partyIndex, slotIndex - 1)" class="remove-character-btn">×</button>
                </div>
                <div v-else class="slot-placeholder">
                  <span>클릭하여 캐릭터 선택</span>
                </div>
              </div>
            </div>
            
            <div class="party-stats">
              <span>효율성: {{ calculatePartyEfficiency(party.slots).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        
        <div class="party-actions">
          <button @click="addParty" class="add-party-btn" :disabled="parties.length >= parseInt(partyCount)">
            파티 추가
          </button>
          <button @click="autoFillParties" class="auto-fill-btn">
            자동 채우기
          </button>
          <button @click="clearAllParties" class="clear-all-btn">
            전체 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 캐릭터 선택 섹션 -->
    <div class="character-selection-section">
      <div class="selection-card">
        <h3>👥 캐릭터 선택</h3>
        
        <div class="search-bar">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="캐릭터명으로 검색..."
            @input="onSearchInput"
          />
          <button @click="clearSearch" class="clear-btn">초기화</button>
        </div>

        <div class="character-filters">
          <label class="filter-checkbox">
            <input type="checkbox" v-model="showFavorites" />
            업둥이만 표시
          </label>
          <label class="filter-checkbox">
            <input type="checkbox" v-model="showDealers" />
            딜러만 표시
          </label>
          <label class="filter-checkbox">
            <input type="checkbox" v-model="showBuffers" />
            버퍼만 표시
          </label>
        </div>

        <div class="character-list">
          <div 
            v-for="character in filteredCharacters" 
            :key="character.characterId"
            class="character-item"
            :class="{ 
              'selected': isCharacterInAnyParty(character.characterId),
              'favorite': character.isFavorite,
              'dealer': character.job?.includes('딜러') || false,
              'buffer': character.job?.includes('버퍼') || false
            }"
            @click="selectCharacterForSlot(character)"
          >
            <div class="character-info">
              <div class="character-name">{{ character.characterName }}</div>
              <div class="character-job">{{ character.job || 'Unknown' }}</div>
              <div class="character-stats">
                <span class="fame">명성: {{ character.fame?.toLocaleString() || 'N/A' }}</span>
                <span class="combat-power">전투력: {{ character.combatPower?.toLocaleString() || 'N/A' }}</span>
              </div>
            </div>
            <div class="selection-indicator">
              <span v-if="isCharacterInAnyParty(character.characterId)" class="selected-icon">✓</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 최적화 실행 섹션 -->
    <div class="optimization-section">
      <div class="optimization-card">
        <h3>🚀 파티 최적화 실행</h3>
        
        <div class="optimization-actions">
          <button 
            @click="executePartyOptimization" 
            :disabled="!canExecutePartyOptimization"
            class="execute-btn primary"
          >
            {{ selectedStrategy ? `${getStrategyInfo(selectedStrategy).name} 최적화 실행` : '파티 최적화 실행' }}
          </button>
          
          <button 
            @click="optimizePartyBalance" 
            :disabled="!canExecutePartyOptimization"
            class="balance-btn secondary"
          >
            파티 밸런스 조정
          </button>
          
          <button 
            @click="runPerformanceTest" 
            class="performance-btn tertiary"
          >
            성능 테스트
          </button>
        </div>

        <div class="optimization-status" v-if="isOptimizing">
          <div class="loading-spinner"></div>
          <span>파티 최적화 실행 중...</span>
        </div>
      </div>
    </div>

    <!-- 결과 표시 섹션 -->
    <div class="results-section" v-if="optimizationResult">
      <div class="results-card">
        <h3>📊 최적화 결과</h3>
        
        <div class="result-summary">
          <div class="summary-item">
            <span class="label">최적화 전략:</span>
            <span class="value">{{ getStrategyInfo(optimizationResult.optimizationType).name }}</span>
          </div>
          <div class="summary-item">
            <span class="label">실행 시간:</span>
            <span class="value">{{ formatExecutionTime(optimizationResult.executionTime) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">총 효율성:</span>
            <span class="value">{{ formatScore(optimizationResult.totalEfficiency || optimizationResult.efficiency) }}</span>
          </div>
        </div>

        <!-- 8인 파티 결과 -->
        <div v-if="partySize === 8 && optimizationResult.party1 && optimizationResult.party2" class="eight-person-result">
          <div class="party-display">
            <div class="party-card">
              <h4>1파티</h4>
              <div class="party-slots">
                <div 
                  v-for="slot in optimizationResult.party1.slots" 
                  :key="slot.slotNumber"
                  class="party-slot"
                  :class="{ 'empty': !slot.isOccupied }"
                >
                  <div v-if="slot.isOccupied" class="slot-content">
                    <div class="character-name">{{ slot.character.characterName }}</div>
                    <div class="character-role">{{ getRoleDisplayName(slot.role) }}</div>
                    <div class="character-fame">{{ slot.character.fame.toLocaleString() }}</div>
                  </div>
                  <div v-else class="empty-slot">빈 슬롯</div>
                </div>
              </div>
              <div class="party-score">
                효율성: {{ formatScore(optimizationResult.party1.efficiency) }}
              </div>
            </div>
            
            <div class="party-card">
              <h4>2파티</h4>
              <div class="party-slots">
                <div 
                  v-for="slot in optimizationResult.party2.slots" 
                  :key="slot.slotNumber"
                  class="party-slot"
                  :class="{ 'empty': !slot.isOccupied }"
                >
                  <div v-if="slot.isOccupied" class="slot-content">
                    <div class="character-name">{{ slot.character.characterName }}</div>
                    <div class="character-role">{{ getRoleDisplayName(slot.role) }}</div>
                    <div class="character-fame">{{ slot.character.fame.toLocaleString() }}</div>
                  </div>
                  <div v-else class="empty-slot">빈 슬롯</div>
                </div>
              </div>
              <div class="party-score">
                효율성: {{ formatScore(optimizationResult.party2.efficiency) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 4인 파티 결과 -->
        <div v-else-if="partySize === 4 && optimizationResult.party" class="four-person-result">
          <div class="party-card">
            <h4>파티 구성</h4>
            <div class="party-slots">
              <div 
                v-for="slot in optimizationResult.party.slots" 
                :key="slot.slotNumber"
                class="party-slot"
                :class="{ 'empty': !slot.isOccupied }"
              >
                <div v-if="slot.isOccupied" class="slot-content">
                  <div class="character-name">{{ slot.character.characterName }}</div>
                  <div class="character-role">{{ getRoleDisplayName(slot.role) }}</div>
                  <div class="character-fame">{{ slot.character.fame.toLocaleString() }}</div>
                </div>
                <div v-else class="empty-slot">빈 슬롯</div>
              </div>
            </div>
            <div class="party-score">
              효율성: {{ formatScore(optimizationResult.party.efficiency) }}
            </div>
          </div>
        </div>

        <!-- 비교 분석 결과 -->
        <div v-if="comparisonResult" class="comparison-result">
          <h4>📈 전략 비교 분석</h4>
          <div class="strategy-scores">
            <div 
              v-for="(strategy, key) in comparisonResult.scores" 
              :key="key"
              class="strategy-score"
              :class="{ 'best': comparisonResult.bestStrategy === key }"
            >
              <div class="strategy-name">{{ getStrategyInfo(String(key)).name }}</div>
              <div class="score-value">{{ formatScore(strategy.score) }}</div>
              <div class="score-rank">{{ strategy.rank }}위</div>
            </div>
          </div>
          
          <div class="recommendation">
            <strong>추천 전략:</strong> {{ getStrategyInfo(comparisonResult.recommendedStrategy).name }}
          </div>
        </div>
      </div>
    </div>

    <!-- 성능 테스트 결과 -->
    <div class="performance-section" v-if="performanceResult">
      <div class="performance-card">
        <h3>⚡ 성능 테스트 결과</h3>
        <div class="performance-metrics">
          <div class="metric">
            <span class="label">테스트 타입:</span>
            <span class="value">{{ performanceResult.testType === 'basic' ? '기본' : '스트레스' }}</span>
          </div>
          <div class="metric">
            <span class="label">반복 횟수:</span>
            <span class="value">{{ performanceResult.iterations }}회</span>
          </div>
          <div class="metric">
            <span class="label">총 실행 시간:</span>
            <span class="value">{{ performanceResult.totalExecutionTime }}ms</span>
          </div>
          <div class="metric">
            <span class="label">평균 실행 시간:</span>
            <span class="value">{{ performanceResult.averageExecutionTime.toFixed(2) }}ms</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 에러 메시지 -->
    <div class="error-message" v-if="errorMessage">
      <div class="error-card">
        <h4>❌ 오류 발생</h4>
        <p>{{ errorMessage }}</p>
        <button @click="clearError" class="error-close-btn">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useCharacterStore } from '@/stores/character'
import { usePartyStore } from '@/stores/party'
import type { Character, Server } from '@/types'
import { apiFetch } from '../config/api'

// Store 사용
const characterStore = useCharacterStore()
const partyStore = usePartyStore()

// 반응형 상태
const selectedServer = ref('')
const selectedDungeon = ref('')
const partySize = ref(4)
const selectedStrategy = ref('')
const partyCount = ref(1)
const searchQuery = ref('')
const showFavorites = ref(false)
const showDealers = ref(false)
const showBuffers = ref(false)

// 파티 구성 관련 상태
const parties = ref<Array<{
  slots: Array<{
    characterId?: string
    characterName?: string
    job?: string
    fame?: number
    totalDamage?: number
    buffPower?: number
  }>
  efficiency?: number
}>>([])

// 최적화 관련 상태
const isOptimizing = ref(false)
const optimizationResult = ref<any>(null)
const comparisonResult = ref<any>(null)
const performanceResult = ref<any>(null)
const errorMessage = ref('')

// 현재 선택된 슬롯 정보
const selectedSlot = ref<{partyIndex: number, slotIndex: number} | null>(null)

// 서버 및 던전 데이터
const servers = ref<Server[]>([])
const dungeons = ref<Array<{id: string, name: string, minFame: number}>>([])
const characters = ref<Character[]>([])

// 던전 목록
const dungeonsList = [
  { id: '1', name: '시로코 레이드', minFame: 50000 },
  { id: '2', name: '바칼 레이드', minFame: 60000 },
  { id: '3', name: '카인 레이드', minFame: 70000 },
  { id: '4', name: '디레지에 레이드', minFame: 80000 },
  { id: '5', name: '일반 던전', minFame: 30000 }
]

// Computed properties
const filteredCharacters = computed(() => {
  let filtered = characters.value.filter(char => {
    // 검색어 필터
    if (searchQuery.value && !char.characterName.toLowerCase().includes(searchQuery.value.toLowerCase())) {
      return false
    }
    
    // 업둥이 필터
    if (showFavorites.value && !char.isFavorite) {
      return false
    }
    
    // 딜러 필터
    if (showDealers.value && !char.job?.includes('딜러')) {
      return false
    }
    
    // 버퍼 필터
    if (showBuffers.value && !char.job?.includes('버퍼')) {
      return false
    }
    
    return true
  })
  
  // 명성 순으로 정렬
  return filtered.sort((a, b) => (b.fame || 0) - (a.fame || 0))
})

// 전략 정보
const getStrategyInfo = (strategy: string) => {
  const strategies = {
    efficiency: { name: '효율성 중심', description: '명성과 전투력을 기준으로 최적화합니다.' },
    balance: { name: '밸런스 중심', description: '직업별 균등한 분배로 최적화합니다.' },
    synergy: { name: '시너지 중심', description: '직업 조합의 시너지를 고려한 최적화입니다.' },
    safety: { name: '안전성 중심', description: '안전 마진을 고려한 최적화입니다.' },
    hybrid: { name: '하이브리드', description: '여러 전략을 조합한 최적화입니다.' }
  }
  return strategies[strategy as keyof typeof strategies] || { name: '', description: '' }
}

// 유틸리티 메서드
const formatScore = (score: number) => {
  return score.toLocaleString()
}

const formatExecutionTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}

const getRoleDisplayName = (role: string) => {
  const roleNames = {
    dealer: '딜러',
    buffer: '버퍼',
    updoongi: '업둥이',
    other: '기타',
    empty: '빈 슬롯'
  }
  return roleNames[role as keyof typeof roleNames] || role
}

const clearSearch = () => {
  searchQuery.value = ''
}

const onSearchInput = () => {
  // 검색 입력 시 추가 로직이 필요하면 여기에 구현
}

const clearError = () => {
  errorMessage.value = ''
}

// 파티 초기화
const initializeParties = () => {
  parties.value = []
  for (let i = 0; i < partyCount.value; i++) {
    const party = {
      slots: Array(partySize.value).fill(null).map(() => ({}))
    }
    parties.value.push(party)
  }
}

// 파티 추가
const addParty = () => {
  if (parties.value.length < partyCount.value) {
    const party = {
      slots: Array(partySize.value).fill(null).map(() => ({}))
    }
    parties.value.push(party)
  }
}

// 파티 삭제
const removeParty = (partyIndex: number) => {
  if (parties.value.length > 1) {
    parties.value.splice(partyIndex, 1)
  }
}

// 캐릭터를 슬롯에 배치
const placeCharacterInSlot = (partyIndex: number, slotIndex: number, character: Character) => {
  if (partyIndex < parties.value.length && slotIndex < partySize.value) {
    parties.value[partyIndex].slots[slotIndex] = {
      characterId: character.characterId,
      characterName: character.characterName,
      job: character.job,
      fame: character.fame,
      totalDamage: character.totalDamage,
      buffPower: character.buffPower
    }
  }
}

// 슬롯에서 캐릭터 제거
const removeCharacterFromSlot = (partyIndex: number, slotIndex: number) => {
  if (partyIndex < parties.value.length && slotIndex < partySize.value) {
    parties.value[partyIndex].slots[slotIndex] = {}
  }
}

// 캐릭터 선택기 열기
const openCharacterSelector = (partyIndex: number, slotIndex: number) => {
  selectedSlot.value = { partyIndex, slotIndex }
}

// 캐릭터를 슬롯에 선택
const selectCharacterForSlot = (character: Character) => {
  if (selectedSlot.value) {
    const { partyIndex, slotIndex } = selectedSlot.value
    
    // 이미 다른 파티에 배치된 캐릭터인지 확인
    if (isCharacterInAnyParty(character.characterId)) {
      // 기존 배치 제거
      removeCharacterFromAllParties(character.characterId)
    }
    
    placeCharacterInSlot(partyIndex, slotIndex, character)
    selectedSlot.value = null
  }
}

// 캐릭터가 어떤 파티에 있는지 확인
const isCharacterInAnyParty = (characterId: string): boolean => {
  return parties.value.some(party => 
    party.slots.some(slot => slot.characterId === characterId)
  )
}

// 모든 파티에서 캐릭터 제거
const removeCharacterFromAllParties = (characterId: string) => {
  parties.value.forEach(party => {
    party.slots.forEach(slot => {
      if (slot.characterId === characterId) {
        slot.characterId = undefined
        slot.characterName = undefined
        slot.job = undefined
        slot.fame = undefined
        slot.totalDamage = undefined
        slot.buffPower = undefined
      }
    })
  })
}

// 파티 효율성 계산
const calculatePartyEfficiency = (slots: any[]): number => {
  const validSlots = slots.filter(slot => slot.characterId)
  if (validSlots.length === 0) return 0
  
  let totalEfficiency = 0
  validSlots.forEach(slot => {
    // 명성 기반 효율성 계산
    if (slot.fame) {
      totalEfficiency += slot.fame
    }
    // 직업별 보너스
    if (slot.job?.includes('딜러') && slot.totalDamage) {
      totalEfficiency += slot.totalDamage / 1000000 // 백만 단위로 정규화
    }
    if (slot.job?.includes('버퍼') && slot.buffPower) {
      totalEfficiency += slot.buffPower / 1000 // 천 단위로 정규화
    }
  })
  
  return totalEfficiency / validSlots.length
}

// 자동 파티 채우기
const autoFillParties = () => {
  const availableCharacters = filteredCharacters.value.filter(char => !isCharacterInAnyParty(char.characterId))
  
  if (availableCharacters.length === 0) return
  
  // 파티별로 균등하게 분배
  let charIndex = 0
  parties.value.forEach(party => {
    party.slots.forEach((slot, slotIndex) => {
      if (!slot.characterId && charIndex < availableCharacters.length) {
        placeCharacterInSlot(
          parties.value.indexOf(party), 
          slotIndex, 
          availableCharacters[charIndex]
        )
        charIndex++
      }
    })
  })
}

// 전체 파티 초기화
const clearAllParties = () => {
  parties.value.forEach(party => {
    party.slots.forEach(slot => {
      slot.characterId = undefined
      slot.characterName = undefined
      slot.job = undefined
      slot.fame = undefined
      slot.totalDamage = undefined
      slot.buffPower = undefined
    })
  })
}

// 파티 최적화 실행 가능 여부
const canExecutePartyOptimization = computed(() => {
  return selectedServer.value && 
         selectedDungeon.value && 
         selectedStrategy.value && 
         parties.value.some(party => 
           party.slots.some(slot => slot.characterId)
         )
})

// 파티 최적화 실행
const executePartyOptimization = async () => {
  if (!canExecutePartyOptimization.value) return
  
  try {
    isOptimizing.value = true
    errorMessage.value = ''
    
    const request = {
      serverId: selectedServer.value,
      dungeonName: selectedDungeon.value,
      partySize: partySize.value,
      optimizationStrategy: selectedStrategy.value,
      parties: parties.value
    }
    
    const response = await apiFetch(`/party-optimization/optimize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const result = await response.json()
    
    if (result.error) {
      throw new Error(result.message)
    }
    
    optimizationResult.value = result
    
  } catch (error) {
    console.error('파티 최적화 실행 중 오류:', error)
    errorMessage.value = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    isOptimizing.value = false
  }
}

// 파티 밸런스 조정
const optimizePartyBalance = async () => {
  if (!canExecutePartyOptimization.value) return
  
  try {
    isOptimizing.value = true
    errorMessage.value = ''
    
    const request = {
      serverId: selectedServer.value,
      dungeonName: selectedDungeon.value,
      partySize: partySize.value,
      parties: parties.value
    }
    
    const response = await apiFetch('/party-optimization/balance', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const result = await response.json()
    
    if (result.error) {
      throw new Error(result.message)
    }
    
    // 밸런스 조정된 파티 정보로 업데이트
    if (result.optimizedParties) {
      parties.value = result.optimizedParties
    }
    
  } catch (error) {
    console.error('파티 밸런스 조정 중 오류:', error)
    errorMessage.value = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    isOptimizing.value = false
  }
}

// 전략 비교 분석
const compareAllStrategies = async () => {
  if (!canExecutePartyOptimization.value) return
  
  try {
    isOptimizing.value = true
    errorMessage.value = ''
    
    const request = {
      serverId: selectedServer.value,
      dungeonName: selectedDungeon.value,
      partySize: partySize.value,
      parties: parties.value
    }
    
    const response = await apiFetch('/party-optimization/compare', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const result = await response.json()
    
    if (result.error) {
      throw new Error(result.message)
    }
    
    comparisonResult.value = result
    
  } catch (error) {
    console.error('전략 비교 분석 중 오류:', error)
    errorMessage.value = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    isOptimizing.value = false
  }
}

// 성능 테스트
const runPerformanceTest = async () => {
  try {
    isOptimizing.value = true
    errorMessage.value = ''
    
    const response = await apiFetch('/party-optimization/performance-test', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        serverId: selectedServer.value,
        dungeonName: selectedDungeon.value,
        partySize: partySize.value,
        parties: parties.value
      })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const result = await response.json()
    
    if (result.error) {
      throw new Error(result.message)
    }
    
    performanceResult.value = result
    
  } catch (error) {
    console.error('성능 테스트 중 오류:', error)
    errorMessage.value = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
  } finally {
    isOptimizing.value = false
  }
}

// 서버 변경 시 던전 로드
const onServerChange = async () => {
  if (selectedServer.value) {
    await characterStore.loadCharacters(selectedServer.value)
    // 던전 목록 설정
    dungeons.value = dungeonsList
    initializeParties()
  }
}

// 전체 파티 효율성 계산
const totalEfficiency = computed(() => {
  return parties.value.reduce((sum, party) => sum + (party.efficiency || 0), 0)
})

// 파티 최적화 결과 표시
const displayOptimizationResult = computed(() => {
  if (optimizationResult.value) {
    if (optimizationResult.value.error) {
      return `❌ 오류: ${optimizationResult.value.message}`
    }
    return `✅ 최적화 완료! 총 효율성: ${formatScore(optimizationResult.value.totalEfficiency || optimizationResult.value.efficiency)}`
  }
  return ''
})

// 비교 분석 결과 표시
const displayComparisonResult = computed(() => {
  if (comparisonResult.value) {
    if (comparisonResult.value.error) {
      return `❌ 오류: ${comparisonResult.value.message}`
    }
    return `✅ 전략 비교 완료! 추천 전략: ${getStrategyInfo(comparisonResult.value.recommendedStrategy).name}`
  }
  return ''
})

// 성능 테스트 결과 표시
const displayPerformanceResult = computed(() => {
  if (performanceResult.value) {
    if (performanceResult.value.error) {
      return `❌ 오류: ${performanceResult.value.message}`
    }
    return `✅ 성능 테스트 완료! 평균 실행 시간: ${performanceResult.value.averageExecutionTime.toFixed(2)}ms`
  }
  return ''
})

// 에러 메시지 표시
const displayErrorMessage = computed(() => {
  if (errorMessage.value) {
    return `❌ 오류: ${errorMessage.value}`
  }
  return ''
})

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  await characterStore.loadServers()
  servers.value = characterStore.servers
  initializeParties()
})

// 서버 변경 시 캐릭터 로드
watch(() => selectedServer.value, async (newServer) => {
  if (newServer) {
    await characterStore.loadCharacters(newServer)
    characters.value = characterStore.characters
    initializeParties()
  }
})

// 파티 크기 변경 시 파티 초기화
watch(() => partySize.value, () => {
  initializeParties()
})

// 파티 수 변경 시 파티 초기화
watch(() => partyCount.value, () => {
  initializeParties()
})

// 파티 구성 변경 시 효율성 재계산
watch(() => parties.value, () => {
  parties.value.forEach(party => {
    party.efficiency = calculatePartyEfficiency(party.slots)
  })
}, { deep: true })
</script>

<style scoped>
.advanced-party-optimization {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.header h1 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 1.1em;
}

.configuration-section,
.character-selection-section,
.optimization-section,
.results-section,
.performance-section {
  margin-bottom: 30px;
}

.config-card,
.selection-card,
.optimization-card,
.results-card,
.performance-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border: 1px solid #e1e8ed;
}

.config-card h3,
.selection-card h3,
.optimization-card h3,
.results-card h3,
.performance-card h3 {
  color: #2c3e50;
  margin-bottom: 20px;
  font-size: 1.3em;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-weight: 600;
  margin-bottom: 8px;
  color: #34495e;
}

.form-group select,
.form-group input {
  padding: 12px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group select:focus,
.form-group input:focus {
  outline: none;
  border-color: #3498db;
}

.character-count-display {
  display: flex;
  align-items: center;
  gap: 5px;
}

.character-count-display .count {
  font-size: 1.5em;
  font-weight: bold;
  color: #3498db;
}

.character-count-display .max {
  color: #7f8c8d;
}

.strategy-info {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.strategy-info h4 {
  margin: 0 0 8px 0;
  color: #2c3e50;
}

/* ========================================
   반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 태블릿 (1024px 이하) */
@media screen and (max-width: 1024px) {
  .advanced-party-optimization {
    padding: 15px;
  }
  
  .config-card,
  .selection-card,
  .optimization-card,
  .results-card,
  .performance-card {
    padding: 20px;
  }
  
  .form-row {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 15px;
  }
}

/* 태블릿 (768px 이하) */
@media screen and (max-width: 768px) {
  .advanced-party-optimization {
    padding: 12px;
  }
  
  .header {
    margin-bottom: 25px;
  }
  
  .header h1 {
    font-size: 1.8rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .config-card,
  .selection-card,
  .optimization-card,
  .results-card,
  .performance-card {
    padding: 16px;
    margin-bottom: 20px;
  }
  
  .config-card h3,
  .selection-card h3,
  .optimization-card h3,
  .results-card h3,
  .performance-card h3 {
    font-size: 1.2em;
    margin-bottom: 15px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .form-group select,
  .form-group input {
    padding: 10px;
    font-size: 14px;
  }
}

/* 중형 모바일 (600px 이하) */
@media screen and (max-width: 600px) {
  .advanced-party-optimization {
    padding: 10px;
  }
  
  .header {
    margin-bottom: 20px;
  }
  
  .header h1 {
    font-size: 1.6rem;
  }
  
  .subtitle {
    font-size: 0.9rem;
  }
  
  .config-card,
  .selection-card,
  .optimization-card,
  .results-card,
  .performance-card {
    padding: 14px;
    margin-bottom: 15px;
  }
  
  .config-card h3,
  .selection-card h3,
  .optimization-card h3,
  .results-card h3,
  .performance-card h3 {
    font-size: 1.1em;
    margin-bottom: 12px;
  }
  
  .form-group select,
  .form-group input {
    padding: 8px;
    font-size: 13px;
  }
  
  .strategy-info {
    padding: 12px;
  }
}

/* 소형 모바일 (480px 이하) */
@media screen and (max-width: 480px) {
  .advanced-party-optimization {
    padding: 8px;
  }
  
  .header {
    margin-bottom: 15px;
  }
  
  .header h1 {
    font-size: 1.4rem;
  }
  
  .subtitle {
    font-size: 0.85rem;
  }
  
  .config-card,
  .selection-card,
  .optimization-card,
  .results-card,
  .performance-card {
    padding: 12px;
    margin-bottom: 12px;
  }
  
  .config-card h3,
  .selection-card h3,
  .optimization-card h3,
  .results-card h3,
  .performance-card h3 {
    font-size: 1em;
    margin-bottom: 10px;
  }
  
  .form-group select,
  .form-group input {
    padding: 6px;
    font-size: 12px;
  }
  
  .strategy-info {
    padding: 10px;
  }
}

/* 초소형 모바일 (320px 이하) */
@media screen and (max-width: 320px) {
  .advanced-party-optimization {
    padding: 5px;
  }
  
  .header {
    margin-bottom: 12px;
  }
  
  .header h1 {
    font-size: 1.2rem;
  }
  
  .subtitle {
    font-size: 0.8rem;
  }
  
  .config-card,
  .selection-card,
  .optimization-card,
  .results-card,
  .performance-card {
    padding: 10px;
    margin-bottom: 10px;
  }
  
  .form-group select,
  .form-group input {
    padding: 5px;
    font-size: 11px;
  }
  
  .strategy-info {
    padding: 8px;
  }
  
  /* 추가 글자 크기 최적화 */
  .form-group label {
    font-size: 0.8rem;
  }
  
  .strategy-info h4 {
    font-size: 0.9rem;
  }
  
  .strategy-info p {
    font-size: 0.7rem;
  }
  
  .character-count-display {
    font-size: 0.8rem;
  }
  
  .character-count-display .count {
    font-size: 1.2rem;
  }
}

.strategy-info p {
  margin: 0;
  color: #7f8c8d;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-bar input {
  flex: 1;
  padding: 12px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 14px;
}

.clear-btn {
  padding: 12px 20px;
  background: #95a5a6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.clear-btn:hover {
  background: #7f8c8d;
}

.character-filters {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.filter-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
}

.character-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
  max-height: 400px;
  overflow-y: auto;
}

.character-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.character-item:hover {
  border-color: #3498db;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.character-item.selected {
  border-color: #27ae60;
  background: #e8f5e8;
}

.character-item.favorite {
  border-left: 4px solid #f39c12;
}

.character-item.dealer {
  border-left: 4px solid #e74c3c;
}

.character-item.buffer {
  border-left: 4px solid #9b59b6;
}

.character-info {
  flex: 1;
}

.character-name {
  font-weight: bold;
  font-size: 1.1em;
  color: #2c3e50;
  margin-bottom: 4px;
}

.character-job {
  color: #7f8c8d;
  font-size: 0.9em;
  margin-bottom: 8px;
}

.character-stats {
  display: flex;
  gap: 15px;
  font-size: 0.85em;
}

.character-stats .fame {
  color: #27ae60;
}

.character-stats .combat-power {
  color: #e74c3c;
}

.selection-indicator {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.selected-icon {
  color: #27ae60;
  font-size: 1.2em;
  font-weight: bold;
}

.optimization-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.execute-btn,
.compare-btn,
.performance-btn {
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.execute-btn {
  background: #3498db;
  color: white;
}

.execute-btn:hover:not(:disabled) {
  background: #2980b9;
  transform: translateY(-2px);
}

.execute-btn:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

.compare-btn {
  background: #9b59b6;
  color: white;
}

.compare-btn:hover:not(:disabled) {
  background: #8e44ad;
}

.performance-btn {
  background: #f39c12;
  color: white;
}

.performance-btn:hover {
  background: #e67e22;
}

.optimization-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e1e8ed;
  border-top: 2px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.summary-item .label {
  font-weight: 600;
  color: #7f8c8d;
  font-size: 0.9em;
}

.summary-item .value {
  font-size: 1.1em;
  color: #2c3e50;
  font-weight: 600;
}

.eight-person-result,
.four-person-result {
  margin-top: 24px;
}

.party-display {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
}

.party-card {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
}

.party-card h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  text-align: center;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.party-slot {
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 2px solid #e1e8ed;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.party-slot.empty {
  background: #ecf0f1;
  color: #7f8c8d;
}

.slot-content {
  text-align: center;
  width: 100%;
}

.slot-content .character-name {
  font-weight: bold;
  margin-bottom: 4px;
  color: #2c3e50;
}

.slot-content .character-role {
  font-size: 0.9em;
  color: #7f8c8d;
  margin-bottom: 4px;
}

.slot-content .character-fame {
  font-size: 0.85em;
  color: #27ae60;
}

.party-score {
  text-align: center;
  font-weight: 600;
  color: #3498db;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e1e8ed;
}

.comparison-result {
  margin-top: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.comparison-result h4 {
  margin: 0 0 16px 0;
  color: #2c3e50;
}

.strategy-scores {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.strategy-score {
  background: white;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  border: 2px solid #e1e8ed;
  transition: all 0.3s;
}

.strategy-score.best {
  border-color: #27ae60;
  background: #e8f5e8;
  transform: scale(1.05);
}

.strategy-score .strategy-name {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.strategy-score .score-value {
  font-size: 1.2em;
  font-weight: bold;
  color: #3498db;
  margin-bottom: 4px;
}

.strategy-score .score-rank {
  font-size: 0.9em;
  color: #7f8c8d;
}

.recommendation {
  text-align: center;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
  color: #2c3e50;
}

.performance-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.metric .label {
  font-weight: 600;
  color: #7f8c8d;
}

.metric .value {
  font-weight: 600;
  color: #2c3e50;
}

.error-message {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}

.error-card {
  background: #e74c3c;
  color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  max-width: 400px;
}

.error-card h4 {
  margin: 0 0 10px 0;
}

.error-card p {
  margin: 0 0 15px 0;
}

.error-close-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.error-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .advanced-party-optimization {
    padding: 15px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .party-display {
    grid-template-columns: 1fr;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
  }
  
  .optimization-actions {
    flex-direction: column;
  }
  
  .strategy-scores {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
