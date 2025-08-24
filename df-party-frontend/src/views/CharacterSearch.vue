<template>
  <div class="character-search">
    <h2>캐릭터 검색</h2>
    
    <!-- 검색 폼 -->
    <div class="search-form">
      <div class="form-group">
        <label for="searchMode">서버:</label>
        <select id="searchMode" v-model="searchMode" @change="onSearchModeChange" required>
          <option value="">서버를 선택하세요</option>
          <option v-for="server in servers" :key="server.serverId" :value="server.serverId">
            {{ server.serverName }}
          </option>
        </select>
      </div>
      
      <div class="form-group">
        <label for="characterName">캐릭터명:</label>
        <input 
          id="characterName"
          v-model="searchQuery" 
          type="text" 
          placeholder="캐릭터명을 입력하세요" 
          required
        >
      </div>
      
      <button @click="searchCharacters" :disabled="isSearchDisabled" class="search-btn">
        {{ searching ? '검색 중...' : '검색' }}
      </button>
      
      <!-- 메시지 표시 영역 (검색 버튼 바로 아래) -->
      <div class="message-area">
        <!-- 에러 메시지 -->
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <!-- 성공 메시지 -->
        <div v-if="successMessage" class="success-message">
          {{ successMessage }}
        </div>
      </div>
      
      <!-- 던담 동기화 버튼들 -->
      <div class="dundam-sync-controls" v-if="selectedAdventure">
        
        <!-- Playwright 버전 (활성화됨) -->
        <button @click="syncAdventureFromDundamPlaywright" class="dundam-sync-button playwright-enabled" :disabled="isSyncing">
          {{ isSyncing ? '🔄 동기화 중...' : '🚀 Playwright 동기화' }}
        </button>
        
        <span class="sync-status">
        </span>
      </div>
    </div>



    <!-- 검색 결과 카드 -->
    <div v-if="searchResults.length > 0" class="search-results">
      <h3>{{ searchMode === 'adventure' ? '모험단 캐릭터' : '검색 결과' }} ({{ searchResults.length }}개)</h3>
        
                        <!-- 전체 던담 동기화 진행바 -->
                <div v-if="isAutoSyncing" class="dundam-sync-progress">
                  <div class="progress-header">
                    <h4>🔄 던담 동기화 진행 중...</h4>
                    <span class="progress-text">{{ syncedCount }}/{{ searchResults.length }} 완료</span>
                  </div>
                  <div class="progress-bar-container">
                    <div class="progress-bar" :style="{ width: syncProgress + '%' }"></div>
                  </div>
                  <div class="progress-info">
                    <div class="current-syncing">
                      현재 동기화 중: {{ currentSyncingCharacter?.characterName || '대기 중...' }}
                    </div>
                    <div class="countdown-timer">
                      예상 대기시간: {{ Math.max(0, Math.ceil((10 - (syncProgress / 90 * 9)))) }}초
                    </div>
                  </div>
                  <div v-if="syncProgress >= 90 && !isCompleted" class="waiting-message">
                    ⏳ 조금만 더 기다려주세요...
                  </div>
                </div>
        
      <div class="results-grid">
        <div 
          v-for="character in searchResults" 
          :key="character.characterId" 
          class="dundam-character-card"
        >
          <div class="character-avatar">
            <div class="avatar-image">
              <!-- DFO API에서 가져온 캐릭터 이미지 또는 기본 플레이스홀더 -->
              <img 
                v-if="character.avatarImageUrl || character.characterImageUrl" 
                :src="character.avatarImageUrl || character.characterImageUrl"
                :alt="character.characterName"
                class="character-img"
                @error="handleImageError"
              />
              <div v-else class="avatar-placeholder">
                {{ character.characterName.charAt(0) }}
              </div>
            </div>
          </div>
          
          <div class="character-info">
            <!-- 서버 - 레벨 - 이름 순서로 표시 -->
            <div class="character-header">
              <span class="server-name">{{ getServerName(character.serverId) }}</span>
              <span class="level-display">Lv.{{ character.level || 0 }}</span>
              <span class="character-name">{{ character.characterName }}</span>
            </div>
            
            <!-- 모험단 정보 개선 -->
            <div class="adventure-name clickable-adventure" 
                 v-if="character.adventureName && character.adventureName !== 'N/A'"
                 @click="goToDungeonStatus(character.adventureName)"
                 :title="`${character.adventureName} 모험단의 던전 클리어 현황 보기`">
              {{ character.adventureName }}
            </div>
            <div v-else class="adventure-name no-adventure">
              모험단 정보 없음
            </div>
            
            <!-- 던전 클리어 상태 - "남은 숙제" 타이틀 추가, 상태 반전 -->
            <div class="dungeon-clear-section">
              <h4 class="dungeon-title">남은 숙제</h4>
              <div class="dungeon-clear-status">
                <div class="dungeon-status-item" :class="{ 'cleared': character.dungeonClearNabel && isNabelEligible(character) }">
                  <span class="dungeon-icon">🌟</span>
                  <span class="dungeon-name">나벨</span>
                  <span class="clear-status" :class="{ 'ineligible': !isNabelEligible(character) }">{{ getDungeonStatus(character, 'nabel') }}</span>
                </div>
                <div class="dungeon-status-item" :class="{ 'cleared': character.dungeonClearVenus && isVenusEligible(character) }">
                  <span class="dungeon-icon">⚡</span>
                  <span class="dungeon-name">베누스</span>
                  <span class="clear-status" :class="{ 'ineligible': !isVenusEligible(character) }">{{ getDungeonStatus(character, 'venus') }}</span>
                </div>
                <div class="dungeon-status-item" :class="{ 'cleared': character.dungeonClearFog && isFogEligible(character) }">
                  <span class="dungeon-icon">🌫️</span>
                  <span class="dungeon-name">안개신</span>
                  <span class="clear-status" :class="{ 'ineligible': !isFogEligible(character) }">{{ getDungeonStatus(character, 'fog') }}</span>
                </div>
                <div class="dungeon-status-item" :class="{ 'cleared': character.dungeonClearTwilight && isTwilightEligible(character) }">
                  <span class="dungeon-icon">🌅</span>
                  <span class="dungeon-name">이내 황혼전</span>
                  <span class="clear-status" :class="{ 'ineligible': !isTwilightEligible(character) }">{{ getDungeonStatus(character, 'twilight') }}</span>
                </div>
              </div>
            </div>
            
            <!-- 명성 정보를 버프력 위로 이동 -->
            <div class="fame-section">
              <span class="fame-label">명성:</span>
              <span class="fame-value">{{ formatNumber(character.fame || 0) }}</span>
            </div>
            
            <!-- 직업에 따른 스탯 표시 개선 -->
            <div class="stats-info">
              <!-- 버퍼인 경우 버프력만 표시 -->
              <div v-if="isBuffer(character)" class="stat-item buff-power">
                <span class="stat-label">버프력</span>
                <span class="stat-value">{{ formatNumber(character.buffPower || 0) }}</span>
                <button @click.stop="showManualInput(character, 'buffPower')" class="edit-btn">✏️</button>
              </div>
              
              <!-- 딜러인 경우 총딜만 표시 -->
              <div v-if="isDealer(character)" class="stat-item total-damage">
                <span class="stat-label">총딜</span>
                <span class="stat-value">{{ formatNumber(character.totalDamage || 0) }}</span>
                <button @click.stop="showManualInput(character, 'totalDamage')" class="edit-btn">✏️</button>
              </div>
            </div>
            
            <div class="job-info">
                              <span class="job-name">{{ formatJobName(character.jobGrowName || character.jobName || '') }}</span>
            </div>
            
            <!-- 던담 동기화 버튼 -->
            <div class="dundam-sync-section">
              <button 
                @click.stop="syncCharacterDundam(character)" 
                class="dundam-sync-btn"
                :disabled="character.isSyncing"
                :title="`${character.characterName}의 전투력/버프력 정보를 던담에서 동기화합니다`"
              >
                {{ character.isSyncing ? '🔄 동기화 중...' : '🔄 던담 동기화' }}
              </button>
            </div>
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
            <label>버프력:</label>
            <input 
              v-model="manualInputData.buffPower" 
              type="number" 
              placeholder="버프력 입력"
              class="manual-input"
            >
          </div>
          
          <div class="input-group">
            <label>총딜:</label>
            <input 
              v-model="manualInputData.totalDamage" 
              type="number" 
              placeholder="총딜 입력"
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



    <!-- 우클릭 컨텍스트 메뉴 -->
    <div v-if="showContextMenu" class="context-menu" :style="contextMenuStyle" @click.stop>
      <div class="context-header">
        <span class="context-character-name">{{ contextCharacter?.characterName }}</span>
        <button @click="hideContextMenu" class="context-close">×</button>
      </div>
      
      <div class="context-section">
        <h4>던전별 업둥이 설정</h4>
        <div class="dungeon-favorites">
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="dungeonFavorites.nabel"
                @change="toggleDungeonFavorite('nabel', $event)"
              />
              <span class="dungeon-icon">🌟</span>
              <span class="dungeon-name">나벨 업둥이</span>
            </label>
          </div>
          
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="dungeonFavorites.venus"
                @change="toggleDungeonFavorite('venus', $event)"
              />
              <span class="dungeon-icon">⚡</span>
              <span class="dungeon-name">베누스 업둥이</span>
            </label>
          </div>
          
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="dungeonFavorites.fog"
                @change="toggleDungeonFavorite('fog', $event)"
              />
              <span class="dungeon-icon">🌫️</span>
              <span class="dungeon-name">안개신 업둥이</span>
            </label>
          </div>
          
          <div class="favorite-item">
            <label>
              <input 
                type="checkbox" 
                :checked="dungeonFavorites.twilight"
                @change="toggleDungeonFavorite('twilight', $event)"
              />
              <span class="dungeon-icon">🌅</span>
                              <span class="dungeon-name">이내 황혼전 업둥이</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- Neople API 링크 -->
  <div class="neople-api-footer">
    <p>Powered by <a href="https://developers.neople.co.kr/" target="_blank" rel="noopener noreferrer">NeoPle OpenAPI</a></p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { dfApiService, type Server } from '../services/dfApi';
