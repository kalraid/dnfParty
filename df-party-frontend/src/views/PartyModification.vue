<template>
  <div class="party-modification">
    <div class="header">
      <h1>파티 수정</h1>
      <div class="dungeon-selector">
        <label for="dungeon">던전 선택:</label>
        <select id="dungeon" v-model="selectedDungeon" @change="onDungeonChange">
          <option value="">던전을 선택하세요</option>
          <option value="나벨">나벨 (명성 63,000+)</option>
          <option value="베누스">베누스 (명성 41,929+)</option>
          <option value="안개신">안개신 (명성 32,253+)</option>
        </select>
      </div>
    </div>

    <div class="main-content">
      <!-- 왼쪽: 파티 구성 -->
      <div class="party-section">
        <div class="party-header">
          <h2>파티 구성</h2>
          <div class="party-controls">
            <button @click="createEightPersonParty" :disabled="!canCreateParty" class="btn-primary">
              8인 파티 구성
            </button>
            <button @click="validateParty" class="btn-secondary">
              파티 검증
            </button>
            <button @click="optimizeParty" class="btn-secondary">
              최적화 제안
            </button>
          </div>
        </div>

        <!-- 8인 파티 표시 -->
        <div v-if="eightPersonParty" class="eight-person-party">
          <div class="party-info">
            <div class="total-efficiency">
              <span class="label">전체 효율성:</span>
              <span class="value">{{ formatNumber(eightPersonParty.totalEfficiency) }}</span>
            </div>
            <div class="party-balance" :class="{ 'balanced': isPartyBalanced, 'unbalanced': !isPartyBalanced }">
              <span class="label">파티 밸런스:</span>
              <span class="value">{{ isPartyBalanced ? '균형' : '불균형' }}</span>
            </div>
          </div>

          <!-- 1파티 -->
          <div class="party-group">
            <h3>1파티 (3딜러 + 1버퍼)</h3>
            <div class="party-slots">
              <div
                v-for="slot in eightPersonParty.party1.slots"
                :key="slot.slotNumber"
                class="party-slot"
                :class="{ 'empty': !slot.characterId }"
                @dragover.prevent
                @drop="onDrop($event, slot, 'party1')"
                @dragenter.prevent
                @dragleave.prevent
              >
                <div
                  v-if="slot.characterId"
                  class="character-card"
                  draggable="true"
                  @dragstart="onDragStart($event, slot, 'party1')"
                  @click="selectCharacter(slot)"
                >
                  <div class="character-avatar">
                    <img :src="getJobImage(slot.jobName)" :alt="slot.jobName" class="job-icon">
                  </div>
                  <div class="character-info">
                    <div class="character-name">{{ slot.characterName }}</div>
                    <div class="character-role">{{ slot.role }}</div>
                    <div class="character-fame">{{ formatNumber(slot.fame) }}</div>
                  </div>
                  <div class="character-actions">
                    <button @click.stop="removeCharacter(slot, 'party1')" class="btn-remove">×</button>
                  </div>
                </div>
                <div v-else class="empty-slot">
                  <span>빈 슬롯 {{ slot.slotNumber }}</span>
                </div>
              </div>
            </div>
            <div class="party-efficiency">
              <span class="label">파티 효율성:</span>
              <span class="value">{{ formatNumber(eightPersonParty.party1.efficiency) }}</span>
            </div>
          </div>

          <!-- 2파티 -->
          <div class="party-group">
            <h3>2파티 (3딜러 + 1버퍼)</h3>
            <div class="party-slots">
              <div
                v-for="slot in eightPersonParty.party2.slots"
                :key="slot.slotNumber"
                class="party-slot"
                :class="{ 'empty': !slot.characterId }"
                @dragover.prevent
                @drop="onDrop($event, slot, 'party2')"
                @dragenter.prevent
                @dragleave.prevent"
              >
                <div
                  v-if="slot.characterId"
                  class="character-card"
                  draggable="true"
                  @dragstart="onDragStart($event, slot, 'party2')"
                  @click="selectCharacter(slot)"
                >
                  <div class="character-avatar">
                    <img :src="getJobImage(slot.jobName)" :alt="slot.jobName" class="job-icon">
                  </div>
                  <div class="character-info">
                    <div class="character-name">{{ slot.characterName }}</div>
                    <div class="character-role">{{ slot.role }}</div>
                    <div class="character-fame">{{ formatNumber(slot.fame) }}</div>
                  </div>
                  <div class="character-actions">
                    <button @click.stop="removeCharacter(slot, 'party2')" class="btn-remove">×</button>
                  </div>
                </div>
                <div v-else class="empty-slot">
                  <span>빈 슬롯 {{ slot.slotNumber }}</span>
                </div>
              </div>
            </div>
            <div class="party-efficiency">
              <span class="label">파티 효율성:</span>
              <span class="value">{{ formatNumber(eightPersonParty.party2.efficiency) }}</span>
            </div>
          </div>
        </div>

        <!-- 파티가 없을 때 -->
        <div v-else class="no-party">
          <p>8인 파티를 구성해주세요.</p>
        </div>
      </div>

      <!-- 오른쪽: 모험단별 캐릭터 목록 -->
      <div class="character-section">
        <div class="character-header">
          <h2>캐릭터 목록</h2>
          <div class="character-filters">
            <input
              v-model="characterSearch"
              type="text"
              placeholder="캐릭터 검색..."
              class="search-input"
            >
            <select v-model="selectedServer" @change="loadCharacters" class="server-select">
              <option value="">서버 선택</option>
              <option v-for="server in servers" :key="server.serverId" :value="server.serverId">
                {{ server.serverName }}
              </option>
            </select>
          </div>
        </div>

        <!-- 캐릭터 목록 -->
        <div class="character-list">
          <div
            v-for="character in filteredCharacters"
            :key="character.characterId"
            class="character-item"
            draggable="true"
            @dragstart="onDragStart($event, character, 'character')"
            @click="selectCharacter(character)"
          >
            <div class="character-avatar">
              <img :src="getJobImage(character.jobName || '')" :alt="character.jobName || 'Unknown'" class="job-icon">
            </div>
            <div class="character-details">
              <div class="character-name">{{ character.characterName }}</div>
              <div class="character-job">{{ character.jobName || 'Unknown' }}</div>
              <div class="character-stats">
                <span class="fame">명성: {{ formatNumber(character.fame || 0) }}</span>
                <span v-if="character.totalDamage" class="damage">전투력: {{ formatNumber(character.totalDamage) }}</span>
                <span v-if="character.buffPower" class="buff">버프력: {{ formatNumber(character.buffPower) }}</span>
              </div>
            </div>
            <div class="character-actions">
              <button
                @click.stop="toggleFavorite(character)"
                class="btn-favorite"
                :class="{ 'favorite': character.isFavorite }"
              >
                {{ character.isFavorite ? '★' : '☆' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 캐릭터가 없을 때 -->
        <div v-if="filteredCharacters.length === 0" class="no-characters">
          <p>캐릭터가 없습니다.</p>
        </div>
      </div>
    </div>

    <!-- 파티 분석 모달 -->
    <div v-if="showAnalysisModal" class="modal-overlay" @click="closeAnalysisModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>파티 분석 결과</h3>
          <button @click="closeAnalysisModal" class="btn-close">×</button>
        </div>
        <div class="modal-body">
          <div v-if="partyAnalysis" class="analysis-content">
            <div class="analysis-section">
              <h4>전체 통계</h4>
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="label">총 딜러:</span>
                  <span class="value">{{ partyAnalysis.totalDealers }}명</span>
                </div>
                <div class="stat-item">
                  <span class="label">총 버퍼:</span>
                  <span class="value">{{ partyAnalysis.totalBuffers }}명</span>
                </div>
                <div class="stat-item">
                  <span class="label">총 업둥이:</span>
                  <span class="value">{{ partyAnalysis.totalUpdoongis }}명</span>
                </div>
                <div class="stat-item">
                  <span class="label">평균 명성:</span>
                  <span class="value">{{ formatNumber(partyAnalysis.averageFame) }}</span>
                </div>
              </div>
            </div>
            <div class="analysis-section">
              <h4>파티별 효율성</h4>
              <div class="efficiency-comparison">
                <div class="efficiency-item">
                  <span class="label">1파티:</span>
                  <span class="value">{{ formatNumber(partyAnalysis.party1Efficiency) }}</span>
                </div>
                <div class="efficiency-item">
                  <span class="label">2파티:</span>
                  <span class="value">{{ formatNumber(partyAnalysis.party2Efficiency) }}</span>
                </div>
                <div class="efficiency-gap">
                  <span class="label">효율성 차이:</span>
                  <span class="value" :class="{ 'good': partyAnalysis.isBalanced, 'bad': !partyAnalysis.isBalanced }">
                    {{ formatNumber(partyAnalysis.efficiencyGap) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 최적화 제안 모달 -->
    <div v-if="showOptimizationModal" class="modal-overlay" @click="closeOptimizationModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>최적화 제안</h3>
          <button @click="closeOptimizationModal" class="btn-close">×</button>
        </div>
        <div class="modal-body">
          <div v-if="optimizationResult" class="optimization-content">
            <div class="optimization-status" :class="optimizationResult.optimization.improvement.toLowerCase()">
              <h4>{{ getOptimizationTitle(optimizationResult.optimization.improvement) }}</h4>
            </div>
            <div class="suggestions">
              <h5>제안사항:</h5>
              <ul>
                <li v-for="suggestion in optimizationResult.optimization.suggestions" :key="suggestion">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
            <div class="efficiency-comparison">
              <div class="efficiency-item">
                <span class="label">현재 효율성:</span>
                <span class="value">{{ formatNumber(optimizationResult.optimization.currentEfficiency) }}</span>
              </div>
              <div class="efficiency-item">
                <span class="label">새로운 효율성:</span>
                <span class="value">{{ formatNumber(optimizationResult.optimization.newEfficiency) }}</span>
              </div>
            </div>
            <div class="optimization-actions">
              <button @click="applyOptimization" class="btn-primary">최적화 적용</button>
              <button @click="closeOptimizationModal" class="btn-secondary">취소</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCharacterStore } from '@/stores/character'
import { usePartyStore } from '@/stores/party'
import type { Character, Server, PartySlot } from '@/types'

// 스토어
const characterStore = useCharacterStore()
const partyStore = usePartyStore()

// 상태
const selectedDungeon = ref('')
const selectedServer = ref('')
const characterSearch = ref('')
const servers = ref<Server[]>([])
const characters = ref<Character[]>([])
const eightPersonParty = ref<any>(null)
const showAnalysisModal = ref(false)
const showOptimizationModal = ref(false)
const partyAnalysis = ref<any>(null)
const optimizationResult = ref<any>(null)
const draggedItem = ref<any>(null)
const draggedSource = ref<string>('')

// 계산된 속성
const filteredCharacters = computed(() => {
  if (!characterSearch.value) return characters.value
  
  return characters.value.filter(character =>
    character.characterName.toLowerCase().includes(characterSearch.value.toLowerCase()) ||
    character.jobName?.toLowerCase().includes(characterSearch.value.toLowerCase())
  )
})

const canCreateParty = computed(() => {
  return selectedDungeon.value && characters.value.length >= 8
})

const isPartyBalanced = computed(() => {
  if (!eightPersonParty.value?.analysis) return false
  return eightPersonParty.value.analysis.isBalanced
})

// 메서드
const loadServers = async () => {
  try {
    const response = await fetch('/api/dfo/servers')
    const data = await response.json()
    servers.value = data
  } catch (error) {
    console.error('서버 목록 로드 실패:', error)
  }
}

const loadCharacters = async () => {
  if (!selectedServer.value) return
  
  try {
    const response = await fetch(`/api/characters/server/${selectedServer.value}`)
    const data = await response.json()
    characters.value = data
  } catch (error) {
    console.error('캐릭터 목록 로드 실패:', error)
  }
}

const createEightPersonParty = async () => {
  if (!canCreateParty.value) return
  
  try {
    const characterIds = characters.value.map(c => c.characterId)
    const response = await fetch('/api/eight-person-party/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        characterIds,
        dungeonName: selectedDungeon.value
      })
    })
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    eightPersonParty.value = result
  } catch (error) {
    console.error('8인 파티 구성 실패:', error)
    alert('8인 파티 구성에 실패했습니다.')
  }
}

