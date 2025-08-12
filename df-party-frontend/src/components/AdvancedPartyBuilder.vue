<template>
  <div class="advanced-party-builder">
    <div class="builder-header">
      <h3>고급 파티 구성</h3>
      <div class="builder-controls">
        <button @click="autoOptimize" class="btn btn-primary" :disabled="loading">
          🚀 자동 최적화
        </button>
        <button @click="validateParty" class="btn btn-secondary">
          ✅ 파티 검증
        </button>
        <button @click="clearParty" class="btn btn-secondary">
          🗑️ 초기화
        </button>
      </div>
    </div>

    <div class="builder-content">
      <!-- 캐릭터 풀 -->
      <div class="character-pool">
        <h4>캐릭터 풀</h4>
        <div class="pool-filters">
          <input 
            v-model="searchTerm" 
            type="text" 
            placeholder="캐릭터 검색..." 
            class="search-input"
          >
          <select v-model="roleFilter" class="role-filter">
            <option value="">모든 역할</option>
            <option value="딜러">딜러</option>
            <option value="버퍼">버퍼</option>
            <option value="업둥이">업둥이</option>
          </select>
        </div>
        
        <div class="character-list">
          <div
            v-for="character in filteredCharacters"
            :key="character.characterId"
            class="character-item"
            draggable="true"
            @dragstart="onDragStart($event, character)"
            @dragend="onDragEnd"
            :class="{ 'dragging': draggedCharacter?.characterId === character.characterId }"
          >
            <div class="character-avatar">
              <img :src="getJobImage(character.jobName)" :alt="character.jobName" class="job-icon">
            </div>
            <div class="character-info">
              <div class="character-name">{{ character.characterName }}</div>
              <div class="character-role">{{ character.jobName }}</div>
              <div class="character-fame">{{ formatNumber(character.fame) }}</div>
            </div>
            <div class="character-actions">
              <button @click="addToParty(character)" class="add-btn" title="파티에 추가">+</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 파티 구성 영역 -->
      <div class="party-builder">
        <div class="party-info">
          <h4>파티 구성</h4>
          <div class="party-stats">
            <span class="stat">효율성: {{ partyEfficiency.toFixed(1) }}</span>
            <span class="stat">밸런스: {{ partyBalance.toFixed(1) }}</span>
            <span class="stat">안전성: {{ partySafety.toFixed(1) }}</span>
          </div>
        </div>

        <!-- 8인 파티 슬롯 -->
        <div class="party-slots-container">
          <div class="party-group">
            <h5>1파티 (3딜러 + 1버퍼)</h5>
            <div class="party-slots">
              <div
                v-for="(slot, index) in party1Slots"
                :key="`party1-${index}`"
                class="party-slot"
                :class="{ 
                  'empty': !slot.character,
                  'drag-over': dragOverSlot === `party1-${index}`,
                  'valid': isValidSlot(slot, index, 'party1'),
                  'invalid': !isValidSlot(slot, index, 'party1')
                }"
                @dragover.prevent
                @drop="onDrop($event, slot, 'party1', index)"
                @dragenter.prevent
                @dragleave.prevent
                @click="selectSlot(slot, 'party1', index)"
              >
                <div v-if="slot.character" class="slot-content">
                  <div class="character-avatar">
                    <img :src="getJobImage(slot.character.jobName)" :alt="slot.character.jobName" class="job-icon">
                  </div>
                  <div class="character-info">
                    <div class="character-name">{{ slot.character.characterName }}</div>
                    <div class="character-role">{{ slot.role }}</div>
                    <div class="character-fame">{{ formatNumber(slot.character.fame) }}</div>
                  </div>
                  <div class="slot-actions">
                    <button @click.stop="removeFromSlot(slot, 'party1', index)" class="remove-btn" title="제거">×</button>
                  </div>
                </div>
                <div v-else class="empty-slot">
                  <div class="slot-role">{{ getSlotRole(index, 'party1') }}</div>
                  <div class="slot-hint">드래그하여 배치</div>
                </div>
              </div>
            </div>
          </div>

          <div class="party-group">
            <h5>2파티 (3딜러 + 1버퍼)</h5>
            <div class="party-slots">
              <div
                v-for="(slot, index) in party2Slots"
                :key="`party2-${index}`"
                class="party-slot"
                :class="{ 
                  'empty': !slot.character,
                  'drag-over': dragOverSlot === `party2-${index}`,
                  'valid': isValidSlot(slot, index, 'party2'),
                  'invalid': !isValidSlot(slot, index, 'party2')
                }"
                @dragover.prevent
                @drop="onDrop($event, slot, 'party2', index)"
                @dragenter.prevent
                @dragleave.prevent
                @click="selectSlot(slot, 'party2', index)"
              >
                <div v-if="slot.character" class="slot-content">
                  <div class="character-avatar">
                    <img :src="getJobImage(slot.character.jobName)" :alt="slot.character.jobName" class="job-icon">
                  </div>
                  <div class="character-info">
                    <div class="character-name">{{ slot.character.characterName }}</div>
                    <div class="character-role">{{ slot.role }}</div>
                    <div class="character-fame">{{ formatNumber(slot.character.fame) }}</div>
                  </div>
                  <div class="slot-actions">
                    <button @click.stop="removeFromSlot(slot, 'party2', index)" class="remove-btn" title="제거">×</button>
                  </div>
                </div>
                <div v-else class="empty-slot">
                  <div class="slot-role">{{ getSlotRole(index, 'party2') }}</div>
                  <div class="slot-hint">드래그하여 배치</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 파티 분석 -->
        <div class="party-analysis">
          <h5>파티 분석</h5>
          <div class="analysis-grid">
            <div class="analysis-item">
              <span class="label">전체 효율성:</span>
              <span class="value" :class="getEfficiencyClass(partyEfficiency)">
                {{ partyEfficiency.toFixed(1) }}
              </span>
            </div>
            <div class="analysis-item">
              <span class="label">역할 밸런스:</span>
              <span class="value" :class="getBalanceClass(partyBalance)">
                {{ partyBalance.toFixed(1) }}
              </span>
            </div>
            <div class="analysis-item">
              <span class="label">명성 분포:</span>
              <span class="value" :class="getSafetyClass(partySafety)">
                {{ partySafety.toFixed(1) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 로딩 오버레이 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <div class="loading-text">파티 최적화 중...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Character } from '@/types'

interface PartySlot {
  character: Character | null
  role: string
  slotNumber: number
}

interface Props {
  characters: Character[]
  dungeonName: string
}

const props = defineProps<Props>()

// 상태
const searchTerm = ref('')
const roleFilter = ref('')
const loading = ref(false)
const draggedCharacter = ref<Character | null>(null)
const dragOverSlot = ref<string | null>(null)

// 파티 슬롯 초기화
const party1Slots = ref<PartySlot[]>([
  { character: null, role: '딜러', slotNumber: 1 },
  { character: null, role: '딜러', slotNumber: 2 },
  { character: null, role: '딜러', slotNumber: 3 },
  { character: null, role: '버퍼', slotNumber: 4 }
])

const party2Slots = ref<PartySlot[]>([
  { character: null, role: '딜러', slotNumber: 5 },
  { character: null, role: '딜러', slotNumber: 6 },
  { character: null, role: '딜러', slotNumber: 7 },
  { character: null, role: '버퍼', slotNumber: 8 }
])

// 계산된 속성
const filteredCharacters = computed(() => {
  let filtered = props.characters

  if (searchTerm.value) {
    filtered = filtered.filter(c => 
      c.characterName.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
      c.jobName?.toLowerCase().includes(searchTerm.value.toLowerCase())
    )
  }

  if (roleFilter.value) {
    filtered = filtered.filter(c => c.jobName?.includes(roleFilter.value))
  }

  return filtered
})

const partyEfficiency = computed(() => {
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  const filledSlots = allSlots.filter(slot => slot.character)
  
  if (filledSlots.length === 0) return 0
  
  const totalFame = filledSlots.reduce((sum, slot) => sum + (slot.character?.fame || 0), 0)
  const avgFame = totalFame / filledSlots.length
  
  // 역할별 가중치 적용
  let roleBonus = 0
  const dealers = filledSlots.filter(slot => slot.role === '딜러').length
  const buffers = filledSlots.filter(slot => slot.role === '버퍼').length
  
  if (dealers >= 6 && buffers >= 2) roleBonus = 20
  else if (dealers >= 5 && buffers >= 2) roleBonus = 15
  else if (dealers >= 4 && buffers >= 1) roleBonus = 10
  
  return Math.min(100, (avgFame / 1000) + roleBonus)
})

const partyBalance = computed(() => {
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  const filledSlots = allSlots.filter(slot => slot.character)
  
  if (filledSlots.length === 0) return 0
  
  // 역할 밸런스 계산
  const dealers = filledSlots.filter(slot => slot.role === '딜러').length
  const buffers = filledSlots.filter(slot => slot.role === '버퍼').length
  
  const idealDealers = 6
  const idealBuffers = 2
  
  const dealerBalance = Math.max(0, 100 - Math.abs(dealers - idealDealers) * 20)
  const bufferBalance = Math.max(0, 100 - Math.abs(buffers - idealBuffers) * 30)
  
  return (dealerBalance + bufferBalance) / 2
})

const partySafety = computed(() => {
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  const filledSlots = allSlots.filter(slot => slot.character)
  
  if (filledSlots.length === 0) return 0
  
  // 명성 분포 계산
  const fames = filledSlots.map(slot => slot.character?.fame || 0).sort((a, b) => a - b)
  const minFame = Math.min(...fames)
  const maxFame = Math.max(...fames)
  const avgFame = fames.reduce((sum, fame) => sum + fame, 0) / fames.length
  
  // 명성 차이가 클수록 안전성 감소
  const fameSpread = (maxFame - minFame) / avgFame
  const safetyScore = Math.max(0, 100 - fameSpread * 50)
  
  return safetyScore
})

// 메서드
const onDragStart = (event: DragEvent, character: Character) => {
  draggedCharacter.value = character
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', character.characterId)
  }
}