import { apiFetch } from '../config/api';
import sseService from '../services/sseService';
import { isBuffer } from '../utils/characterUtils';

const router = useRouter()

// 모험단 클릭 시 던전 클리어현황으로 이동
const goToDungeonStatus = (adventureName: string) => {
  console.log(`${adventureName} 모험단의 던전 클리어 현황으로 이동합니다.`);
  
  // 던전 클리어현황 페이지로 이동하면서 모험단 이름 전달
  router.push({
    name: 'dungeon-status',
    query: { adventure: adventureName }
  });
};

// 반응형 데이터
const selectedServer = ref('');
const searchMode = ref(''); // serverId (전체 서버 옵션 제거)
const searchQuery = ref(''); // 통합 검색어 (모험단명 또는 캐릭터명)
const servers = ref<Server[]>([]);
const searchResults = ref<any[]>([]);
const selectedCharacter = ref<any>(null);

const searching = ref(false);
const error = ref<string>('');
const successMessage = ref<string>('');



// 컨텍스트 메뉴 관련
const showContextMenu = ref(false);
const contextCharacter = ref<any>(null);
const contextMenuStyle = ref({});
const dungeonFavorites = ref({
  nabel: false,
  venus: false,
  fog: false,
  twilight: false
});

// 수동 입력 관련
const showManualInputModal = ref(false);
const manualInputCharacter = ref<any>(null);
const manualInputData = ref({
  buffPower: null as number | null,
  totalDamage: null as number | null
});

// 동기화 상태 관련
const syncStatus = ref({
  schedulerEnabled: false,
  isRunning: false,
  totalCharacters: 0,
  currentIndex: 0,
  lastFullSync: null as string | null,
  nextSyncIn: '1분 후',
  syncInterval: 60000
});

// 던담 동기화 관련
const isSyncing = ref(false);
const syncStatusMessage = ref('');
const selectedAdventure = ref<string | null>(null);

// 던담 동기화 관련 (자동 + 수동 동기화)
const isAutoSyncing = ref(false);
const syncedCount = ref(0);
const syncProgress = ref(0);
const currentSyncingCharacter = ref<any>(null);
const isCompleted = ref(false);

// 던전별 명성컷 기준
const DUNGEON_FAME_REQUIREMENTS = {
  nabel: 47684,      // 나벨
  venus: 41929,      // 베누스
  fog: 30135,        // 안개신
  twilight: 72688    // 이내 황혼전
};

// 던전 적격 여부 확인 메서드들
const isNabelEligible = (character: any) => {
  if (!character.fame) return false;
  return character.fame >= DUNGEON_FAME_REQUIREMENTS.nabel;
};

const isVenusEligible = (character: any) => {
  if (!character.fame) return false;
  return character.fame >= DUNGEON_FAME_REQUIREMENTS.venus;
};

const isFogEligible = (character: any) => {
  if (!character.fame) return false;
  return character.fame >= DUNGEON_FAME_REQUIREMENTS.fog;
};

const isTwilightEligible = (character: any) => {
  if (!character.fame) return false;
  return character.fame >= DUNGEON_FAME_REQUIREMENTS.twilight;
};

// 던전 상태 표시 메서드
const getDungeonStatus = (character: any, dungeonType: string) => {
  if (dungeonType === 'nabel') {
    if (!isNabelEligible(character)) return '명성 부족';
    return character.dungeonClearNabel ? 'X' : 'O';
  } else if (dungeonType === 'venus') {
    if (!isVenusEligible(character)) return '명성 부족';
    return character.dungeonClearVenus ? 'X' : 'O';
  } else if (dungeonType === 'fog') {
    if (!isFogEligible(character)) return '명성 부족';
    return character.dungeonClearFog ? 'X' : 'O';
  } else if (dungeonType === 'twilight') {
    if (!isTwilightEligible(character)) return '명성 부족';
    return character.dungeonClearTwilight ? 'X' : 'O';
  }
  return 'O';
};

// WebSocket 이벤트 리스너 등록
const handleCharacterUpdated = (event: any) => {
  console.log('캐릭터 업데이트 이벤트 수신:', event);
  
  if (event.type === 'CHARACTER_UPDATED' && event.data) {
    const { characterId, serverId, updateResult, characterInfo } = event.data;
    
    // 현재 검색 결과에서 해당 캐릭터 찾기
    const characterIndex = searchResults.value.findIndex(
      char => char.characterId === characterId && char.serverId === serverId
    );
    
    if (characterIndex !== -1) {
      // 캐릭터 정보 업데이트
      const character = searchResults.value[characterIndex];
      
      console.log(`🔄 SSE 업데이트 시작: ${character.characterName} (${character.adventureName})`);
      console.log(`   기존 스탯 - 전투력: ${character.combatPower}, 버프력: ${character.buffPower}, 총딜: ${character.totalDamage}`);
      
      // ⚠️ SSE로 받은 데이터에서 명성과 레벨은 업데이트하지 않음 (DFO API가 소스)
      // characterInfo에서 직접 값 가져오기 (백엔드에서 추가된 필드)
      if (characterInfo) {
        // 총딜 업데이트 (0인 경우 기존값 유지)
        if (characterInfo.totalDamage !== undefined && characterInfo.totalDamage !== null) {
          if (characterInfo.totalDamage === 0 && character.totalDamage && character.totalDamage > 0) {
            console.log(`   ❌ ${character.characterName} 총딜 0값 무시, 기존값 유지: ${character.totalDamage}`);
          } else {
            const oldTotalDamage = character.totalDamage;
            character.totalDamage = characterInfo.totalDamage;
            console.log(`   ✅ ${character.characterName} 총딜 업데이트: ${oldTotalDamage} → ${characterInfo.totalDamage}`);
          }
        }
        
        // 버프력 업데이트
        if (characterInfo.buffPower !== undefined && characterInfo.buffPower !== null) {
          const oldBuffPower = character.buffPower;
          character.buffPower = characterInfo.buffPower;
          console.log(`   ✅ ${character.characterName} 버프력 업데이트: ${oldBuffPower} → ${characterInfo.buffPower}`);
        }
        
        // 전투력 업데이트
        if (characterInfo.combatPower !== undefined && characterInfo.combatPower !== null) {
          const oldCombatPower = character.combatPower;
          character.combatPower = characterInfo.combatPower;
          console.log(`   ✅ ${character.characterName} 전투력 업데이트: ${oldCombatPower} → ${characterInfo.combatPower}`);
        }
      }
      
      // updateResult에서도 확인 (기존 로직 유지)
      if (updateResult && updateResult.characterInfo) {
        const { buffPower, totalDamage, combatPower } = updateResult.characterInfo;
        if (buffPower !== undefined && buffPower !== null) {
          const oldBuffPower = character.buffPower;
          character.buffPower = buffPower;
          console.log(`   ✅ ${character.characterName} 버프력 업데이트 (updateResult): ${oldBuffPower} → ${buffPower}`);
        }
        if (totalDamage !== undefined && totalDamage !== null) {
          // totalDamage가 0이고 기존값이 유효한 경우 기존값 유지
          if (totalDamage === 0 && character.totalDamage && character.totalDamage > 0) {
            console.log(`   ❌ ${character.characterName} 총딜 0값 무시, 기존값 유지: ${character.totalDamage}`);
          } else {
            const oldTotalDamage = character.totalDamage;
            character.totalDamage = totalDamage;
            console.log(`   ✅ ${character.characterName} 총딜 업데이트 (updateResult): ${oldTotalDamage} → ${totalDamage}`);
          }
        }
        if (combatPower !== undefined && combatPower !== null) {
          const oldCombatPower = character.combatPower;
          character.combatPower = combatPower;
          console.log(`   ✅ ${character.characterName} 전투력 업데이트 (updateResult): ${oldCombatPower} → ${combatPower}`);
        }
      }
      
      console.log(`   업데이트 후 스탯 - 전투력: ${character.combatPower}, 버프력: ${character.buffPower}, 총딜: ${character.totalDamage}`);
      console.log(`🎉 ${character.characterName} 정보가 SSE로 업데이트되었습니다.`);
      
      // UI 강제 업데이트를 위해 배열 재할당
      searchResults.value = [...searchResults.value];
    }
  }
};

  // 컴포넌트 마운트 시 서버 목록 로드 및 SSE 연결