const validateParty = async () => {
  if (!eightPersonParty.value) return
  
  try {
    const response = await fetch('/api/eight-person-party/validate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        party1: eightPersonParty.value.party1,
        party2: eightPersonParty.value.party2,
        dungeonName: selectedDungeon.value
      })
    })
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    // 검증 결과 표시
    if (result.isValid) {
      alert('파티 구성이 유효합니다!')
    } else {
      alert(`파티 구성에 문제가 있습니다:\n${result.errors.join('\n')}`)
    }
  } catch (error) {
    console.error('파티 검증 실패:', error)
    alert('파티 검증에 실패했습니다.')
  }
}

const optimizeParty = async () => {
  if (!eightPersonParty.value) return
  
  try {
    const characterIds = characters.value.map(c => c.characterId)
    const response = await fetch('/api/eight-person-party/optimize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        characterIds,
        dungeonName: selectedDungeon.value,
        currentParty: eightPersonParty.value
      })
    })
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    optimizationResult.value = result
    showOptimizationModal.value = true
  } catch (error) {
    console.error('파티 최적화 실패:', error)
    alert('파티 최적화에 실패했습니다.')
  }
}

const onDungeonChange = () => {
  // 던전 변경 시 파티 재구성
  if (eightPersonParty.value) {
    eightPersonParty.value = null
  }
}

