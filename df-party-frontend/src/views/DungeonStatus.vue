<template>
  <div class="dungeon-status">
    <h2>던전 클리어 현황</h2>
    
    <!-- 검색바 (모험단 검색) -->
    <div class="search-form">
      <div class="form-group">
        <label for="adventureName">모험단 검색:</label>
        <input 
          id="adventureName" 
          v-model="adventureName" 
          type="text" 
          placeholder="모험단명을 입력하세요" 
          @keyup.enter="searchAdventure"
        >
      </div>
      
      <button @click="searchAdventure" :disabled="searching" class="search-btn">
        {{ searching ? '검색 중...' : '모험단 검색' }}
      </button>
      
      <!-- 모험단 전체 최신화 버튼 -->
      <button v-if="selectedAdventure && filteredCharacters.length > 0" 
              @click="refreshAllCharacters" 
              :disabled="refreshingAll" 
              class="refresh-all-btn">
        {{ refreshingAll ? '최신화 중...' : '🔄 전체 최신화' }}
      </button>
      
      <!-- 정렬 초기화 버튼 -->
      <button v-if="sortField !== 'characterName' || sortOrder !== 'asc'" 
              @click="resetSort" 
              class="reset-sort-btn">
        🔄 정렬 초기화
      </button>

    </div>



    <!-- 던전 클리어 현황 테이블 -->
    <div v-if="filteredCharacters.length > 0" class="dungeon-status-table">
      <h3>던전 클리어 현황 ({{ filteredCharacters.length }}개 캐릭터)</h3>
      
      <!-- 통계 요약 -->
      <div class="summary-stats">
        <div class="stat-card">
          <span class="stat-label">나벨 클리어</span>
          <span class="stat-value">{{ dungeonStats.nabel }}/{{ dungeonStats.nabelTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('nabel') }}%)</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">베누스 클리어</span>
          <span class="stat-value">{{ dungeonStats.venus }}/{{ dungeonStats.venusTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('venus') }}%)</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">안개신 클리어</span>
          <span class="stat-value">{{ dungeonStats.fog }}/{{ dungeonStats.fogTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('fog') }}%)</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">황혼전 클리어</span>
          <span class="stat-value">{{ dungeonStats.twilight }}/{{ dungeonStats.twilightTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('twilight') }}%)</span>
          <span class="coming-soon">(개발중)</span>
        </div>
      </div>

      <table class="characters-table">
        <thead>
          <tr>
            <th @click="sortBy('adventureName')" class="sortable">
              모험단 
              <span v-if="sortField === 'adventureName'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('characterName')" class="sortable">
              캐릭터명
              <span v-if="sortField === 'characterName'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('level')" class="sortable">
              레벨
              <span v-if="sortField === 'level'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('fame')" class="sortable">
              명성
              <span v-if="sortField === 'fame'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('jobName')" class="sortable">
              직업
              <span v-if="sortField === 'jobName'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('buffPower')" class="sortable">
              스탯
              <span v-if="sortField === 'buffPower'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('nabel')" class="sortable dungeon-clear-column">
              나벨
              <span v-if="sortField === 'nabel'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('venus')" class="sortable dungeon-clear-column">
              베누스
              <span v-if="sortField === 'venus'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th @click="sortBy('fog')" class="sortable dungeon-clear-column">
              안개신
              <span v-if="sortField === 'fog'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th>마지막 업데이트</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="character in sortedCharacters" :key="character.characterId" 
              :class="{ 'all-cleared': character.dungeonClearNabel && character.dungeonClearVenus && character.dungeonClearFog }">
            <td class="adventure-name">{{ character.adventureName || 'N/A' }}</td>
            <td class="character-name">{{ character.characterName }}</td>
            <td>{{ character.level || 'N/A' }}</td>
            <td>{{ formatNumber(character.fame) }}</td>
            <td class="job-name">
              <span v-if="character.jobGrowName">{{ formatJobName(character.jobGrowName || '') }}</span>
              <span v-else>{{ formatJobName(character.jobName || '') }}</span>
            </td>
            <td>
              <div class="stat-with-edit">
                <!-- 버퍼면 버프력, 딜러면 총딜 표시 -->
                <span v-if="isBuffer(character)">
                  <span class="stat-label">버프력:</span>
                  {{ formatBuffPower(character.buffPower || 0) }}
                </span>
                <span v-else>
                  <span class="stat-label">총딜:</span>
                  {{ formatTotalDamage(character.totalDamage || 0) }}
                </span>
              </div>
            </td>
            <td class="dungeon-status-cell nabel-cell">
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearNabel,
                     excluded: character.isExcludedNabel,
                     skip: character.isSkipNabel 
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedNabel ? '-' : (character.dungeonClearNabel ? '✅' : '❌') }}
                </span>
                <span class="clear-text">
                  {{ character.isExcludedNabel ? '안감' : (character.isSkipNabel ? '업둥' : (character.dungeonClearNabel ? '클리어' : '미클리어')) }}
                </span>
              </div>
              <!-- 나벨 난이도 표시 -->
              <div class="nabel-difficulty-indicator">
                <!-- 일반 나벨 표시 -->
                <span v-if="character.isNormalNabelEligible" class="normal-indicator">일반</span>
                <!-- 하드 나벨 표시 -->
                <button 
                  v-if="character.isHardNabelEligible"
                  @click="toggleHardMode(character)"
                  :class="{ active: getHardModeState(character) }"
                  class="hard-mode-btn"
                  title="하드 모드 토글">
                  하드
                </button>
                <button 
                  v-else-if="character.isNormalNabelEligible"
                  disabled
                  class="hard-mode-btn disabled"
                  title="하드 조건 미충족">
                  하드
                </button>
                <span v-else class="not-eligible">미대상</span>
              </div>
              <div class="action-buttons-mini">
                <button @click="toggleExclude(character, 'nabel')" 
                        class="exclude-btn" 
                        :class="{ active: character.isExcludedNabel }"
                        title="안감">안감</button>
                <button @click="toggleSkip(character, 'nabel')" 
                        class="skip-btn" 
                        :class="{ active: character.isSkipNabel }"
                        title="업둥">업둥</button>
              </div>
            </td>
            <td class="dungeon-status-cell venus-cell">
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearVenus,
                     excluded: character.isExcludedVenus,
                     skip: character.isSkipVenus 
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedVenus ? '-' : (character.dungeonClearVenus ? '✅' : '❌') }}
                </span>
                <span class="clear-text">
                  {{ character.isExcludedVenus ? '안감' : (character.isSkipVenus ? '업둥' : (character.dungeonClearVenus ? '클리어' : '미클리어')) }}
                </span>
              </div>
              <div class="action-buttons-mini">
                <button @click="toggleExclude(character, 'venus')" 
                        class="exclude-btn" 
                        :class="{ active: character.isExcludedVenus }"
                        title="안감">안감</button>
                <button @click="toggleSkip(character, 'venus')" 
                        class="skip-btn" 
                        :class="{ active: character.isSkipVenus }"
                        title="업둥">업둥</button>
              </div>
            </td>
            <td class="dungeon-status-cell fog-cell">
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearFog,
                     excluded: character.isExcludedFog,
                     skip: character.isSkipFog 
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedFog ? '-' : (character.dungeonClearFog ? '✅' : '❌') }}
                </span>
                <span class="clear-text">
                  {{ character.isExcludedFog ? '안감' : (character.isSkipFog ? '업둥' : (character.dungeonClearFog ? '클리어' : '미클리어')) }}
                </span>
              </div>
              <div class="action-buttons-mini">
                <button @click="toggleExclude(character, 'fog')" 
                        class="exclude-btn" 
                        :class="{ active: character.isExcludedFog }"
                        title="안감">안감</button>
                <button @click="toggleSkip(character, 'fog')" 
                        class="skip-btn" 
                        :class="{ active: character.isSkipFog }"
                        title="업둥">업둥</button>
              </div>
            </td>
            <td class="update-time">
              {{ formatDateTime(character.lastDungeonCheck || new Date().toISOString()) }}
            </td>
            <td>
              <div class="action-grid">
                <div class="action-cell">
                  <div class="action-label">던담초기화</div>
                  <div class="dundam-actions">
                    <button @click="syncCharacterFromDundam(character)" 
                            class="action-btn dundam-sync-btn" 
                            :class="{ 'syncing': syncingCharacters.has(character.characterId) }"
                            :disabled="syncingCharacters.has(character.characterId) || !isWithinTimeLimit(character.characterId) || !isAdventureSyncAvailable(character.adventureName)" 
                            title="던담 크롤링으로 총딜/버프력 최신화">
                      <span v-if="syncingCharacters.has(character.characterId)" class="syncing-text">
                        <span class="spinner">🔄</span> 실행
                      </span>
                      <span v-else>🔄 실행</span>
                    </button>
                    <a :href="getDundamLink(character)" target="_blank" class="dundam-link" :title="getDundamLinkTitle(character)">
                      🔗 링크
                    </a>
                  </div>
                  <div v-if="syncingCharacters.has(character.characterId)" class="syncing-status-message">
                    🔄 던담 동기화 진행 중...
                  </div>
                  <div v-else-if="!isAdventureSyncAvailable(character.adventureName)" class="adventure-time-limit-message">
                    모험단 제한: {{ getRemainingTime(character.adventureName) }} 후 가능
                  </div>
                  <div v-else-if="!isWithinTimeLimit(character.characterId)" class="time-limit-message">
                    케릭터 제한: {{ getRemainingTime(character.characterId) }} 후 가능
                  </div>
                  <div v-if="characterErrors.get(character.characterId)" class="character-error-message">
                    {{ characterErrors.get(character.characterId) }}
                  </div>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">케릭정보 최신화</div>
                  <button @click="refreshCharacterInfo(character)" class="action-btn refresh-btn" :disabled="refreshingCharacters.includes(character.characterId)" title="DFO API로 명성 최신화">
                    {{ refreshingCharacters.includes(character.characterId) ? '🔄' : '🔄' }}
                  </button>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">타임라인 최신화</div>
                  <button @click="refreshDungeonStatus(character)" class="action-btn timeline-btn" :disabled="refreshingTimeline.includes(character.characterId)" title="나벨, 베누스, 안개신 클리어 여부 확인">
                    {{ refreshingTimeline.includes(character.characterId) ? '🔄' : '🔄' }}
                  </button>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">스탯 수정</div>
                  <button @click="showManualInput(character, isBuffer(character) ? 'buffPower' : 'totalDamage')" class="action-btn edit-btn" title="수동으로 스탯 수정">
                    ✏️
                  </button>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 검색 결과가 없을 때 -->
    <div v-else-if="!searching && adventureName && characters.length === 0" class="no-results">
      <p>검색 결과가 없습니다.</p>
      <p v-if="selectedAdventure">'{{ selectedAdventure }}' 모험단의 캐릭터가 데이터베이스에 없습니다.</p>
      <p v-else>해당 모험단명의 캐릭터가 데이터베이스에 없습니다.</p>
      <p><strong>해결 방법:</strong> 먼저 <router-link to="/character-search">캐릭터 검색</router-link>에서 해당 모험단의 캐릭터들을 검색하여 추가해주세요.</p>
    </div>

    <!-- 초기 상태 -->
    <div v-else-if="!searching" class="initial-state">
      <p>캐릭터를 검색해주세요.</p>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="searching" class="loading">
      <p>던전 클리어 현황을 불러오는 중...</p>
    </div>

    <!-- 실시간 업데이트 상태 -->
    <div v-if="updating" class="update-status">
      <div class="update-header">
        <h3>실시간 업데이트 진행 중...</h3>
        <div class="connection-status" :class="{ 'connected': isConnected }">
          {{ isConnected ? '🟢 연결됨' : '🔴 연결끊김' }}
        </div>
      </div>
      
      <div v-if="updateProgress > 0" class="progress-bar">
        <div class="progress-fill" :style="{ width: updateProgress + '%' }"></div>
        <span class="progress-text">{{ updateProgress }}%</span>
      </div>
      
      <div v-if="updateStatus" class="update-message">{{ updateStatus }}</div>
    </div>

    <!-- 실시간 알림 -->
    <div v-if="realtimeNotifications.length > 0" class="realtime-notifications">
      <h4>실시간 알림</h4>
      <div class="notification-list">
        <div v-for="notification in realtimeNotifications" :key="notification.id" class="notification-item">
          <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
          <span class="notification-message">{{ notification.message }}</span>
        </div>
      </div>
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <!-- 성공 메시지 -->
    <div v-if="successMessage" class="success-message">
      {{ successMessage }}
    </div>

    <!-- 수동 입력 모달 -->
    <div v-if="showManualInputModal" class="modal-overlay" @click="closeManualInputModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ manualInputCharacter?.characterName }} {{ manualInputCharacter && isBuffer(manualInputCharacter) ? '버프력' : '총딜' }} 수정</h3>
          <button @click="closeManualInputModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <!-- 버퍼면 버프력만, 딜러면 총딜만 표시 -->
          <div v-if="manualInputCharacter && isBuffer(manualInputCharacter)" class="input-group">
            <label>버프력:</label>
            <input 
              v-model.number="manualInputData.buffPower" 
              type="number" 
              placeholder="버프력 입력"
              min="0"
            />
          </div>
          
          <div v-if="manualInputCharacter && !isBuffer(manualInputCharacter)" class="input-group">
            <label>총딜:</label>
            <input 
              v-model.number="manualInputData.totalDamage" 
              type="number" 
              placeholder="총딜 입력"
              min="0"
            />
          </div>
          

          

          

          

          

          

        </div>
        
        <div class="modal-footer">
          <button @click="saveManualInput" class="save-btn">저장</button>
          <button @click="closeManualInputModal" class="cancel-btn">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { dfApiService } from '../services/dfApi';