onMounted(async () => {
    // SSE 연결
    try {
      await sseService.connect();
      
      // CHARACTER_UPDATED 이벤트 리스너 등록
      sseService.addEventListener('CHARACTER_UPDATED', handleCharacterUpdated);
      
      console.log('SSE 연결 및 이벤트 리스너 등록 완료');
    } catch (error) {
      console.error('SSE 연결 실패:', error);
      console.log('SSE 연결 실패로 인해 실시간 업데이트가 비활성화됩니다. 던담 동기화는 정상 작동합니다.');
    }
  
  // 저장된 검색 상태 복원
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('restore') === 'true') {
    const savedState = localStorage.getItem('characterSearchState')
    if (savedState) {
      try {
        const state = JSON.parse(savedState)
        searchQuery.value = state.searchQuery || ''
        selectedServer.value = state.selectedServer || ''
        searchResults.value = state.searchResults || []
        selectedCharacter.value = state.selectedCharacter || null
        
        // 복원 후 저장된 상태 삭제
        localStorage.removeItem('characterSearchState')
      } catch (error) {
        console.error('저장된 상태 복원 실패:', error)
      }
    }
  }
  
  // 서버 목록 로드
  await loadServers()
})

// 컴포넌트 언마운트 시 SSE 연결 해제
onUnmounted(() => {
  sseService.removeEventListener('CHARACTER_UPDATED', handleCharacterUpdated);
  sseService.disconnect();
  console.log('SSE 연결 해제 완료');
})

// 검색 버튼 비활성화 상태
const isSearchDisabled = computed(() => {
  return searching.value || !searchMode.value || searchMode.value === '';
});





// 자동 던담 동기화 메서드
const startAutoDundamSync = async () => {
  if (searchResults.value.length === 0) {
    return;
  }
  
  try {
    // 진행바 상태 초기화 및 활성화
    isAutoSyncing.value = true;
    syncedCount.value = 0;
    syncProgress.value = 0;
    isCompleted.value = false;
    error.value = '';
    
    console.log('자동 던담 동기화 시작:', searchResults.value.length, '개 캐릭터');
    
    // 카운트다운 진행바 설정 (10초 → 1초)
    const maxWaitTime = 10000; // 10초
    const minWaitTime = 1000;  // 1초
    const countdownInterval = 100; // 100ms마다 업데이트
    let currentWaitTime = maxWaitTime;
    
    // 카운트다운 진행바 시작
    const countdownTimer = setInterval(() => {
      if (currentWaitTime > minWaitTime) {
        currentWaitTime -= countdownInterval;
        // 진행률 계산: 10초에서 1초로 줄어들면서 0%에서 90%까지
        const progressRatio = (maxWaitTime - currentWaitTime) / (maxWaitTime - minWaitTime);
        syncProgress.value = progressRatio * 90;
      }
    }, countdownInterval);
    
    for (let i = 0; i < searchResults.value.length; i++) {
      const character = searchResults.value[i];
      currentSyncingCharacter.value = character;
      
      // 현재 캐릭터를 동기화 중 상태로 설정
      character.isSyncing = true;
      
      try {
        console.log(`캐릭터 ${i + 1}/${searchResults.value.length} 동기화 중:`, character.characterName);
        
        // 던담 동기화 API 호출
        const response = await apiFetch(`/dundam-sync/character/${character.serverId}/${character.characterId}?method=playwright`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          const result = await response.json();
          if (result.success) {
            // 성공 시 캐릭터 정보 업데이트 (WebSocket으로 실시간 업데이트됨)
            if (result.characterInfo) {
              if (result.characterInfo.buffPower !== undefined) {
                character.buffPower = result.characterInfo.buffPower;
              }
              if (result.characterInfo.totalDamage !== undefined) {
                character.totalDamage = result.characterInfo.totalDamage;
              }
              if (result.characterInfo.combatPower !== undefined) {
                character.combatPower = result.characterInfo.combatPower;
              }
            }
            syncedCount.value++;
            console.log(`${character.characterName} 동기화 성공`);
          } else {
            console.warn(`${character.characterName} 동기화 실패:`, result.message);
          }
        } else {
          console.warn(`${character.characterName} 동기화 요청 실패:`, response.status);
        }
        
        // 1초 대기 (API 제한 고려)
        await new Promise(resolve => setTimeout(resolve, 1000));
        
      } catch (err) {
        console.error(`${character.characterName} 동기화 중 오류:`, err);
      } finally {
        // 동기화 완료 후 상태 해제
        character.isSyncing = false;
      }
    }
    
    // 카운트다운 타이머 정리
    clearInterval(countdownTimer);
    
    // 성공 시 급격하게 100%로 채우기
    syncProgress.value = 100;
    isCompleted.value = true;
    
    // 진행바에서 성공 메시지 표시
    successMessage.value = `완료되었습니다! 던담 동기화: ${syncedCount.value}/${searchResults.value.length} 성공`;
    console.log('자동 던담 동기화 완료');
    
    // WebSocket으로 실시간 데이터 업데이트 대기 (2초 후 진행바 숨김)
    setTimeout(() => {
      // 진행바와 메시지 숨김
      successMessage.value = '';
      isAutoSyncing.value = false;
      currentSyncingCharacter.value = null;
      isCompleted.value = false;
      syncProgress.value = 0;
    }, 2000);
    
  } catch (err) {
    console.error('자동 던담 동기화 실패:', err);
    error.value = '던담 동기화 중 오류가 발생했습니다.';
    isAutoSyncing.value = false;
    currentSyncingCharacter.value = null;
    syncProgress.value = 0;
    isCompleted.value = false;
  }
};