const onDragStart = (event: DragEvent, item: any, source: string) => {
  draggedItem.value = item
  draggedSource.value = source
  event.dataTransfer?.setData('text/plain', JSON.stringify(item))
}

const onDrop = async (event: DragEvent, targetSlot: any, targetParty: string) => {
  event.preventDefault()
  
  if (!draggedItem.value || !eightPersonParty.value) return
  
  try {
    let response
    if (draggedSource.value === 'character') {
      // 캐릭터를 파티에 추가
      response = await fetch('/api/party-modification/add', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          party: eightPersonParty.value[targetParty],
          characterId: draggedItem.value.characterId,
          slotNumber: targetSlot.slotNumber - 1
        })
      })
    } else {
      // 파티 내에서 캐릭터 교체
      response = await fetch('/api/party-modification/swap', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          party: eightPersonParty.value[draggedSource.value],
          sourceSlot: draggedItem.value.slotNumber - 1,
          targetSlot: targetSlot.slotNumber - 1
        })
      })
    }
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    // 파티 업데이트
    eightPersonParty.value[targetParty] = result.party
    
    // 파티 재분석
    await analyzeParty()
    
  } catch (error) {
    console.error('캐릭터 이동 실패:', error)
    alert('캐릭터 이동에 실패했습니다.')
  }
  
  draggedItem.value = null
  draggedSource.value = ''
}

