<template>
  <div class="dungeon-status">
          <h2>던전 클리어 현황</h2>
      
      <!-- 모험단 검색 섹션 -->
      <div class="adventure-selection">
        <div class="search-section">
          <div class="search-form">
            <div class="form-group">
              <label for="searchQuery">모험단명 검색:</label>
              <input 
                id="searchQuery" 
                v-model="searchQuery" 
                type="text" 
                placeholder="모험단명을 입력하세요" 
                @keyup.enter="searchAdventure"
                list="adventureList"
              >
              <datalist id="adventureList">
                <option v-for="adventure in recentSearchedAdventures" :key="adventure" :value="adventure">
                  {{ adventure }}
                </option>
              </datalist>
            </div>
            
            <button @click="searchAdventure" :disabled="searching" class="search-btn">
              {{ searching ? '🔍 검색 중...' : '🔍 검색' }}
            </button>
          </div>
          
          <!-- 최근 검색한 모험단 선택 -->
          <div v-if="recentSearchedAdventures.length > 0" class="recent-adventures">
            <label for="recentAdventure">최근 검색:</label>
            <select 
              id="recentAdventure" 
              v-model="selectedRecentAdventure" 
              @change="selectRecentAdventure"
              class="recent-adventure-select"
            >
              <option value="">모험단 선택</option>
              <option v-for="adventure in recentSearchedAdventures" :key="adventure" :value="adventure">
                {{ adventure }}
              </option>
            </select>
          </div>
          

        </div>
      </div>
      
      <!-- 선택된 모험단 표시 (회색 칸 밖으로) -->
      <div v-if="selectedAdventure" class="selected-adventure-display">
        <div class="selected-adventure-info">
          <span class="selected-label">선택된 모험단:</span>
          <span class="selected-adventure-name">{{ selectedAdventure }}</span>
          <span class="character-count">({{ filteredCharacters.length }}개 캐릭터)</span>
        </div>
        
        <!-- 버튼 그룹 -->
        <div class="button-group">
          <!-- 정렬 초기화 버튼 -->
          <button v-if="sortField !== 'characterName' || sortOrder !== 'asc'" 
                  @click="resetSort" 
                  class="reset-sort-btn">
            🔄 정렬 초기화
          </button>
          
          <!-- 전체 최신화 버튼 -->
          <button v-if="filteredCharacters.length > 0" 
                  @click="refreshAllCharacters" 
                  :disabled="refreshingAll" 
                  class="refresh-all-btn">
            {{ refreshingAll ? '🔄 최신화 중...' : '🔄 전체 최신화' }}
            <div class="refresh-subtitle">(던담 동기화 포함)</div>
          </button>
        </div>
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
          <span class="stat-limit">최대 {{ getDungeonLimit('nabel') }}캐릭</span>
          <div class="clear-breakdown">
            <span class="clear-type">일반: {{ dungeonStats.nabelNormal - dungeonStats.nabelHard }}/{{ dungeonStats.nabelTotal }}</span>
            <span class="clear-type">하드: {{ dungeonStats.nabelHard }}/4</span>
          </div>
        </div>
        <div class="stat-card">
          <span class="stat-label">베누스 클리어</span>
          <span class="stat-value">{{ dungeonStats.venus }}/{{ dungeonStats.venusTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('venus') }}%)</span>
          <span class="stat-limit">최대 {{ getDungeonLimit('venus') }}캐릭</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">안개신 클리어</span>
          <span class="stat-value">{{ dungeonStats.fog }}/{{ dungeonStats.fogTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('fog') }}%)</span>
          <span class="stat-limit">최대 {{ getDungeonLimit('fog') }}캐릭</span>
        </div>
        <div class="stat-card">
                        <span class="stat-label">이내 황혼전 클리어</span>
          <span class="stat-value">{{ dungeonStats.twilight }}/{{ dungeonStats.twilightTotal }}</span>
          <span class="stat-percentage">({{ getDungeonPercentage('twilight') }}%)</span>
          <span class="stat-limit">최대 {{ getDungeonLimit('twilight') }}캐릭</span>
        </div>
      </div>

      <table class="characters-table">
        <thead>
          <tr>

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
            <th @click="sortBy('twilight')" class="sortable dungeon-clear-column">
              이내 황혼전
              <span v-if="sortField === 'twilight'">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
            </th>
            <th>액션</th>
            <th>마지막 업데이트</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="character in sortedCharacters" :key="character.characterId" 
              :class="{ 'all-cleared': character.dungeonClearNabel && character.dungeonClearVenus && character.dungeonClearFog && character.dungeonClearTwilight }">
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
                  <div class="stat-display">
                    <div class="stat-label">버프력</div>
                    <div class="stat-value">{{ formatBuffPower(character.buffPower || 0) }}</div>
                  </div>
                </span>
                <span v-else>
                  <div class="stat-display">
                    <div class="stat-label">총딜</div>
                    <div class="stat-value">{{ formatTotalDamage(character.totalDamage || 0) }}</div>
                  </div>
                </span>
              </div>
            </td>
            <td class="dungeon-status-cell nabel-cell">
              <!-- 위쪽: 클리어 여부 -->
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearNabel,
                     excluded: character.isExcludedNabel
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedNabel ? '-' : (character.dungeonClearNabel ? '✅' : '❌') }}
                </span>
                <span class="clear-text">
                  {{ character.isExcludedNabel ? '안감' : (character.dungeonClearNabel ? '클리어' : '미클리어') }}
                </span>
              </div>
              
              
              <!-- 아래쪽: 난이도 선택 버튼들 (2x2 그리드) -->
              <div class="nabel-difficulty-buttons">
                <div class="difficulty-row">
                  <!-- 하드 나벨 버튼 -->
                  <button 
                    @click="setNabelDifficulty(character, 'hard')"
                    :class="{ active: getNabelDifficulty(character) === 'hard' }"
                    :disabled="!isHardEligible(character)"
                    class="difficulty-btn hard-btn"
                    :title="`하드 모드 선택 (isHardNabelEligible: ${character.isHardNabelEligible}, 계산값: ${isHardEligible(character)})`">
                    하드
                  </button>
                  <!-- 일반 나벨 버튼 -->
                  <button 
                    @click="setNabelDifficulty(character, 'normal')"
                    :class="{ active: getNabelDifficulty(character) === 'normal' }"
                    :disabled="!isNormalEligible(character)"
                    class="difficulty-btn normal-btn"
                    :title="`일반 모드 선택 (isNormalNabelEligible: ${character.isNormalNabelEligible}, 계산값: ${isNormalEligible(character)})`">
                    일반
                  </button>
                </div>
                <div class="difficulty-row">
                  <!-- 매칭 나벨 버튼 -->
                  <button 
                    @click="setNabelDifficulty(character, 'matching')"
                    :class="{ active: getNabelDifficulty(character) === 'matching' }"
                    :disabled="!isMatchingEligible(character)"
                    class="difficulty-btn matching-btn"
                    :title="`매칭 모드 선택 (isMatchingNabelEligible: ${character.isMatchingNabelEligible}, 계산값: ${isMatchingEligible(character)})`">
                    매칭
                  </button>
                  <!-- 안감 버튼 -->
                  <button @click="toggleExclude(character, 'nabel')" 
                    :class="{ active: character.isExcludedNabel }"
                    class="difficulty-btn exclude-btn"
                    title="안감">
                    안감
                  </button>
                </div>
              </div>
            </td>

            <td class="dungeon-status-cell venus-cell">
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearVenus,
                     excluded: character.isExcludedVenus
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedVenus ? '-' : (character.dungeonClearVenus ? '✅' : '❌') }}
                </span>
                                  <span class="clear-text">
                    {{ character.isExcludedVenus ? '안감' : (character.dungeonClearVenus ? '클리어' : '미클리어') }}
                  </span>
              </div>
              <div class="action-buttons-mini">
                <button @click="toggleExclude(character, 'venus')" 
                        class="exclude-btn" 
                        :class="{ active: character.isExcludedVenus }"
                        title="안감">안감</button>
                
              </div>
            </td>
            <td class="dungeon-status-cell fog-cell">
              <div class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearFog,
                     excluded: character.isExcludedFog
                   }">
                <span class="clear-icon">
                  {{ character.isExcludedFog ? '-' : (character.dungeonClearFog ? '✅' : '❌') }}
                </span>
                                  <span class="clear-text">
                    {{ character.isExcludedFog ? '안감' : (character.dungeonClearFog ? '클리어' : '미클리어') }}
                  </span>
              </div>
              <div class="action-buttons-mini">
                <button @click="toggleExclude(character, 'fog')" 
                        class="exclude-btn" 
                        :class="{ active: character.isExcludedFog }"
                        title="안감">안감</button>
                
              </div>
            </td>
            <td class="dungeon-status-cell twilight-cell">
              <div v-if="character.isTwilightEligible" class="dungeon-clear-status" 
                   :class="{ 
                     cleared: character.dungeonClearTwilight,
                     excluded: false
                   }">
                <span class="clear-icon">
                  {{ character.dungeonClearTwilight ? '✅' : '❌' }}
                </span>
                <span class="clear-text">
                  {{ character.dungeonClearTwilight ? '클리어' : '미클리어' }}
                </span>
              </div>
              <div v-else class="dungeon-clear-status excluded">
                <span class="clear-icon">-</span>
                <span class="clear-text">스펙 부족</span>
              </div>
              <div class="action-buttons-mini">
                <span class="coming-soon-text">안감</span>
              </div>
            </td>
            <td>
              <div class="action-grid">
                <div class="action-cell">
                  <div class="action-label">던담초기화</div>
                  <div class="dundam-actions">
                    <!-- Playwright 버전 (활성화됨) -->
                    <button @click="syncCharacterFromDundamPlaywright(character)" 
                            class="action-btn dundam-sync-btn playwright-enabled" 
                            :class="{ 'syncing': syncingCharacters.has(character.characterId) }"
                            :disabled="syncingCharacters.has(character.characterId) || !isWithinTimeLimit(character.characterId) || isAnyCharacterSyncing()" 
                            title="Playwright로 던담 크롤링하여 총딜/버프력 최신화">
                      <span v-if="syncingCharacters.has(character.characterId)" class="syncing-text">
                        <span class="spinner">🔄</span>
                      </span>
                      <span v-else class="button-content">
                        <span class="button-icon">🚀</span>
                      </span>
                    </button>
                  </div>
                  <!-- 고정 높이 메시지 영역 -->
                  <div class="status-message-container">
                    <div v-if="syncingCharacters.has(character.characterId)" class="syncing-status-message">
                      동기화중
                    </div>

                    <div v-else-if="!isWithinTimeLimit(character.characterId)" class="time-limit-message">
                      {{ getRemainingTime(character.characterId) }}
                    </div>
                    <div v-else-if="characterErrors.get(character.characterId)" class="character-error-message">
                      {{ characterErrors.get(character.characterId) }}
                    </div>
                    <div v-else class="status-message-placeholder">
                      <!-- 빈 공간으로 높이 유지 -->
                    </div>
                  </div>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">케릭정보 최신화</div>
                  <button @click="refreshCharacterInfo(character)" class="action-btn refresh-btn" :disabled="refreshingCharacters.includes(character.characterId)" title="DFO API로 명성 최신화">
                    {{ refreshingCharacters.includes(character.characterId) ? '🔄' : '🔄' }}
                  </button>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">던담 링크</div>
                  <a :href="getDundamLink(character)" target="_blank" class="dundam-link" :title="getDundamLinkTitle(character)">
                    🔗 링크
                  </a>
                </div>
                
                <div class="action-cell">
                  <div class="action-label">스탯 수정</div>
                  <button @click="showManualInput(character, isBuffer(character) ? 'buffPower' : 'totalDamage')" class="action-btn edit-btn" title="수동으로 스탯 수정">
                    ✏️
                  </button>
                </div>
              </div>
            </td>
            <td class="update-time">
              {{ formatDateTime(character.lastDungeonCheck || new Date().toISOString()) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 검색 결과가 없을 때 -->
    <div v-else-if="selectedAdventure && characters.length === 0" class="no-results">
      <p>'{{ selectedAdventure }}' 모험단의 캐릭터가 데이터베이스에 없습니다.</p>
      <p><strong>해결 방법:</strong> 먼저 <router-link to="/character-search">캐릭터 검색</router-link>에서 해당 모험단의 캐릭터들을 검색하여 추가해주세요.</p>
    </div>

    <!-- 초기 상태 -->
    <div v-else-if="!selectedAdventure" class="initial-state">
      <p>모험단을 선택해주세요.</p>
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
import { ref, onMounted, computed, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useCharacterStore } from '../stores/character'
import { usePartyStore } from '../stores/party'
import sseService from '../services/sseService'
import { apiFetch } from '../config/api'
import { isBuffer } from '../utils/characterUtils'
import type { Character } from '../types'

// RealtimeEvent 타입 정의
interface RealtimeEvent {
  id: string
  type: 'CHARACTER_UPDATED' | 'CHARACTER_DELETED' | 'PARTY_CREATED' | 'PARTY_UPDATED' | 'PARTY_DELETED' | 'PARTY_OPTIMIZED' | 'RECOMMENDATION_GENERATED' | 'USER_JOINED' | 'USER_LEFT' | 'SYSTEM_NOTIFICATION'
  targetId?: string
  userId: string
  data?: Record<string, any>
  timestamp: string
  message: string
  broadcast: boolean
}

// 라우터 정보 가져오기
const route = useRoute();

// 반응형 데이터
const searchQuery = ref(''); // 모험단 검색어
const selectedAdventure = ref('');
const characters = ref<Character[]>([]);
const searching = ref(false); // 검색 중 상태
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

// 실시간 타이머를 위한 상태
const timerInterval = ref<number | null>(null);

// 최근에 검색한 모험단 목록 (로컬스토리지에 저장)
const recentSearchedAdventures = ref<string[]>([]);

// 최근 검색한 모험단 선택용
const selectedRecentAdventure = ref('');

// 2분 제한 확인 함수
const isWithinTimeLimit = (characterId: string): boolean => {
  const lastSync = lastSyncTimes.value.get(characterId);
  if (!lastSync) return true;
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const diffMinutes = diffMs / (1000 * 60);
  
  // 디버깅용 로그
  // console.log(`케릭터 ${characterId} 동기화 제한 확인:`, {
  //   lastSync: lastSync.toISOString(),
  //   now: now.toISOString(),
  //   diffMinutes: diffMinutes,
  //   isAvailable: diffMinutes >= 2
  // });
  
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
  // console.log(`모험단 ${adventureName} 동기화 제한 확인:`, {
  //   lastSync: lastSync.toISOString(),
  //   now: now.toISOString(),
  //   diffMinutes: diffMinutes,
  //   isAvailable: diffMinutes >= 2
  // });
  
  return diffMinutes >= 2;
};

// 남은 시간 계산 함수 (초 단위만 표시)
const getRemainingTime = (characterId: string): string => {
  const lastSync = lastSyncTimes.value.get(characterId);
  if (!lastSync) return '';
  
  const now = new Date();
  const diffMs = now.getTime() - lastSync.getTime();
  const totalRemainingSeconds = Math.max(0, 120 - Math.floor(diffMs / 1000));
  
  if (totalRemainingSeconds > 0) {
    const minutes = Math.floor(totalRemainingSeconds / 60);
    const seconds = totalRemainingSeconds % 60;
    
    if (minutes > 0) {
      return `${minutes}분 ${seconds}초`;
    } else {
      return `${seconds}초`;
    }
  }
  return '';
};

// 실시간 타이머 시작
const startTimer = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value);
  }
  
  timerInterval.value = setInterval(() => {
    // 강제로 리렌더링 (Vue의 반응성 시스템 활용)
    characters.value = [...characters.value];
  }, 1000); // 1초마다 업데이트
};