// import websocketService, { type RealtimeEvent } from '../services/websocketService';
import { type RealtimeEvent } from '../services/websocketService';
import { type Character } from '../types';

// 반응형 데이터
const adventureName = ref(''); // 모험단 검색용
const selectedAdventure = ref('');
const characters = ref<Character[]>([]);
const searching = ref(false);
const refreshing = ref(false);
const updating = ref(false);
const refreshingAll = ref(false);
const error = ref<string>('');
const isConnected = ref(false);
const updateProgress = ref(0);
const updateStatus = ref('');
const realtimeNotifications = ref<RealtimeEvent[]>([]);
const successMessage = ref<string>('');

// 각 캐릭터별 에러 메시지 저장
const characterErrors = ref<Map<string, string>>(new Map());

// 각 캐릭터별 마지막 동기화 시간 저장 (2분 제한용)
const lastSyncTimes = ref<Map<string, Date>>(new Map());

// 모험단별 마지막 동기화 시간 저장 (모험단 제한용)
const lastAdventureSyncTimes = ref<Map<string, Date>>(new Map());

// 2분 제한 확인 함수
const isWithinTimeLimit = (characterId: string): boolean => {
  const lastSync = lastSyncTimes.value.get(characterId);
  if (!lastSync) return true;
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const diffMinutes = diffMs / (1000 * 60);
  
  // 디버깅용 로그
  console.log(`케릭터 ${characterId} 동기화 제한 확인:`, {
    lastSync: lastSync.toISOString(),
    now: now.toISOString(),
    diffMinutes: diffMinutes,
    isAvailable: diffMinutes >= 2
  });
  
  return diffMinutes >= 2;
};