const onDragEnd = () => {
  draggedCharacter.value = null
  dragOverSlot.value = null
}

const onDrop = (event: DragEvent, slot: PartySlot, partyType: string, index: number) => {
  event.preventDefault()
  
  if (!draggedCharacter.value) return
  
  // 슬롯에 캐릭터 배치
  slot.character = draggedCharacter.value
  slot.role = determineRole(draggedCharacter.value, index, partyType)
  
  // 다른 슬롯에서 중복 제거
  removeCharacterFromOtherSlots(draggedCharacter.value.characterId)
  
  dragOverSlot.value = null
}

const removeCharacterFromOtherSlots = (characterId: string) => {
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  allSlots.forEach(slot => {
    if (slot.character?.characterId === characterId && slot.character !== draggedCharacter.value) {
      slot.character = null
    }
  })
}

const determineRole = (character: Character, index: number, partyType: string): string => {
  if (partyType === 'party1') {
    return index === 3 ? '버퍼' : '딜러'
  } else {
    return index === 7 ? '버퍼' : '딜러'
  }
}

const getSlotRole = (index: number, partyType: string): string => {
  if (partyType === 'party1') {
    return index === 3 ? '버퍼' : '딜러'
  } else {
    return index === 7 ? '버퍼' : '딜러'
  }
}