// 던담 동기화 메서드 (Playwright)
const syncAdventureFromDundamPlaywright = async () => {
  if (!selectedAdventure.value) {
    error.value = '동기화할 모험단이 선택되지 않았습니다.';
    return;
  }
  
  try {
    isSyncing.value = true;
    syncStatusMessage.value = 'Playwright로 던담에서 모험단 정보를 동기화하고 있습니다...';
    error.value = '';
    
    const response = await apiFetch(`/dundam-sync/adventure/${encodeURIComponent(selectedAdventure.value)}?method=playwright`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    const result = await response.json();
    
    if (result.success) {
      successMessage.value = result.message;
      syncStatusMessage.value = `Playwright 동기화 완료: ${result.successCount}개 성공, ${result.failCount}개 실패`;
      
      // 검색 결과 새로고침
      if (searchResults.value.length > 0) {
        await searchCharacters();
      }
    } else {
      error.value = result.message || 'Playwright 던담 동기화에 실패했습니다.';
      syncStatusMessage.value = 'Playwright 동기화 실패';
    }
  } catch (err) {
    console.error('Playwright 던담 동기화 실패:', err);
    error.value = 'Playwright 던담 동기화 중 오류가 발생했습니다.';
    syncStatusMessage.value = 'Playwright 동기화 오류';
  } finally {
    isSyncing.value = false;
  }
};

/**
 * 개별 캐릭터의 던담 동기화
 */
const syncCharacterDundam = async (character: any) => {
  try {
    // 동기화 상태 설정
    character.isSyncing = true;
    
    console.log('캐릭터 던담 동기화 시작:', character.characterName);
    
    // 던담 동기화 API 호출
    const response = await apiFetch(`/dundam-sync/character/${character.serverId}/${character.characterId}?method=playwright`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 성공 시 캐릭터 정보 업데이트
        if (result.characterInfo) {
          if (result.characterInfo.buffPower !== undefined) {
            character.buffPower = result.characterInfo.buffPower;
          }
          if (result.characterInfo.totalDamage !== undefined) {
            character.totalDamage = result.characterInfo.totalDamage;
          }
          if (result.characterInfo.combatPower !== undefined) {
            character.combatPower = result.characterInfo.combatPower;
          }
        }
        
        successMessage.value = `${character.characterName}의 던담 정보 동기화가 완료되었습니다.`;
        console.log('캐릭터 던담 동기화 완료:', result);
      } else {
        error.value = `${character.characterName}의 던담 동기화에 실패했습니다: ${result.message || '알 수 없는 오류'}`;
      }
    } else {
      error.value = `${character.characterName}의 던담 동기화 요청이 실패했습니다.`;
    }
    
  } catch (err) {
    console.error('캐릭터 던담 동기화 실패:', err);
    error.value = `${character.characterName}의 던담 동기화 중 오류가 발생했습니다.`;
  } finally {
    // 동기화 상태 해제
    character.isSyncing = false;
  }
};

// 서버 목록 로드
const loadServers = async () => {
  try {
    const serverList = await dfApiService.getServers();
    servers.value = serverList;
  } catch (err) {
    console.error('서버 목록 로드 실패:', err);
    error.value = '서버 목록을 불러오는데 실패했습니다.';
  }
};





// 캐릭터 검색
const searchCharacters = async () => {
  if (!searchMode.value) {
    error.value = '서버를 선택해주세요.';
    return;
  }
  
  if (!searchQuery.value.trim()) {
    error.value = '캐릭터명을 입력해주세요.';
    return;
  }

  try {
    searching.value = true;
    error.value = '';
    successMessage.value = '';
    selectedCharacter.value = null; // 검색 시 선택된 캐릭터 초기화
    
    // 새로운 검색 시 이전 진행바 상태 초기화
    isAutoSyncing.value = false;
    syncedCount.value = 0;
    syncProgress.value = 0;
    currentSyncingCharacter.value = null;
    isCompleted.value = false;

    // 캐릭터 검색 (DFO API 호출)
    const serverId = searchMode.value;
    const response = await apiFetch(`/characters/search?serverId=${serverId}&characterName=${encodeURIComponent(searchQuery.value)}`);
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        searchResults.value = Array.isArray(data.characters) ? data.characters : [data.character];
        successMessage.value = `${searchResults.value.length}개의 캐릭터를 찾았습니다.`;
        
        // 검색 결과 디버그 출력
        console.log('캐릭터 검색 결과:', searchResults.value);
        searchResults.value.forEach((char: any, index: number) => {
          console.log(`캐릭터 ${index + 1}:`, {
            characterName: char.characterName,
            adventureName: char.adventureName,
            serverId: char.serverId,
            level: char.level,
              fame: char.fame
            });
          });
          
        // 모험단 모드일 때 selectedAdventure 설정
        if (searchMode.value === 'adventure' && searchResults.value.length > 0) {
          const firstCharacter = searchResults.value[0];
          if (firstCharacter.adventureName && firstCharacter.adventureName !== 'N/A') {
            selectedAdventure.value = firstCharacter.adventureName;
            console.log('선택된 모험단:', selectedAdventure.value);
          }
        }
          
        // 검색 기록을 localStorage에 저장
        saveToSearchHistory(searchResults.value);
        
        // 검색 완료 후 자동 던담 동기화 시작
        if (searchResults.value.length > 0) {
          successMessage.value = `검색 완료! ${searchResults.value.length}개 캐릭터를 찾았습니다. 던담 동기화를 시작합니다...`;
          
          // DOM 렌더링 완료 후 자동 동기화 시작
          console.log('검색 완료, DOM 렌더링 대기 중...');
          await nextTick();
          console.log('DOM 렌더링 완료, 던담 동기화 시작');
          await startAutoDundamSync();
        }
      } else {
        // 백엔드에서 반환한 에러 메시지 사용
        error.value = data.message || '캐릭터 검색에 실패했습니다.';
      }
    } else {
      error.value = '검색 요청이 실패했습니다.';
    }

  } catch (err) {
    console.error('검색 실패:', err);
    error.value = '검색 중 오류가 발생했습니다.';
  } finally {
    searching.value = false;
  }
};





// 캐릭터를 DB에 저장
const saveCharacterToDB = async (character: any) => {
  try {
    const response = await apiFetch('/characters', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(character)
    });

    if (response.ok) {
      successMessage.value = `${character.characterName} 캐릭터가 DB에 저장되었습니다.`;

    } else {
      error.value = '캐릭터 저장에 실패했습니다.';
    }
  } catch (err) {
    console.error('캐릭터 저장 실패:', err);
    error.value = '캐릭터 저장 중 오류가 발생했습니다.';
  }
};



// 검색 모드 변경 핸들러
const onSearchModeChange = () => {
  // 서버 선택이 변경되면 기존 검색 결과와 입력값 초기화
  searchQuery.value = '';
  searchResults.value = [];
  selectedCharacter.value = null;
  error.value = '';
  successMessage.value = '';
  
  // 서버 선택에 따라 selectedServer 값 설정
  selectedServer.value = searchMode.value;
  
  // 모험단 모드일 때 selectedAdventure 설정
  if (searchMode.value === 'adventure') {
    selectedAdventure.value = null; // 초기화
  }
};

// 유틸리티 함수들
const getServerName = (serverId: string): string => {
  const server = servers.value.find(s => s.serverId === serverId);
  return server?.serverName || serverId;
};

// 이미지 로드 에러 처리
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  // 이미지 로드 실패 시 숨기고 플레이스홀더 표시
  img.style.display = 'none';
  console.warn('캐릭터 이미지 로드 실패:', img.src);
};

// 컨텍스트 메뉴 관련 함수들
const showContextMenuForCharacter = async (event: MouseEvent, character: any) => {
  event.preventDefault();
  contextCharacter.value = character;
  
  // 컨텍스트 메뉴 위치 계산
  contextMenuStyle.value = {
    position: 'fixed',
    left: event.clientX + 'px',
    top: event.clientY + 'px',
    zIndex: 1000
  };
  
  // 현재 캐릭터의 던전별 업둥이 상태 로드
  await loadDungeonFavorites(character.characterId);
  
  showContextMenu.value = true;
  
  // 클릭 외부 감지를 위한 이벤트 리스너 추가
  document.addEventListener('click', hideContextMenu);
};

const hideContextMenu = () => {
  showContextMenu.value = false;
  document.removeEventListener('click', hideContextMenu);
};

// 던전별 업둥이 상태 로드
const loadDungeonFavorites = async (characterId: string) => {
  try {
    const response = await apiFetch(`/characters/${characterId}/favorites`);
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        dungeonFavorites.value = result.data.favorites;
      }
    }
  } catch (error) {
    console.error('던전별 업둥이 상태 로드 실패:', error);
  }
};