const removeCharacter = async (slot: any, partyKey: string) => {
  try {
    const response = await fetch('/api/party-modification/remove', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        party: eightPersonParty.value[partyKey],
        slotNumber: slot.slotNumber - 1
      })
    })
    
    const result = await response.json()
    if (result.error) {
      alert(result.error)
      return
    }
    
    // 파티 업데이트
    eightPersonParty.value[partyKey] = result.party
    
    // 파티 재분석
    await analyzeParty()
    
  } catch (error) {
    console.error('캐릭터 제거 실패:', error)
    alert('캐릭터 제거에 실패했습니다.')
  }
}

const analyzeParty = async () => {
  if (!eightPersonParty.value) return
  
  try {
    const response = await fetch('/api/eight-person-party/analyze', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        party1: eightPersonParty.value.party1,
        party2: eightPersonParty.value.party2
      })
    })
    
    const result = await response.json()
    if (result.error) {
      console.error('파티 분석 실패:', result.error)
      return
    }
    
    eightPersonParty.value.analysis = result
    
  } catch (error) {
    console.error('파티 분석 실패:', error)
  }
}

const selectCharacter = (character: any) => {
  // 캐릭터 선택 로직
  console.log('선택된 캐릭터:', character)
}

const toggleFavorite = async (character: Character) => {
  try {
    const response = await fetch(`/api/characters/${character.serverId}/${character.characterId}/favorite`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        isFavorite: !character.isFavorite
      })
    })
    
    if (response.ok) {
      character.isFavorite = !character.isFavorite
    }
  } catch (error) {
    console.error('즐겨찾기 토글 실패:', error)
  }
}

const applyOptimization = () => {
  if (!optimizationResult.value) return
  
  eightPersonParty.value = optimizationResult.value.newParty
  closeOptimizationModal()
}

const closeAnalysisModal = () => {
  showAnalysisModal.value = false
  partyAnalysis.value = null
}

const closeOptimizationModal = () => {
  showOptimizationModal.value = false
  optimizationResult.value = null
}

const getOptimizationTitle = (improvement: string) => {
  switch (improvement) {
    case 'BETTER': return '효율성 향상!'
    case 'WORSE': return '효율성 감소'
    case 'SAME': return '효율성 동일'
    case 'NEW': return '새로운 파티 구성'
    default: return '최적화 결과'
  }
}