// 타이머 정리
const clearTimer = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value);
    timerInterval.value = null;
  }
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

// 아무 캐릭터라도 던담 동기화 중인지 확인 (전체 최신화 포함)
const isAnyCharacterSyncing = (): boolean => {
  return syncingCharacters.value.size > 0 || refreshingAll.value;
};
const refreshingCharacters = ref<string[]>([]);
const refreshingTimeline = ref<string[]>([]);

// 하드 파티 관련
const hardPartyCharacters = ref<Set<string>>(new Set()); // 하드 파티로 가는 캐릭터들

// 정렬 관련
const sortField = ref<string>('fame');
const sortOrder = ref<'asc' | 'desc'>('desc');







// 모험단 검색 함수
const searchAdventure = async () => {
  console.log('=== 모험단 검색 시작 ===');
  console.log('검색어:', searchQuery.value);
  
  if (!searchQuery.value.trim()) {
    error.value = '검색어를 입력해주세요.';
    console.log('검색어가 비어있음');
    return;
  }

  try {
    searching.value = true;
    error.value = '';
    successMessage.value = '';
    
    console.log('API 호출 시작:', `/api/characters/adventure/${encodeURIComponent(searchQuery.value)}`);
          const response = await apiFetch(`/characters/adventure/${encodeURIComponent(searchQuery.value)}`);
    
    console.log('API 응답 상태:', response.status);
    
    if (response.ok) {
      const data = await response.json();
      console.log('API 응답 데이터:', data);
      
      if (data.success) {
        characters.value = data.characters || [];
        console.log('캐릭터 로드 완료:', characters.value.length, '개');
        
        successMessage.value = `'${searchQuery.value}' 모험단의 ${characters.value.length}개 캐릭터를 찾았습니다.`;
        
        // 모험단 선택 상태 업데이트
        selectedAdventure.value = searchQuery.value;
        console.log('선택된 모험단 업데이트:', selectedAdventure.value);
        
        // 로컬스토리지에 검색한 모험단 저장
        saveRecentSearchedAdventure(searchQuery.value);
        
        // 캐릭터 로드 후 동기화 시간 초기화
        initializeSyncTimes();
        
        console.log('=== 모험단 검색 완료 ===');
      } else {
        error.value = data.message || '검색에 실패했습니다.';
        console.log('API 응답 실패:', data.message);
      }
    } else {
      error.value = '검색 중 오류가 발생했습니다.';
      console.log('HTTP 오류:', response.status, response.statusText);
    }

  } catch (err) {
    console.error('검색 실패:', err);
    error.value = '검색 중 오류가 발생했습니다.';
  } finally {
    searching.value = false;
  }
};