// 던전별 업둥이 설정 토글
const toggleDungeonFavorite = async (dungeonType: string, event: Event) => {
  const target = event.target as HTMLInputElement;
  const isFavorite = target.checked;
  
  if (!contextCharacter.value) return;
  
  try {
          const response = await apiFetch(
        `/characters/${contextCharacter.value.characterId}/favorite/${dungeonType}?isFavorite=${isFavorite}`,
      { method: 'POST' }
    );
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 로컬 상태 업데이트
        (dungeonFavorites.value as any)[dungeonType] = isFavorite;
        
        // 성공 메시지 표시
        successMessage.value = result.message;
        setTimeout(() => {
          successMessage.value = '';
        }, 3000);
      } else {
        // 실패 시 체크박스 상태 되돌리기
        target.checked = !isFavorite;
        error.value = result.message;
      }
    } else {
      // 실패 시 체크박스 상태 되돌리기
      target.checked = !isFavorite;
      error.value = '업둥이 설정에 실패했습니다.';
    }
  } catch (err) {
    // 실패 시 체크박스 상태 되돌리기
    target.checked = !isFavorite;
    console.error('업둥이 설정 오류:', err);
    error.value = '업둥이 설정 중 오류가 발생했습니다.';
  }
};

const formatNumber = (num?: number): string => {
  if (num === undefined || num === null) return 'N/A';
  if (num >= 100000000) {
    return (num / 100000000).toFixed(1) + '억';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만';
  }
  return num.toLocaleString();
};

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

// 직업 판별 함수들
const isDealer = (character: any): boolean => {
  // 버퍼가 아니면 딜러로 간주
  return !isBuffer(character);
};

const formatDate = (dateString: string): string => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleDateString('ko-KR');
};

// 수동 입력 관련 메서드들
const showManualInput = (character: any, statType: string) => {
  manualInputCharacter.value = character;
  
  // 기존 수동 입력 값으로 초기화
  manualInputData.value = {
    buffPower: character.manualBuffPower || null,
    totalDamage: character.manualTotalDamage || null
  };
  
  showManualInputModal.value = true;
};

const hideManualInput = () => {
  showManualInputModal.value = false;
  manualInputCharacter.value = null;
  manualInputData.value = {
    buffPower: null,
    totalDamage: null
  };
};

const saveManualInput = async () => {
  try {
    if (!manualInputCharacter.value) return;
    
    const response = await apiFetch(`/characters/${manualInputCharacter.value.characterId}/manual-stats`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...manualInputData.value,
        updatedBy: '사용자'
      })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        successMessage.value = '수동 입력이 저장되었습니다.';
        
        // 검색 결과 업데이트
        const characterIndex = searchResults.value.findIndex(
          c => c.characterId === manualInputCharacter.value.characterId
        );
        if (characterIndex !== -1) {
          searchResults.value[characterIndex] = {
            ...searchResults.value[characterIndex],
            manualBuffPower: manualInputData.value.buffPower,
            manualTotalDamage: manualInputData.value.totalDamage
          };
        }
        
        hideManualInput();
      } else {
        error.value = result.message || '저장에 실패했습니다.';
      }
    } else {
      error.value = '저장에 실패했습니다.';
    }
  } catch (err: any) {
    console.error('수동 입력 저장 실패:', err);
    error.value = '저장 중 오류가 발생했습니다: ' + err.message;
  }
};

// 동기화 상태 관련 메서드들
const loadSyncStatus = async () => {
  try {
    const response = await apiFetch('/character-sync/status');
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        syncStatus.value = result.data;
      }
    }
  } catch (err: any) {
    console.error('동기화 상태 로드 실패:', err);
  }
};

const startManualSync = async () => {
  try {
    const response = await apiFetch('/character-sync/start', {
      method: 'POST'
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        successMessage.value = '수동 동기화가 시작되었습니다.';
        await loadSyncStatus(); // 상태 새로고침
      } else {
        error.value = result.message || '동기화 시작에 실패했습니다.';
      }
    } else {
      error.value = '동기화 시작에 실패했습니다.';
    }
  } catch (err: any) {
    console.error('수동 동기화 시작 실패:', err);
    error.value = '동기화 시작 중 오류가 발생했습니다: ' + err.message;
  }
};

// 컴포넌트 마운트 시 동기화 상태도 로드
onMounted(async () => {
  await loadServers();
  await loadSyncStatus();
});

// 캐릭터 상세 페이지로 이동
const goToCharacterDetail = (character: any) => {
  // 현재 검색 상태 저장
  const searchState = {
    searchResults: searchResults.value,
    selectedCharacter: character,
    searchQuery: searchQuery.value,
    selectedServer: selectedServer.value
  }
  localStorage.setItem('characterSearchState', JSON.stringify(searchState))
  
  router.push(`/character/${character.characterId}`)
}

// 검색 기록을 localStorage에 저장
const saveToSearchHistory = (characters: any[]) => {
  try {
    // 기존 검색 기록 가져오기
    const existingHistory = JSON.parse(localStorage.getItem('df_search_history') || '[]');
    
    // 새로운 검색 결과를 기존 기록에 추가
    const newRecords = characters.map(char => ({
      characterId: char.characterId,
      characterName: char.characterName,
      serverId: char.serverId,
      adventureName: char.adventureName,
      level: char.level,
      fame: char.fame,
      jobName: char.jobName,
      jobGrowName: char.jobGrowName,
      buffPower: char.buffPower,
      totalDamage: char.totalDamage,
      dungeonClearNabel: char.dungeonClearNabel,
      dungeonClearVenus: char.dungeonClearVenus,
      dungeonClearFog: char.dungeonClearFog,
      dungeonClearTwilight: char.dungeonClearTwilight,
      searchTimestamp: new Date().toISOString()
    }));
    
    console.log('저장할 새로운 기록:', newRecords);
    
    // 중복 제거 (characterId 기준)
    const existingIds = new Set(existingHistory.map((record: any) => record.characterId));
    const uniqueNewRecords = newRecords.filter(record => !existingIds.has(record.characterId));
    
    console.log('중복 제거 후 새로운 기록:', uniqueNewRecords);
    
    // 기존 기록과 새로운 기록 합치기
    const updatedHistory = [...existingHistory, ...uniqueNewRecords];
    
    // localStorage에 저장
    localStorage.setItem('df_search_history', JSON.stringify(updatedHistory));
    console.log('검색 기록 저장 완료:', updatedHistory.length, '개 캐릭터');
    
    // 던전 클리어 현황용 모험단 기록도 저장
    saveAdventureToDungeonHistory(characters);
    
    // 저장된 데이터 확인
    const savedData = JSON.parse(localStorage.getItem('df_search_history') || '[]');
    console.log('localStorage에 저장된 최종 데이터:', savedData);
    
  } catch (error) {
    console.error('검색 기록 저장 실패:', error);
  }
};

// 던전 클리어 현황용 모험단 기록 저장
const saveAdventureToDungeonHistory = (characters: any[]) => {
  try {
    // 기존 던전 모험단 기록 가져오기
    const existingDungeonHistory = JSON.parse(localStorage.getItem('df_dungeon_adventure_history') || '[]');
    
    // 검색된 캐릭터들의 모험단명 추출 (중복 제거)
    const adventureNames = [...new Set(characters
      .map(char => char.adventureName)
      .filter(name => name && name !== 'N/A')
    )];
    
    // 새로운 모험단명들을 기존 기록에 추가
    const newAdventures = adventureNames.filter(name => !existingDungeonHistory.includes(name));
    
    if (newAdventures.length > 0) {
      // 최대 10개까지만 저장
      const updatedDungeonHistory = [...existingDungeonHistory, ...newAdventures];
      if (updatedDungeonHistory.length > 10) {
        updatedDungeonHistory.splice(0, updatedDungeonHistory.length - 10);
      }
      
      // 로컬스토리지에 저장
      localStorage.setItem('df_dungeon_adventure_history', JSON.stringify(updatedDungeonHistory));
      console.log('던전 모험단 기록 저장 완료:', updatedDungeonHistory);
    }
  } catch (error) {
    console.error('던전 모험단 기록 저장 실패:', error);
  }
};
</script>