const isValidSlot = (slot: PartySlot, index: number, partyType: string): boolean => {
  if (!slot.character) return true
  
  const expectedRole = getSlotRole(index, partyType)
  const actualRole = slot.role
  
  return expectedRole === actualRole
}

const addToParty = (character: Character) => {
  // 빈 슬롯 찾기
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  const emptySlot = allSlots.find(slot => !slot.character)
  
  if (emptySlot) {
    const slotIndex = allSlots.indexOf(emptySlot)
    const partyType = slotIndex < 4 ? 'party1' : 'party2'
    const localIndex = slotIndex % 4
    
    if (partyType === 'party1') {
      party1Slots.value[localIndex].character = character
      party1Slots.value[localIndex].role = determineRole(character, localIndex, partyType)
    } else {
      party2Slots.value[localIndex].character = character
      party2Slots.value[localIndex].role = determineRole(character, localIndex, partyType)
    }
    
    removeCharacterFromOtherSlots(character.characterId)
  }
}

const removeFromSlot = (slot: PartySlot, partyType: string, index: number) => {
  slot.character = null
}

const selectSlot = (slot: PartySlot, partyType: string, index: number) => {
  // 슬롯 선택 로직 (필요시 구현)
  console.log('슬롯 선택:', partyType, index, slot)
}