// 로컬스토리지에서 최근 검색한 모험단 목록 로드
const loadRecentSearchedAdventures = () => {
  try {
    const saved = localStorage.getItem('df_dungeon_adventure_history');
    if (saved) {
      recentSearchedAdventures.value = JSON.parse(saved);
      console.log('던전 모험단 기록 로드 완료:', recentSearchedAdventures.value);
    }
  } catch (error) {
    console.error('던전 모험단 기록 로드 실패:', error);
    recentSearchedAdventures.value = [];
  }
};

// 로컬스토리지에 최근 검색한 모험단 저장
const saveRecentSearchedAdventure = (adventureName: string) => {
  try {
    // 기존 던전 모험단 기록 가져오기
    const existingDungeonHistory = JSON.parse(localStorage.getItem('df_dungeon_adventure_history') || '[]');
    
    // 이미 존재하는지 확인
    if (!existingDungeonHistory.includes(adventureName)) {
      // 최대 10개까지만 저장
      const updatedDungeonHistory = [...existingDungeonHistory, adventureName];
      if (updatedDungeonHistory.length > 10) {
        updatedDungeonHistory.splice(0, updatedDungeonHistory.length - 10);
      }
      
      // 로컬스토리지에 저장
      localStorage.setItem('df_dungeon_adventure_history', JSON.stringify(updatedDungeonHistory));
      
      // 로컬 상태도 업데이트
      recentSearchedAdventures.value = updatedDungeonHistory;
      
      console.log('던전 모험단 기록 저장 완료:', updatedDungeonHistory);
    }
  } catch (error) {
    console.error('던전 모험단 기록 저장 실패:', error);
  }
};

// 최근 검색한 모험단 선택 처리
const selectRecentAdventure = async () => {
  if (selectedRecentAdventure.value) {
    // 선택된 모험단으로 검색 실행
    searchQuery.value = selectedRecentAdventure.value;
    await searchAdventure();
    
    // 선택 초기화
    selectedRecentAdventure.value = '';
  }
};

// 선택된 모험단 초기화
const clearSelectedAdventure = () => {
  selectedAdventure.value = '';
  characters.value = [];
  searchQuery.value = '';
  error.value = '';
  successMessage.value = '';
  console.log('선택된 모험단 초기화됨');
};

