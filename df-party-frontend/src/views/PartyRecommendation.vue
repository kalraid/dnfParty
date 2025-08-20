<template>
  <div class="party-recommendation">
    <div class="header">
      <h1>🎯 파티 구성 추천 시스템</h1>
      <p>AI가 분석하여 최적의 파티 구성을 추천해드립니다</p>
    </div>

    <!-- 서버 및 캐릭터 선택 -->
    <div class="selection-section">
      <div class="server-selection">
        <h3>서버 선택</h3>
        <select v-model="selectedServer" @change="onServerChange">
          <option value="">서버를 선택하세요</option>
          <option v-for="server in servers" :key="server.serverId" :value="server.serverId">
            {{ server.serverName }}
          </option>
        </select>
      </div>

      <div class="character-selection">
        <h3>캐릭터 선택</h3>
        <div class="character-list">
          <div v-if="characters.length === 0" class="no-characters">
            서버를 선택하면 캐릭터 목록이 표시됩니다.
          </div>
          <div v-else class="character-grid">
            <div
              v-for="character in characters"
              :key="character.characterId"
              :class="['character-card', { selected: selectedCharacters.includes(character.characterId) }]"
              @click="toggleCharacter(character.characterId)"
            >
              <div class="character-info">
                <div class="character-name">{{ character.characterName }}</div>
                <div class="character-job">{{ character.jobName || '직업 없음' }}</div>
                <div class="character-fame">명성: {{ character.fame?.toLocaleString() || 0 }}</div>
                <div v-if="character.isFavorite" class="updoongi-badge">업둥이</div>
              </div>
            </div>
          </div>
        </div>
        <div class="selection-info">
          선택된 캐릭터: {{ selectedCharacters.length }}명
        </div>
      </div>
    </div>

    <!-- 던전 및 파티 크기 선택 -->
    <div class="dungeon-section">
      <h3>던전 및 파티 설정</h3>
      <div class="dungeon-settings">
        <div class="dungeon-select">
          <label>던전 선택:</label>
          <select v-model="selectedDungeon">
            <option value="">던전을 선택하세요</option>
            <option value="발할라">발할라</option>
            <option value="오즈마">오즈마</option>
            <option value="시로코">시로코</option>
            <option value="일반">일반 던전</option>
          </select>
        </div>
        
        <div class="party-size-select">
          <label>파티 크기:</label>
          <div class="radio-group">
            <label>
              <input type="radio" v-model="partySize" :value="4" :disabled="selectedCharacters.length < 4">
              4인 파티
            </label>
            <label>
              <input type="radio" v-model="partySize" :value="8" :disabled="selectedCharacters.length < 8">
              8인 파티
            </label>
          </div>
        </div>
      </div>
    </div>

    <!-- 추천 전략 선택 -->
    <div class="strategy-section">
      <h3>추천 전략 선택</h3>
      <div class="strategy-grid">
        <div
          v-for="strategy in availableStrategies"
          :key="strategy.id"
          :class="['strategy-card', { selected: selectedStrategy === strategy.id }]"
          @click="selectedStrategy = strategy.id"
        >
          <h4>{{ strategy.name }}</h4>
          <p>{{ strategy.description }}</p>
        </div>
      </div>
    </div>

    <!-- 개인화 설정 -->
    <div class="personalization-section">
      <h3>개인화 설정</h3>
      <div class="personalization-options">
        <label class="checkbox-option">
          <input type="checkbox" v-model="personalization.preferUpdoongis">
          업둥이 캐릭터 우선 배치
        </label>
        <label class="checkbox-option">
          <input type="checkbox" v-model="personalization.preferHighFame">
          고명성 캐릭터 우선 배치
        </label>
        <label class="checkbox-option">
          <input type="checkbox" v-model="personalization.preferJobSynergy">
          직업 시너지 고려
        </label>
      </div>
    </div>

    <!-- 추천 실행 버튼들 -->
    <div class="action-section">
      <h3>추천 실행</h3>
      <div class="action-buttons">
        <button
          @click="generateBasicRecommendation"
          :disabled="!canGenerateRecommendation"
          class="btn btn-primary"
        >
          🎯 기본 파티 추천
        </button>
        
        <button
          @click="generateDungeonSpecificRecommendations"
          :disabled="!canGenerateRecommendation"
          class="btn btn-secondary"
        >
          🏰 던전별 추천 전략
        </button>
        
        <button
          @click="generatePersonalizedRecommendation"
          :disabled="!canGenerateRecommendation"
          class="btn btn-success"
        >
          👤 개인화된 추천
        </button>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>AI가 최적의 파티 구성을 분석하고 있습니다...</p>
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="error-message">
      <p>❌ {{ error }}</p>
      <button @click="clearError" class="btn btn-error">에러 지우기</button>
    </div>

    <!-- 추천 결과 표시 -->
    <div v-if="recommendationResult" class="recommendation-result">
      <h3>🎉 파티 구성 추천 결과</h3>
      
      <!-- 추천 전략 정보 -->
      <div class="strategy-info">
        <h4>사용된 전략: {{ getStrategyName(recommendationResult.strategy) }}</h4>
        <p>생성 시간: {{ formatDate(recommendationResult.generatedAt) }}</p>
      </div>

      <!-- 던전 요구사항 -->
      <div class="dungeon-requirements">
        <h4>던전 요구사항</h4>
        <div class="requirements-grid">
          <div class="requirement-item">
            <span class="label">던전:</span>
            <span class="value">{{ recommendationResult.dungeonRequirements.dungeonName }}</span>
          </div>
          <div class="requirement-item">
            <span class="label">최소 명성:</span>
            <span class="value">{{ recommendationResult.dungeonRequirements.minFame?.toLocaleString() }}</span>
          </div>
          <div class="requirement-item">
            <span class="label">난이도:</span>
            <span class="value">{{ recommendationResult.dungeonRequirements.difficulty }}</span>
          </div>
        </div>
      </div>

      <!-- 캐릭터 풀 분석 -->
      <div class="character-pool-analysis">
        <h4>캐릭터 풀 분석</h4>
        <div class="pool-stats">
          <div class="stat-item">
            <span class="label">총 캐릭터:</span>
            <span class="value">{{ recommendationResult.characterAnalysis.totalCharacters }}명</span>
          </div>
          <div class="stat-item">
            <span class="label">딜러:</span>
            <span class="value">{{ recommendationResult.characterAnalysis.dealers }}명</span>
          </div>
          <div class="stat-item">
            <span class="label">버퍼:</span>
            <span class="value">{{ recommendationResult.characterAnalysis.buffers }}명</span>
          </div>
          <div class="stat-item">
            <span class="label">업둥이:</span>
            <span class="value">{{ recommendationResult.characterAnalysis.updoongis }}명</span>
          </div>
          <div class="stat-item">
            <span class="label">평균 명성:</span>
            <span class="value">{{ recommendationResult.characterAnalysis.averageFame?.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <!-- 추천 파티 구성 -->
      <div class="recommended-party">
        <h4>추천 파티 구성</h4>
        <div v-if="isEightPersonParty(recommendationResult.recommendation)" class="eight-person-party">
          <div class="party-group">
            <h5>1파티</h5>
            <PartyDisplay :party="recommendationResult.recommendation.party1" />
          </div>
          <div class="party-group">
            <h5>2파티</h5>
            <PartyDisplay :party="recommendationResult.recommendation.party2" />
          </div>
        </div>
        <div v-else class="four-person-party">
          <PartyDisplay :party="recommendationResult.recommendation" />
        </div>
      </div>

      <!-- 분석 결과 -->
      <div class="analysis-result">
        <h4>분석 결과</h4>
        <div class="analysis-grid">
          <div class="analysis-item">
            <span class="label">전체 점수:</span>
            <span class="value score">{{ recommendationResult.analysis.totalScore?.toFixed(1) }}</span>
          </div>
          <div class="analysis-item">
            <span class="label">던전 호환성:</span>
            <span class="value">{{ (recommendationResult.analysis.dungeonCompatibility * 100).toFixed(1) }}%</span>
          </div>
          <div class="analysis-item">
            <span class="label">전체 등급:</span>
            <span class="value">{{ (recommendationResult.analysis.overallRating * 100).toFixed(1) }}%</span>
          </div>
        </div>
      </div>

      <!-- 대안 파티 구성 -->
      <div v-if="recommendationResult.alternatives.length > 0" class="alternative-parties">
        <h4>대안 파티 구성</h4>
        <div class="alternatives-grid">
          <div
            v-for="(alternative, index) in recommendationResult.alternatives"
            :key="index"
            class="alternative-party"
          >
            <h5>{{ getStrategyName(alternative.strategy) }}</h5>
            <PartyDisplay :party="alternative" />
          </div>
        </div>
      </div>

      <!-- 액션 버튼 -->
      <div class="result-actions">
        <button @click="applyRecommendation" class="btn btn-primary">
          ✅ 이 파티 구성 적용하기
        </button>
        <button @click="clearRecommendationResult" class="btn btn-secondary">
          🔄 새로운 추천 받기
        </button>
      </div>
    </div>

    <!-- 던전별 추천 결과 -->
    <div v-if="dungeonSpecificRecommendations" class="dungeon-specific-result">
      <h3>🏰 던전별 추천 전략 결과</h3>
      
      <div class="optimal-strategy">
        <h4>최적 전략: {{ getStrategyName(dungeonSpecificRecommendations.optimalStrategy) }}</h4>
      </div>

      <div class="recommendations-grid">
        <div class="recommendation-group">
          <h4>4인 파티 추천</h4>
          <div v-if="isEightPersonParty(dungeonSpecificRecommendations.party4.recommendation)" class="eight-person-party">
            <div class="party-group">
              <h5>1파티</h5>
              <PartyDisplay :party="dungeonSpecificRecommendations.party4.recommendation.party1" />
            </div>
            <div class="party-group">
              <h5>2파티</h5>
              <PartyDisplay :party="dungeonSpecificRecommendations.party4.recommendation.party2" />
            </div>
          </div>
          <div v-else class="four-person-party">
            <PartyDisplay :party="dungeonSpecificRecommendations.party4.recommendation" />
          </div>
        </div>

        <div class="recommendation-group">
          <h4>8인 파티 추천</h4>
          <div v-if="isEightPersonParty(dungeonSpecificRecommendations.party8.recommendation)" class="eight-person-party">
            <div class="party-group">
              <h5>1파티</h5>
              <PartyDisplay :party="dungeonSpecificRecommendations.party8.recommendation.party1" />
            </div>
            <div class="party-group">
              <h5>2파티</h5>
              <PartyDisplay :party="dungeonSpecificRecommendations.party8.recommendation.party2" />
            </div>
          </div>
          <div v-else class="four-person-party">
            <PartyDisplay :party="dungeonSpecificRecommendations.party8.recommendation" />
          </div>
        </div>
      </div>

      <div class="result-actions">
        <button @click="applyDungeonSpecificRecommendation" class="btn btn-primary">
          ✅ 던전별 추천 적용하기
        </button>
        <button @click="clearDungeonSpecificResult" class="btn btn-secondary">
          🔄 새로운 추천 받기
        </button>
      </div>
    </div>

    <!-- 개인화된 추천 결과 -->
    <div v-if="personalizedRecommendation" class="personalized-result">
      <h3>👤 개인화된 파티 추천 결과</h3>
      
      <div class="personalization-factors">
        <h4>개인화 요소</h4>
        <div class="factors-grid">
          <div class="factor-item">
            <span class="label">선호 전략:</span>
            <span class="value">{{ getStrategyName(personalizedRecommendation.personalizationFactors.strategy) }}</span>
          </div>
          <div class="factor-item">
            <span class="label">업둥이 선호:</span>
            <span class="value">{{ personalizedRecommendation.personalizationFactors.preferUpdoongis ? '예' : '아니오' }}</span>
          </div>
          <div class="factor-item">
            <span class="label">고명성 선호:</span>
            <span class="value">{{ personalizedRecommendation.personalizationFactors.preferHighFame ? '예' : '아니오' }}</span>
          </div>
          <div class="factor-item">
            <span class="label">시너지 고려:</span>
            <span class="value">{{ personalizedRecommendation.personalizationFactors.preferJobSynergy ? '예' : '아니오' }}</span>
          </div>
        </div>
      </div>

      <!-- 파티 구성 표시 (기본 추천과 동일) -->
      <div class="recommended-party">
        <h4>개인화된 파티 구성</h4>
        <div v-if="isEightPersonParty(personalizedRecommendation.recommendation)" class="eight-person-party">
          <div class="party-group">
            <h5>1파티</h5>
            <PartyDisplay :party="personalizedRecommendation.recommendation.party1" />
          </div>
          <div class="party-group">
            <h5>2파티</h5>
            <PartyDisplay :party="personalizedRecommendation.recommendation.party2" />
          </div>
        </div>
        <div v-else class="four-person-party">
          <PartyDisplay :party="personalizedRecommendation.recommendation" />
        </div>
      </div>

      <div class="result-actions">
        <button @click="applyPersonalizedRecommendation" class="btn btn-primary">
          ✅ 개인화 추천 적용하기
        </button>
        <button @click="clearPersonalizedResult" class="btn btn-secondary">
          🔄 새로운 추천 받기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineComponent } from 'vue'
import { useCharacterStore } from '@/stores/character'
import { usePartyStore } from '@/stores/party'
import type { Character, Server, PartyRecommendationRequest, PersonalizedRecommendationRequest } from '@/types'

// 컴포넌트
const PartyDisplay = defineComponent({
  props: {
    party: {
      type: Object,
      required: true
    }
  },
  template: `
    <div class="party-display">
      <div v-if="party.slots" class="party-slots">
        <div v-for="slot in party.slots" :key="slot.slotNumber" class="party-slot">
          <div class="slot-number">{{ slot.slotNumber }}</div>
          <div v-if="slot.character" class="character-info">
            <div class="character-name">{{ slot.character.characterName }}</div>
            <div class="character-role">{{ slot.role }}</div>
            <div class="character-fame">{{ slot.character.fame?.toLocaleString() }}</div>
          </div>
          <div v-else class="empty-slot">빈 슬롯</div>
        </div>
      </div>
      <div v-if="party.efficiency" class="party-stats">
        <div class="stat">효율성: {{ party.efficiency.toFixed(1) }}</div>
      </div>
    </div>
  `
})

// 스토어
const characterStore = useCharacterStore()
const partyStore = usePartyStore()

// 반응형 상태
const selectedServer = ref('')
const selectedCharacters = ref<string[]>([])
const selectedDungeon = ref('')
const partySize = ref(4)
const selectedStrategy = ref('balance')
const personalization = ref({
  preferUpdoongis: true,
  preferHighFame: true,
  preferJobSynergy: true
})

// 계산된 속성
const servers = computed(() => characterStore.servers)
const characters = computed(() => characterStore.characters)
const loading = computed(() => partyStore.loading)
const error = computed(() => partyStore.error)
const recommendationResult = computed(() => partyStore.recommendationResult)
const dungeonSpecificRecommendations = computed(() => partyStore.dungeonSpecificRecommendations)
const personalizedRecommendation = computed(() => partyStore.personalizedRecommendation)

const canGenerateRecommendation = computed(() => {
  return selectedServer.value && 
         selectedCharacters.value.length >= partySize.value && 
         selectedDungeon.value
})

const availableStrategies = computed(() => [
  { id: 'efficiency', name: '효율성 중심', description: '최고 명성 캐릭터들을 우선 배치하여 파티 효율성을 극대화' },
  { id: 'balance', name: '밸런스 중심', description: '효율성과 안전성의 균형을 맞춘 파티 구성' },
  { id: 'safety', name: '안전성 중심', description: '업둥이 캐릭터를 우선 배치하여 안전한 파티 구성' },
  { id: 'synergy', name: '시너지 중심', description: '직업 간 시너지를 고려한 파티 구성' },
  { id: 'hybrid', name: '하이브리드', description: '여러 전략의 장점을 결합한 최적 파티 구성' }
])

// 메서드
const onServerChange = async () => {
  if (selectedServer.value) {
    await characterStore.loadCharacters(selectedServer.value)
    selectedCharacters.value = []
  }
}

const toggleCharacter = (characterId: string) => {
  const index = selectedCharacters.value.indexOf(characterId)
  if (index > -1) {
    selectedCharacters.value.splice(index, 1)
  } else {
    selectedCharacters.value.push(characterId)
  }
}

const generateBasicRecommendation = async () => {
  try {
    const request: PartyRecommendationRequest = {
      serverId: selectedServer.value,
      characterIds: selectedCharacters.value,
      dungeonName: selectedDungeon.value,
      partySize: partySize.value,
      preferences: {
        strategy: selectedStrategy.value,
        ...personalization.value
      }
    }
    
    await partyStore.generatePartyRecommendation(request)
  } catch (error) {
    console.error('기본 파티 추천 실패:', error)
  }
}

const generateDungeonSpecificRecommendations = async () => {
  try {
    await partyStore.generateDungeonSpecificRecommendations(
      selectedServer.value,
      selectedCharacters.value,
      selectedDungeon.value
    )
  } catch (error) {
    console.error('던전별 추천 전략 생성 실패:', error)
  }
}

const generatePersonalizedRecommendation = async () => {
  try {
    const request: PersonalizedRecommendationRequest = {
      serverId: selectedServer.value,
      characterIds: selectedCharacters.value,
      dungeonName: selectedDungeon.value,
      partySize: partySize.value,
      userPreferences: {
        strategy: selectedStrategy.value,
        ...personalization.value
      },
      playHistory: {
        frequentDungeons: [selectedDungeon.value],
        preferredPartySize: partySize.value,
        successRate: 0.8
      }
    }
    
    await partyStore.generatePersonalizedRecommendation(request)
  } catch (error) {
    console.error('개인화된 파티 추천 실패:', error)
  }
}

const applyRecommendation = () => {
  if (recommendationResult.value) {
    // 파티 스토어에 추천 결과 적용
    partyStore.loadPartyFromHistory(recommendationResult.value.recommendation)
    // 파티 수정 페이지로 이동
    // router.push('/party-modification')
  }
}

const applyDungeonSpecificRecommendation = () => {
  if (dungeonSpecificRecommendations.value) {
    // 8인 파티 추천을 우선 적용
    if (partySize.value === 8) {
      partyStore.loadPartyFromHistory(dungeonSpecificRecommendations.value.party8.recommendation)
    } else {
      partyStore.loadPartyFromHistory(dungeonSpecificRecommendations.value.party4.recommendation)
    }
    // 파티 수정 페이지로 이동
    // router.push('/party-modification')
  }
}

const applyPersonalizedRecommendation = () => {
  if (personalizedRecommendation.value) {
    partyStore.loadPartyFromHistory(personalizedRecommendation.value.recommendation)
    // 파티 수정 페이지로 이동
    // router.push('/party-modification')
  }
}

const clearRecommendationResult = () => {
  partyStore.clearRecommendationData()
}

const clearDungeonSpecificResult = () => {
  partyStore.clearRecommendationData()
}

const clearPersonalizedResult = () => {
  partyStore.clearRecommendationData()
}

const clearError = () => {
  partyStore.clearError()
}

const getStrategyName = (strategyId: string) => {
  const strategy = availableStrategies.value.find(s => s.id === strategyId)
  return strategy ? strategy.name : strategyId
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('ko-KR')
}

const isEightPersonParty = (party: any) => {
  return party && (party.party1 || party.party2)
}

// 라이프사이클
onMounted(async () => {
  await characterStore.loadServers()
})
</script>

<style scoped>
.party-recommendation {
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

.header p {
  color: #7f8c8d;
  font-size: 18px;
}

.selection-section {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
  margin-bottom: 30px;
}

.server-selection h3,
.character-selection h3 {
  margin-bottom: 15px;
  color: #2c3e50;
}

.server-selection select {
  width: 100%;
  padding: 10px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
}

.character-list {
  max-height: 400px;
  overflow-y: auto;
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 15px;
}

.no-characters {
  text-align: center;
  color: #7f8c8d;
  padding: 40px 20px;
}

.character-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.character-card {
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.character-card:hover {
  border-color: #3498db;
  transform: translateY(-2px);
}

.character-card.selected {
  border-color: #27ae60;
  background-color: #e8f5e8;
}

.character-info {
  text-align: center;
}

.character-name {
  font-weight: bold;
  margin-bottom: 5px;
  color: #2c3e50;
}

.character-job {
  color: #7f8c8d;
  margin-bottom: 5px;
}

.character-fame {
  color: #e74c3c;
  font-weight: bold;
}

.updoongi-badge {
  position: absolute;
  top: 5px;
  right: 5px;
}

/* ========================================
   반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 태블릿 (1024px 이하) */
@media screen and (max-width: 1024px) {
  .party-recommendation {
    padding: 15px;
  }
  
  .selection-section {
    gap: 20px;
    margin-bottom: 25px;
  }
  
  .character-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
  }
  
  .character-card {
    padding: 12px;
  }
}

/* 태블릿 (768px 이하) */
@media screen and (max-width: 768px) {
  .party-recommendation {
    padding: 12px;
  }
  
  .header {
    margin-bottom: 25px;
  }
  
  .header h1 {
    font-size: 1.8rem;
  }
  
  .header p {
    font-size: 16px;
  }
  
  .selection-section {
    grid-template-columns: 1fr;
    gap: 20px;
    margin-bottom: 20px;
  }
  
  .server-selection select {
    padding: 8px;
    font-size: 14px;
  }
  
  .character-list {
    max-height: 300px;
    padding: 12px;
  }
  
  .character-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
  }
  
  .character-card {
    padding: 10px;
  }
  
  .dungeon-settings {
    flex-direction: column;
    gap: 15px;
  }
  
  .strategy-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}

/* 중형 모바일 (600px 이하) */
@media screen and (max-width: 600px) {
  .party-recommendation {
    padding: 10px;
  }
  
  .header {
    margin-bottom: 20px;
  }
  
  .header h1 {
    font-size: 1.6rem;
  }
  
  .header p {
    font-size: 14px;
  }
  
  .selection-section {
    gap: 15px;
    margin-bottom: 15px;
  }
  
  .server-selection h3,
  .character-selection h3 {
    font-size: 1.1rem;
    margin-bottom: 10px;
  }
  
  .server-selection select {
    padding: 6px;
    font-size: 13px;
  }
  
  .character-list {
    max-height: 250px;
    padding: 10px;
  }
  
  .character-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 8px;
  }
  
  .character-card {
    padding: 8px;
  }
  
  .character-name {
    font-size: 0.9rem;
  }
  
  .character-job,
  .character-fame {
    font-size: 0.8rem;
  }
}