const autoOptimize = async () => {
  loading.value = true
  
  try {
    // 백엔드 API 호출하여 자동 최적화
    const characterIds = props.characters.map(c => c.characterId)
    const response = await fetch('/api/eight-person-party/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        characterIds,
        dungeonName: props.dungeonName
      })
    })
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    // 결과를 슬롯에 적용
    applyOptimizationResult(result)
    
  } catch (error) {
    console.error('자동 최적화 실패:', error)
    alert('자동 최적화에 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const applyOptimizationResult = (result: any) => {
  // 1파티 적용
  if (result.party1?.slots) {
    result.party1.slots.forEach((slotData: any, index: number) => {
      if (slotData.characterId) {
        const character = props.characters.find(c => c.characterId === slotData.characterId)
        if (character) {
          party1Slots.value[index].character = character
          party1Slots.value[index].role = slotData.role || getSlotRole(index, 'party1')
        }
      }
    })
  }
  
  // 2파티 적용
  if (result.party2?.slots) {
    result.party2.slots.forEach((slotData: any, index: number) => {
      if (slotData.characterId) {
        const character = props.characters.find(c => c.characterId === slotData.characterId)
        if (character) {
          party2Slots.value[index].character = character
          party2Slots.value[index].role = slotData.role || getSlotRole(index, 'party2')
        }
      }
    })
  }
}

const validateParty = () => {
  const allSlots = [...party1Slots.value, ...party2Slots.value]
  const filledSlots = allSlots.filter(slot => slot.character)
  
  if (filledSlots.length < 8) {
    alert('8명의 캐릭터가 필요합니다.')
    return
  }
  
  // 역할 검증
  const errors: string[] = []
  
  // 1파티 검증
  const party1Dealers = party1Slots.value.filter(slot => slot.role === '딜러' && slot.character).length
  const party1Buffers = party1Slots.value.filter(slot => slot.role === '버퍼' && slot.character).length
  
  if (party1Dealers < 3) errors.push('1파티에 딜러가 부족합니다.')
  if (party1Buffers < 1) errors.push('1파티에 버퍼가 부족합니다.')
  
  // 2파티 검증
  const party2Dealers = party2Slots.value.filter(slot => slot.role === '딜러' && slot.character).length
  const party2Buffers = party2Slots.value.filter(slot => slot.role === '버퍼' && slot.character).length
  
  if (party2Dealers < 3) errors.push('2파티에 딜러가 부족합니다.')
  if (party2Buffers < 1) errors.push('2파티에 버퍼가 부족합니다.')
  
  if (errors.length > 0) {
    alert(`파티 구성에 문제가 있습니다:\n${errors.join('\n')}`)
  } else {
    alert('파티 구성이 유효합니다!')
  }
}

const clearParty = () => {
  party1Slots.value.forEach(slot => slot.character = null)
  party2Slots.value.forEach(slot => slot.character = null)
}

const getJobImage = (jobName: string): string => {
  // 직업별 아이콘 이미지 반환 (실제 구현에서는 적절한 이미지 경로 사용)
  return `/icons/${jobName?.toLowerCase() || 'default'}.png`
}

const formatNumber = (num: number): string => {
  return num?.toLocaleString() || '0'
}

const getEfficiencyClass = (efficiency: number): string => {
  if (efficiency >= 80) return 'excellent'
  if (efficiency >= 60) return 'good'
  if (efficiency >= 40) return 'fair'
  return 'poor'
}

const getBalanceClass = (balance: number): string => {
  if (balance >= 80) return 'excellent'
  if (balance >= 60) return 'good'
  if (balance >= 40) return 'fair'
  return 'poor'
}

const getSafetyClass = (safety: number): string => {
  if (safety >= 80) return 'excellent'
  if (safety >= 60) return 'good'
  if (safety >= 40) return 'fair'
  return 'poor'
}

// 드래그 앤 드롭 이벤트 처리
const onDragOver = (event: DragEvent, slotId: string) => {
  event.preventDefault()
  dragOverSlot.value = slotId
}

const onDragLeave = () => {
  dragOverSlot.value = null
}
</script>

<style scoped>
.advanced-party-builder {
  position: relative;
  background: var(--color-background);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.builder-header {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  color: white;
  padding: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.builder-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: bold;
}

.builder-controls {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.builder-content {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 2rem;
  padding: 2rem;
  min-height: 600px;
}

.character-pool {
  background: var(--color-background-soft);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  border: 1px solid var(--color-border);
}

.character-pool h4 {
  margin: 0 0 1rem 0;
  color: var(--color-heading);
  font-size: 1.2rem;
}

.pool-filters {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-input, .role-filter {
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  background: var(--color-background);
  color: var(--color-text);
  font-size: 0.9rem;
}

.character-list {
  max-height: 500px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.character-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius);
  cursor: grab;
  transition: var(--transition);
  position: relative;
}

.character-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
}

.character-item.dragging {
  opacity: 0.5;
  transform: rotate(5deg);
}

.character-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-background-mute);
  display: flex;
  align-items: center;
  justify-content: center;
}