const getJobImage = (jobName: string) => {
  // 직업별 이미지 반환 (추후 구현)
  return '/images/jobs/default.png'
}

const formatNumber = (num: number) => {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString()
}

// 라이프사이클
onMounted(() => {
  loadServers()
})
</script>

<style scoped>
.party-modification {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}

.header h1 {
  margin: 0;
  color: #333;
  font-size: 2rem;
}

.dungeon-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dungeon-selector label {
  font-weight: bold;
  color: #555;
}

.dungeon-selector select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  min-width: 200px;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  min-height: 600px;
}

/* 파티 섹션 */
.party-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e0e0e0;
}

.party-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ddd;
}

.party-header h2 {
  margin: 0;
  color: #333;
}

.party-controls {
  display: flex;
  gap: 10px;
}

.btn-primary, .btn-secondary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #545b62;
}

/* 8인 파티 */
.eight-person-party {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.party-info {
  display: flex;
  justify-content: space-between;
  background: white;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.total-efficiency, .party-balance {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.label {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.party-balance.balanced .value {
  color: #28a745;
}

.party-balance.unbalanced .value {
  color: #dc3545;
}

/* 파티 그룹 */
.party-group {
  background: white;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #ddd;
}

.party-group h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.2rem;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 15px;
}

.party-slot {
  min-height: 80px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.party-slot:hover {
  border-color: #007bff;
  background: #f8f9ff;
}

.party-slot.empty {
  background: #f8f9fa;
}

.character-card {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 10px;
  background: white;
  border-radius: 6px;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: all 0.2s;
}

.character-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.character-avatar {
  margin-right: 10px;
}

.job-icon {
  width: 32px;
  height: 32px;
  border-radius: 4px;
}

.character-info {
  flex: 1;
}

.character-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 2px;
}

.character-role {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.character-fame {
  font-size: 12px;
  color: #007bff;
  font-weight: bold;
}

.character-actions {
  margin-left: 10px;
}

.btn-remove {
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}

.empty-slot {
  color: #999;
  font-size: 14px;
}

.party-efficiency {
  text-align: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #ddd;
}

/* 캐릭터 섹션 */
.character-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e0e0e0;
}

.character-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ddd;
}

.character-header h2 {
  margin: 0 0 15px 0;
  color: #333;
}

.character-filters {
  display: flex;
  gap: 10px;
}

.search-input, .server-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.search-input {
  flex: 1;
}

.server-select {
  min-width: 150px;
}

.character-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 500px;
  overflow-y: auto;
}

.character-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: white;
  border-radius: 8px;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: all 0.2s;
}

.character-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.character-details {
  flex: 1;
  margin-left: 15px;
}

.character-name {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.character-job {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.character-stats {
  display: flex;
  gap: 15px;
  font-size: 12px;
}

.fame {
  color: #007bff;
  font-weight: bold;
}

.damage {
  color: #dc3545;
}

.buff {
  color: #28a745;
}

.btn-favorite {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #ccc;
  transition: color 0.2s;
}

.btn-favorite.favorite {
  color: #ffc107;
}

.no-characters, .no-party {
  text-align: center;
  padding: 40px;
  color: #666;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ddd;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.analysis-section, .optimization-content {
  margin-bottom: 20px;
}

.analysis-section h4, .optimization-content h4 {
  margin: 0 0 15px 0;
  color: #333;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.efficiency-comparison {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.efficiency-item {
  display: flex;
  justify-content: space-between;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.efficiency-gap .value.good {
  color: #28a745;
}

.efficiency-gap .value.bad {
  color: #dc3545;
}

.optimization-status.BETTER {
  color: #28a745;
}

.optimization-status.WORSE {
  color: #dc3545;
}

.optimization-status.SAME {
  color: #6c757d;
}

.optimization-status.NEW {
  color: #007bff;
}

.suggestions {
  margin: 20px 0;
}

.suggestions h5 {
  margin: 0 0 10px 0;
  color: #333;
}

.suggestions ul {
  margin: 0;
  padding-left: 20px;
}

.suggestions li {
  margin-bottom: 5px;
  color: #555;
}

.optimization-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 반응형 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .party-controls {
    flex-direction: column;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
  }
  
  .character-filters {
    flex-direction: column;
  }
}
</style>