// 모험단별 동기화 제한 확인 함수 (모험단당 1개씩만)
const isAdventureSyncAvailable = (adventureName: string): boolean => {
  const lastSync = lastAdventureSyncTimes.value.get(adventureName);
  if (!lastSync) return true;
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const diffMinutes = diffMs / (1000 * 60);
  
  // 디버깅용 로그
  console.log(`모험단 ${adventureName} 동기화 제한 확인:`, {
    lastSync: lastSync.toISOString(),
    now: now.toISOString(),
    diffMinutes: diffMinutes,
    isAvailable: diffMinutes >= 2
  });
  
  return diffMinutes >= 2;
};

// 남은 시간 계산 함수
const getRemainingTime = (characterId: string): string => {
  const lastSync = lastSyncTimes.value.get(characterId);
  if (!lastSync) return '';
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));
  const diffSeconds = Math.floor((diffMs % (1000 * 60)) / 1000);
  
  if (diffMinutes < 2) {
    const remainingMinutes = 2 - diffMinutes - 1;
    const remainingSeconds = 60 - diffSeconds;
    if (remainingMinutes > 0) {
      return `${remainingMinutes}분 ${remainingSeconds}초`;
    } else {
      return `${remainingSeconds}초`;
    }
  }
  return '';
};

// 모험단별 남은 시간 계산 함수
const getAdventureRemainingTime = (adventureName: string): string => {
  const lastSync = lastAdventureSyncTimes.value.get(adventureName);
  if (!lastSync) return '';
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));
  const diffSeconds = Math.floor((diffMs % (1000 * 60)) / 1000);
  
  if (diffMinutes < 2) {
    const remainingMinutes = 2 - diffMinutes - 1;
    const remainingSeconds = 60 - diffSeconds;
    if (remainingMinutes > 0) {
      return `${remainingMinutes}분 ${remainingSeconds}초`;
    } else {
      return `${remainingSeconds}초`;
    }
  }
  return '';
};

// 수동 입력 관련
const showManualInputModal = ref(false);
const manualInputCharacter = ref<Character | null>(null);
const manualInputData = ref({
  buffPower: null as number | null,
  totalDamage: null as number | null
});

// 던담 동기화 관련
const syncingCharacters = ref<Set<string>>(new Set()); // 동기화 진행 중인 캐릭터들
const refreshingCharacters = ref<string[]>([]);
const refreshingTimeline = ref<string[]>([]);

// 정렬 관련
const sortField = ref<string>('characterName');
const sortOrder = ref<'asc' | 'desc'>('asc');

// WebSocket 연결
const connectWebSocket = () => {
  try {
    const ws = new WebSocket('ws://localhost:8080/ws');
    
    ws.onopen = () => {
      console.log('WebSocket 연결됨');
      
      // character-updates 토픽 구독
      ws.send(JSON.stringify({
        type: 'SUBSCRIBE',
        topic: 'character-updates'
      }));
    };
    
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('WebSocket 메시지 수신:', data);
        
        // 캐릭터 업데이트 처리
        if (data.type === 'CHARACTER_UPDATED') {
          handleCharacterUpdate(data);
        }
      } catch (error) {
        console.error('WebSocket 메시지 파싱 오류:', error);
      }
    };
    
    ws.onclose = () => {
      console.log('WebSocket 연결 끊어짐');
      // 재연결 시도
      setTimeout(connectWebSocket, 5000);
    };
    
    ws.onerror = (error) => {
      console.error('WebSocket 오류:', error);
    };
  } catch (error) {
    console.error('WebSocket 연결 실패:', error);
  }
};



// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  await initializeWebSocket();
  connectWebSocket();
});

onUnmounted(() => {
  // websocketService.removeAllEventListeners(); // 임시 비활성화
  // websocketService.disconnect(); // 임시 비활성화
});

// 모험단 검색
const searchAdventure = async () => {
  if (!adventureName.value.trim()) {
    error.value = '모험단명을 입력해주세요.';
    return;
  }

  try {
    searching.value = true;
    error.value = '';
    successMessage.value = '';

    // 백엔드 API를 통한 모험단 검색
    const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(adventureName.value)}`);
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        characters.value = data.characters || [];
        successMessage.value = `'${adventureName.value}' 모험단의 ${characters.value.length}개 캐릭터를 찾았습니다.`;
        
        // 모험단 선택 상태 업데이트
        selectedAdventure.value = adventureName.value;
        
        // 캐릭터 로드 후 동기화 시간 초기화
        initializeSyncTimes();
      } else {
        error.value = data.message || '모험단 검색에 실패했습니다.';
      }
    } else {
      error.value = '모험단 검색 중 오류가 발생했습니다.';
    }

  } catch (err) {
    console.error('모험단 검색 실패:', err);
    error.value = '모험단 검색 중 오류가 발생했습니다.';
  } finally {
    searching.value = false;
  }
};

// 서버 변경 시 - 더 이상 필요하지 않음
// const onServerChange = () => {
//   if (characterName.value.trim()) {
//     searchCharacters();
//   }
// };

// 모험단 목록 (백엔드에서 로드)
const allAdventures = ref<string[]>([]);



// 직업이 버퍼인지 확인하는 함수
const isBuffer = (character: Character): boolean => {
  if (!character.jobName || character.jobName === 'N/A') return false;
  
  // "眞" 문자를 제거한 후 버퍼 직업 판별
  const cleanJobName = formatJobName(character.jobName);
  const cleanJobGrowName = character.jobGrowName ? formatJobName(character.jobGrowName) : '';
  
  // 버퍼 직업 목록 (眞 제거 후 판별)
  const bufferJobs = ['뮤즈', '패러메딕', '크루세이더', '인챈트리스'];
  
  return bufferJobs.some(job => 
    cleanJobName.includes(job) || cleanJobGrowName.includes(job)
  );
};

// 모험단별 필터링 - DB에서 로드된 것과 localStorage 검색 기록 합쳐서 표시
const availableAdventures = computed(() => {
  const adventures = new Set<string>();
  
  // DB에서 로드된 모험단들 추가
  allAdventures.value.forEach(name => adventures.add(name));
  
  // localStorage 검색 기록에서 모험단들 추가
  try {
    const searchHistory = JSON.parse(localStorage.getItem('df_search_history') || '[]');
    searchHistory.forEach((record: any) => {
      if (record.adventureName && record.adventureName !== 'N/A') {
        adventures.add(record.adventureName);
      }
    });
  } catch (error) {
    console.error('localStorage 검색 기록 로드 실패:', error);
  }
  
  // 현재 검색 결과의 모험단들도 추가
  characters.value.forEach(char => {
    if (char.adventureName && char.adventureName !== 'N/A') {
      adventures.add(char.adventureName);
    }
  });
  
  return Array.from(adventures).sort();
});

const filteredCharacters = computed(() => {
  if (!selectedAdventure.value) {
    return characters.value;
  }
  return characters.value.filter(char => char.adventureName === selectedAdventure.value);
});

const filterByAdventure = async () => {
  if (selectedAdventure.value) {
    // 모험단을 선택하면 해당 모험단의 모든 캐릭터를 로드
    try {
      searching.value = true;
      const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(selectedAdventure.value)}`);
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          characters.value = data.characters;
          adventureName.value = ''; // 검색어 초기화
          successMessage.value = `'${selectedAdventure.value}' 모험단의 ${characters.value.length}개 캐릭터를 로드했습니다.`;
        }
      }
    } catch (err) {
      console.error('모험단별 캐릭터 로드 실패:', err);
      error.value = '모험단별 캐릭터 로드에 실패했습니다.';
    } finally {
      searching.value = false;
    }
  }
  // 전체 모험단 선택 시는 필터링만 적용
};