.job-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.character-name {
  font-weight: 600;
  color: var(--color-heading);
  font-size: 0.9rem;
  margin-bottom: 0.25rem;
}

.character-role {
  color: var(--color-text);
  font-size: 0.8rem;
  margin-bottom: 0.25rem;
}

.character-fame {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 0.8rem;
}

.character-actions {
  display: flex;
  gap: 0.25rem;
}

.add-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: var(--color-success);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: bold;
  transition: var(--transition);
}

.add-btn:hover {
  background: var(--color-accent);
  transform: scale(1.1);
}

.party-builder {
  background: var(--color-background-soft);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  border: 1px solid var(--color-border);
}

.party-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.party-info h4 {
  margin: 0;
  color: var(--color-heading);
  font-size: 1.2rem;
}

.party-stats {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.stat {
  background: var(--color-background);
  padding: 0.5rem 0.75rem;
  border-radius: var(--border-radius);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.party-slots-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  margin-bottom: 2rem;
}

.party-group h5 {
  margin: 0 0 1rem 0;
  color: var(--color-heading);
  font-size: 1rem;
  text-align: center;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.party-slot {
  min-height: 120px;
  border: 2px dashed var(--color-border);
  border-radius: var(--border-radius);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}

.party-slot.empty {
  background: var(--color-background-mute);
}

.party-slot.drag-over {
  border-color: var(--color-primary);
  background: var(--color-primary);
  opacity: 0.7;
  transform: scale(1.05);
}

.party-slot.valid {
  border-color: var(--color-success);
  background: var(--color-background);
}

.party-slot.invalid {
  border-color: var(--color-danger);
  background: var(--color-background);
}

.slot-content {
  width: 100%;
  text-align: center;
}

.empty-slot {
  text-align: center;
  color: var(--color-text);
}

.slot-role {
  font-weight: 600;
  color: var(--color-heading);
  margin-bottom: 0.5rem;
}

.slot-hint {
  font-size: 0.8rem;
  color: var(--color-text);
  opacity: 0.7;
}

.slot-actions {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}

.remove-btn {
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: var(--color-danger);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: bold;
  transition: var(--transition);
}

.remove-btn:hover {
  background: var(--color-danger);
  transform: scale(1.1);
}

.party-analysis {
  background: var(--color-background);
  border-radius: var(--border-radius);
  padding: 1rem;
  border: 1px solid var(--color-border);
}

.party-analysis h5 {
  margin: 0 0 1rem 0;
  color: var(--color-heading);
  font-size: 1rem;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.analysis-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0.75rem;
  background: var(--color-background-soft);
  border-radius: var(--border-radius);
  border: 1px solid var(--color-border);
}

.analysis-item .label {
  font-size: 0.8rem;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.analysis-item .value {
  font-size: 1.2rem;
  font-weight: bold;
}

.value.excellent { color: var(--color-success); }
.value.good { color: var(--color-accent); }
.value.fair { color: var(--color-warning); }
.value.poor { color: var(--color-danger); }

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  z-index: 1000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

.loading-text {
  font-size: 1.1rem;
  font-weight: 500;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 반응형 디자인 */
@media (max-width: 1024px) {
  .builder-content {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  
  .party-slots {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .builder-header {
    flex-direction: column;
    text-align: center;
  }
  
  .builder-controls {
    justify-content: center;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
  }
  
  .party-stats {
    flex-direction: column;
    align-items: center;
  }
}

@media (max-width: 480px) {
  .builder-content {
    padding: 1rem;
  }
  
  .character-item {
    flex-direction: column;
    text-align: center;
  }
  
  .character-actions {
    justify-content: center;
  }
}
</style>