/* 소형 모바일 (480px 이하) */
@media screen and (max-width: 480px) {
  .party-recommendation {
    padding: 8px;
  }
  
  .header {
    margin-bottom: 15px;
  }
  
  .header h1 {
    font-size: 1.4rem;
  }
  
  .header p {
    font-size: 13px;
  }
  
  .selection-section {
    gap: 12px;
    margin-bottom: 12px;
  }
  
  .server-selection h3,
  .character-selection h3 {
    font-size: 1rem;
    margin-bottom: 8px;
  }
  
  .server-selection select {
    padding: 5px;
    font-size: 12px;
  }
  
  .character-list {
    max-height: 200px;
    padding: 8px;
  }
  
  .character-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 6px;
  }
  
  .character-card {
    padding: 6px;
  }
  
  .character-name {
    font-size: 0.8rem;
  }
  
  .character-job,
  .character-fame {
    font-size: 0.7rem;
  }
}

/* 초소형 모바일 (320px 이하) */
@media screen and (max-width: 320px) {
  .party-recommendation {
    padding: 5px;
  }
  
  .header {
    margin-bottom: 12px;
  }
  
  .header h1 {
    font-size: 1.2rem;
  }
  
  .header p {
    font-size: 12px;
  }
  
  .selection-section {
    gap: 10px;
    margin-bottom: 10px;
  }
  
  .server-selection h3,
  .character-selection h3 {
    font-size: 0.9rem;
    margin-bottom: 6px;
  }
  
  .server-selection select {
    padding: 4px;
    font-size: 11px;
  }
  
  .character-list {
    max-height: 180px;
    padding: 6px;
  }
  
  .character-grid {
    grid-template-columns: 1fr;
    gap: 5px;
  }
  
  .character-card {
    padding: 5px;
  }
  
  .character-name {
    font-size: 0.75rem;
  }
  
  .character-job,
  .character-fame {
    font-size: 0.65rem;
  }
  
  /* 추가 글자 크기 최적화 */
  .selection-info {
    font-size: 0.7rem;
  }
  
  .dungeon-section h3 {
    font-size: 0.9rem;
  }
  
  .dungeon-select label,
  .party-size-select label {
    font-size: 0.8rem;
  }
  
  .radio-group label {
    font-size: 0.7rem;
  }
  
  .strategy-section h3 {
    font-size: 0.9rem;
  }
  
  .strategy-card h4 {
    font-size: 0.8rem;
  }
  
  .strategy-card p {
    font-size: 0.7rem;
  }
}
  background-color: #f39c12;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
}