// 던전 통계 (안감 제외)
const dungeonStats = computed(() => {
  const stats = {
    nabel: 0,
    venus: 0,
    fog: 0,
    twilight: 0,
    nabelTotal: 0,
    venusTotal: 0,
    fogTotal: 0,
    twilightTotal: 0
  };
  
  filteredCharacters.value.forEach(char => {
    // 안감되지 않은 캐릭터만 통계에 포함
    if (!char.isExcludedNabel) {
      stats.nabelTotal++;
      if (char.dungeonClearNabel) stats.nabel++;
    }
    if (!char.isExcludedVenus) {
      stats.venusTotal++;
      if (char.dungeonClearVenus) stats.venus++;
    }
    if (!char.isExcludedFog) {
      stats.fogTotal++;
      if (char.dungeonClearFog) stats.fog++;
    }
    // 황혼전은 아직 안감 기능 없음
    stats.twilightTotal++;
    if (char.dungeonClearTwilight) stats.twilight++;
  });
  
  return stats;
});

// 던전 클리어 비율 계산 (안감 제외)
const getDungeonPercentage = (dungeon: 'nabel' | 'venus' | 'fog' | 'twilight') => {
  const totalKey = `${dungeon}Total` as keyof typeof dungeonStats.value;
  const total = dungeonStats.value[totalKey] as number;
  if (total === 0) return 0;
  return Math.round((dungeonStats.value[dungeon] / total) * 100);
};

// 정렬 기능
const sortBy = (field: string) => {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortField.value = field;
    sortOrder.value = 'asc';
  }
};

// 정렬 초기화
const resetSort = () => {
  sortField.value = 'characterName';
  sortOrder.value = 'asc';
};

// 던담 링크 생성 함수
const getDundamLink = (character: Character): string => {
  // serverId가 유효하지 않은 경우 기본값 사용
  const serverId = character.serverId && character.serverId !== 'all' ? character.serverId : 'bakal';
  return `https://dundam.xyz/character?server=${serverId}&key=${character.characterId}`;
};

// 던담 링크 툴팁 생성 함수
const getDundamLinkTitle = (character: Character): string => {
  if (!character.serverId || character.serverId === 'all') {
    return '서버 정보가 없어 기본 서버(bakal)로 연결됩니다';
  }
  return `던담에서 ${character.characterName} 캐릭터 정보 보기`;
};

const sortedCharacters = computed(() => {
  return [...filteredCharacters.value].sort((a, b) => {
    // 나벨 정렬
    if (sortField.value === 'nabel') {
      if (a.dungeonClearNabel !== b.dungeonClearNabel) {
        return sortOrder.value === 'asc' ? 
          (a.dungeonClearNabel ? -1 : 1) : 
          (a.dungeonClearNabel ? 1 : -1);
      }
      // 동점 시 캐릭터 이름으로 정렬
      return a.characterName.localeCompare(b.characterName);
    }
    
    // 베누스 정렬
    if (sortField.value === 'venus') {
      if (a.dungeonClearVenus !== b.dungeonClearVenus) {
        return sortOrder.value === 'asc' ? 
          (a.dungeonClearVenus ? -1 : 1) : 
          (a.dungeonClearVenus ? 1 : -1);
      }
      // 동점 시 캐릭터 이름으로 정렬
      return a.characterName.localeCompare(b.characterName);
    }
    
    // 안개신 정렬
    if (sortField.value === 'fog') {
      if (a.dungeonClearFog !== b.dungeonClearFog) {
        return sortOrder.value === 'asc' ? 
          (a.dungeonClearFog ? -1 : 1) : 
          (a.dungeonClearFog ? 1 : -1);
      }
      // 동점 시 캐릭터 이름으로 정렬
      return a.characterName.localeCompare(b.characterName);
    }
    
    // 기존 정렬 로직
    let aVal = a[sortField.value as keyof Character];
    let bVal = b[sortField.value as keyof Character];
    
    // null/undefined 처리
    if (aVal == null) aVal = '';
    if (bVal == null) bVal = '';
    
    // 숫자 정렬
    if (typeof aVal === 'number' && typeof bVal === 'number') {
      return sortOrder.value === 'asc' ? aVal - bVal : bVal - aVal;
    }
    
    // 문자열 정렬
    const aStr = String(aVal).toLowerCase();
    const bStr = String(bVal).toLowerCase();
    
    if (sortOrder.value === 'asc') {
      return aStr.localeCompare(bStr);
    } else {
      return bStr.localeCompare(aStr);
    }
  });
});



// 실시간 업데이트 (웹소켓 연결)
const realTimeUpdate = async (character: Character) => {
  if (updating.value) return;
  
  try {
    updating.value = true;
    
    // 던담에서 크롤링 + DFO API 타임라인 체크
    await refreshDungeonStatus(character);
    
    successMessage.value = `${character.characterName}의 실시간 정보가 업데이트되었습니다.`;
  } catch (err) {
    console.error('실시간 업데이트 실패:', err);
    error.value = '실시간 업데이트에 실패했습니다.';
  } finally {
    updating.value = false;
  }
};

// 동기화 시간 초기화 (페이지 로드 시)
const initializeSyncTimes = () => {
  // 현재 시간에서 3분 전으로 설정하여 즉시 동기화 가능하게 함
  const threeMinutesAgo = new Date(Date.now() - 3 * 60 * 1000);
  
  // 모든 캐릭터의 동기화 시간을 3분 전으로 설정
  characters.value.forEach(character => {
    lastSyncTimes.value.set(character.characterId, threeMinutesAgo);
  });
  
  // 모든 모험단의 동기화 시간을 3분 전으로 설정
  const uniqueAdventures = [...new Set(characters.value.map(c => c.adventureName))];
  uniqueAdventures.forEach(adventureName => {
    lastAdventureSyncTimes.value.set(adventureName, threeMinutesAgo);
  });
  
  console.log('동기화 시간 초기화 완료:', {
    characters: lastSyncTimes.value.size,
    adventures: lastAdventureSyncTimes.value.size
  });
};

// 웹소켓 초기화
const initializeWebSocket = async () => {
  try {
    // websocketService.connect(); // 임시 비활성화
    // isConnected.value = websocketService.getConnectionStatus().value; // 임시 비활성화
    
    // 실시간 이벤트 리스너 등록 // 임시 비활성화
    // websocketService.addEventListener('CHARACTER_UPDATED', handleCharacterUpdate); // 임시 비활성화
    // websocketService.addEventListener('SYSTEM_NOTIFICATION', handleSystemNotification); // 임시 비활성화
    
    console.log('웹소켓 연결 완료 (임시 비활성화)');
  } catch (err) {
    console.error('웹소켓 연결 실패:', err);
    error.value = '실시간 업데이트 연결에 실패했습니다.';
  }
};