// 나벨 난이도 설정
const setNabelDifficulty = async (character: Character, difficulty: 'hard' | 'normal' | 'matching') => {
  try {
    // 백엔드에 저장
    const response = await apiFetch(`/characters/${character.characterId}/nabel-difficulty?difficulty=${difficulty}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    if (response.ok) {
      // 로컬스토리지에도 저장 (백업용)
      const key = `nabelDifficulty_${character.characterId}`;
      localStorage.setItem(key, difficulty);
      console.log(`캐릭터 ${character.characterName} 나벨 난이도 저장 완료: ${difficulty}`);
      
      // 성공 메시지 표시
      successMessage.value = `캐릭터 ${character.characterName}의 나벨 난이도가 ${difficulty}로 설정되었습니다.`;
    } else {
      console.error('나벨 난이도 저장 실패:', response.statusText);
      error.value = '나벨 난이도 저장에 실패했습니다.';
    }
  } catch (err) {
    console.error('나벨 난이도 저장 중 오류:', err);
    error.value = '나벨 난이도 저장 중 오류가 발생했습니다.';
  }
};

// 하드 나벨 대상자 여부 계산 (백엔드 값이 잘못되었을 때 대체)
const isHardEligible = (character: Character): boolean => {
  // 백엔드에서 받은 값이 있으면 사용
  if (character.isHardNabelEligible !== null) {
    return character.isHardNabelEligible === true;
  }
  
  // 백엔드 값이 null이면 조건으로 계산
  if (character.fame === undefined || character.fame === null || character.fame < 47684) {
    return false;
  }
  
  // 버퍼 여부 판단
  const isBuffer = character.jobGrowName?.includes("크루세이더") || 
                   character.jobGrowName?.includes("뮤즈") || 
                   character.jobGrowName?.includes("패러메딕") || 
                   character.jobGrowName?.includes("인챈트리스");
  
  if (isBuffer) {
    // 버퍼: 버프력 500만 이상
    const buffPower = character.manualBuffPower || character.buffPower;

    return buffPower !== undefined && buffPower !== null && buffPower >= 5000000;
  } else {
    // 딜러: 총딜 100억 이상
    const totalDamage = character.manualTotalDamage || character.totalDamage;
    return totalDamage !== undefined && totalDamage !== null && totalDamage >= 10000000000;
  }
};

// 일반 나벨 대상자 여부 계산 (백엔드 값이 잘못되었을 때 대체)
const isNormalEligible = (character: Character): boolean => {
  // 백엔드에서 받은 값이 있으면 사용
  if (character.isNormalNabelEligible !== null) {
    return character.isNormalNabelEligible === true;
  }
  
  // 백엔드 값이 null이면 조건으로 계산
  if (character.fame === undefined || character.fame === null || character.fame < 47684) {
    return false;
  }
  
  // 버퍼 여부 판단
  const isBuffer = character.jobGrowName?.includes("크루세이더") || 
                   character.jobGrowName?.includes("뮤즈") || 
                   character.jobGrowName?.includes("패러메딕") || 
                   character.jobGrowName?.includes("인챈트리스");
  
  if (isBuffer) {
    // 버퍼: 버프력 400만 이상
    const buffPower = character.manualBuffPower || character.buffPower;
    return buffPower !== undefined && buffPower !== null && buffPower >= 4000000;
  } else {
    // 딜러: 총딜 30억 이상
    const totalDamage = character.manualTotalDamage || character.totalDamage;
    return totalDamage !== undefined && totalDamage !== null && totalDamage >= 3000000000;
  }
};

// 매칭 나벨 대상자 여부 계산 (백엔드 값이 null일 때 대체)
const isMatchingEligible = (character: Character): boolean => {
  // 백엔드에서 받은 값이 있으면 사용
  if (character.isMatchingNabelEligible !== null) {
    return character.isMatchingNabelEligible === true;
  }
  
  // 백엔드 값이 null이면 명성 기준으로 계산
  return character.fame !== undefined && character.fame !== null && character.fame >= 47684;
};

// 나벨 난이도 가져오기
const getNabelDifficulty = (character: Character): 'hard' | 'normal' | 'matching' | null => {
  // 백엔드에서 받은 데이터가 있으면 우선 사용
  if (character.selectedNabelDifficulty) {
    return character.selectedNabelDifficulty as 'hard' | 'normal' | 'matching';
  }
  
  // 백엔드 데이터가 없으면 로컬스토리지에서 가져오기 (백업용)
  const key = `nabelDifficulty_${character.characterId}`;
  const saved = localStorage.getItem(key);
  if (saved && ['hard', 'normal', 'matching'].includes(saved)) {
    return saved as 'hard' | 'normal' | 'matching';
  }
  
  // 자동 선택: 조건에 맞는 가장 높은 난이도 선택
  if (character.isHardNabelEligible) {
    return 'hard';
  } else if (character.isNormalNabelEligible) {
    return 'normal';
  } else if (isMatchingEligible(character)) {
    return 'matching';
  }
  
  return null;
};

// 주기적 체크를 위한 타이머
let syncCheckTimer: number | null = null;

// 필터 조건 복원 함수
const restoreFilterCondition = () => {
  const savedAdventure = localStorage.getItem('df_dungeon_filter_adventure');
  if (savedAdventure) {
    selectedAdventure.value = savedAdventure;
  }
};

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  loadRecentSearchedAdventures(); // 최근 검색 모험단 로드
  restoreDundamSyncState(); // 던담 동기화 상태 복원
  await initializeSSE();
  
  // 저장된 필터 조건 복원
  restoreFilterCondition();
  
  // URL 파라미터에서 adventure 값 확인
  const adventureParam = route.query.adventure as string;
  if (adventureParam) {
    console.log('URL 파라미터에서 모험단 발견:', adventureParam);
    selectedAdventure.value = adventureParam;
    searchQuery.value = adventureParam;
    // 해당 모험단으로 자동 검색
    await searchAdventure();
  }
  
  // 10초마다 제한시간 초과된 동기화 상태 체크
  syncCheckTimer = window.setInterval(() => {
    checkAndClearExpiredSyncs();
  }, 10000); // 10초마다 체크
  
  // 실시간 타이머 시작
  startTimer();
});

// 캐릭터 목록이 로드된 후 하드 파티 상태 복원
watch(characters, (newCharacters) => {
  if (newCharacters.length > 0) {
    restoreHardPartyState();
  }
}, { immediate: true });

// 던담 동기화 상태 변화 감지 및 Local Storage 저장
watch(syncingCharacters, (newValue) => {
  saveDundamSyncState();
  console.log('던담 동기화 상태 변경됨:', Array.from(newValue));
}, { deep: true });

onUnmounted(() => {
      // 이벤트 리스너 제거
    sseService.removeEventListener('CHARACTER_UPDATED', handleCharacterUpdate)
    sseService.removeEventListener('SYSTEM_NOTIFICATION', handleSystemNotification)
  sseService.disconnect();
  
  // 타이머 정리
  if (syncCheckTimer) {
    window.clearInterval(syncCheckTimer);
    syncCheckTimer = null;
  }
  
  // 실시간 타이머 정리
  clearTimer();
})



// 서버 변경 시 - 더 이상 필요하지 않음
// const onServerChange = () => {
//   if (characterName.value.trim()) {
//     searchCharacters();
//   }
// };

// 모험단 목록 (백엔드에서 로드)
const allAdventures = ref<string[]>([]);





// 모험단별 필터링 - CharacterSearch.vue와 동일한 localStorage 키 사용
const availableAdventures = computed(() => {
  const adventures = new Set<string>();
  
  // DB에서 로드된 모험단들 추가
  allAdventures.value.forEach(name => adventures.add(name));
  
  // CharacterSearch.vue와 동일한 localStorage 키 사용
  try {
    const dungeonAdventureHistory = JSON.parse(localStorage.getItem('df_dungeon_adventure_history') || '[]');
    dungeonAdventureHistory.forEach((adventureName: string) => {
      if (adventureName && adventureName !== 'N/A') {
        adventures.add(adventureName);
      }
    });
  } catch (error) {
    console.error('localStorage 던전 모험단 기록 로드 실패:', error);
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
      const response = await apiFetch(`/characters/adventure/${encodeURIComponent(selectedAdventure.value)}`);
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          characters.value = data.characters;
          searchQuery.value = ''; // 검색어 초기화
          successMessage.value = `'${selectedAdventure.value}' 모험단의 ${characters.value.length}개 캐릭터를 로드했습니다.`;
          
          // 로컬스토리지에 선택한 모험단 저장
          saveRecentSearchedAdventure(selectedAdventure.value);
          
          // 필터 조건을 로컬스토리지에 저장
          localStorage.setItem('df_dungeon_filter_adventure', selectedAdventure.value);
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

// 던전 통계 (안감 제외, 최대치 제한 적용)
const dungeonStats = computed(() => {
  const stats = {
    nabel: 0,
    venus: 0,
    fog: 0,
    twilight: 0,
    nabelTotal: 0,
    venusTotal: 0,
    fogTotal: 0,
    twilightTotal: 0,
    nabelNormal: 0,
    nabelHard: 0,
    venusNormal: 0,
    venusHard: 0,
    fogNormal: 0,
    fogHard: 0
  };
  
  // 안감되지 않은 캐릭터만 필터링
  const eligibleCharacters = filteredCharacters.value.filter(char => 
    !char.isExcludedNabel || !char.isExcludedVenus || !char.isExcludedFog
  );
  
  // 각 던전별로 최대치 제한 적용
  eligibleCharacters.forEach(char => {
    if (!char.isExcludedNabel) {
      if (stats.nabelTotal < getDungeonLimit('nabel')) {
        stats.nabelTotal++;
        if (char.dungeonClearNabel) stats.nabel++;
        // 하드 클리어 횟수 계산
        if (char.dungeonClearNabel) {
          stats.nabelHard++;
        }
      }
    }
    if (!char.isExcludedVenus) {
      if (stats.venusTotal < getDungeonLimit('venus')) {
        stats.venusTotal++;
        if (char.dungeonClearVenus) stats.venus++;
        // 베누스는 일반 모드만 있음 (임시로 클리어 여부로 계산)
        if (char.dungeonClearVenus) {
          stats.venusNormal++;
        }
      }
    }
    if (!char.isExcludedFog) {
      if (stats.fogTotal < getDungeonLimit('fog')) {
        stats.fogTotal++;
        if (char.dungeonClearFog) stats.fog++;
        // 안개신은 일반 모드만 있음 (임시로 클리어 여부로 계산)
        if (char.dungeonClearFog) {
          stats.fogNormal++;
        }
      }
    }
            // 이내 황혼전 적격성 체크 (명성 72,688, 버퍼 버프력 5,200,000, 딜러 총딜 12,000,000,000)
    if (char.isTwilightEligible) {
      if (stats.twilightTotal < getDungeonLimit('twilight')) {
        stats.twilightTotal++;
        if (char.dungeonClearTwilight) stats.twilight++;
      }
    }
  });
  
  // 일반 클리어는 전체 적격 캐릭터 - 하드 클리어한 캐릭터
  stats.nabelNormal = stats.nabelTotal - stats.nabelHard;
  
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
  sortField.value = 'fame';
  sortOrder.value = 'desc';
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
    
            // 이내 황혼전 정렬
    if (sortField.value === 'twilight') {
      if (a.dungeonClearTwilight !== b.dungeonClearTwilight) {
        return sortOrder.value === 'asc' ? 
          (a.dungeonClearTwilight ? -1 : 1) : 
          (a.dungeonClearTwilight ? 1 : -1);
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
  const uniqueAdventures = [...new Set(characters.value.map(c => c.adventureName).filter(name => name))];
  uniqueAdventures.forEach(adventureName => {
    if (adventureName) {
      lastAdventureSyncTimes.value.set(adventureName, threeMinutesAgo);
    }
  });
  
  console.log('동기화 시간 초기화 완료:', {
    characters: lastSyncTimes.value.size,
    adventures: lastAdventureSyncTimes.value.size
  });
};

// SSE 초기화
const initializeSSE = async () => {
  try {
    console.log('🚀 === SSE 초기화 시작 ===')
    
    // 이미 연결되어 있으면 연결하지 않음
    if (sseService.getConnectionStatus.value) {
      console.log('🔗 SSE 이미 연결됨, 이벤트 리스너만 등록')
      console.log('📊 현재 연결 상태:', sseService.getConnectionInfo())
    } else {
      console.log('🔄 SSE 새 연결 시도...')
      await sseService.connect()
      isConnected.value = sseService.getConnectionStatus.value
      console.log('✅ SSE 연결 완료')
      console.log('📊 연결 후 상태:', sseService.getConnectionInfo())
    }
    
    // 실시간 이벤트 리스너 등록 (중복 등록 방지)
    console.log('👂 이벤트 리스너 등록 시작...')
    
    sseService.removeEventListener('CHARACTER_UPDATED', handleCharacterUpdate)
    sseService.removeEventListener('SYSTEM_NOTIFICATION', handleSystemNotification)
    
    sseService.addEventListener('CHARACTER_UPDATED', handleCharacterUpdate)
    sseService.addEventListener('SYSTEM_NOTIFICATION', handleSystemNotification)
    
    console.log('✅ 이벤트 리스너 등록 완료')
    console.log('📊 최종 연결 상태:', sseService.getConnectionInfo())
    console.log('=== SSE 초기화 완료 ===')
    
  } catch (err) {
    console.error('❌ SSE 연결 실패:', err)
    console.log('📊 연결 실패 시 상태:', sseService.getConnectionInfo())
    error.value = '실시간 업데이트 연결에 실패했습니다.'
  }
}

// 캐릭터 업데이트 이벤트 처리 (SSE)
const handleCharacterUpdate = (event: RealtimeEvent) => {
  try {
    if (event.type === 'CHARACTER_UPDATED' && event.data) {
      const { characterId, serverId, updateResult, characterInfo } = event.data;
      
      if (updateResult && updateResult.success) {
        // 해당 캐릭터 찾아서 업데이트
        const characterIndex = characters.value.findIndex(c => c.characterId === characterId);
        if (characterIndex !== -1) {
          const character = characters.value[characterIndex];
          
          // 스탯 업데이트
          if (characterInfo) {
            if (characterInfo.totalDamage !== undefined && characterInfo.totalDamage !== null) {
              character.totalDamage = characterInfo.totalDamage;
            }
            if (characterInfo.buffPower !== undefined && characterInfo.buffPower !== null) {
              character.buffPower = characterInfo.buffPower;
            }
            if (characterInfo.combatPower !== undefined && characterInfo.combatPower !== null) {
              character.combatPower = characterInfo.combatPower;
            }
          }
          
          // 성공 메시지 표시
          successMessage.value = `${character.characterName}의 던담 동기화가 완료되었습니다.`;
          
          console.log('캐릭터 실시간 업데이트 완료:', character.characterName);
        }
      }
    }
  } catch (error) {
    console.error('캐릭터 업데이트 처리 오류:', error);
  }
};

// 시스템 알림 이벤트 처리
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
  
  // 알림을 배열에 추가 (최근 5개만 유지)
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
    
    const response = await apiFetch(`/realtime/adventure/${encodeURIComponent(selectedAdventure.value)}/refresh`, {
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
    const response = await apiFetch(`/realtime/character/${characterId}/refresh`, {
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
    
            const response = await apiFetch(`/characters/${character.characterId}/exclude-dungeon`, {
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



// 모험단 전체 캐릭터 최신화 (던담 동기화 포함)
const refreshAllCharacters = async () => {
  if (!selectedAdventure.value) {
    error.value = '모험단을 선택해주세요.';
    return;
  }
  
  try {
    refreshingAll.value = true;
    error.value = '';
    successMessage.value = '';
    
    // 던담 동기화가 진행되는 동안 다른 던담 초기화 버튼들 비활성화
    const response = await apiFetch(`/characters/adventure/${encodeURIComponent(selectedAdventure.value)}/refresh`, {
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
        if (selectedAdventure.value) {
          await filterByAdventure();
        }
        
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
    const response = await apiFetch(`/characters/${manualInputCharacter.value.characterId}/manual-stats`, {
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



// 던담 동기화 함수 (Playwright)
const syncCharacterFromDundamPlaywright = async (character: Character) => {
  // 이미 크롤링 중인 캐릭터가 있으면 중복 크롤링 방지
  if (syncingCharacters.value.size > 0) {
    const syncingCharacterId = Array.from(syncingCharacters.value)[0];
    const syncingCharacter = characters.value.find(c => c.characterId === syncingCharacterId);
    error.value = `'${syncingCharacter?.characterName}' 동기화가 진행 중입니다. 완료 후 다시 시도해주세요.`;
    return;
  }
  
  // 동기화 시작
  syncingCharacters.value.add(character.characterId);
  
  try {
    // 동기화 시작 시간 저장
    saveDundamSyncStartTime(character.characterId);
    
    const response = await apiFetch(`/dundam-sync/character/${character.serverId}/${character.characterId}?method=playwright`, {
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
        if (character.adventureName) {
          lastAdventureSyncTimes.value.set(character.adventureName, new Date());
        }
        
        // 화면 강제 업데이트를 위해 캐릭터 목록 새로고침
        if (selectedAdventure.value) {
          await filterByAdventure();
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
    // 동기화 시작 시간 제거
    localStorage.removeItem(`dundamSyncStart_${character.characterId}`);
  }
};

// 던담 동기화 진행 중인지 확인하는 함수
const isAnyDundamSyncInProgress = () => {
  return syncingCharacters.value.size > 0;
};

// Local Storage에서 동기화 상태 복원 (제한시간 체크 포함)
const restoreDundamSyncState = () => {
  try {
    const savedState = localStorage.getItem('dundam-sync-state');
    if (savedState) {
      const parsedState = JSON.parse(savedState);
      const currentTime = Date.now();
      
      // 제한시간(2분) 체크하여 유효한 동기화만 복원
      const validCharacters = [];
      for (const characterId of parsedState.syncingCharacters || []) {
        const startTime = localStorage.getItem(`dundamSyncStart_${characterId}`);
        if (startTime) {
          const elapsedTime = currentTime - parseInt(startTime);
          const timeLimit = 2 * 60 * 1000; // 2분을 밀리초로
          
          if (elapsedTime < timeLimit) {
            validCharacters.push(characterId);
          } else {
            // 제한시간 초과된 동기화 상태 제거
            localStorage.removeItem(`dundamSyncStart_${characterId}`);
            console.log(`캐릭터 ${characterId} 동기화 상태 제한시간 초과로 자동 해제`);
          }
        }
      }
      
      syncingCharacters.value = new Set(validCharacters);
      console.log('던담 동기화 상태 복원됨 (제한시간 체크 후):', Array.from(syncingCharacters.value));
    }
  } catch (error) {
    console.error('던담 동기화 상태 복원 실패:', error);
    localStorage.removeItem('dundam-sync-state');
  }
};

// Local Storage에 동기화 상태 저장
const saveDundamSyncState = () => {
  try {
    const state = {
      syncingCharacters: Array.from(syncingCharacters.value),
      timestamp: Date.now()
    };
    localStorage.setItem('dundam-sync-state', JSON.stringify(state));
  } catch (error) {
    console.error('던담 동기화 상태 저장 실패:', error);
  }
};

// 동기화 시작 시간 저장
const saveDundamSyncStartTime = (characterId: string) => {
  try {
    localStorage.setItem(`dundamSyncStart_${characterId}`, Date.now().toString());
  } catch (error) {
    console.error('동기화 시작 시간 저장 실패:', error);
  }
};

// 제한시간 초과된 동기화 상태 자동 해제
const checkAndClearExpiredSyncs = () => {
  const currentTime = Date.now();
  const timeLimit = 2 * 60 * 1000; // 2분을 밀리초로
  const expiredCharacters = [];
  
  for (const characterId of syncingCharacters.value) {
    const startTime = localStorage.getItem(`dundamSyncStart_${characterId}`);
    if (startTime) {
      const elapsedTime = currentTime - parseInt(startTime);
      if (elapsedTime >= timeLimit) {
        expiredCharacters.push(characterId);
        localStorage.removeItem(`dundamSyncStart_${characterId}`);
        console.log(`캐릭터 ${characterId} 동기화 상태 제한시간 초과로 자동 해제`);
      }
    }
  }
  
  // 만료된 동기화 상태 제거
  expiredCharacters.forEach(characterId => {
    syncingCharacters.value.delete(characterId);
  });
  
  if (expiredCharacters.length > 0) {
    console.log('제한시간 초과로 자동 해제된 동기화:', expiredCharacters);
  }
};

// 하드 파티 토글 함수
const toggleHardParty = (characterId: string) => {
  if (hardPartyCharacters.value.has(characterId)) {
    hardPartyCharacters.value.delete(characterId);
  } else {
    hardPartyCharacters.value.add(characterId);
  }
  // 로컬스토리지에 저장
  localStorage.setItem('hardPartyCharacters', JSON.stringify(Array.from(hardPartyCharacters.value)));
};

// 하드 파티 상태 복원
const restoreHardPartyState = () => {
  try {
    const savedState = localStorage.getItem('hardPartyCharacters');
    if (savedState) {
      const parsedState = JSON.parse(savedState);
      hardPartyCharacters.value = new Set(parsedState);
    } else {
      // 기본값: 모든 캐릭터를 하드 파티로 설정
      const allCharacterIds = characters.value.map(c => c.characterId);
      hardPartyCharacters.value = new Set(allCharacterIds);
      localStorage.setItem('hardPartyCharacters', JSON.stringify(allCharacterIds));
    }
  } catch (error) {
    console.error('하드 파티 상태 복원 실패:', error);
    // 에러 시 기본값으로 설정
    const allCharacterIds = characters.value.map(c => c.characterId);
    hardPartyCharacters.value = new Set(allCharacterIds);
  }
};

// 캐릭터 정보 최신화 함수
const refreshCharacterInfo = async (character: Character) => {
  if (refreshingCharacters.value.includes(character.characterId)) return;
  
  try {
    refreshingCharacters.value.push(character.characterId);
    
    const response = await apiFetch(`/characters/${character.serverId}/${character.characterId}/refresh`, {
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
    
    const response = await apiFetch(`/dungeon-clear/${character.serverId}/${character.characterId}`, {
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
          char.dungeonClearTwilight = result.clearStatus?.twilight || false;
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

// SSE 연결 해제
const disconnectSSE = () => {
  console.log('🔌 SSE 연결 해제 시작...')
  
  // 이벤트 리스너 제거
  sseService.removeEventListener('CHARACTER_UPDATED', handleCharacterUpdate)
  sseService.removeEventListener('SYSTEM_NOTIFICATION', handleSystemNotification)
  
  // SSE 연결 해제
  sseService.disconnect()
  isConnected.value = false
  
  console.log('✅ SSE 연결 해제 완료')
}

// SSE 연결 상태 표시
const getSSEStatusText = computed(() => {
  const status = sseService.getConnectionStatus.value
  switch (status) {
    case 'connected': return '연결됨'
    case 'connecting': return '연결 중...'
    case 'reconnecting': return '재연결 중...'
    case 'error': return '연결 오류'
    case 'disconnected': return '연결 끊김'
    default: return '알 수 없음'
  }
})

const getSSEStatusClass = computed(() => {
  const status = sseService.getConnectionStatus.value
  switch (status) {
    case 'connected': return 'text-green-600'
    case 'connecting': return 'text-yellow-600'
    case 'reconnecting': return 'text-orange-600'
    case 'error': return 'text-red-600'
    case 'disconnected': return 'text-gray-600'
    default: return 'text-gray-600'
  }
})

// 던전별 최대 캐릭터 수 제한
const getDungeonLimit = (dungeon: 'nabel' | 'venus' | 'fog' | 'twilight'): number => {
  switch (dungeon) {
    case 'nabel':
      return 4; // 나벨 하드는 4케릭 제한
    case 'venus':
      return 20; // 베누스는 20케릭 제한
    case 'fog':
      return 20; // 안개신은 20케릭 제한
    case 'twilight':
              return 8; // 이내 황혼전은 8케릭 제한
    default:
      return 0;
  }
};
</script>

<style scoped>
.dungeon-status {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.adventure-selection {
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border-radius: 12px;
  border: 2px solid #90caf9;
  box-shadow: 0 4px 12px rgba(144, 202, 249, 0.3);
}

.search-section {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.search-form {
  display: flex;
  align-items: center;
  gap: 15px;
}

.form-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-group label {
  font-weight: 600;
  color: #1976d2;
  white-space: nowrap;
  font-size: 14px;
}

.form-group input {
  padding: 8px 12px;
  border: 2px solid #90caf9;
  border-radius: 8px;
  font-size: 14px;
  min-width: 200px;
  background: white;
  transition: all 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}

.search-btn {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

.search-btn:hover {
  background: linear-gradient(135deg, #1565c0, #1976d2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
}

.search-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.recent-adventures {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recent-adventures label {
  font-size: 14px;
  font-weight: 600;
  color: #1976d2;
  white-space: nowrap;
}

.recent-adventure-select {
  padding: 8px 12px;
  border: 2px solid #90caf9;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #495057;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 200px;
}

.recent-adventure-select:hover {
  border-color: #1976d2;
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.1);
}

.recent-adventure-select:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
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

.refresh-all-btn {
  background: linear-gradient(135deg, #28a745, #20c997);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.refresh-subtitle {
  font-size: 11px;
  font-weight: 400;
  opacity: 0.9;
  line-height: 1.2;
}

.refresh-all-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #20c997, #28a745);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(40, 167, 69, 0.4);
}

.refresh-all-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.button-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reset-sort-btn {
  background: linear-gradient(135deg, #6c757d, #5a6268);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

.reset-sort-btn:hover {
  background: linear-gradient(135deg, #5a6268, #6c757d);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(108, 117, 125, 0.4);
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

.stat-limit {
  font-size: 10px;
  color: #6c757d;
  font-weight: 600;
  margin-top: 4px;
  padding: 2px 6px;
  background: #e9ecef;
  border-radius: 3px;
  border: 1px solid #dee2e6;
}

.clear-breakdown {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.clear-type {
  font-size: 9px;
  color: #495057;
  font-weight: 500;
  padding: 1px 4px;
  background: #f8f9fa;
  border-radius: 2px;
  border: 1px solid #dee2e6;
  text-align: center;
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
  padding: 8px;
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
  background: #e3f2fd;
  color: #1976d2;
  border: 1px solid #bbdefb;
}

.characters-table th.sortable.dungeon-clear-column:hover {
  background: #bbdefb;
  color: #1565c0;
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
  padding: 6px;
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

.twilight-cell {
          border: 2px solid #f39c12; /* 주황색 - 이내 황혼전 */
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
  .adventure-selection {
    padding: 15px;
  }
  
  .search-section {
    padding: 10px;
  }
  
  .search-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .form-group input {
    min-width: auto;
  }
  
  .recent-adventures {
    flex-direction: column;
    align-items: stretch;
  }
  
  .adventure-dropdown {
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

.coming-soon-text {
  font-size: 10px;
  color: #999;
  font-style: italic;
  text-align: center;
  padding: 2px 6px;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 3px;
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



/* 스탯 표시 스타일 */
.stat-label {
  font-weight: 600;
  color: #495057;
  font-size: 12px;
  text-align: center;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 13px;
  color: #212529;
  font-weight: 500;
  text-align: center;
}

.stat-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
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
  gap: 2px;
  padding: 2px;
  min-width: 180px;
}

.action-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0px;
  padding: 0px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  background: #f8f9fa;
}

.action-label {
  font-size: 12px;
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
  position: relative;
  overflow: hidden;
}

.dundam-sync-btn.syncing {
  background: #ffc107;
  color: #212529;
  cursor: not-allowed;
}

.dundam-link-btn {
  background: #17a2b8;
  color: white;
  text-decoration: none;
}

.dundam-link-btn:hover {
  background: #138496;
  transform: scale(1.1);
}

/* 버튼 내부 콘텐츠 정렬 */
.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
}

.button-icon {
  font-size: 14px;
  line-height: 1;
}

.button-text {
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.syncing-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
  font-size: 12px;
}

.spinner {
  animation: spin 1s linear infinite;
  font-size: 14px;
  line-height: 1;
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

/* 매칭 상태 배경색 */
.matching-indicator {
  display: inline-block;
  padding: 2px 6px;
  background: #17a2b8;
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



/* 던담 동기화 차단 메시지 스타일 */
.dundam-sync-blocked-message {
  font-size: 11px;
  color: #856404;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
  padding: 4px 8px;
  margin-top: 4px;
  text-align: center;
  animation: pulse 2s infinite;
}

/* 고정 높이 메시지 컨테이너 */
.status-message-container {
  min-height: 32px; /* 메시지가 없을 때도 높이 유지 */
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
}

/* 상태 메시지 공통 스타일 */
.status-message-container > div {
  width: 100%;
  text-align: center;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 빈 공간 플레이스홀더 */
.status-message-placeholder {
  height: 20px; /* 최소 높이 유지 */
  border: none !important;
  background: transparent !important;
}

/* 기존 메시지 스타일 개선 */
.syncing-status-message {
  color: #0c5460;
  background: #d1ecf1;
  border-color: #bee5eb;
}

.adventure-time-limit-message {
  color: #721c24;
  background: #f8d7da;
  border-color: #f5c6cb;
}

.time-limit-message {
  color: #721c24;
  background: #f8d7da;
  border-color: #f5c6cb;
  text-align: center;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.character-error-message {
  color: #721c24;
  background: #f8d7da;
  border-color: #f5c6cb;
}

/* 비활성화된 던담 동기화 버튼 스타일 */
.dundam-sync-btn.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f5f5;
  border-color: #ddd;
  color: #999;
}

.dundam-sync-btn.disabled:hover {
  background: #f5f5f5;
  border-color: #ddd;
  transform: none;
  box-shadow: none;
}

/* Playwright 던담 동기화 버튼 스타일 */
.dundam-sync-btn.playwright-enabled {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
  font-weight: 600;
}

.dundam-sync-btn.playwright-enabled:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  border-color: #764ba2;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.dundam-sync-btn.playwright-enabled:disabled {
  background: #ccc;
  border-color: #999;
  color: #666;
  transform: none;
  box-shadow: none;
}

/* 나벨 클리어 상세 정보 스타일 */
.nabel-clear-details {
  margin: 8px 0;
  padding: 6px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #dee2e6;
}

.clear-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 11px;
}

.clear-detail-row:last-child {
  margin-bottom: 0;
}

.clear-detail-label {
  color: #6c757d;
  font-weight: 600;
  min-width: 30px;
}

.clear-detail-value {
  color: #dc3545;
  font-weight: bold;
  font-size: 12px;
}

.clear-detail-value.cleared {
  color: #28a745;
}

/* 나벨 난이도 버튼 스타일 (2x2 그리드) */
.nabel-difficulty-buttons {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
  align-items: center;
}

.difficulty-row {
  display: flex;
  gap: 2px;
}

.difficulty-btn {
  padding: 2px 4px;
  border: 1px solid #90caf9;
  border-radius: 4px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  color: #000;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 32px;
  text-align: center;
  margin: 0;
  line-height: 1.2;
}

.difficulty-btn:hover {
  background: linear-gradient(135deg, #f3e5f5, #e3f2fd);
  border-color: #1976d2;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.2);
}

.difficulty-btn.active {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  border-color: #1976d2;
  color: white;
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.3);
}

.difficulty-btn.hard-btn {
  border-color: #dc3545;
  color: #000;
}

.difficulty-btn.hard-btn:hover {
  border-color: #dc3545;
  background: linear-gradient(135deg, #dc3545, #c82333);
  color: white;
}

.difficulty-btn.hard-btn.active {
  background: linear-gradient(135deg, #dc3545, #c82333);
  border-color: #dc3545;
}

.difficulty-btn.normal-btn {
  border-color: #28a745;
  color: #000;
}

.difficulty-btn.normal-btn:hover {
  border-color: #28a745;
  background: linear-gradient(135deg, #28a745, #20c997);
  color: white;
}

.difficulty-btn.normal-btn.active {
  background: linear-gradient(135deg, #28a745, #20c997);
  border-color: #28a745;
}

.difficulty-btn.matching-btn {
  border-color: #ffc107;
  color: #000;
}

.difficulty-btn.matching-btn:hover {
  border-color: #ffc107;
  background: linear-gradient(135deg, #ffc107, #e0a800);
  color: white;
}

.difficulty-btn.matching-btn.active {
  background: linear-gradient(135deg, #ffc107, #e0a800);
  border-color: #ffc107;
}

.difficulty-btn.exclude-btn {
  border-color: #6c757d;
  color: #000;
}

.difficulty-btn.exclude-btn:hover {
  border-color: #6c757d;
  background: linear-gradient(135deg, #6c757d, #5a6268);
  color: white;
}

.difficulty-btn.exclude-btn.active {
  background: linear-gradient(135deg, #6c757d, #5a6268);
  border-color: #6c757d;
  color: white;
}

/* disabled 상태 스타일 */
.difficulty-btn:disabled {
  background: #f8f9fa !important;
  border-color: #dee2e6 !important;
  color: #adb5bd !important;
  cursor: not-allowed;
  opacity: 0.6;
  transform: none !important;
  box-shadow: none !important;
}

.difficulty-btn:disabled:hover {
  background: #f8f9fa !important;
  border-color: #dee2e6 !important;
  color: #adb5bd !important;
  transform: none !important;
  box-shadow: none !important;
}

.recent-adventure-select option:hover {
  background: #f8f9fa;
}

/* 선택된 모험단 표시 스타일 */
.selected-adventure-display {
  margin: 20px 0;
  padding: 20px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border: 2px solid #90caf9;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(144, 202, 249, 0.3);
}

.selected-adventure-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.selected-label {
  font-size: 16px;
  font-weight: 600;
  color: #1976d2;
}

.selected-adventure-name {
  font-size: 20px;
  font-weight: 700;
  color: #1565c0;
  background: white;
  padding: 8px 16px;
  border-radius: 8px;
  border: 2px solid #1976d2;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

.character-count {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.change-adventure-btn {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.change-adventure-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
  background: linear-gradient(135deg, #ee5a24, #ff6b6b);
}

/* ========================================
   세밀한 반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 대형 데스크탑 (1920px 이상) */
@media screen and (min-width: 1920px) {
  .dungeon-status-container {
    max-width: 1600px;
    padding: 30px;
  }
  
  .status-header h1 {
    font-size: 3.5rem;
  }
  
  .character-grid {
    grid-template-columns: repeat(5, 1fr);
    gap: 25px;
  }
  
  .difficulty-filters {
    gap: 15px;
  }
  
  .difficulty-btn {
    padding: 15px 25px;
    font-size: 16px;
  }
}

/* 데스크탑 (1600px ~ 1919px) */
@media screen and (min-width: 1600px) and (max-width: 1919px) {
  .dungeon-status-container {
    max-width: 1400px;
    padding: 25px;
  }
  
  .status-header h1 {
    font-size: 3.2rem;
  }
  
  .character-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 22px;
  }
}

/* 노트북 (1200px ~ 1599px) */
@media screen and (min-width: 1200px) and (max-width: 1599px) {
  .dungeon-status-container {
    max-width: 1200px;
    padding: 20px;
  }
  
  .status-header h1 {
    font-size: 3rem;
  }
  
  .character-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }
}

/* 태블릿 가로 (1024px ~ 1199px) */
@media screen and (min-width: 1024px) and (max-width: 1199px) {
  .dungeon-status-container {
    padding: 18px;
  }
  
  .status-header {
    flex-direction: row;
    gap: 20px;
    text-align: left;
  }
  
  .status-header h1 {
    font-size: 2.5rem;
  }
  
  .recent-adventure-select {
    width: auto;
    min-width: 250px;
  }
  
  .difficulty-filters {
    justify-content: flex-start;
    gap: 12px;
  }
  
  .difficulty-btn {
    padding: 12px 20px;
    font-size: 15px;
  }
  
  .character-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }
  
  .selected-adventure-display {
    flex-direction: row;
    gap: 20px;
    text-align: left;
  }
}

/* 태블릿 세로 (768px ~ 1023px) */
@media screen and (min-width: 768px) and (max-width: 1023px) {
  .dungeon-status-container {
    padding: 15px;
  }
  
  .status-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .status-header h1 {
    font-size: 2.2rem;
  }
  
  .recent-adventure-select {
    width: 100%;
    max-width: 300px;
    font-size: 16px;
    padding: 14px;
  }
  
  .difficulty-filters {
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;
  }
  
  .difficulty-btn {
    padding: 12px 18px;
    font-size: 14px;
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
  
  .selected-adventure-display {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
}

/* 중형 모바일 (600px ~ 767px) */
@media screen and (min-width: 600px) and (max-width: 767px) {
  .dungeon-status-container {
    padding: 12px;
  }
  
  .status-header h1 {
    font-size: 2rem;
  }
  
  .recent-adventure-select {
    width: 100%;
    font-size: 16px;
    padding: 13px;
  }
  
  .difficulty-filters {
    flex-direction: column;
    gap: 8px;
  }
  
  .difficulty-btn {
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
  .dungeon-status-container {
    padding: 10px;
  }
  
  .status-header h1 {
    font-size: 1.8rem;
  }
  
  .recent-adventure-select {
    width: 100%;
    font-size: 16px;
    padding: 12px;
  }
  
  .difficulty-filters {
    flex-direction: column;
    gap: 8px;
  }
  
  .difficulty-btn {
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
  
  .selected-adventure-display {
    padding: 12px;
  }
  
  .selected-adventure-name {
    font-size: 16px;
    padding: 6px 10px;
  }
  
  .change-adventure-btn {
    width: 100%;
    padding: 12px;
    font-size: 16px;
  }
}

/* 초소형 모바일 (320px ~ 479px) */
@media screen and (min-width: 320px) and (max-width: 479px) {
  .dungeon-status-container {
    padding: 8px;
  }
  
  .status-header h1 {
    font-size: 1.6rem;
  }
  
  .recent-adventure-select {
    width: 100%;
    font-size: 16px;
    padding: 10px;
  }
  
  .difficulty-filters {
    flex-direction: column;
    gap: 6px;
  }
  
  .difficulty-btn {
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
    min-height: 100px;
  }
  
  .character-info h3 {
    font-size: 0.9rem;
  }
  
  .selected-adventure-display {
    padding: 10px;
  }
  
  .selected-adventure-name {
    font-size: 14px;
    padding: 5px 8px;
  }
  
  .change-adventure-btn {
    width: 100%;
    padding: 10px;
    font-size: 16px;
  }
}

/* 매우 작은 화면 (400px 이하) 추가 최적화 */
@media screen and (max-width: 400px) {
  .dungeon-status-container {
    padding: 5px;
  }
  
  .status-header h1 {
    font-size: 1.4rem;
  }
  
  .character-card {
    padding: 8px;
    min-height: 80px;
  }
  
  .character-info h3 {
    font-size: 0.8rem;
  }
  
  .difficulty-btn {
    padding: 8px;
    font-size: 14px;
  }
}

/* 터치 디바이스 최적화 */
@media (hover: none) and (pointer: coarse) {
  .difficulty-btn {
    min-height: 44px;
  }
  
  .change-adventure-btn {
    min-height: 44px;
  }
  
  .recent-adventure-select {
    min-height: 44px;
  }
}
</style>