<style scoped>
.character-search {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 작은 화면에서 컨테이너 조정 */
@media screen and (max-width: 600px) {
  .character-search {
    padding: 10px;
  }
  
  .character-search h2 {
    font-size: 1.5rem;
    margin-bottom: 15px;
  }
}

/* 400px 이하 컨테이너 특별 최적화 */
@media screen and (max-width: 400px) {
  .character-search {
    padding: 5px;
  }
  
  .character-search h2 {
    font-size: 1.3rem;
    margin-bottom: 10px;
  }
  
  .search-results h3 {
    font-size: 1.1rem;
    margin-bottom: 10px;
  }
}

.search-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

/* 작은 화면에서 검색 폼 조정 */
@media screen and (max-width: 600px) {
  .search-form {
    padding: 15px;
    margin-bottom: 20px;
  }
  
  .form-group {
    margin-bottom: 12px;
  }
  
  .form-group label {
    font-size: 14px;
  }
  
  .form-group select,
  .form-group input {
    padding: 8px;
    font-size: 14px;
  }
  
  .search-btn {
    padding: 10px 20px;
    font-size: 14px;
  }
}

/* 400px 이하 검색 폼 특별 최적화 */
@media screen and (max-width: 400px) {
  .search-form {
    padding: 10px;
    margin-bottom: 15px;
  }
  
  .form-group {
    margin-bottom: 8px;
  }
  
  .form-group label {
    font-size: 12px;
  }
  
  .form-group select,
  .form-group input {
    padding: 6px;
    font-size: 12px;
  }
  
  .search-btn {
    padding: 8px 16px;
    font-size: 12px;
  }
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group select,
.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.search-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  width: 100%;
}

.search-btn:hover:not(:disabled) {
  background: #0056b3;
}

.search-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.search-results {
  margin-bottom: 30px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 15px;
}

/* 작은 화면에서 그리드 조정 */
@media screen and (max-width: 600px) {
  .results-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}

/* 400px 이하 특별 최적화 */
@media screen and (max-width: 400px) {
  .results-grid {
    gap: 10px;
  }
  
  .dundam-character-card {
    padding: 8px;
    min-height: 80px;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .character-avatar {
    margin-right: 0;
    margin-bottom: 8px;
  }
  
  .avatar-image {
    width: 50px;
    height: 50px;
  }
  
  .character-info {
    width: 100%;
  }
  
  .character-header {
    flex-direction: column;
    gap: 4px;
    align-items: center;
  }
  
  .character-name {
    font-size: 0.9rem;
  }
  
  .server-name, .level-display {
    font-size: 0.8rem;
  }
  
  /* 추가 글자 크기 최적화 */
  .character-stats {
    font-size: 0.75rem;
  }
  
  .adventure-name {
    font-size: 0.8rem;
  }
  
  .nabel-eligibility {
    font-size: 0.7rem;
  }
}

/* 던담 스타일 캐릭터 카드 */
.dundam-character-card {
  display: flex;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  min-height: 120px;
}

/* 작은 화면에서 카드 크기 조정 */
@media screen and (max-width: 600px) {
  .dundam-character-card {
    padding: 12px;
    min-height: 100px;
  }
}

.dundam-character-card:hover {
  border-color: #4a90e2;
  box-shadow: 0 4px 16px rgba(74, 144, 226, 0.15);
  transform: translateY(-2px);
}

.dundam-character-card.selected {
  border-color: #4a90e2;
  background: linear-gradient(135deg, #f8fbff 0%, #e3f2fd 100%);
  box-shadow: 0 4px 20px rgba(74, 144, 226, 0.25);
}

.character-avatar {
  position: relative;
  margin-right: 16px;
  flex-shrink: 0;
}

.avatar-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 작은 화면에서 아바타 크기 조정 */
@media screen and (max-width: 600px) {
  .avatar-image {
    width: 60px;
    height: 60px;
    border-radius: 8px;
  }
  
  .character-avatar {
    margin-right: 12px;
  }
}

.character-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  color: white;
  text-transform: uppercase;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.character-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.character-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.server-name {
  font-size: 12px;
  font-weight: 500;
  color: #3498db;
  background: #ecf0f1;
  padding: 2px 8px;
  border-radius: 12px;
  margin: 0;
}

.level-display {
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 500;
  background: #ecf0f1;
  padding: 2px 8px;
  border-radius: 12px;
  margin: 0;
}

.character-name {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
}

.adventure-name {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

.clickable-adventure {
  cursor: pointer;
  color: #007bff;
  text-decoration: underline;
  transition: color 0.2s ease;
}

.clickable-adventure:hover {
  color: #0056b3;
  text-decoration: none;
}

.no-adventure {
  color: #6c757d;
  font-style: italic;
}

.stats-info {
  display: flex;
  gap: 16px;
  margin: 4px 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-label {
  font-size: 12px;
  color: #95a5a6;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #e67e22;
}

.buff-power .stat-value {
  color: #e74c3c;
}

.meta-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.fame-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fame-icon {
  font-size: 14px;
}

.fame-value {
  font-size: 12px;
  font-weight: 500;
  color: #f39c12;
}

.job-info {
  display: flex;
  justify-content: flex-end;
}

.job-name {
  font-size: 13px;
  color: #27ae60;
  font-weight: 500;
  background: rgba(39, 174, 96, 0.1);
  padding: 2px 8px;
  border-radius: 8px;
}

/* 던전 클리어 상태 스타일 */
.dungeon-clear-status {
  display: flex;
  gap: 8px;
  margin: 8px 0;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 11px;
}

.dungeon-clear-section {
  margin-top: 15px;
}

.dungeon-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: bold;
}

.character-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.server-name {
  font-size: 12px;
  font-weight: 500;
  color: #3498db;
  background: #ecf0f1;
  padding: 2px 8px;
  border-radius: 12px;
  margin: 0;
}

.level-display {
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 500;
  background: #ecf0f1;
  padding: 2px 8px;
  border-radius: 12px;
  margin: 0;
}

.character-name {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
}

.edit-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.edit-btn:hover {
  background: #f8f9fa;
  transform: scale(1.1);
}

.dungeon-status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 6px;
  border-radius: 4px;
  background: white;
  border: 1px solid #e5e5e5;
  min-width: 40px;
  transition: all 0.2s ease;
}

.dungeon-status-item.cleared {
  background: #e8f5e8;
  border-color: #4caf50;
}

.dungeon-status-item:not(.cleared) {
  background: #fff5f5;
  border-color: #f44336;
}

.dungeon-icon {
  font-size: 12px;
  margin-bottom: 2px;
}

.dungeon-name {
  font-size: 10px;
  font-weight: bold;
  color: #666;
  margin-bottom: 1px;
}

.clear-status {
  font-size: 12px;
  font-weight: bold;
}

.dungeon-status-item.cleared .clear-status {
  color: #4caf50;
}

.dungeon-status-item:not(.cleared) .clear-status {
  color: #f44336;
}

/* 명성컷 미달로 인한 "명성 부족" 표시 스타일 */
.clear-status.ineligible {
  color: #f44336;
  font-weight: normal;
  font-size: 11px;
}

/* 컨텍스트 메뉴 스타일 */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  padding: 0;
  min-width: 280px;
  z-index: 1000;
  font-size: 14px;
}

.context-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e5e5e5;
  border-radius: 8px 8px 0 0;
}

.context-character-name {
  font-weight: bold;
  color: #333;
}

.context-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.context-close:hover {
  color: #333;
}

.context-section {
  padding: 16px;
}

.context-section h4 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 14px;
}

.dungeon-favorites {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-item {
  display: flex;
  align-items: center;
}

.favorite-item label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 0;
  width: 100%;
}

.favorite-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #4a90e2;
}

.dungeon-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.dungeon-name {
  flex: 1;
  color: #333;
  font-weight: 500;
}

.coming-soon {
  font-size: 12px;
  color: #999;
  font-style: italic;
}

.favorite-item:hover {
  background: #f8f9fa;
  border-radius: 4px;
  margin: 0 -8px;
  padding: 6px 8px;
}

.favorite-item:hover label {
  padding: 6px 0;
}

.character-detail {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 12px;
  margin-top: 20px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  overflow: hidden;
}

.detail-header {
  background: #f8f9fa;
  padding: 20px;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #e9ecef;
  color: #333;
}

.detail-content {
  padding: 20px;
}

.detail-section {
  margin-bottom: 25px;
}

.detail-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 18px;
  border-bottom: 2px solid #007bff;
  padding-bottom: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.info-item .label {
  font-weight: 600;
  color: #495057;
}

.info-item .value {
  color: #212529;
  font-weight: 500;
}

.detail-actions {
  display: flex;
  gap: 15px;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #dee2e6;
}

.character-info h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 18px;
}

.character-info p {
  margin: 5px 0;
  color: #666;
}

.character-stats {
  margin: 15px 0;
  padding: 15px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
}