// 캐릭터 업데이트 이벤트 처리 (WebSocket + 기존 통합)
const handleCharacterUpdate = (data: any) => {
  try {
    // WebSocket 메시지 처리
    if (data.data && data.data.updateType === 'dundam_sync') {
      const { characterId, updateResult } = data.data;
      
      if (updateResult.success) {
        // 해당 캐릭터 찾아서 업데이트
        const characterIndex = characters.value.findIndex(c => c.characterId === characterId);
        if (characterIndex !== -1) {
          const character = characters.value[characterIndex];
          
                  // 스탯 업데이트
        if (updateResult.characterInfo) {
          character.totalDamage = updateResult.characterInfo.totalDamage || 0;
          character.buffPower = updateResult.characterInfo.buffPower || 0;
        }
          
          // 성공 메시지 표시
          successMessage.value = `${character.characterName}의 던담 동기화가 완료되었습니다.`;
          
          console.log('캐릭터 실시간 업데이트 완료:', character.characterName);
        }
      }
    }
    // 기존 RealtimeEvent 처리 (임시 비활성화)
    else if (data.data) {
      const updatedCharacter = data.data;
      
      // 현재 캐릭터 목록에서 해당 캐릭터 업데이트
      const index = characters.value.findIndex(c => c.characterId === updatedCharacter.characterId);
      if (index !== -1) {
        characters.value[index] = { ...characters.value[index], ...updatedCharacter };
      }
      
      // 진행률 업데이트
      if (updatedCharacter.progress) {
        updateProgress.value = updatedCharacter.progress;
      }
      
      successMessage.value = `${updatedCharacter.characterName} 정보가 업데이트되었습니다.`;
      
      // 완료 시 상태 초기화
      if (updatedCharacter.completed) {
        setTimeout(() => {
          updating.value = false;
          updateProgress.value = 0;
          updateStatus.value = '';
        }, 1000);
      }
    }
  } catch (error) {
    console.error('캐릭터 업데이트 처리 오류:', error);
  }
};

// 시스템 알림 이벤트 처리 // 임시 비활성화
const handleSystemNotification = (event: RealtimeEvent) => {
  updateStatus.value = event.message;
  
  if (event.data) {
    if (event.data.progress) {
      updateProgress.value = event.data.progress;
    }
    
    if (event.data.completed) {
      updating.value = false;
      updateProgress.value = 100;
      setTimeout(() => {
        updateProgress.value = 0;
        updateStatus.value = '';
      }, 2000);
    }
  }
  
  // 알림을 배열에 추가 (최근 5개만 유지) // 임시 비활성화
  realtimeNotifications.value.unshift(event);
  if (realtimeNotifications.value.length > 5) {
    realtimeNotifications.value = realtimeNotifications.value.slice(0, 5);
  }
};

// 모험단 실시간 업데이트
const refreshAdventure = async () => {
  if (!selectedAdventure.value) {
    error.value = '모험단을 선택해주세요.';
    return;
  }
  
  try {
    updating.value = true;
    updateProgress.value = 0;
    updateStatus.value = '업데이트를 시작합니다...';
    error.value = '';
    
    const response = await fetch(`http://localhost:8080/api/realtime/adventure/${encodeURIComponent(selectedAdventure.value)}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: 'anonymous'
      })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        updateStatus.value = `'${selectedAdventure.value}' 모험단 실시간 업데이트가 시작되었습니다.`;
      } else {
        throw new Error(result.message || '업데이트 요청에 실패했습니다.');
      }
    } else {
      const errorText = await response.text();
      console.error('실시간 업데이트 API 에러:', response.status, errorText);
      throw new Error(`서버 요청 실패 (${response.status}): ${errorText}`);
    }
    
  } catch (err: any) {
    console.error('실시간 업데이트 실패:', err);
    error.value = err.message || '실시간 업데이트에 실패했습니다.';
    updating.value = false;
    updateProgress.value = 0;
    updateStatus.value = '';
    
    // 에러 로그 상세 출력
    if (err.message) {
      console.error('에러 상세:', err.message);
    }
  }
};

// 개별 캐릭터 실시간 업데이트
const refreshCharacter = async (characterId: string) => {
  try {
    const response = await fetch(`http://localhost:8080/api/realtime/character/${characterId}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: 'anonymous' // 임시 비활성화
      })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        successMessage.value = `${result.characterName} 업데이트를 시작했습니다.`;
      } else {
        throw new Error(result.message || '캐릭터 업데이트 요청에 실패했습니다.');
      }
    } else {
      throw new Error('서버 요청에 실패했습니다.');
    }
    
  } catch (err: any) {
    console.error('캐릭터 업데이트 실패:', err);
    error.value = err.message || '캐릭터 업데이트에 실패했습니다.';
  }
};

// 유틸리티 함수들
const formatNumber = (num?: number): string => {
  if (num === undefined || num === null) return 'N/A';
  return num.toLocaleString();
};

// 총딜 포맷팅 함수 (천만 자리에서 반올림, 억 단위 표시)
const formatTotalDamage = (num?: number): string => {
  if (num === undefined || num === null) return 'N/A';
  if (num >= 100000000) { // 1억 이상
    const billion = Math.round(num / 10000000) / 10; // 천만 자리에서 반올림 후 10으로 나누기
    return `${billion}억`;
  } else if (num >= 10000) { // 1만 이상
    const tenThousand = Math.round(num / 10000);
    return `${tenThousand}만`;
  } else if (num >= 1000) { // 1천 이상
    const thousand = Math.round(num / 1000);
    return `${thousand}천`;
  }
  return num.toLocaleString();
};

// 버프력 포맷팅 함수 (천 자리에서 반올림, 만 단위 표시)
const formatBuffPower = (num?: number): string => {
  if (num === undefined || num === null) return 'N/A';
  if (num >= 10000) { // 1만 이상
    const tenThousand = Math.round(num / 1000) / 10; // 천 자리에서 반올림 후 10으로 나누기
    return `${tenThousand}만`;
  } else if (num >= 1000) { // 1천 이상
    const thousand = Math.round(num / 1000);
    return `${thousand}천`;
  }
  return num.toLocaleString();
};

// 직업명 포맷팅 함수
const formatJobName = (jobName: string): string => {
  if (!jobName || jobName === 'N/A') return 'N/A';
  
  // 이미 정리된 직업명이면 그대로 반환
  if (!jobName.includes('(') && !jobName.includes('眞')) {
    return jobName;
  }
  
  // 괄호 안의 내용만 추출 (예: "귀검사(여) (베가본드)" → "베가본드")
  const match = jobName.match(/\(([^)]+)\)$/);
  if (match) {
    return match[1].replace(/眞\s*/, '').trim(); // "眞" 문자 제거 및 공백 정리
  }
  
  // 괄호가 없으면 "眞" 문자만 제거
  return jobName.replace(/眞\s*/, '').trim();
};

// DB에서 가져온 하드 나벨 대상자 여부 사용
// meetsHardCondition 함수는 더 이상 필요하지 않음

// 하드 모드 상태 관리
const hardModeStates = ref<Map<string, boolean>>(new Map());

// 하드 모드 토글
const toggleHardMode = (character: any) => {
  if (character.isHardNabelEligible) {
    const currentState = hardModeStates.value.get(character.characterId) || false;
    hardModeStates.value.set(character.characterId, !currentState);
    console.log(`${character.characterName} 하드 모드: ${!currentState ? '활성화' : '비활성화'}`);
  }
};

// 하드 모드 상태 가져오기
const getHardModeState = (character: any): boolean => {
  return hardModeStates.value.get(character.characterId) || false;
};

// 이미지 로드 에러 처리
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.style.display = 'none';
  console.warn('캐릭터 이미지 로드 실패:', img.src);
};

const formatDateTime = (dateStr?: string): string => {
  if (!dateStr) return 'N/A';
  const date = new Date(dateStr);
  return date.toLocaleString('ko-KR');
};

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('ko-KR', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  });
};

