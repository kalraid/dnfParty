<template>
  <div class="character-detail-page">
    <div class="page-header">
      <button @click="goBack" class="back-btn">← 뒤로가기</button>
      <h1>{{ character?.characterName }} 상세 정보</h1>
    </div>
    
    <div v-if="character" class="character-detail-card">
      <!-- 캐릭터 기본 정보 -->
      <div class="basic-info-section">
        <h2>기본 정보</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">서버:</span>
            <span class="info-value">{{ getServerName(character.serverId) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">모험단:</span>
            <span class="info-value">{{ character.adventureName || '정보 없음' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">레벨:</span>
            <span class="info-value">{{ character.level || 0 }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">직업:</span>
                            <span class="info-value">{{ formatJobName(character.jobGrowName || character.jobName || '') }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">명성:</span>
            <span class="info-value">{{ formatNumber(character.fame || 0) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 스탯 정보 -->
      <div class="stats-section">
        <h2>스탯 정보</h2>
        <div class="stats-grid">
          <div v-if="isBuffer(character)" class="stat-card buffer">
            <h3>버프력</h3>
            <div class="stat-value">{{ formatNumber(character.buffPower || 0) }}</div>
            <button @click="showManualInput(character, 'buffPower')" class="edit-btn">✏️ 수정</button>
          </div>
          <div v-if="isDealer(character)" class="stat-card dealer">
            <h3>총딜</h3>
            <div class="stat-value">{{ formatNumber(character.totalDamage || 0) }}</div>
            <button @click="showManualInput(character, 'totalDamage')" class="edit-btn">✏️ 수정</button>
          </div>
        </div>
      </div>
      
      <!-- 던전 클리어 현황 -->
      <div class="dungeon-section">
        <h2>던전 클리어 현황</h2>
        <div class="dungeon-grid">
          <div class="dungeon-item" :class="{ 'cleared': character.dungeonClearNabel }">
            <span class="dungeon-icon">🌟</span>
            <span class="dungeon-name">나벨</span>
            <span class="clear-status">{{ character.dungeonClearNabel ? 'O' : 'X' }}</span>
          </div>
          <div class="dungeon-item" :class="{ 'cleared': character.dungeonClearVenus }">
            <span class="dungeon-icon">⚡</span>
            <span class="dungeon-name">베누스</span>
            <span class="clear-status">{{ character.dungeonClearVenus ? 'O' : 'X' }}</span>
          </div>
          <div class="dungeon-item" :class="{ 'cleared': character.dungeonClearFog }">
            <span class="dungeon-icon">🌫️</span>
            <span class="dungeon-name">안개신</span>
            <span class="clear-status">{{ character.dungeonClearFog ? 'O' : 'X' }}</span>
          </div>
        </div>
      </div>
      
      <!-- 업둥이 설정 -->
      <div class="favorites-section">
        <h2>업둥이 설정</h2>
        <div class="favorites-grid">
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="character.isFavoriteNabel"
                @change="toggleFavorite('nabel', $event)"
              />
              <span class="dungeon-icon">🌟</span>
              <span class="dungeon-name">나벨 업둥이</span>
            </label>
          </div>
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="character.isFavoriteVenus"
                @change="toggleFavorite('venus', $event)"
              />
              <span class="dungeon-icon">⚡</span>
              <span class="dungeon-name">베누스 업둥이</span>
            </label>
          </div>
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="character.isFavoriteFog"
                @change="toggleFavorite('fog', $event)"
              />
              <span class="dungeon-icon">🌫️</span>
              <span class="dungeon-name">안개신 업둥이</span>
            </label>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 수동 입력 모달 -->
    <div v-if="showManualInputModal" class="manual-input-modal" @click.stop>
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ manualInputCharacter?.characterName }} - 수동 입력</h3>
          <button @click="hideManualInput" class="modal-close">×</button>
        </div>
        
        <div class="modal-body">
          <div class="input-group">
            <label>{{ manualInputType === 'buffPower' ? '버프력' : '총딜' }}:</label>
            <input 
              v-model="manualInputValue" 
              type="number" 
              :placeholder="manualInputType === 'buffPower' ? '버프력 입력' : '총딜 입력'"
              class="manual-input"
            >
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="saveManualInput" class="save-btn">저장</button>
          <button @click="hideManualInput" class="cancel-btn">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dfApi } from '@/services/dfApi'
import { isBuffer } from '../utils/characterUtils'

const route = useRoute()
const router = useRouter()

const character = ref<any>(null)
const showManualInputModal = ref(false)
const manualInputCharacter = ref<any>(null)
const manualInputType = ref<string>('')
const manualInputValue = ref<number>(0)

onMounted(async () => {
  const characterId = route.params.characterId as string
  if (characterId) {
    await loadCharacterDetail(characterId)
  }
})

const loadCharacterDetail = async (characterId: string) => {
  try {
    // characterId에서 serverId를 추출하거나, 기본값 사용
    // 실제로는 characterId를 통해 serverId를 알아내야 함
    const serverId = 'bakal' // 기본값, 실제로는 characterId로부터 추출해야 함
    
    const response = await dfApi.getCharacterDetail(serverId, characterId)
    if (response.success) {
      character.value = response.data
    } else {
      console.error('캐릭터 정보 로드 실패:', response.message)
    }
  } catch (error) {
    console.error('캐릭터 정보 로드 중 오류:', error)
  }
}

const goBack = () => {
  // 저장된 검색 상태가 있으면 복원
  const savedState = localStorage.getItem('characterSearchState')
  if (savedState) {
    try {
      const state = JSON.parse(savedState)
      // CharacterSearch 페이지로 돌아가면서 상태 전달
      router.push({
        path: '/character-search',
        query: { 
          restore: 'true',
          server: state.selectedServer,
          query: state.searchQuery
        }
      })
    } catch (error) {
      console.error('저장된 상태 복원 실패:', error)
      router.back()
    }
  } else {
    router.back()
  }
}

const getServerName = (serverId: string) => {
  const serverMap: { [key: string]: string } = {
    'bakal': '바칼',
    'cain': '카인',
    'casillas': '카시야스',
    'diregie': '디레지에',
    'hilder': '힐더',
    'prey': '프레이',
    'siroco': '시로코'
  }
  return serverMap[serverId] || serverId
}

const formatNumber = (num: number) => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만'
  }
  return num.toLocaleString()
}

// 직업명 포맷팅 함수
const formatJobName = (jobName: string): string => {
  if (!jobName || jobName === 'N/A') return 'N/A';
  
  // 괄호 안의 내용만 추출 (예: "귀검사(여) (베가본드)" → "베가본드")
  const match = jobName.match(/\(([^)]+)\)$/);
  if (match) {
    return match[1].replace(/眞\s*/, ''); // "眞" 문자도 제거
  }
  
  // 괄호가 없으면 "眞" 문자만 제거
  return jobName.replace(/眞\s*/, '');
};



const isDealer = (character: any): boolean => {
  return !isBuffer(character)
}

const showManualInput = (char: any, type: string) => {
  manualInputCharacter.value = char
  manualInputType.value = type
  manualInputValue.value = type === 'buffPower' ? (char.buffPower || 0) : (char.totalDamage || 0)
  showManualInputModal.value = true
}

const hideManualInput = () => {
  showManualInputModal.value = false
  manualInputCharacter.value = null
  manualInputType.value = ''
  manualInputValue.value = 0
}

const saveManualInput = async () => {
  try {
    const updateData = {
      characterId: manualInputCharacter.value.characterId,
      [manualInputType.value]: manualInputValue.value
    }
    
    const response = await dfApi.updateCharacterStatsManual(updateData)
    if (response.success) {
      // 캐릭터 정보 새로고침
      await loadCharacterDetail(manualInputCharacter.value.characterId)
      hideManualInput()
    } else {
      console.error('스탯 업데이트 실패:', response.message)
    }
  } catch (error) {
    console.error('스탯 업데이트 중 오류:', error)
  }
}

const toggleFavorite = async (dungeonType: string, event: Event) => {
  const isChecked = (event.target as HTMLInputElement).checked
  try {
    const updateData = {
      characterId: character.value.characterId,
      [`isFavorite${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}`]: isChecked
    }
    
    const response = await dfApi.updateCharacterFavorites(updateData)
    if (response.success) {
      // 캐릭터 정보 새로고침
      await loadCharacterDetail(character.value.characterId)
    } else {
      console.error('즐겨찾기 업데이트 실패:', response.message)
    }
  } catch (error) {
    console.error('즐겨찾기 업데이트 중 오류:', error)
  }
}
</script>

<style scoped>
.character-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.back-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
}

.back-btn:hover {
  background: #5a6268;
}

.page-header h1 {
  margin: 0;
  color: #2c3e50;
  font-size: 28px;
}

.character-detail-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.basic-info-section,
.stats-section,
.dungeon-section,
.favorites-section {
  padding: 24px;
  border-bottom: 1px solid #e9ecef;
}

.basic-info-section:last-child,
.stats-section:last-child,
.dungeon-section:last-child,
.favorites-section:last-child {
  border-bottom: none;
}

.basic-info-section h2,
.stats-section h2,
.dungeon-section h2,
.favorites-section h2 {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.info-label {
  font-weight: 600;
  color: #495057;
}

.info-value {
  color: #212529;
  font-weight: 500;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.stat-card {
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  border: 2px solid;
}

.stat-card.buffer {
  background: #e8f5e8;
  border-color: #28a745;
}

.stat-card.dealer {
  background: #fff3cd;
  border-color: #ffc107;
}

.stat-card h3 {
  margin: 0 0 16px 0;
  color: #2c3e50;
  font-size: 18px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 16px;
}

.stat-card.buffer .stat-value {
  color: #28a745;
}

.stat-card.dealer .stat-value {
  color: #e67e22;
}

/* ========================================
   반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 태블릿 (1024px 이하) */
@media screen and (max-width: 1024px) {
  .character-detail-page {
    padding: 15px;
  }
  
  .page-header {
    gap: 15px;
    margin-bottom: 25px;
  }
  
  .page-header h1 {
    font-size: 24px;
  }
  
  .basic-info-section,
  .stats-section,
  .dungeon-section,
  .favorites-section {
    padding: 20px;
  }
  
  .info-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 14px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 18px;
  }
}

/* 태블릿 (768px 이하) */
@media screen and (max-width: 768px) {
  .character-detail-page {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
    margin-bottom: 20px;
  }
  
  .page-header h1 {
    font-size: 22px;
  }
  
  .back-btn {
    padding: 8px 16px;
    font-size: 14px;
  }
  
  .basic-info-section,
  .stats-section,
  .dungeon-section,
  .favorites-section {
    padding: 16px;
  }
  
  .basic-info-section h2,
  .stats-section h2,
  .dungeon-section h2,
  .favorites-section h2 {
    font-size: 18px;
    margin-bottom: 15px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .stat-card {
    padding: 20px;
  }
  
  .stat-value {
    font-size: 28px;
  }
}

/* 중형 모바일 (600px 이하) */
@media screen and (max-width: 600px) {
  .character-detail-page {
    padding: 10px;
  }
  
  .page-header {
    gap: 12px;
    margin-bottom: 18px;
  }
  
  .page-header h1 {
    font-size: 20px;
  }
  
  .back-btn {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .basic-info-section,
  .stats-section,
  .dungeon-section,
  .favorites-section {
    padding: 14px;
  }
  
  .basic-info-section h2,
  .stats-section h2,
  .dungeon-section h2,
  .favorites-section h2 {
    font-size: 16px;
    margin-bottom: 12px;
  }
  
  .info-item {
    padding: 10px 12px;
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}

/* 소형 모바일 (480px 이하) */
@media screen and (max-width: 480px) {
  .character-detail-page {
    padding: 8px;
  }
  
  .page-header {
    gap: 10px;
    margin-bottom: 15px;
  }
  
  .page-header h1 {
    font-size: 18px;
  }
  
  .back-btn {
    padding: 5px 10px;
    font-size: 12px;
  }
  
  .basic-info-section,
  .stats-section,
  .dungeon-section,
  .favorites-section {
    padding: 12px;
  }
  
  .basic-info-section h2,
  .stats-section h2,
  .dungeon-section h2,
  .favorites-section h2 {
    font-size: 15px;
    margin-bottom: 10px;
  }
  
  .info-item {
    padding: 8px 10px;
  }
  
  .stat-card {
    padding: 14px;
  }
  
  .stat-value {
    font-size: 20px;
  }
}

/* 초소형 모바일 (320px 이하) */
@media screen and (max-width: 320px) {
  .character-detail-page {
    padding: 5px;
  }
  
  .page-header {
    gap: 8px;
    margin-bottom: 12px;
  }
  
  .page-header h1 {
    font-size: 16px;
  }
  
  .back-btn {
    padding: 4px 8px;
    font-size: 11px;
  }
  
  .basic-info-section,
  .stats-section,
  .dungeon-section,
  .favorites-section {
    padding: 10px;
  }
  
  .basic-info-section h2,
  .stats-section h2,
  .dungeon-section h2,
  .favorites-section h2 {
    font-size: 14px;
    margin-bottom: 8px;
  }
  
  .info-item {
    padding: 6px 8px;
  }
  
  .stat-card {
    padding: 12px;
  }
  
  .stat-value {
    font-size: 18px;
  }
  
  /* 추가 글자 크기 최적화 */
  .info-label,
  .info-value {
    font-size: 0.8rem;
  }
  
  .stat-card h3 {
    font-size: 0.9rem;
  }
  
  .dungeon-item {
    font-size: 0.8rem;
  }
  
  .favorite-item label {
    font-size: 0.8rem;
  }
}

.edit-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.edit-btn:hover {
  background: #0056b3;
}

.dungeon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.dungeon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  border: 2px solid;
  background: #fff5f5;
  border-color: #f44336;
}

.dungeon-item.cleared {
  background: #e8f5e8;
  border-color: #4caf50;
}

.dungeon-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.dungeon-name {
  font-weight: 600;
  color: #495057;
  margin-bottom: 8px;
}

.clear-status {
  font-size: 24px;
  font-weight: 700;
}

.dungeon-item:not(.cleared) .clear-status {
  color: #f44336;
}

.dungeon-item.cleared .clear-status {
  color: #4caf50;
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.favorite-item label {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  cursor: pointer;
}

.favorite-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
}

.dungeon-icon {
  font-size: 20px;
}

.dungeon-name {
  font-weight: 500;
  color: #495057;
}

/* 모달 스타일 */
.manual-input-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e9ecef;
}

.modal-header h3 {
  margin: 0;
  color: #2c3e50;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6c757d;
}

.modal-body {
  padding: 24px;
}

.input-group {
  margin-bottom: 20px;
}

.input-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #495057;
}

.manual-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 16px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 20px 24px;
  border-top: 1px solid #e9ecef;
}

.save-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.save-btn:hover {
  background: #218838;
}

.cancel-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.cancel-btn:hover {
  background: #5a6268;
}
</style>