.character-stats h5 {
  margin: 0 0 10px 0;
  color: #333;
}

.character-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.save-btn,
.history-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.save-btn {
  background: #28a745;
  color: white;
}

.save-btn:hover {
  background: #218838;
}

.history-btn {
  background: #17a2b8;
  color: white;
}

.history-btn:hover {
  background: #138496;
}

.search-history {
  background: #e9ecef;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.history-list {
  margin-top: 15px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: white;
  border-radius: 6px;
  margin-bottom: 10px;
  border: 1px solid #dee2e6;
}

.server-name {
  font-weight: bold;
  color: #495057;
  min-width: 80px;
}

.adventure-name {
  color: #6c757d;
  min-width: 120px;
}

.character-name {
  color: #212529;
  min-width: 120px;
}

.timestamp {
  color: #6c757d;
  font-size: 0.9rem;
  min-width: 100px;
}

.load-btn,
.remove-btn {
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.load-btn {
  background: #007bff;
  color: white;
}

.load-btn:hover {
  background: #0056b3;
}

.remove-btn {
  background: #dc3545;
  color: white;
}

.remove-btn:hover {
  background: #c82333;
}

/* 메시지 영역 스타일 */
.message-area {
  margin-top: 15px;
  margin-bottom: 15px;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 10px;
  text-align: center;
  border: 1px solid #f5c6cb;
}

.success-message {
  background: #d4edda;
  color: #155724;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 10px;
  text-align: center;
  border: 1px solid #c3e6cb;
}

@media (max-width: 768px) {
  .character-search {
    padding: 10px;
  }
  
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .history-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .character-actions {
    flex-direction: column;
  }
}

/* 빠른 테스트 섹션 스타일 */
.quick-test-section {
  margin: 30px 0;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.quick-test-section h3 {
  margin: 0 0 20px 0;
  text-align: center;
  font-size: 1.4rem;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.test-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.test-group {
  background: rgba(255, 255, 255, 0.1);
  padding: 20px;
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.test-group h4 {
  margin: 0 0 15px 0;
  text-align: center;
  font-size: 1.1rem;
  color: #f8f9fa;
}

.test-btn {
  display: block;
  width: 100%;
  padding: 12px 16px;
  margin: 8px 0;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.test-btn.buffer {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  box-shadow: 0 4px 15px rgba(238, 90, 36, 0.3);
}

.test-btn.buffer:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(238, 90, 36, 0.4);
}

.test-btn.dealer {
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  color: white;
  box-shadow: 0 4px 15px rgba(68, 160, 141, 0.3);
}

.test-btn.dealer:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(68, 160, 141, 0.4);
}

.test-btn.direct {
  background: linear-gradient(135deg, #a8edea, #fed6e3);
  color: #333;
  box-shadow: 0 4px 15px rgba(168, 237, 234, 0.3);
}

.test-btn.direct:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(168, 237, 234, 0.4);
}

.direct-test {
  display: flex;
  gap: 10px;
  align-items: center;
}

.direct-test-input {
  flex: 1;
  padding: 10px 12px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  color: #333;
}

  .direct-test-input:focus {
    outline: none;
    background: white;
    box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3);
  }

  /* 자동 테스트 스타일 */
  .auto-test-controls {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }

  .test-mode-selector {
    display: flex;
    gap: 15px;
    justify-content: center;
    margin-bottom: 10px;
  }

  .mode-label {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    transition: all 0.3s ease;
  }

  .mode-label:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  .mode-label input[type="radio"] {
    margin: 0;
  }

  .mode-label span {
    font-size: 14px;
    color: #f8f9fa;
    font-weight: 500;
  }

  .test-btn.auto-start {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  }

  .test-btn.auto-start:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  }

  .test-btn.auto-stop {
    background: linear-gradient(135deg, #ff6b6b, #ee5a24);
    color: white;
    box-shadow: 0 4px 15px rgba(255, 107, 107, 0.3);
  }

  .test-btn.auto-stop:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
  }

  .test-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none !important;
  }

  .auto-test-progress {
    background: rgba(255, 255, 255, 0.1);
    padding: 15px;
    border-radius: 8px;
    text-align: center;
  }

  .progress-bar {
    width: 100%;
    height: 8px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 10px;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #4ecdc4, #44a08d);
    transition: width 0.3s ease;
    border-radius: 4px;
  }

  .progress-text {
    font-size: 14px;
    color: #f8f9fa;
    font-weight: 600;
  }

  /* 테스트 결과 스타일 */
  .test-results {
    margin: 30px 0;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 12px;
    border: 1px solid #dee2e6;
  }

  .test-results h3 {
    margin: 0 0 20px 0;
    text-align: center;
    color: #495057;
  }

  .results-summary {
    display: flex;
    justify-content: space-around;
    margin-bottom: 20px;
    padding: 15px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }

  .summary-item {
    text-align: center;
  }

  .summary-label {
    display: block;
    font-size: 12px;
    color: #6c757d;
    margin-bottom: 5px;
  }

  .summary-value {
    display: block;
    font-size: 18px;
    font-weight: bold;
  }

  .summary-value.success {
    color: #28a745;
  }

  .summary-value.error {
    color: #dc3545;
  }

  .test-results-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }

  .test-result-item {
    background: white;
    border-radius: 8px;
    padding: 15px;
    border-left: 4px solid #dee2e6;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  }

  .test-result-item.success {
    border-left-color: #28a745;
    background: #f8fff9;
  }

  .test-result-item.error {
    border-left-color: #dc3545;
    background: #fff8f8;
  }

  .result-header {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 10px;
    padding-bottom: 10px;
    border-bottom: 1px solid #e9ecef;
  }

  .result-number {
    background: #6c757d;
    color: white;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: bold;
  }

  .result-name {
    font-weight: bold;
    color: #495057;
    flex: 1;
  }

  .result-type {
    font-size: 12px;
    color: #6c757d;
    background: #e9ecef;
    padding: 4px 8px;
    border-radius: 12px;
  }

  .result-status {
    font-size: 18px;
  }

  .result-details {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .result-expected,
  .result-actual,
  .result-error {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .detail-label {
    font-weight: 600;
    color: #495057;
    min-width: 80px;
  }

  .detail-value {
    color: #6c757d;
  }

  .detail-value.error {
    color: #dc3545;
  }

@media (max-width: 768px) {
  .test-buttons {
    grid-template-columns: 1fr;
  }
  
  .direct-test {
    flex-direction: column;
  }
  
  .direct-test-input {
    width: 100%;
  }
}

/* 수동 입력 모달 스타일 */
.manual-input-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.input-group {
  margin-bottom: 15px;
}

.input-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #333;
}

.manual-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.manual-input:focus {
  outline: none;
  border-color: #4a90e2;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.2);
}

.modal-footer {
  display: flex;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #e9ecef;
  justify-content: flex-end;
}

.save-btn, .cancel-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 600;
}

.save-btn {
  background: #28a745;
  color: white;
}

.save-btn:hover {
  background: #218838;
}

.cancel-btn {
  background: #6c757d;
  color: white;
}

.cancel-btn:hover {
  background: #5a6268;
}

/* 동기화 상태 표시 스타일 */
.sync-status-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  margin: 30px 0;
  border: 1px solid #dee2e6;
}

.sync-status-section h3 {
  margin: 0 0 20px 0;
  color: #495057;
  text-align: center;
}

.sync-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.sync-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.sync-label {
  font-weight: 600;
  color: #495057;
}

.sync-value {
  color: #6c757d;
  font-weight: 500;
}

.sync-value.running {
  color: #28a745;
  font-weight: 600;
}

.sync-value.disabled {
  color: #dc3545;
  font-weight: 600;
}

.sync-btn {
  background: #4a90e2;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  font-weight: 600;
  width: 100%;
  max-width: 200px;
  margin: 0 auto;
  display: block;
}

.sync-btn:hover {
  background: #357abd;
}

.sync-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.sync-notice {
  margin-top: 20px;
  padding: 15px;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  text-align: center;
}

.sync-notice p {
  margin: 5px 0;
  color: #856404;
  font-size: 14px;
}

/* 수동 입력 값과 동기화 값 표시 스타일 */
.stat-values {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
}

.manual-value, .synced-value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.value-label {
  color: #6c757d;
  font-weight: 500;
  min-width: 40px;
}

.stat-value.manual {
  color: #28a745;
  font-weight: 600;
}