// 안감 토글
const toggleExclude = async (character: Character, dungeonType: string) => {
  try {
    const currentState = character[`isExcluded${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}` as keyof Character] as boolean;
    const newState = !currentState;
    
            const response = await fetch(`http://localhost:8080/api/characters/${character.characterId}/exclude-dungeon`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            dungeonType: dungeonType,
            isExcluded: newState
          })
        });
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        // 로컬 상태 업데이트
        (character as any)[`isExcluded${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}`] = newState;
        
        // 안감 설정 시 업둥 해제
        if (newState) {
          (character as any)[`isSkip${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}`] = false;
        }
        
        successMessage.value = `${character.characterName}의 ${dungeonType} ${newState ? '안감' : '안감 해제'} 설정이 완료되었습니다.`;
      } else {
        error.value = data.message || '안감 설정에 실패했습니다.';
      }
    } else {
      error.value = '안감 설정 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('안감 설정 오류:', err);
    error.value = '안감 설정 중 오류가 발생했습니다.';
  }
};

// 업둥 토글
const toggleSkip = async (character: Character, dungeonType: string) => {
  try {
    const currentState = character[`isSkip${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}` as keyof Character] as boolean;
    const newState = !currentState;
    
    const response = await fetch(`http://localhost:8080/api/characters/${character.characterId}/skip-dungeon`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        dungeonType: dungeonType,
        isSkip: newState
      })
    });
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        // 로컬 상태 업데이트
        (character as any)[`isSkip${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}`] = newState;
        
        // 업둥 설정 시 안감 해제
        if (newState) {
          (character as any)[`isExcluded${dungeonType.charAt(0).toUpperCase() + dungeonType.slice(1)}`] = false;
        }
        
        successMessage.value = `${character.characterName}의 ${dungeonType} ${newState ? '업둥' : '업둥 해제'} 설정이 완료되었습니다.`;
      } else {
        error.value = data.message || '업둥 설정에 실패했습니다.';
      }
    } else {
      error.value = '업둥 설정 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('업둥 설정 오류:', err);
    error.value = '업둥 설정 중 오류가 발생했습니다.';
  }
};

// 모험단 전체 캐릭터 최신화
const refreshAllCharacters = async () => {
  if (!selectedAdventure.value) {
    error.value = '모험단을 선택해주세요.';
    return;
  }
  
  try {
    refreshingAll.value = true;
    error.value = '';
    successMessage.value = '';
    
    const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(selectedAdventure.value)}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        // 최신화 결과 메시지 표시
        successMessage.value = data.message;
        
        // 캐릭터 목록 다시 로드
        await searchAdventure();
        
        console.log('모험단 전체 최신화 완료:', data.data);
      } else {
        error.value = data.message || '모험단 전체 최신화에 실패했습니다.';
      }
    } else {
      error.value = '모험단 전체 최신화 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('모험단 전체 최신화 오류:', err);
    error.value = '모험단 전체 최신화 중 오류가 발생했습니다.';
  } finally {
    refreshingAll.value = false;
  }
};

// 수동 입력 관련 함수들
const showManualInput = (character: Character, statType: string) => {
  manualInputCharacter.value = character;
  
  // 버퍼면 버프력만, 딜러면 총딜만 표시
  if (statType === 'buffPower') {
    manualInputData.value = {
      buffPower: character.buffPower || null,
      totalDamage: null
    };
  } else {
    manualInputData.value = {
      buffPower: null,
      totalDamage: character.totalDamage || null
    };
  }
  
  showManualInputModal.value = true;
};

const closeManualInputModal = () => {
  showManualInputModal.value = false;
  manualInputCharacter.value = null;
  manualInputData.value = {
    buffPower: null,
    totalDamage: null
  };
};

const saveManualInput = async () => {
  if (!manualInputCharacter.value) return;
  
  try {
    const response = await fetch(`http://localhost:8080/api/characters/${manualInputCharacter.value.characterId}/manual-stats`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        buffPower: manualInputData.value.buffPower,
        totalDamage: manualInputData.value.totalDamage,
        updatedBy: 'manual'
      })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 로컬 상태 업데이트 (해당 스탯만 업데이트)
        const character = characters.value.find(c => c.characterId === manualInputCharacter.value?.characterId);
        if (character) {
          if (isBuffer(character)) {
            character.buffPower = manualInputData.value.buffPower || 0;
          } else {
            character.totalDamage = manualInputData.value.totalDamage || 0;
          }
        }
        
        successMessage.value = '스탯이 성공적으로 저장되었습니다.';
        setTimeout(() => {
          successMessage.value = '';
        }, 3000);
        
        closeManualInputModal();
      } else {
        error.value = result.message || '스탯 저장에 실패했습니다.';
      }
    } else {
      error.value = '스탯 저장 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('스탯 저장 오류:', err);
    error.value = '스탯 저장 중 오류가 발생했습니다.';
  }
};

// 던담 동기화 함수
const syncCharacterFromDundam = async (character: Character) => {
  if (syncingCharacters.value.has(character.characterId)) return;
  
  try {
    syncingCharacters.value.add(character.characterId);
    
    const response = await fetch(`http://localhost:8080/api/dundam-sync/character/${character.serverId}/${character.characterId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 로컬 상태 업데이트
        const char = characters.value.find(c => c.characterId === character.characterId);
        if (char) {
          char.buffPower = result.characterInfo?.buffPower || 0;
          char.totalDamage = result.characterInfo?.totalDamage || 0;
        }
        
        successMessage.value = '던담 동기화가 완료되었습니다.';
        // 에러 메시지 제거 및 동기화 시간 업데이트
        characterErrors.value.delete(character.characterId);
        lastSyncTimes.value.set(character.characterId, new Date());
        lastAdventureSyncTimes.value.set(character.adventureName, new Date());
        
        // 화면 강제 업데이트를 위해 캐릭터 목록 새로고침
        if (selectedAdventure.value) {
          await searchAdventure();
        }
        
        setTimeout(() => {
          successMessage.value = '';
        }, 3000);
      } else {
        // 다양한 에러 상태 확인
        let errorMessage = '';
        if (result.timeLimitExceeded) {
          const remainingMinutes = result.remainingMinutes || 0;
          errorMessage = `던담 동기화는 2분에 한 번만 가능합니다. ${remainingMinutes}분 후에 다시 시도해주세요.`;
        } else if (result.crawlingDisabled) {
          errorMessage = '던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.';
        } else if (result.thursdayRestriction) {
          errorMessage = '목요일에는 던담 크롤링이 제한되어 데이터를 제공할 수 없습니다.';
        } else {
          errorMessage = result.message || '던담 동기화에 실패했습니다.';
        }
        characterErrors.value.set(character.characterId, errorMessage);
      }
    } else {
      error.value = '던담 동기화 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('던담 동기화 오류:', err);
    error.value = '던담 동기화 중 오류가 발생했습니다.';
  } finally {
    syncingCharacters.value.delete(character.characterId);
  }
};

// 캐릭터 정보 최신화 함수
const refreshCharacterInfo = async (character: Character) => {
  if (refreshingCharacters.value.includes(character.characterId)) return;
  
  try {
    refreshingCharacters.value.push(character.characterId);
    
    const response = await fetch(`http://localhost:8080/api/characters/${character.serverId}/${character.characterId}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 로컬 상태 업데이트
        const char = characters.value.find(c => c.characterId === character.characterId);
        if (char) {
          char.fame = result.character?.fame || 0;
          char.level = result.character?.level || 0;
        }
        
        successMessage.value = '캐릭터 정보가 최신화되었습니다.';
        setTimeout(() => {
          successMessage.value = '';
        }, 3000);
      } else {
        // 다양한 에러 상태 확인
        let errorMessage = '';
        if (result.timeLimitExceeded) {
          const remainingMinutes = result.remainingMinutes || 0;
          errorMessage = `케릭터 정보 최신화는 2분에 한 번만 가능합니다. ${remainingMinutes}분 후에 다시 시도해주세요.`;
        } else if (result.crawlingDisabled) {
          errorMessage = '던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.';
        } else if (result.thursdayRestriction) {
          errorMessage = '목요일에는 던담 크롤링이 제한되어 데이터를 제공할 수 없습니다.';
        } else {
          errorMessage = result.message || '케릭터 정보 최신화에 실패했습니다.';
        }
        characterErrors.value.set(character.characterId, errorMessage);
      }
    } else {
      error.value = '캐릭터 정보 최신화 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('캐릭터 정보 최신화 오류:', err);
    error.value = '캐릭터 정보 최신화 중 오류가 발생했습니다.';
  } finally {
    const index = refreshingCharacters.value.indexOf(character.characterId);
    if (index > -1) {
      refreshingCharacters.value.splice(index, 1);
    }
  }
};