.selection-info {
  margin-top: 15px;
  text-align: center;
  font-weight: bold;
  color: #27ae60;
}

.dungeon-section,
.strategy-section,
.personalization-section {
  margin-bottom: 30px;
}

.dungeon-section h3,
.strategy-section h3,
.personalization-section h3 {
  margin-bottom: 15px;
  color: #2c3e50;
}

.dungeon-settings {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.dungeon-select select,
.party-size-select {
  width: 100%;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-group label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.strategy-card {
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.strategy-card:hover {
  border-color: #3498db;
  transform: translateY(-2px);
}

.strategy-card.selected {
  border-color: #27ae60;
  background-color: #e8f5e8;
}

.strategy-card h4 {
  margin-bottom: 10px;
  color: #2c3e50;
}

.strategy-card p {
  color: #7f8c8d;
  line-height: 1.5;
}

.personalization-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.checkbox-option {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.action-section {
  margin-bottom: 30px;
}

.action-section h3 {
  margin-bottom: 15px;
  color: #2c3e50;
}

.action-buttons {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #3498db;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-secondary {
  background-color: #95a5a6;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #7f8c8d;
}

.btn-success {
  background-color: #27ae60;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background-color: #229954;
}

.btn-error {
  background-color: #e74c3c;
  color: white;
}

.loading {
  text-align: center;
  padding: 40px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  background-color: #fdf2f2;
  border: 2px solid #fecaca;
  border-radius: 8px;
  padding: 20px;
  margin: 20px 0;
  text-align: center;
}

.error-message p {
  color: #dc2626;
  margin-bottom: 15px;
}

.recommendation-result,
.dungeon-specific-result,
.personalized-result {
  background-color: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 30px;
  margin: 30px 0;
}

.recommendation-result h3,
.dungeon-specific-result h3,
.personalized-result h3 {
  color: #2c3e50;
  margin-bottom: 25px;
  text-align: center;
}

.strategy-info,
.dungeon-requirements,
.character-pool-analysis,
.recommended-party,
.analysis-result,
.alternative-parties,
.personalization-factors {
  margin-bottom: 25px;
}

.strategy-info h4,
.dungeon-requirements h4,
.character-pool-analysis h4,
.recommended-party h4,
.analysis-result h4,
.alternative-parties h4,
.personalization-factors h4 {
  color: #2c3e50;
  margin-bottom: 15px;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 10px;
}

.requirements-grid,
.pool-stats,
.analysis-grid,
.factors-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.requirement-item,
.stat-item,
.analysis-item,
.factor-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background-color: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.requirement-item .label,
.stat-item .label,
.analysis-item .label,
.factor-item .label {
  font-weight: bold;
  color: #4a5568;
}

.requirement-item .value,
.stat-item .value,
.analysis-item .value,
.factor-item .value {
  color: #2c3e50;
}

.analysis-item .score {
  font-weight: bold;
  color: #27ae60;
}

.eight-person-party {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
}

.party-group h5 {
  color: #2c3e50;
  margin-bottom: 15px;
  text-align: center;
}

.party-display {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.party-slot {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px;
  text-align: center;
}

.slot-number {
  background-color: #3498db;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
  font-size: 12px;
  font-weight: bold;
}

.character-info .character-name {
  font-weight: bold;
  margin-bottom: 3px;
}

.character-info .character-role {
  color: #7f8c8d;
  font-size: 12px;
  margin-bottom: 3px;
}

.character-info .character-fame {
  color: #e74c3c;
  font-size: 12px;
  font-weight: bold;
}

.empty-slot {
  color: #bdc3c7;
  font-style: italic;
}

.party-stats {
  text-align: center;
  padding-top: 15px;
  border-top: 1px solid #e2e8f0;
}

.party-stats .stat {
  color: #27ae60;
  font-weight: bold;
}

.alternatives-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.alternative-party {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.alternative-party h5 {
  color: #2c3e50;
  margin-bottom: 15px;
  text-align: center;
}

.result-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .selection-section {
    grid-template-columns: 1fr;
  }
  
  .dungeon-settings {
    grid-template-columns: 1fr;
  }
  
  .eight-person-party {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .result-actions {
    flex-direction: column;
  }
}
</style>