.stat-value.synced {
  color: #6c757d;
}

.edit-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-left: 8px;
}

.edit-btn:hover {
  background: #5a6268;
}

/* 명성 섹션 스타일 */
.fame-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.fame-label {
  font-size: 14px;
  font-weight: 600;
  color: #495057;
}

.fame-value {
  font-size: 16px;
  font-weight: 700;
  color: #f39c12;
}

/* 던전 클리어 상태 반전 스타일 */
.dungeon-status-item.cleared {
  background: #fff5f5;
  border-color: #f44336;
}

.dungeon-status-item.cleared .clear-status {
  color: #f44336;
}

.dungeon-status-item:not(.cleared) {
  background: #e8f5e8;
  border-color: #4caf50;
}

.dungeon-status-item:not(.cleared) .clear-status {
  color: #4caf50;
}

/* 스탯 표시 개선 */
.stats-info {
  display: flex;
  gap: 16px;
  margin: 8px 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.stat-label {
  font-size: 14px;
  font-weight: 600;
  color: #495057;
  min-width: 50px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #e67e22;
  min-width: 80px;
  text-align: right;
}

/* 서버 선택 관련 스타일 */
.form-group select:invalid,
.form-group select[value=""] {
  border-color: #dc3545;
  background-color: #fff5f5;
}

.form-group select:invalid:focus,
.form-group select[value=""]:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
}

.search-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.search-btn:disabled:hover {
  background-color: #6c757d;
}

/* Neople API 푸터 스타일 */
.neople-api-footer {
  text-align: center;
  padding: 20px;
  margin-top: 40px;
  border-top: 1px solid #e5e5e5;
  background: #f8f9fa;
}

.neople-api-footer p {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
}

.neople-api-footer a {
  color: #007bff;
  text-decoration: none;
  font-weight: 500;
}

.neople-api-footer a:hover {
  text-decoration: underline;
  color: #0056b3;
}

/* 던담 동기화 버튼 스타일 */
.dundam-sync-controls {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.dundam-sync-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.dundam-sync-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.dundam-sync-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 캐릭터 카드 내 던담 동기화 버튼 */
.dundam-sync-section {
  margin-top: 12px;
  text-align: center;
}

.dundam-sync-btn {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.3);
  width: 100%;
}

.dundam-sync-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.dundam-sync-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  background: #6c757d;
}



/* Playwright 버튼 (활성화됨) */
.dundam-sync-button.playwright-enabled {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  border: 2px solid #28a745;
  box-shadow: 0 4px 15px rgba(40, 167, 69, 0.3);
}

.dundam-sync-button.playwright-enabled:hover:not(:disabled) {
  background: linear-gradient(135deg, #20c997 0%, #28a745 100%);
  border-color: #20c997;
  box-shadow: 0 6px 20px rgba(40, 167, 69, 0.4);
}

.sync-status {
  font-size: 12px;
  color: #6c757d;
  text-align: center;
  max-width: 300px;
  word-wrap: break-word;
}

/* 자동 던담 동기화 진행바 스타일 */
.dundam-sync-progress {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 400px 이하에서 진행바 최적화 */
@media screen and (max-width: 400px) {
  .dundam-sync-progress {
    padding: 12px;
    margin-bottom: 15px;
  }
  
  .progress-header {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
  
  .progress-header h4 {
    font-size: 14px;
  }
  
  .progress-text {
    font-size: 12px;
  }
  
  .progress-info {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
  
  .current-syncing,
  .countdown-timer {
    font-size: 12px;
  }
  
  .waiting-message {
    font-size: 12px;
    padding: 6px 10px;
  }
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-header h4 {
  margin: 0;
  color: #495057;
  font-size: 16px;
}

.progress-text {
  color: #6c757d;
  font-size: 14px;
  font-weight: 500;
}

.progress-bar-container {
  width: 100%;
  height: 12px;
  background-color: #e9ecef;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
  border-radius: 6px;
  transition: width 0.3s ease;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.current-syncing {
  color: #6c757d;
  font-size: 14px;
}

.countdown-timer {
  color: #dc3545;
  font-size: 14px;
  font-weight: 600;
  background: #f8d7da;
  padding: 4px 8px;
  border-radius: 4px;
}

.waiting-message {
  color: #856404;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
  padding: 8px 12px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

/* ========================================
   세밀한 반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 대형 데스크탑 (1920px 이상) */
@media screen and (min-width: 1920px) {
  .search-container {
    max-width: 1600px;
    padding: 30px;
  }
  
  .character-grid {
    grid-template-columns: repeat(5, 1fr);
    gap: 25px;
  }
  
  .search-input {
    font-size: 18px;
    padding: 18px;
  }
  
  .search-button {
    padding: 18px 30px;
    font-size: 18px;
  }
}

/* 데스크탑 (1600px ~ 1919px) */
@media screen and (min-width: 1600px) and (max-width: 1919px) {
  .search-container {
    max-width: 1400px;
    padding: 25px;
  }
  
  .character-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 22px;
  }
}

/* 노트북 (1200px ~ 1599px) */
@media screen and (min-width: 1200px) and (max-width: 1599px) {
  .search-container {
    max-width: 1200px;
    padding: 20px;
  }
  
  .character-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
}

/* 태블릿 가로 (1024px ~ 1199px) */
@media screen and (min-width: 1024px) and (max-width: 1199px) {
  .search-container {
    padding: 18px;
  }
  
  .search-form {
    flex-direction: row;
    gap: 12px;
  }
  
  .search-button {
    width: auto;
    padding: 12px 24px;
  }
  
  .character-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }
}

/* 태블릿 세로 (768px ~ 1023px) */
@media screen and (min-width: 768px) and (max-width: 1023px) {
  .search-container {
    padding: 15px;
  }
  
  .search-form {
    flex-direction: column;
    gap: 15px;
  }
  
  .search-input {
    font-size: 16px;
    padding: 14px;
  }
  
  .search-button {
    width: 100%;
    padding: 14px;
    font-size: 16px;
  }
  
  .character-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .character-card {
    padding: 16px;
  }
  
  .character-info h3 {
    font-size: 1.1rem;
  }
  
  .dundam-sync-progress {
    padding: 16px;
  }
}

/* 중형 모바일 (600px ~ 767px) */
@media screen and (min-width: 600px) and (max-width: 767px) {
  .search-container {
    padding: 12px;
  }
  
  .search-form {
    flex-direction: column;
    gap: 12px;
  }
  
  .search-input {
    font-size: 16px;
    padding: 13px;
  }
  
  .search-button {
    width: 100%;
    padding: 13px;
    font-size: 16px;
  }
  
  .character-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  
  .character-card {
    padding: 14px;
  }
  
  .character-info h3 {
    font-size: 1rem;
  }
}

/* 소형 모바일 (480px ~ 599px) */
@media screen and (min-width: 480px) and (max-width: 599px) {
  .search-container {
    padding: 10px;
  }
  
  .search-form {
    flex-direction: column;
    gap: 10px;
  }
  
  .search-input {
    font-size: 16px;
    padding: 12px;
  }
  
  .search-button {
    width: 100%;
    padding: 12px;
    font-size: 16px;
  }
  
  .character-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .character-card {
    padding: 12px;
  }
  
  .character-info h3 {
    font-size: 1rem;
  }
  
  .character-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .dundam-sync-progress {
    padding: 12px;
  }
  
  .progress-header {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }
  
  .progress-info {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}

/* 초소형 모바일 (320px ~ 479px) */
@media screen and (min-width: 320px) and (max-width: 479px) {
  .search-container {
    padding: 8px;
  }
  
  .search-form {
    flex-direction: column;
    gap: 8px;
  }
  
  .search-input {
    font-size: 16px;
    padding: 10px;
  }
  
  .search-button {
    width: 100%;
    padding: 10px;
    font-size: 16px;
  }
  
  .character-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .character-card {
    padding: 10px;
  }
  
  .character-info h3 {
    font-size: 0.9rem;
  }
  
  .dundam-sync-progress {
    padding: 10px;
  }
}

/* 터치 디바이스 최적화 */
@media (hover: none) and (pointer: coarse) {
  .search-button {
    min-height: 44px;
  }
  
  .character-card {
    min-height: 120px;
  }
  
  .dundam-sync-button {
    min-height: 44px;
  }
}
</style> 