// 타임라인 최신화 함수 (던전 클리어 현황)
const refreshDungeonStatus = async (character: Character) => {
  if (refreshingTimeline.value.includes(character.characterId)) return;
  
  try {
    refreshingTimeline.value.push(character.characterId);
    
    const response = await fetch(`http://localhost:8080/api/dungeon-clear/${character.serverId}/${character.characterId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        // 로컬 상태 업데이트
        const char = characters.value.find(c => c.characterId === character.characterId);
        if (char) {
          char.dungeonClearNabel = result.clearStatus?.nabel || false;
          char.dungeonClearVenus = result.clearStatus?.venus || false;
          char.dungeonClearFog = result.clearStatus?.fog || false;
          char.lastDungeonCheck = new Date().toISOString();
        }
        
        successMessage.value = '던전 클리어 현황이 최신화되었습니다.';
        setTimeout(() => {
          successMessage.value = '';
        }, 3000);
      } else {
        // 다양한 에러 상태 확인
        let errorMessage = '';
        if (result.timeLimitExceeded) {
          const remainingMinutes = result.remainingMinutes || 0;
          errorMessage = `타임라인 최신화는 2분에 한 번만 가능합니다. ${remainingMinutes}분 후에 다시 시도해주세요.`;
        } else if (result.crawlingDisabled) {
          errorMessage = '던담 크롤링이 비활성화되어 있습니다. DFO API와 수동 입력을 사용하세요.';
        } else if (result.thursdayRestriction) {
          errorMessage = '목요일에는 던담 크롤링이 제한되어 데이터를 제공할 수 없습니다.';
        } else {
          errorMessage = result.message || '던전 클리어 현황 최신화에 실패했습니다.';
        }
        characterErrors.value.set(character.characterId, errorMessage);
      }
    } else {
      error.value = '던전 클리어 현황 최신화 요청에 실패했습니다.';
    }
  } catch (err) {
    console.error('던전 클리어 현황 최신화 오류:', err);
    error.value = '던전 클리어 현황 최신화 중 오류가 발생했습니다.';
  } finally {
    const index = refreshingTimeline.value.indexOf(character.characterId);
    if (index > -1) {
      refreshingTimeline.value.splice(index, 1);
    }
  }
};
</script>

<style scoped>
.dungeon-status {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.search-form {
  display: flex;
  gap: 15px;
  align-items: end;
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: bold;
  color: #495057;
}

.form-group select,
.form-group input {
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
}

.search-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn:hover {
  background: #0056b3;
}

.search-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.refresh-all-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: background 0.3s ease;
}

.refresh-all-btn:hover:not(:disabled) {
  background: #218838;
}

.refresh-all-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.reset-sort-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 10px;
  font-size: 12px;
}

.reset-sort-btn:hover {
  background: #5a6268;
}

.adventure-selection {
  margin-bottom: 30px;
  padding: 20px;
  background: #e9ecef;
  border-radius: 8px;
}

.adventure-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
}

.adventure-dropdown label {
  font-weight: bold;
  color: #495057;
}

.adventure-dropdown select {
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.summary-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background: white;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  min-width: 120px;
}

.stat-label {
  font-size: 12px;
  color: #6c757d;
  font-weight: bold;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #212529;
  margin: 5px 0;
}

.stat-percentage {
  font-size: 12px;
  color: #28a745;
}

.characters-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.characters-table th,
.characters-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #dee2e6;
}

.characters-table th {
  background: #f8f9fa;
  font-weight: bold;
  color: #495057;
}

.characters-table th.sortable {
  cursor: pointer;
  user-select: none;
}

.characters-table th.sortable:hover {
  background: #e9ecef;
}

.characters-table th.sortable.dungeon-clear-column {
  background: linear-gradient(135deg, #ff6b6b, #4ecdc4, #45b7d1);
  color: white;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.3);
}

.characters-table th.sortable.dungeon-clear-column:hover {
  background: linear-gradient(135deg, #ff5252, #26d0ce, #29b6f6);
  transform: scale(1.02);
  transition: all 0.2s ease;
}

.sort-priority-info {
  font-size: 10px;
  font-weight: normal;
  margin-top: 2px;
  opacity: 0.8;
  line-height: 1.2;
}

.characters-table tr.all-cleared {
  background: rgba(40, 167, 69, 0.1);
}

.adventure-name {
  font-weight: bold;
  color: #495057;
}

.character-name {
  font-weight: bold;
  color: #212529;
}

.dungeon-clear-column {
  text-align: center;
  min-width: 120px;
}

.dungeon-status-cell {
  text-align: center;
  padding: 8px;
}

/* 던전별 바운더리 색상 */
.nabel-cell {
  border: 2px solid #ff6b6b; /* 빨간색 - 나벨 */
}

.venus-cell {
  border: 2px solid #4ecdc4; /* 청록색 - 베누스 */
}

.fog-cell {
  border: 2px solid #45b7d1; /* 파란색 - 안개신 */
}

.dungeon-clear-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

/* 안감 상태 배경색 */
.dungeon-clear-status.excluded {
  background-color: #f8f9fa;
  color: #6c757d;
  border: 1px dashed #dee2e6;
}

/* 업둥 상태 배경색 */
.dungeon-clear-status.skip {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
  font-weight: bold;
}

.dungeon-clear-status.cleared .clear-icon {
  color: #28a745;
}

.dungeon-clear-status.cleared .clear-text {
  color: #28a745;
  font-weight: bold;
}

.clear-icon {
  font-size: 16px;
}

.clear-text {
  font-size: 11px;
  color: #6c757d;
}

.update-time {
  font-size: 12px;
  color: #6c757d;
}

.action-buttons {
  display: flex;
  gap: 5px;
  justify-content: center;
}

.refresh-btn,
.realtime-btn {
  border: none;
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
}

.refresh-btn:hover,
.realtime-btn:hover {
  background: #e9ecef;
}

.refresh-btn:disabled,
.realtime-btn:disabled {
  background: #6c757d;
  color: white;
  cursor: not-allowed;
}

.no-results,
.initial-state {
  text-align: center;
  padding: 40px;
  color: #6c757d;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #007bff;
}

.error-message {
  padding: 15px;
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  margin-top: 15px;
}

.success-message {
  padding: 15px;
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
  border-radius: 4px;
  margin-top: 15px;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .summary-stats {
    justify-content: center;
  }
  
  .characters-table {
    font-size: 14px;
  }
  
  .characters-table th,
  .characters-table td {
    padding: 8px 4px;
  }
}

/* 실시간 업데이트 스타일 */
.refresh-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 10px;
  font-size: 14px;
  transition: background 0.3s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #218838;
}

.refresh-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.update-status {
  background: #e3f2fd;
  border: 1px solid #90caf9;
  border-radius: 8px;
  padding: 20px;
  margin: 20px 0;
}

.update-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.update-header h3 {
  margin: 0;
  color: #1976d2;
}

.connection-status {
  font-size: 14px;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 4px;
  background: #ffebee;
  color: #c62828;
}

.connection-status.connected {
  background: #e8f5e8;
  color: #2e7d32;
}

.progress-bar {
  position: relative;
  background: #f5f5f5;
  border-radius: 4px;
  height: 25px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  background: linear-gradient(90deg, #4caf50, #81c784);
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 4px;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-weight: bold;
  color: #333;
  font-size: 12px;
}

.update-message {
  color: #555;
  font-size: 14px;
  margin-top: 10px;
}

.realtime-notifications {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 15px;
  margin: 20px 0;
}

.realtime-notifications h4 {
  margin: 0 0 10px 0;
  color: #856404;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  display: flex;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px solid #f0f0f0;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-time {
  font-size: 12px;
  color: #666;
  min-width: 70px;
}

.notification-message {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.coming-soon {
  font-size: 11px;
  color: #999;
  font-style: italic;
  margin-left: 5px;
}

/* 안감/업둥 버튼 스타일 */
.action-buttons-mini {
  display: flex;
  gap: 4px;
  justify-content: center;
  margin-top: 4px;
}

.exclude-btn,
.skip-btn {
  padding: 2px 6px;
  font-size: 10px;
  border: 1px solid #dee2e6;
  border-radius: 3px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.exclude-btn {
  color: #6c757d;
}

.exclude-btn.active {
  background: #f8f9fa;
  color: #495057;
  border-color: #adb5bd;
  font-weight: bold;
}

.exclude-btn:hover {
  background: #e9ecef;
  border-color: #adb5bd;
}

.skip-btn {
  color: #856404;
}

.skip-btn.active {
  background: #fff3cd;
  color: #856404;
  border-color: #ffeaa7;
  font-weight: bold;
  box-shadow: 0 0 0 2px rgba(255, 193, 7, 0.25);
}

.skip-btn:hover {
  background: #fff3cd;
  border-color: #ffeaa7;
}

/* 수정 버튼 스타일 */
.stat-with-edit {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.edit-btn-mini {
  background: #6c757d;
  color: white;
  border: none;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.edit-btn-mini:hover {
  background: #5a6268;
}

/* 모달 스타일 */
.modal-overlay {
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
  border-radius: 8px;
  padding: 0;
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 15px 20px;
  border-bottom: 1px solid #e9ecef;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6c757d;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.close-btn:hover {
  background: #f8f9fa;
  color: #495057;
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
  color: #495057;
  font-size: 14px;
}

.input-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.input-group input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 15px 20px 20px 20px;
  border-top: 1px solid #e9ecef;
}

.save-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s ease;
}

.save-btn:hover {
  background: #218838;
}

.cancel-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s ease;
}

.cancel-btn:hover {
  background: #5a6268;
}

/* 직업 컬럼 스타일 */
.job-name {
  font-size: 14px;
  color: #495057;
  font-weight: 500;
}

.job-name span {
  display: inline-block;
  padding: 4px 8px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

/* 스탯 표시 스타일 */
.stat-label {
  font-weight: 600;
  color: #495057;
  margin-right: 8px;
  font-size: 13px;
}

.stat-with-edit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-with-edit .edit-btn-mini {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 2px;
  border-radius: 3px;
  transition: background 0.2s ease;
}

.stat-with-edit .edit-btn-mini:hover {
  background: #f8f9fa;
}

/* 액션칸 4개 분할 스타일 */
.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  min-width: 200px;
}

.action-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  background: #f8f9fa;
}

.action-label {
  font-size: 10px;
  color: #6c757d;
  font-weight: bold;
  text-align: center;
  line-height: 1.2;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover:not(:disabled) {
  transform: scale(1.1);
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dundam-sync-btn {
  background: #007bff;
  color: white;
}

.dundam-sync-btn.syncing {
  background: #ffc107;
  color: #212529;
  cursor: not-allowed;
}

.syncing-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.dundam-sync-btn:hover:not(:disabled) {
  background: #0056b3;
}

.refresh-btn {
  background: #28a745;
  color: white;
}

.refresh-btn:hover:not(:disabled) {
  background: #1e7e34;
}

.timeline-btn {
  background: #ffc107;
  color: #212529;
}

.timeline-btn:hover:not(:disabled) {
  background: #e0a800;
}

.edit-btn {
  background: #6f42c1;
  color: white;
}

.edit-btn:hover:not(:disabled) {
  background: #5a32a3;
}

.character-error-message {
  font-size: 10px;
  color: #dc3545;
  margin-top: 3px;
  text-align: center;
  word-break: break-word;
  max-width: 120px;
  line-height: 1.2;
  padding: 2px;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 3px;
}

.time-limit-message {
  font-size: 10px;
  color: #6c757d;
  margin-top: 3px;
  text-align: center;
  word-break: break-word;
  max-width: 120px;
  line-height: 1.2;
  padding: 2px;
  background: #e9ecef;
  border: 1px solid #dee2e6;
  border-radius: 3px;
}

.adventure-time-limit-message {
  font-size: 10px;
  color: #fd7e14;
  margin-top: 3px;
  text-align: center;
  word-break: break-word;
  max-width: 120px;
  line-height: 1.2;
  padding: 2px;
  background: #fff3e0;
  border: 1px solid #ffcc02;
  border-radius: 3px;
  font-weight: bold;
}

/* 나벨 난이도 표시 스타일 */
.nabel-difficulty-indicator {
  text-align: center;
  margin: 4px 0;
}

.hard-mode-btn {
  padding: 4px 8px;
  font-size: 10px;
  border: 1px solid #6c757d;
  border-radius: 4px;
  background: #f8f9fa;
  color: #6c757d;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 40px;
}

.hard-mode-btn:hover:not(:disabled) {
  background: #e9ecef;
  border-color: #495057;
}

.hard-mode-btn.active {
  background: #dc3545;
  color: white;
  border-color: #dc3545;
}

.hard-mode-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #e9ecef;
}

/* 일반 나벨 표시 스타일 */
.normal-indicator {
  display: inline-block;
  padding: 2px 6px;
  background: #28a745;
  color: white;
  border-radius: 3px;
  font-size: 10px;
  font-weight: bold;
  text-align: center;
  min-width: 30px;
}

/* 미대상 표시 스타일 */
.not-eligible {
  display: inline-block;
  padding: 2px 6px;
  background: #6c757d;
  color: white;
  border-radius: 3px;
  font-size: 10px;
  font-weight: bold;
  text-align: center;
  min-width: 30px;
}

.syncing-status-message {
  color: #ffc107;
  font-size: 11px;
  margin-top: 2px;
  text-align: center;
  font-weight: bold;
  animation: pulse 1.5s ease-in-out infinite;
  max-width: 120px;
  line-height: 1.2;
  padding: 2px;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 3px;
}

/* 던담 액션 레이아웃 */
.dundam-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.dundam-link {
  display: inline-block;
  padding: 4px 8px;
  background: #17a2b8;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 10px;
  transition: all 0.2s;
  text-align: center;
  min-width: 40px;
}

.dundam-link:hover {
  background: #138496;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

/* 그라데이션 애니메이션 */
.dundam-sync-btn.syncing {
  background: linear-gradient(45deg, #007bff, #28a745, #ffc107, #dc3545);
  background-size: 400% 400%;
  animation: gradientShift 2s ease-in-out infinite;
  color: white;
  border: none;
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0,0,0,0.3);
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  25% {
    background-position: 50% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  75% {
    background-position: 50% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
</style>
