<template>
  <div class="party-formation">
    <h2>파티 구성</h2>
    

    
    <!-- 던전 선택 버튼들 -->
    <div class="dungeon-selection-container">
      <div class="dungeon-selection-row">
        <button 
          @click="setDungeon('nabel-normal')" 
          :class="{ active: selectedDungeon === 'nabel-normal' }"
          class="dungeon-btn nabel-normal-btn">
          나벨 일반
        </button>
        <button 
          @click="setDungeon('nabel-hard')" 
          :class="{ active: selectedDungeon === 'nabel-hard' }"
          class="dungeon-btn nabel-hard-btn">
          나벨 하드
        </button>
        <button 
          @click="setDungeon('venus')" 
          :class="{ active: selectedDungeon === 'venus' }"
          class="dungeon-btn venus-btn">
          베누스
        </button>
        <button 
          @click="setDungeon('fog')" 
          :class="{ active: selectedDungeon === 'fog' }"
          class="dungeon-btn fog-btn">
          안개신
        </button>
        <button 
          @click="setDungeon('twilight')" 
          :class="{ active: selectedDungeon === 'twilight' }"
          class="dungeon-btn twilight-btn">
                      이내 황혼전
        </button>
      </div>
    </div>
    
    <!-- 상단 선택 바 -->
    <div class="top-bar">
      <!-- 첫 번째 줄: 모험단명 검색 + 검색된 모험단 목록 -->
      <div class="first-row">
        <!-- 모험단명 검색 (좌측 반) -->
        <div class="search-section">
          <div class="form-group">
            <label for="searchQuery">모험단명 검색:</label>
            <div class="search-input-container">
              <input 
                id="searchQuery" 
                v-model="searchQuery" 
                type="text" 
                placeholder="모험단명을 입력하세요" 
                @keyup.enter="searchCharacters"
                list="adventureList"
                class="search-input"
              >
              <datalist id="adventureList">
                <option v-for="adventure in recentSearchedAdventures" :key="adventure" :value="adventure">
                  {{ adventure }}
                </option>
              </datalist>
              <button @click="searchCharacters" :disabled="isSearchDisabled" class="search-btn">
                {{ searching ? '검색 중...' : '검색' }}
              </button>
            </div>
          </div>
        </div>
        
        <!-- 검색된 모험단 목록 (우측 반) -->
        <div class="adventures-section">
          <div class="form-group">
            <label for="adventureSelect">검색된 모험단 목록:</label>
            <div class="multi-select-container">
              <div class="adventure-select-container">
                <select @change="addAdventure" class="adventure-select">
                  <option value="">모험단 추가...</option>
                  <option v-for="adventure in availableAdventures.filter(a => !selectedAdventures.includes(a))" 
                          :key="adventure" :value="adventure">
                    {{ adventure }}
                  </option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 두 번째 줄: 선택된 모험단 -->
      <div class="second-row">
        <div class="selected-adventures-section">
          <div class="form-group">
            <label>선택된 모험단:</label>
            <div class="selected-adventures">
              <span v-if="selectedAdventures.length === 0" class="placeholder">모험단을 선택하세요</span>
              <div v-for="adventure in selectedAdventures" :key="adventure" class="selected-adventure">
                {{ adventure }}
                <button @click="removeAdventure(adventure)" class="remove-btn">×</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
              
    <!-- 파티 구성 영역 (좌우 1:3 비율) -->
    <div v-if="selectedDungeon && selectedAdventures.length > 0" class="party-controls">
      <!-- 좌측: 파티 구성 규칙 (1/3) -->
      <div class="party-rules-section">
        <div class="party-info">
          <p><strong>Basic 기본 규칙:</strong></p>
          <ul>
            <li>한 파티에 모험단 하나씩만 배치</li>
            <li>버퍼 역순, 쩔딜러 정순, 약한딜러 역순</li>
            <li>최소 1버퍼, 1딜러 구성</li>
            <li>기능 변경이 필요하면 Advanced<br>
                <small>( 개발 중 )</small></li>
          </ul>
        </div>
      </div>
      
      <!-- 우측: 컨트롤 및 옵션 (3/3) -->
      <div class="party-controls-right">
        <!-- 우측 상단: 컨트롤 버튼들 (1/3) -->
        <div class="control-buttons-section">
          <div class="control-buttons">
            <button @click="refreshSelectedAdventures" 
                    :disabled="refreshingAdventures" 
                    class="control-btn refresh-btn">
              {{ refreshingAdventures ? '최신화 중...' : '🔄 모험단 최신화' }}
            </button>
            <button @click="clearParty" class="control-btn clear-btn">파티 초기화</button>
            <button @click="copyPartyForWhisper" class="control-btn optimize-btn">
              📋 귓속말용 파티 복사
            </button>
            <button @click="copyPartyToClipboard" class="control-btn copy-btn">📋 카카오톡용 파티 복사</button>
            <button @click="autoGenerateParty" :disabled="loading" class="control-btn auto-btn">
              {{ loading ? '생성 중...' : '자동 파티 생성' }}
            </button>
          </div>
        </div>
        
        <!-- 우측 하단: 파티 구성 옵션 (3/3) -->
        <div class="party-options-section">
          <div class="party-options-box">
            <p><strong>파티 구성 옵션:</strong></p>
            <div class="option-selector">
              <label for="partyFormationMode">자동 파티 생성 방식:</label>
              <select 
                id="partyFormationMode" 
                v-model="selectedPartyFormationMode" 
                class="option-dropdown"
              >
                <option value="basic">Basic (기본)</option>
                <option value="advanced">Advanced (고급)</option>
              </select>
            </div>
            
            <!-- Basic 모드 설명 -->
            <div v-if="selectedPartyFormationMode === 'basic'" class="mode-description">
              <small>기본 파티 구성 알고리즘을 사용합니다.</small>
            </div>
            
            <!-- Advanced 모드 옵션들 -->
            <div v-if="selectedPartyFormationMode === 'advanced'" class="advanced-options">
              <div class="advanced-section">
                <h4>알고리즘:</h4>
                <div class="checkbox-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="advancedOptions.bufferPriority" class="checkbox-input">
                    버퍼 우선
                  </label>
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="advancedOptions.dealerPriority" class="checkbox-input">
                    딜러 우선
                  </label>
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="advancedOptions.adventurePriority" class="checkbox-input">
                    모험단 우선
                  </label>
                </div>
              </div>
              
              <div class="advanced-section">
                <h4>기능 제한 해제:</h4>
                <div class="checkbox-group">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="advancedOptions.ignoreSlotRoles" class="checkbox-input">
                    버퍼-딜러 칸 무시
                  </label>
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="advancedOptions.ignoreMinRequirements" class="checkbox-input">
                    딜러-버퍼 최소 인원 제한 해제
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 컨텐츠 영역 -->
    <div v-if="selectedDungeon && selectedAdventures.length > 0" class="main-content">
      <!-- 좌측: 파티 테이블 -->
      <div class="left-panel">
        <h3>파티 구성</h3>
        <div class="party-tables">
          <div v-for="(party, index) in parties" :key="index" class="party-table">
            <div class="party-header">
              <div class="party-title-stats">
                <span class="party-title">파티{{ index + 1 }}</span>
                <span class="party-separator">|</span>
                <span class="party-combat-power">총 전투력: {{ getPartyTotalDamageInBillion(party) }} 억</span>
                <span class="party-separator">|</span>
                <span class="party-buff-power">버프력: {{ getPartyTotalBuffPowerInTenThousand(party) }}만</span>
                <span class="party-separator">|</span>
                <span class="party-coefficient">파티 계수: {{ getPartyCoefficient(party) }}</span>
              </div>
            </div>
            <div class="party-slots">
              <div 
                v-for="slotIndex in 4" 
                :key="slotIndex"
                class="party-slot"
                :class="{ 'filled': party[slotIndex - 1] }"
                @drop="onDrop($event, index, slotIndex - 1)"
                @dragover="onDragOver"
              >
                <div v-if="party[slotIndex - 1]" 
                     class="character-card in-party"
                     draggable="true"
                     @dragstart="onPartyCharacterDragStart($event, party[slotIndex - 1], index, slotIndex - 1)">
                  <div class="character-avatar">
                    <img 
                      v-if="party[slotIndex - 1].avatarImageUrl || party[slotIndex - 1].characterImageUrl" 
                      :src="party[slotIndex - 1].avatarImageUrl || party[slotIndex - 1].characterImageUrl"
                      :alt="party[slotIndex - 1].characterName"
                      class="character-img"
                      @error="handleImageError"
                    />
                    <div v-else class="avatar-placeholder">
                      {{ party[slotIndex - 1].characterName.charAt(0) }}
        </div>
      </div>
                  <div class="character-info">
                    <div class="character-name">{{ party[slotIndex - 1].characterName }}</div>
                    <div class="adventure-name">{{ party[slotIndex - 1].adventureName }}</div>
                                      <div class="character-stats">
                    <div v-if="!isBuffer(party[slotIndex - 1])" class="stat dealer-stat">
                      전투력: {{ formatNumber(party[slotIndex - 1].totalDamage || 0) }}
                    </div>
                    <div v-if="isBuffer(party[slotIndex - 1])" class="stat buffer-stat">
                      버프력: {{ formatNumber(party[slotIndex - 1].buffPower || 0) }}
                    </div>
                  </div>
                    <div class="dungeon-status">
                      <span :class="getDungeonClearClass(party[slotIndex - 1])">
                        {{ getDungeonClearText(party[slotIndex - 1]) }}
                      </span>
            </div>
            </div>
                  <button @click="removeFromParty(index, slotIndex - 1)" class="remove-from-party">×</button>
            </div>
                                <div v-else class="empty-slot">
                  <div class="slot-placeholder">
                    <span class="slot-number">{{ getSlotRole(slotIndex) }}</span>
                    <span class="slot-text">드래그해서 추가</span>
                  </div>
                </div>
      </div>
        </div>
      </div>
    </div>

        <!-- 파티 추가 버튼 -->
        <button @click="addNewParty" class="add-party-btn">+ 파티 추가</button>
      </div>
      
      <!-- 우측: 모험단별 캐릭터 목록 -->
      <div class="right-panel">
        <h3>사용 가능한 캐릭터</h3>
        <div class="adventure-panels">
          <div v-for="adventure in selectedAdventures" :key="adventure" class="adventure-panel">
                        <div class="adventure-header">
              <h4>{{ adventure }}</h4>
              <div class="character-counts">
                <span class="dealer-count">딜러: {{ getDealerCount(adventure) }}명</span>
                <span class="buffer-count">버퍼: {{ getBufferCount(adventure) }}명</span>
              </div>
            </div>
            <div class="character-list">
              
              <!-- 딜러 섹션 -->
              <div class="character-section dealer-section">
                <div class="section-header dealer-header">
                  <h5>딜러</h5>
                </div>
                <div class="section-content">
                  <div 
                    v-for="character in getFilteredCharacters(adventure).filter(c => !isBuffer(c))" 
                    :key="character.characterId"
                    class="character-card"
                    :class="{ 
                      'in-use': isCharacterInParty(character.characterId),
                      'is-helper': isHelperCharacter(character),
                      'draggable': !isCharacterInParty(character.characterId)
                    }"
                    :draggable="!isCharacterInParty(character.characterId)"
                    @dragstart="onDragStart($event, character)"
                  >
                    <!-- 파티 포함 표시 - 카드 왼쪽 상단에 배치 -->
                    <div v-if="isCharacterInParty(character.characterId)" class="in-party-badge-left">
                      🔒
                    </div>
                    <div class="character-avatar">
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
                    <div class="character-info">
                      <div class="character-name">{{ character.characterName }}</div>
                      <div class="character-stats">
                        <div class="stat dealer-stat">
                          전투력: {{ formatNumber(character.totalDamage || 0) }}
                        </div>
                      </div>
                      <div class="dungeon-status">
                        <span :class="getDungeonClearClass(character)">
                          {{ getDungeonClearText(character) }}
                        </span>
                      </div>
                      <div class="character-fame">명성: {{ formatNumber(character.fame || 0) }}</div>
                      <!-- 업둥 표시 -->
                      <div v-if="isHelperCharacter(character)" class="helper-badge">
                        ⭐ 업둥
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 버퍼 섹션 -->
              <div class="character-section buffer-section">
                <div class="section-header buffer-header">
                  <h5>버퍼</h5>
                </div>
                <div class="section-content">
                  <div 
                    v-for="character in getFilteredCharacters(adventure).filter(c => isBuffer(c))" 
                    :key="character.characterId"
                    class="character-card"
                    :class="{ 
                      'in-use': isCharacterInParty(character.characterId),
                      'is-helper': isHelperCharacter(character),
                      'draggable': !isCharacterInParty(character.characterId)
                    }"
                    :draggable="!isCharacterInParty(character.characterId)"
                    @dragstart="onDragStart($event, character)"
                  >
                    <!-- 파티 포함 표시 - 카드 왼쪽 상단에 배치 -->
                    <div v-if="isCharacterInParty(character.characterId)" class="in-party-badge-left">
                      🔒
                    </div>
                    <div class="character-avatar">
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
                    <div class="character-info">
                      <div class="character-name">{{ character.characterName }}</div>
                      <div class="character-stats">
                        <div class="stat buffer-stat">
                          버프력: {{ formatNumber(character.buffPower || 0) }}
                        </div>
                      </div>
                      <div class="dungeon-status">
                        <span :class="getDungeonClearClass(character)">
                          {{ getDungeonClearText(character) }}
                        </span>
                      </div>
                      <div class="character-fame">명성: {{ formatNumber(character.fame || 0) }}</div>
                      <!-- 업둥 표시 -->
                      <div v-if="isHelperCharacter(character)" class="helper-badge">
                        ⭐ 업둥
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
    </div>
      </div>
        </div>
      </div>
      
    <!-- 데이터가 없는 경우 -->
    <div v-else-if="!selectedDungeon" class="no-selection">
      <p>던전을 선택해주세요.</p>
    </div>

    <div v-else-if="selectedAdventures.length === 0" class="no-selection">
      <p>모험단을 선택해주세요.</p>
      <RouterLink to="/character-search" class="search-link">캐릭터 검색하러 가기</RouterLink>
    </div>

    <!-- 로딩 및 에러 메시지 -->
    <div v-if="loading" class="loading">파티를 구성하는 중...</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { apiFetch } from '../config/api';
import { isBuffer } from '../utils/characterUtils';

// 반응형 데이터
const searchMode = ref(''); // 검색 모드 (character 또는 adventure)
const searchQuery = ref(''); // 검색어 (캐릭터명 또는 모험단명)
const searching = ref(false); // 검색 중 상태
const recentSearchedAdventures = ref<string[]>([]); // 최근 검색한 모험단 목록
const selectedRecentAdventure = ref(''); // 최근 검색에서 선택한 모험단

const selectedDungeon = ref('');
const selectedAdventures = ref<string[]>([]);
const parties = ref<Array<Array<any>>>([[]]);
const loading = ref(false);
const error = ref('');
const successMessage = ref('');
const allCharacters = ref<any[]>([]);
const refreshingAdventures = ref(false);
const selectedPartyFormationMode = ref('basic'); // 파티 구성 방식 (기본값: basic)
const advancedOptions = ref({
  bufferPriority: false,      // 버퍼 우선
  dealerPriority: false,      // 딜러 우선
  adventurePriority: false,   // 모험단 우선
  ignoreSlotRoles: false,     // 버퍼-딜러 칸 무시
  ignoreMinRequirements: false // 딜러-버퍼 최소 인원 제한 해제
});

// Advanced 옵션 저장
const saveAdvancedOptions = () => {
  localStorage.setItem('dnfPartyAdvancedOptions', JSON.stringify(advancedOptions.value));
  console.log('Advanced 옵션 저장됨:', advancedOptions.value);
};

// Advanced 옵션 복원
const loadAdvancedOptions = () => {
  try {
    const saved = localStorage.getItem('dnfPartyAdvancedOptions');
    if (saved) {
      const parsed = JSON.parse(saved);
      advancedOptions.value = { ...advancedOptions.value, ...parsed };
      console.log('Advanced 옵션 복원됨:', advancedOptions.value);
    }
  } catch (err) {
    console.error('Advanced 옵션 복원 실패:', err);
  }
};

// Advanced 옵션 변경 감지 및 저장
watch(advancedOptions, () => {
  saveAdvancedOptions();
}, { deep: true });

// 컴포넌트 마운트
onMounted(() => {
  console.log('=== 파티 구성 페이지 마운트 ===');
  // 기본값: 나벨 일반 선택
  selectedDungeon.value = 'nabel-normal';
  // 검색 모드를 adventure로 설정 (모험단 검색 전용)
  searchMode.value = 'adventure';
  loadSearchHistory();
  loadCharactersFromAPI();
  loadAdvancedOptions(); // Advanced 옵션 복원
});

// 검색 기록에서 모험단 목록 가져오기 (CharacterSearch.vue와 동일한 localStorage 키 사용)
const availableAdventures = computed(() => {
  try {
    const dungeonAdventureHistory = JSON.parse(localStorage.getItem('df_dungeon_adventure_history') || '[]');
    console.log('LocalStorage에서 로드된 던전 모험단 기록:', dungeonAdventureHistory);
    
    const adventures = new Set<string>();
    
    dungeonAdventureHistory.forEach((adventureName: string) => {
      if (adventureName && adventureName !== 'N/A') {
        adventures.add(adventureName);
        console.log('모험단 추가됨:', adventureName);
      }
    });
    
    const result = Array.from(adventures).sort();
    console.log('최종 모험단 목록:', result);
    return result;
  } catch (error) {
    console.error('LocalStorage 던전 모험단 기록 로드 실패:', error);
    return [];
  }
});

// 특정 모험단의 캐릭터만 로드 (검색 시 사용)
const loadCharactersForAdventure = async (adventureName: string) => {
  try {
    console.log(`=== 모험단 '${adventureName}' 캐릭터 로드 시작 ===`);
    
    const response = await apiFetch(`/characters/adventure/${encodeURIComponent(adventureName)}`);
    
    if (response.ok) {
      const data = await response.json();
      if (data.success && data.characters) {
        console.log(`모험단 '${adventureName}' 캐릭터 ${data.characters.length}개 로드됨`);
        
        // 기존 캐릭터 목록에서 같은 모험단의 캐릭터들을 제거하고 새로 추가
        const existingCharacters = allCharacters.value.filter(char => 
          char.adventureName !== adventureName
        );
        
        // 새로 검색된 캐릭터들을 기존 목록에 추가
        allCharacters.value = [...existingCharacters, ...data.characters];
        
        console.log(`기존 캐릭터 수: ${existingCharacters.length}, 새로 추가된 캐릭터 수: ${data.characters.length}`);
        
        console.log(`전체 캐릭터 수: ${allCharacters.value.length}`);
      }
    }
  } catch (err) {
    console.error(`모험단 '${adventureName}' 로드 중 오류:`, err);
  }
};

// API에서 캐릭터 데이터 로드
const loadCharactersFromAPI = async () => {
  try {
    loading.value = true;
    error.value = '';
    
    console.log('=== 캐릭터 데이터 로드 시작 ===');
    
    // 모든 모험단의 캐릭터를 가져오기
    const adventureNames = availableAdventures.value;
    console.log('로드할 모험단 목록:', adventureNames);
    
    if (adventureNames.length === 0) {
      console.warn('로드할 모험단이 없습니다.');
      allCharacters.value = [];
      return;
    }
    
    const allCharacterPromises = adventureNames.map(async (adventureName) => {
      try {
        // console.log(`모험단 '${adventureName}' 캐릭터 로드 시작...`);
        const response = await apiFetch(`/characters/adventure/${encodeURIComponent(adventureName)}`);
        
        // console.log(`모험단 '${adventureName}' API 응답 상태:`, response.status);
        
        if (response.ok) {
          const data = await response.json();
          // console.log(`모험단 '${adventureName}' API 응답 데이터:`, data);
          
          if (data.success && data.characters) {
            // console.log(`모험단 '${adventureName}' 캐릭터 ${data.characters.length}개 로드됨`);
            return data.characters;
          } else {
            // console.warn(`모험단 '${adventureName}' 캐릭터 데이터 없음:`, data);
            return [];
          }
        } else {
          // console.error(`모험단 '${adventureName}' API 호출 실패:`, response.status, response.statusText);
          return [];
    }
  } catch (err) {
        console.error(`모험단 '${adventureName}' 로드 중 오류:`, err);
        return [];
      }
    });
    
    const results = await Promise.all(allCharacterPromises);
    const flatResults = results.flat();
    
    // console.log('로드된 전체 캐릭터 결과:', flatResults);
    // console.log('총 캐릭터 수:', flatResults.length);
    
    allCharacters.value = flatResults;
    
    if (flatResults.length === 0) {
      console.warn('로드된 캐릭터가 없습니다. LocalStorage 또는 백엔드 데이터를 확인하세요.');
      // 모험단이 선택되었을 때만 에러 메시지 표시
      if (selectedAdventures.value.length > 0) {
        error.value = '사용 가능한 캐릭터가 없습니다. 먼저 캐릭터 검색에서 캐릭터를 검색해주세요.';
      }
    }
    
  } catch (err) {
    console.error('캐릭터 데이터 로드 실패:', err);
    error.value = '캐릭터 데이터를 불러오는데 실패했습니다.';
  } finally {
    loading.value = false;
    console.log('=== 캐릭터 데이터 로드 완료 ===');
  }
};

// 검색 버튼 비활성화 상태
const isSearchDisabled = computed(() => {
  return searching.value;
});

// 던전 선택 함수
const setDungeon = (dungeon: string) => {
  selectedDungeon.value = dungeon;
  console.log('선택된 던전:', dungeon);
  // 던전이 변경되면 파티 초기화
  clearParty();
};

// 검색 모드 변경 핸들러
const onSearchModeChange = () => {
  // 검색 모드가 변경되면 기존 검색 결과와 입력값 초기화
  searchQuery.value = '';
  error.value = '';
  successMessage.value = '';
};

// 최근 검색 모험단 추가 함수
const addSelectedRecentAdventure = () => {
  if (selectedRecentAdventure.value && !selectedAdventures.value.includes(selectedRecentAdventure.value)) {
    selectedAdventures.value.push(selectedRecentAdventure.value);
    selectedRecentAdventure.value = '';
    console.log('최근 검색 모험단 추가됨:', selectedRecentAdventures.value);
  }
};

// 최근 검색한 모험단 목록 로드
const loadRecentSearchedAdventures = () => {
  try {
    const saved = localStorage.getItem('df_dungeon_adventure_history');
    if (saved) {
      recentSearchedAdventures.value = JSON.parse(saved);
      // console.log('던전 모험단 기록 로드 완료:', recentSearchedAdventures.value);
    }
  } catch (error) {
    // console.error('던전 모험단 기록 로드 실패:', error);
    recentSearchedAdventures.value = [];
  }
};

// 로컬스토리지에 최근 검색한 모험단 저장 (CharacterSearch.vue와 동일한 방식)
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

// 모험단 검색 함수
const searchCharacters = async () => {
  if (!searchQuery.value.trim()) {
    error.value = '검색어를 입력해주세요.';
    return;
  }

  try {
    searching.value = true;
    error.value = '';
    successMessage.value = '';

    // 모험단 검색 API 호출
    const response = await apiFetch(`/characters/adventure/${encodeURIComponent(searchQuery.value)}`);
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        successMessage.value = `'${searchQuery.value}' 모험단의 ${data.characters?.length || 0}개 캐릭터를 찾았습니다.`;
        
        // 로컬스토리지에 검색한 모험단 저장
        saveRecentSearchedAdventure(searchQuery.value);
        
        // 검색된 모험단을 자동으로 selectedAdventures에 추가
        if (!selectedAdventures.value.includes(searchQuery.value)) {
          selectedAdventures.value.push(searchQuery.value);
          // 로컬스토리지에 선택된 모험단들 저장
          saveSelectedAdventures();
        }
        
        // 새로 검색된 모험단의 캐릭터만 로드 (전체 초기화 방지)
        await loadCharactersForAdventure(searchQuery.value);
      } else {
        error.value = data.message || '검색에 실패했습니다.';
      }
    } else {
      error.value = '검색 중 오류가 발생했습니다.';
    }

  } catch (err) {
    console.error('검색 실패:', err);
    error.value = '검색 중 오류가 발생했습니다.';
  } finally {
    searching.value = false;
  }
};

// 선택된 모험단들을 로컬스토리지에 저장
const saveSelectedAdventures = () => {
  try {
    localStorage.setItem('df_party_selected_adventures', JSON.stringify(selectedAdventures.value));
  } catch (error) {
    console.error('선택된 모험단 저장 실패:', error);
  }
};

// 최근 검색한 모험단 선택 처리
const selectRecentAdventure = async () => {
  if (selectedRecentAdventure.value) {
    // 선택된 모험단으로 검색 실행
    searchQuery.value = selectedRecentAdventure.value;
    searchMode.value = 'adventure';
    await searchCharacters();
    
    // 선택 초기화
    selectedRecentAdventure.value = '';
  }
};

// 검색 기록 로드
const loadSearchHistory = () => {
  // 컴포넌트 마운트 시 검색 기록을 로드하여 모험단 목록 갱신
  loadRecentSearchedAdventures();
};

// 던전 변경 시
const onDungeonChange = () => {
  
  // 파티 초기화
  parties.value = [[]];
  error.value = '';
};



// 파티 구성 결과를 클립보드로 복사
const copyPartyToClipboard = async () => {
  try {
    let clipboardText = '';
    
    parties.value.forEach((party, partyIndex) => {
      if (party.length > 0 && party.some(slot => slot !== null)) {
        clipboardText += `파티${partyIndex + 1} - `;
        
        const partyMembers = party
          .filter(slot => slot !== null)
          .map(character => {
            const stat = character.totalDamage > 0 ? 
              `${character.characterName}(${formatNumber(character.totalDamage)})` : 
              `${character.characterName}(${formatNumber(character.buffPower)})`;
            return stat;
          })
          .join(', ');
        
        clipboardText += partyMembers + '\n';
      }
    });
    
    if (clipboardText.trim()) {
      await navigator.clipboard.writeText(clipboardText.trim());
      alert(`카카오톡용 파티 정보가 클립보드에 복사되었습니다!\n\n복사된 내용:\n${clipboardText.trim()}`);
    } else {
      error.value = '복사할 파티 구성이 없습니다.';
    }
  } catch (err) {
    console.error('클립보드 복사 실패:', err);
    error.value = '클립보드 복사에 실패했습니다.';
  }
};

// 모험단 추가/제거
const addAdventure = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  const adventure = target.value;
  if (adventure && !selectedAdventures.value.includes(adventure)) {
    selectedAdventures.value.push(adventure);
    target.value = ''; // 선택 초기화
    // 모험단이 추가되면 파티 초기화
    clearParty();
  }
};

const removeAdventure = (adventure: string) => {
  selectedAdventures.value = selectedAdventures.value.filter(a => a !== adventure);
  // 모험단이 제거되면 파티 초기화
  clearParty();
};

// 선택된 던전에 따라 조건에 맞는 캐릭터 필터링 (안감 제외, 업둥 포함)
const getFilteredCharacters = (adventureName: string) => {
  // console.log(`getFilteredCharacters 호출: adventureName="${adventureName}"`);
  
  // allCharacters가 undefined이거나 null인 경우 빈 배열 반환
  if (!allCharacters.value || !Array.isArray(allCharacters.value)) {
    console.warn('allCharacters가 유효하지 않습니다:', allCharacters.value);
    return [];
  }
  
  // 1. 모험단별 캐릭터 필터링
  const adventureCharacters = allCharacters.value.filter(c => c.adventureName === adventureName);
  
  if (adventureCharacters.length === 0) {
    return [];
  }
  
  // 2. 던전이 선택되지 않았다면 모든 캐릭터 반환 (안감만 제외)
  if (!selectedDungeon.value) {
    return adventureCharacters; // 던전 선택 안했을 때는 모든 캐릭터 표시
  }
  
  // 3. 선택된 던전에 따라 필터링
  const filteredCharacters = adventureCharacters.filter(character => {
    let dungeonCondition = false;
    let isExcluded = false;
    
  switch (selectedDungeon.value) {
      case 'nabel-normal':
        dungeonCondition = !character.dungeonClearNabel; // 클리어 안한 캐릭터
        isExcluded = character.isExcludedNabel; // 안감 여부
        // 일반: 일반 대상자만 포함 (30억 딜러, 400만 버퍼) + 하드 대상자 제외
        dungeonCondition = dungeonCondition && character.isNormalNabelEligible && !character.isHardNabelEligible;
        break;
      case 'nabel-hard':
        dungeonCondition = !character.dungeonClearNabel; // 클리어 안한 캐릭터
        isExcluded = character.isExcludedNabel; // 안감 여부
        // 하드: 하드 대상자만 포함 (100억 딜러, 500만 버퍼)
        dungeonCondition = dungeonCondition && character.isHardNabelEligible;
        break;
      case 'venus':
        dungeonCondition = !character.dungeonClearVenus;
        isExcluded = character.isExcludedVenus;
        break;
      case 'fog':
        dungeonCondition = !character.dungeonClearFog;
        isExcluded = character.isExcludedFog;
        break;
      case 'twilight':
        dungeonCondition = !character.dungeonClearTwilight; // 클리어 안한 캐릭터
        isExcluded = false; // 이내 황혼전은 아직 안감 기능 없음
        break;
      default:
        dungeonCondition = true;
        isExcluded = false;
    }
    
    // 안감인 경우 제외, 그 외에는 던전 조건에 맞는 캐릭터만 포함
    const shouldInclude = !isExcluded && dungeonCondition;
    
    return shouldInclude;
  });
  
  // 4. 딜러와 버퍼를 각각 정렬하여 반환
  const dealers = filteredCharacters.filter(char => !isBuffer(char));
  const buffers = filteredCharacters.filter(char => isBuffer(char));
  
  // 딜러: 전투력 기준 내림차순 정렬 (강한 순)
  dealers.sort((a, b) => (b.totalDamage || 0) - (a.totalDamage || 0));
  
  // 버퍼: 버프력 기준 내림차순 정렬 (강한 순)
  buffers.sort((a, b) => (b.buffPower || 0) - (a.buffPower || 0));
  
  // 딜러 먼저, 그 다음 버퍼 순서로 반환
  return [...dealers, ...buffers];
};

// 파티에 들어간 캐릭터 ID들을 추적하는 함수
const getCharactersInParties = (): string[] => {
  const characterIds: string[] = [];
  parties.value.forEach(party => {
    party.forEach(slot => {
      if (slot && slot.characterId) {
        characterIds.push(slot.characterId);
      }
    });
  });
  return characterIds;
};

// 드래그 앤 드롭 핸들러
const onDragStart = (event: DragEvent, character: any) => {
  // 파티에 이미 들어간 캐릭터는 드래그 불가
  if (isCharacterInParty(character.characterId)) {
    event.preventDefault();
    return;
  }
  
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', JSON.stringify({
      type: 'new',
      character: character
    }));
  }
};

// 파티 내 캐릭터 드래그 시작
const onPartyCharacterDragStart = (event: DragEvent, character: any, partyIndex: number, slotIndex: number) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', JSON.stringify({
      type: 'move',
      character: character,
      sourceParty: partyIndex,
      sourceSlot: slotIndex
    }));
  }
};

const onDragOver = (event: DragEvent) => {
  event.preventDefault();
};

const onDrop = (event: DragEvent, partyIndex: number, slotIndex: number) => {
  event.preventDefault();
  
  if (event.dataTransfer) {
    const data = event.dataTransfer.getData('text/plain');
    const dragData = JSON.parse(data);
    
    if (dragData.type === 'move') {
      // 파티 내 캐릭터 이동
      const { character, sourceParty, sourceSlot } = dragData;
      
      // 역할 체크 (버퍼/딜러)
      if (!canAddCharacterToSlot(character, slotIndex)) {
        const roleName = getSlotRole(slotIndex);
        const characterRole = isBuffer(character) ? '버퍼' : '딜러';
        error.value = `${roleName} 칸에는 ${characterRole} 캐릭터를 넣을 수 없습니다.`;
        return;
      }
      
      // 파티 배열이 충분히 크지 않으면 확장
      while (parties.value[partyIndex].length <= slotIndex) {
        parties.value[partyIndex].push(null);
      }
      
      // 기존 슬롯의 캐릭터와 교환
      const targetCharacter = parties.value[partyIndex][slotIndex];
      parties.value[partyIndex][slotIndex] = character;
      parties.value[sourceParty][sourceSlot] = targetCharacter;
      
    } else if (dragData.type === 'new') {
      // 새로운 캐릭터 추가
      const character = dragData.character;
      
      // 파티당 모험단 제한 체크
      if (!canAddCharacterToParty(character, partyIndex)) {
        error.value = `파티 ${partyIndex + 1}에는 이미 같은 모험단('${character.adventureName}')의 캐릭터가 있습니다. 한 파티당 하나의 모험단만 허용됩니다.`;
        return;
      }
      
      // 역할 체크 (버퍼/딜러)
      if (!canAddCharacterToSlot(character, slotIndex)) {
        const roleName = getSlotRole(slotIndex);
        const characterRole = isBuffer(character) ? '버퍼' : '딜러';
        error.value = `${roleName} 칸에는 ${characterRole} 캐릭터를 넣을 수 없습니다.`;
        return;
      }
  
      // 파티 배열이 충분히 크지 않으면 확장
      while (parties.value[partyIndex].length <= slotIndex) {
        parties.value[partyIndex].push(null);
      }
      
      // 기존 캐릭터가 있다면 교체, 없다면 추가
      parties.value[partyIndex][slotIndex] = character;
    }
  }
};

// 파티당 모험단 제한 체크 함수
const canAddCharacterToParty = (character: any, partyIndex: number): boolean => {
  const party = parties.value[partyIndex];
  if (!party) return true;
  
  // 파티에 이미 있는 캐릭터들의 모험단 확인
  const existingAdventures = party
    .filter(slot => slot !== null)
    .map(char => char.adventureName)
    .filter(adventure => adventure && adventure !== 'N/A');
  
  // 파티가 비어있으면 추가 가능
  if (existingAdventures.length === 0) return true;
  
  // 같은 모험단이 이미 있으면 추가 불가 (요구사항: 같은 모험단이 있으면 포함되지 않는 기능)
  if (existingAdventures.includes(character.adventureName)) return false;
  
  // 다른 모험단이면 추가 가능
  return true;
};

// 슬롯 역할 체크 함수 (버퍼/딜러)
// slotIndex는 실제 배열 인덱스 (0-3)
const canAddCharacterToSlot = (character: any, slotIndex: number): boolean => {
  const isCharacterBuffer = isBuffer(character);
  const slotRole = getSlotRole(slotIndex + 1); // 0->1, 1->2, 2->3, 3->4
  
  // 슬롯 1번(버퍼)에는 버퍼만, 슬롯 2-4번(딜러)에는 딜러만
  if (slotRole === '버퍼' && !isCharacterBuffer) {
    return false; // 버퍼 칸에 딜러 넣으려고 함
  }
  if (slotRole === '딜러' && isCharacterBuffer) {
    return false; // 딜러 칸에 버퍼 넣으려고 함
  }
  
  return true;
};

// 파티 관리
const addNewParty = () => {
  parties.value.push([]);
};

const removeFromParty = (partyIndex: number, slotIndex: number) => {
  parties.value[partyIndex][slotIndex] = null;
};

const clearParty = () => {
  parties.value = [[]];
};

// Advanced 파티 생성
const generateAdvancedParty = async () => {
  try {
    loading.value = true;
    error.value = '';
    
    console.log('=== Advanced 자동 파티 생성 시작 ===');
    console.log('선택된 옵션:', advancedOptions.value);
    
    // 선택된 옵션에 따른 파티 생성 로직
    if (advancedOptions.value.ignoreSlotRoles) {
      console.log('⚠️ 버퍼-딜러 칸 역할 제한 해제됨');
    }
    
    if (advancedOptions.value.ignoreMinRequirements) {
      console.log('⚠️ 최소 인원 제한 해제됨');
    }
    
    // 기본 파티 생성 로직을 기반으로 Advanced 옵션 적용
    await generateBasicParty();
    
    console.log('=== Advanced 자동 파티 생성 완료 ===');
    
  } catch (err) {
    console.error('Advanced 파티 생성 실패:', err);
    error.value = 'Advanced 파티 생성에 실패했습니다.';
  } finally {
    loading.value = false;
  }
};

// 자동 파티 생성
const autoGenerateParty = async () => {
  try {
    loading.value = true;
    error.value = '';
    
    // 선택된 방식에 따라 다른 로직 실행
    if (selectedPartyFormationMode.value === 'basic') {
      await generateBasicParty();
      return;
    } else if (selectedPartyFormationMode.value === 'advanced') {
      await generateAdvancedParty();
      return;
    }
    
    const availableCharacters = selectedAdventures.value.flatMap(adventure => getFilteredCharacters(adventure));
    
    if (availableCharacters.length < 4) {
      error.value = '파티 구성에 필요한 캐릭터가 부족합니다.';
      return;
    }
    
    // 모험단별로 캐릭터 그룹화
    const charactersByAdventure = new Map<string, any[]>();
    availableCharacters.forEach(character => {
      const adventure = character.adventureName || 'Unknown';
      if (!charactersByAdventure.has(adventure)) {
        charactersByAdventure.set(adventure, []);
      }
      charactersByAdventure.get(adventure)!.push(character);
    });
    
    // 각 모험단의 캐릭터들을 직업에 맞는 스탯 순으로 정렬
    charactersByAdventure.forEach((characters, adventure) => {
      characters.sort((a, b) => {
        let aStat = 0;
        let bStat = 0;
        
        if (isBuffer(a)) {
          aStat = a.buffPower || 0;
    } else {
          aStat = a.totalDamage || 0;
        }
        
        if (isBuffer(b)) {
          bStat = b.buffPower || 0;
        } else {
          bStat = b.totalDamage || 0;
        }
        
        return bStat - aStat;
      });
    });
    
    // 모험단별로 파티 구성 (한 파티당 하나의 모험단만)
    const newParties: Array<Array<any>> = [];
    const adventureEntries = Array.from(charactersByAdventure.entries());
    
    // 각 모험단에서 최고 스탯 캐릭터 1명씩 선택하여 파티 구성
    for (let i = 0; i < adventureEntries.length; i++) {
      const [adventure, characters] = adventureEntries[i];
      
      // 해당 모험단의 최고 스탯 캐릭터 1명 선택
      if (characters.length > 0) {
        const bestCharacter = characters[0]; // 이미 스탯 순으로 정렬되어 있음
        newParties.push([bestCharacter]);
      }
    }
    
    // 4명이 되지 않는 파티는 빈 슬롯으로 채움
    while (newParties.length < 4) {
      newParties.push([]);
    }
    
    parties.value = newParties;
    
  } catch (err) {
    error.value = '자동 파티 생성에 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

// Basic 방식 자동 파티 생성 로직
const generateBasicParty = async () => {
  console.log('=== Basic 자동 파티 생성 시작 ===');
  
  const availableCharacters = selectedAdventures.value.flatMap(adventure => getFilteredCharacters(adventure));
  
  if (availableCharacters.length < 4) {
    error.value = '파티 구성에 필요한 캐릭터가 부족합니다. (최소 4명 필요)';
    return;
  }
  
  // 1. 캐릭터 분류 및 정렬
  const dealers = availableCharacters.filter(char => !isBuffer(char));
  const buffers = availableCharacters.filter(char => isBuffer(char));
  
  // 딜러 리스트: 전투력 기준 내림차순 정렬 (강한 순)
  dealers.sort((a, b) => (b.totalDamage || 0) - (a.totalDamage || 0));
  
  // 버퍼 리스트: 버프력 기준 내림차순 정렬 (강한 순)
  buffers.sort((a, b) => (b.buffPower || 0) - (a.buffPower || 0));
  
  console.log(`총 캐릭터 수: ${availableCharacters.length} (딜러: ${dealers.length}, 버퍼: ${buffers.length})`);
  console.log('딜러 순서 (강한 순):', dealers.map(d => `${d.characterName}(${(d.totalDamage || 0).toLocaleString()})`));
  console.log('버퍼 순서 (강한 순):', buffers.map(b => `${b.characterName}(${(b.buffPower || 0).toLocaleString()})`));
  
  // 2. 파티 구성 반복
  const newParties: Array<Array<any>> = [];
  const usedCharacters = new Set<string>(); // 이미 사용된 캐릭터 추적
  const excludedStrongDealers: any[] = []; // 모험단 중복으로 제외된 딜러들
  const excludedWeakenDealers: any[] = []; // 모험단 중복으로 제외된 딜러들
  const excludedBuffers: any[] = []; // 모험단 중복으로 제외된 버퍼들
  
  while (dealers.length >= 1 && buffers.length >= 1) {
    const party: any[] = [];
    console.log(`\n--- 파티 ${newParties.length + 1} 구성 시작 ---`);
    
    
    // 2-1. 버퍼 추가 (1): 버퍼 리스트에서 역순(약한 순)으로 캐릭터를 하나씩 뽑아 모험단 중복 체크
    if (buffers.length > 0) {
      // 약한 순으로 버퍼 선택 (리스트의 끝에서부터)
      const weakBuffer = buffers.pop()!;
      console.log(`🔄 버퍼 추가 시도: ${weakBuffer.characterName} (버프력: ${(weakBuffer.buffPower || 0).toLocaleString()}, 모험단: ${weakBuffer.adventureName})`);
      party.push(weakBuffer);
      usedCharacters.add(weakBuffer.characterId);

    }

    // 2-2. 딜러 추가 (1): 딜러 리스트에서 가장 강한 딜러 1명을 파티에 추가 (모험단 중복 시 다음 강한 딜러 시도)
    let strongestDealerAdded = false;
    while (dealers.length > 0 && !strongestDealerAdded) {
      const strongestDealer = dealers.shift()!;
      
      console.log(`🔄 최강 딜러 추가 시도: ${strongestDealer.characterName} (전투력: ${(strongestDealer.totalDamage || 0).toLocaleString()}, 모험단: ${strongestDealer.adventureName})`);
      
      const partyAdventures = party.map(char => char.adventureName).filter(adv => adv && adv !== 'N/A');
      if (!partyAdventures.includes(strongestDealer.adventureName)) {
        party.push(strongestDealer);
        usedCharacters.add(strongestDealer.characterId);
        strongestDealerAdded = true;
        console.log(`✅ 파티 ${newParties.length + 1}에 최강 딜러 추가: ${strongestDealer.characterName} (전투력: ${(strongestDealer.totalDamage || 0).toLocaleString()}, 모험단: ${strongestDealer.adventureName})`);
      } else {
        console.log(`❌ 모험단 중복으로 인해 딜러 제외: ${strongestDealer.characterName} (모험단: ${strongestDealer.adventureName}) - 파티 내 모험단: [${partyAdventures.join(', ')}]`);
        // 제외된 딜러는 별도 배열에 보관
        excludedStrongDealers.push(strongestDealer);
        console.log(`🔄 다음 강한 딜러 시도...`);
      }
    }
    
    // 강한 딜러를 추가하지 못했다면 파티 구성 실패
    if (!strongestDealerAdded) {
      console.log(`⚠️ 강한 딜러 추가 실패: 모든 강한 딜러가 모험단 중복으로 제외됨`);
      // 사용된 캐릭터들을 원래 리스트로 되돌리기
      party.forEach(char => {
        if (isBuffer(char)) {
          buffers.unshift(char);
          console.log(`   🔄 버퍼 복원: ${char.characterName} → 버퍼 리스트`);
        }
        usedCharacters.delete(char.characterId);
      });
      continue; // 다음 파티 구성 시도
    }
    
    // 2-3. 딜러 추가 (2, 3): 딜러 리스트의 반대 방향(약한 순)에서 캐릭터를 하나씩 뽑아 모험단 중복 체크
    let dealerCount = 1;
    while (dealerCount < 3 && dealers.length > 0) {
      // 약한 순으로 딜러 선택 (리스트의 끝에서부터)
      const weakDealer = dealers.pop()!;
      console.log(`🔄 딜러 추가 시도 (${dealerCount + 1}/3): ${weakDealer.characterName} (전투력: ${(weakDealer.totalDamage || 0).toLocaleString()}, 모험단: ${weakDealer.adventureName})`);
      
      // 모험단 중복 체크 - 현재 파티에 있는 모험단들과 비교
      const partyAdventures = party.map(char => char.adventureName).filter(adv => adv && adv !== 'N/A');
      if (!partyAdventures.includes(weakDealer.adventureName)) {
        party.push(weakDealer);
        usedCharacters.add(weakDealer.characterId);
        dealerCount++;
        console.log(`✅ 파티 ${newParties.length + 1}에 약한 딜러 추가: ${weakDealer.characterName} (모험단: ${weakDealer.adventureName})`);
      } else {
        console.log(`❌ 모험단 중복으로 인해 딜러 제외: ${weakDealer.characterName} (모험단: ${weakDealer.adventureName}) - 파티 내 모험단: [${partyAdventures.join(', ')}]`);
        // 제외된 딜러는 별도 배열에 보관
        excludedWeakenDealers.push(weakDealer);
      }
    }
    
    
    // 최소 구성 조건 체크: 1딜러 + 1버퍼가 있어야 파티 구성
    const hasDealer = party.some(char => !isBuffer(char));
    const hasBuffer = party.some(char => isBuffer(char));
    
    if (hasDealer && hasBuffer) {
      // 파티가 완성되면 추가
      newParties.push(party);
      const partyDealers = party.filter(char => !isBuffer(char));
      const partyBuffers = party.filter(char => isBuffer(char));
      console.log(`🎉 파티 ${newParties.length} 완성!`);
      console.log(`   딜러: ${partyDealers.map(d => `${d.characterName}(${(d.totalDamage || 0).toLocaleString()})`).join(', ')}`);
      console.log(`   버퍼: ${partyBuffers.map(b => `${b.characterName}(${(b.buffPower || 0).toLocaleString()})`).join(', ')}`);
      console.log(`   모험단: [${party.map(char => char.adventureName).filter(adv => adv && adv !== 'N/A').join(', ')}]`);
      
            // 🔄 파티 구성 완료 후 제외된 캐릭터들을 뒤로 추가
      if (excludedWeakenDealers.length > 0) {
        dealers.push(...excludedWeakenDealers);
        console.log(`🔄 제외된 딜러 ${excludedWeakenDealers.length}명을 딜러 리스트 뒤로 추가`);
        excludedWeakenDealers.length = 0; // 배열 초기화
      }
      if (excludedStrongDealers.length > 0) {
        dealers.unshift(...excludedStrongDealers);
        console.log(`🔄 제외된 딜러 ${excludedStrongDealers.length}명을 딜러 리스트 뒤로 추가`);
        excludedStrongDealers.length = 0; // 배열 초기화
      }
      if (excludedBuffers.length > 0) {
        buffers.push(...excludedBuffers);
        console.log(`🔄 제외된 버퍼 ${excludedBuffers.length}명을 버퍼 리스트 뒤로 추가`);
        excludedBuffers.length = 0; // 배열 초기화
      }
    } else {
      // 최소 구성 조건을 만족하지 않으면 파티 구성하지 않음
      console.log(`⚠️ 파티 구성 조건 미달: 딜러=${hasDealer}, 버퍼=${hasBuffer}, 파티 구성 취소`);
      console.log(`   현재 파티 구성: ${party.map(char => `${char.characterName}(${isBuffer(char) ? '버퍼' : '딜러'})`).join(', ')}`);
      // 사용된 캐릭터들을 원래 리스트로 되돌리기
      party.forEach(char => {
        if (isBuffer(char)) {
          buffers.unshift(char);
          console.log(`   🔄 버퍼 복원: ${char.characterName} → 버퍼 리스트`);
        } else {
          dealers.unshift(char);
          console.log(`   🔄 딜러 복원: ${char.characterName} → 딜러 리스트`);
        }
        usedCharacters.delete(char.characterId);
      });
    }
  }
  
  // 3. 최종 결과 요약
  console.log('\n=== Basic 자동 파티 생성 완료 ===');
  console.log(`총 생성된 파티 수: ${newParties.length}`);
  console.log(`남은 딜러 수: ${dealers.length}`);
  console.log(`남은 버퍼 수: ${buffers.length}`);
  
  if (newParties.length > 0) {
    console.log('\n📊 생성된 파티 상세 정보:');
    newParties.forEach((party, index) => {
      const partyDealers = party.filter(char => !isBuffer(char));
      const partyBuffers = party.filter(char => isBuffer(char));
      const totalCombatPower = partyDealers.reduce((sum, char) => sum + (char.totalDamage || 0), 0);
      const totalBuffPower = partyBuffers.reduce((sum, char) => sum + (char.buffPower || 0), 0);
      const coefficient = (totalCombatPower / 100000000) * (totalBuffPower / 1000000);
      
      console.log(`파티 ${index + 1}:`);
      console.log(`  딜러: ${partyDealers.map(d => `${d.characterName}(${(d.totalDamage || 0).toLocaleString()})`).join(', ')}`);
      console.log(`  버퍼: ${partyBuffers.map(b => `${b.characterName}(${(b.buffPower || 0).toLocaleString()})`).join(', ')}`);
      console.log(`  총 전투력: ${(totalCombatPower / 100000000).toFixed(1)}억`);
      console.log(`  총 버프력: ${(totalBuffPower / 10000).toFixed(0)}만`);
      console.log(`  파티 계수: ${coefficient.toFixed(2)}`);
      console.log(`  모험단: [${party.map(char => char.adventureName).filter(adv => adv && adv !== 'N/A').join(', ')}]`);
    });
  }
  
  if (dealers.length > 0 || buffers.length > 0) {
    console.log('\n📋 파티 구성에 사용되지 않은 남은 캐릭터:');
    if (dealers.length > 0) {
      console.log(`딜러: ${dealers.map(d => `${d.characterName}(${(d.totalDamage || 0).toLocaleString()}, ${d.adventureName})`).join(', ')}`);
    }
    if (buffers.length > 0) {
      console.log(`버퍼: ${buffers.map(b => `${b.characterName}(${(b.buffPower || 0).toLocaleString()}, ${b.adventureName})`).join(', ')}`);
    }
  }
  
  console.log('=== 로그 끝 ===\n');
  
  // 4. 결과 표시
  if (newParties.length > 0) {
    parties.value = newParties;
    console.log('파티 생성 완료:', newParties);
  } else {
    // 파티 생성 실패 시 alert로 원인 설명
    let failureReason = '';
    if (dealers.length === 0) {
      failureReason = '사용 가능한 딜러가 없습니다.';
    } else if (buffers.length === 0) {
      failureReason = '사용 가능한 버퍼가 없습니다.';
    } else {
      failureReason = '모험단 중복으로 인해 파티를 구성할 수 없습니다. 다양한 모험단의 캐릭터가 필요합니다.';
    }
    
    alert(`파티 자동 생성에 실패했습니다.\n\n원인: ${failureReason}`);
    error.value = '파티를 구성할 수 없습니다.';
  }
};

// 귓속말용 파티 복사 (딜러/버프력만 구분해서 복사)
const copyPartyForWhisper = async () => {
  try {
    let whisperText = '';
    
    parties.value.forEach((party, partyIndex) => {
      if (party.length > 0 && party.some(slot => slot !== null)) {
        const partyStats = party
          .filter(slot => slot !== null)
          .map(character => {
            if (character.totalDamage > 0) {
              // 딜러: 억 단위로 변환
              return (character.totalDamage / 100000000).toFixed(1);
            } else {
              // 버퍼: 만 단위로 변환
              return (character.buffPower / 10000).toFixed(0);
            }
          });
        
        whisperText += partyStats.join(', ') + '\n';
      }
    });
    
    if (whisperText.trim()) {
      await navigator.clipboard.writeText(whisperText.trim());
      alert(`귓속말용 파티 정보가 클립보드에 복사되었습니다!\n\n복사된 내용:\n${whisperText.trim()}`);
    } else {
      error.value = '복사할 파티 구성이 없습니다.';
    }
  } catch (err) {
    console.error('귓속말용 파티 복사 실패:', err);
    error.value = '귓속말용 파티 복사에 실패했습니다.';
  }
};



// 유틸리티 함수들
const isCharacterInParty = (characterId: string): boolean => {
  return parties.value.some(party => 
    party.some(member => member && member.characterId === characterId)
  );
};

// 업둥 캐릭터 여부 확인 (선택된 던전에 따라)
const isHelperCharacter = (character: any): boolean => {
  if (!selectedDungeon.value) return false;
  
  switch (selectedDungeon.value) {
    case 'nabel':
      return character.isSkipNabel === true;
    case 'venus':
      return character.isSkipVenus === true;
    case 'fog':
      return character.isSkipFog === true;
    case 'twilight':
      return false; // 이내 황혼전은 아직 업둥 기능 없음
    default:
      return false;
  }
};

const getPartyTotalDamage = (party: any[]): number => {
  return party.reduce((total, member) => total + (member?.totalDamage || 0), 0);
};

const getPartyTotalBuffPower = (party: any[]): number => {
  return party.reduce((total, member) => total + (member?.buffPower || 0), 0);
};

// 파티 총 전투력을 억 단위로 표시
const getPartyTotalDamageInBillion = (party: any[]): string => {
  const totalDamage = getPartyTotalDamage(party);
  return (totalDamage / 100000000).toFixed(1);
};

// 파티 총 버프력을 만 단위로 표시
const getPartyTotalBuffPowerInTenThousand = (party: any[]): string => {
  const totalBuffPower = getPartyTotalBuffPower(party);
  return (totalBuffPower / 10000).toFixed(0);
};

// 파티 계수: (총 전투력 / 억) * (버프력 / 백만)
const getPartyCoefficient = (party: any[]): string => {
  const totalDamage = getPartyTotalDamage(party);
  const totalBuffPower = getPartyTotalBuffPower(party);
  const damageInBillion = totalDamage / 100000000; // 억 단위
  const buffPowerInMillion = totalBuffPower / 1000000; // 백만 단위
  const coefficient = damageInBillion * buffPowerInMillion;
  return coefficient.toFixed(2); // 소수점 2자리까지 표시
};

// 슬롯 역할 표시 (버퍼, 딜러, 딜러, 딜러)
// slotIndex는 1부터 4까지의 값 (HTML에서 사용)
const getSlotRole = (slotIndex: number): string => {
  switch (slotIndex) {
    case 1: return '버퍼';    // 슬롯 1번
    case 2: return '딜러';   // 슬롯 2번
    case 3: return '딜러';   // 슬롯 3번
    case 4: return '딜러';   // 슬롯 4번
    default: return slotIndex.toString();
  }
};

// 모험단별 딜러 수 계산
const getDealerCount = (adventureName: string): number => {
  return getFilteredCharacters(adventureName).filter(char => !isBuffer(char)).length;
};

// 모험단별 버퍼 수 계산
const getBufferCount = (adventureName: string): number => {
  return getFilteredCharacters(adventureName).filter(char => isBuffer(char)).length;
};

const getDungeonClearClass = (character: any): string => {
  const cleared = getDungeonClearStatus(character);
  return cleared ? 'cleared' : 'not-cleared';
};

const getDungeonClearText = (character: any): string => {
  const cleared = getDungeonClearStatus(character);
  return cleared ? '클리어' : '';
};

const getDungeonClearStatus = (character: any): boolean => {
  switch (selectedDungeon.value) {
    case 'nabel':
      return character.dungeonClearNabel || false;
    case 'venus':
      return character.dungeonClearVenus || false;
    case 'fog':
      return character.dungeonClearFog || false;
    case 'twilight':
      return character.dungeonClearTwilight || false;
    default:
      return false;
  }
};

const formatNumber = (num: number): string => {
  if (num >= 100000000) {
    return (num / 100000000).toFixed(1) + '억';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만';
  }
  return num.toLocaleString();
};

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement;
  img.style.display = 'none';
};

// 선택된 모험단들 최신화
const refreshSelectedAdventures = async () => {
  if (selectedAdventures.value.length === 0) {
    error.value = '선택된 모험단이 없습니다.';
    return;
  }
  
  try {
    refreshingAdventures.value = true;
    error.value = '';
    
    const results = [];
    let totalSuccess = 0;
    let totalFail = 0;
    
    // 각 모험단별로 순차 최신화
    for (const adventureName of selectedAdventures.value) {
      try {
        console.log(`모험단 '${adventureName}' 최신화 시작...`);
        
        const response = await apiFetch(`/characters/adventure/${encodeURIComponent(adventureName)}/refresh`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          if (data.success) {
            const adventureResult = data.data;
            totalSuccess += adventureResult.successCount || 0;
            totalFail += adventureResult.failCount || 0;
            results.push(`${adventureName}: ✅ 성공 ${adventureResult.successCount}개, 실패 ${adventureResult.failCount}개`);
            console.log(`모험단 '${adventureName}' 최신화 완료:`, adventureResult);
          } else {
            results.push(`${adventureName}: ❌ ${data.message}`);
            console.error(`모험단 '${adventureName}' 최신화 실패:`, data.message);
          }
        } else {
          results.push(`${adventureName}: ❌ 서버 오류`);
          console.error(`모험단 '${adventureName}' 최신화 서버 오류:`, response.status);
        }
      } catch (err) {
        results.push(`${adventureName}: ❌ 요청 실패`);
        console.error(`모험단 '${adventureName}' 최신화 예외:`, err);
      }
    }
    
    
    // 캐릭터 목록 다시 로드
    await loadCharactersFromAPI();
    
  } catch (err) {
    console.error('모험단 최신화 오류:', err);
    error.value = '모험단 최신화 중 오류가 발생했습니다.';
  } finally {
    refreshingAdventures.value = false;
  }
};

const debugLocalStorage = async () => {
  console.log('=== 파티 구성 디버깅 시작 ===');
  
  // 1. LocalStorage 검색 기록 확인
  const searchHistory = JSON.parse(localStorage.getItem('df_search_history') || '[]');
  console.log('전체 검색 기록:', searchHistory);
  console.log('검색 기록 개수:', searchHistory.length);
  
  // 2. 모험단 필터링 확인
  const validAdventures = new Set<string>();
  const invalidAdventures = new Set<string>();
  
  searchHistory.forEach((record: any) => {
    if (record.adventureName) {
      if (record.adventureName !== 'N/A' && record.adventureName !== '모험단 정보 없음') {
        validAdventures.add(record.adventureName);
      } else {
        invalidAdventures.add(record.adventureName);
      }
    }
  });
  
  console.log('유효한 모험단:', Array.from(validAdventures));
  console.log('제외된 모험단:', Array.from(invalidAdventures));
  
  // 3. 백엔드 API 테스트
  const apiResults = [];
  for (const adventureName of validAdventures) {
    try {
      console.log(`API 테스트: ${adventureName}`);
      const response = await apiFetch(`/characters/adventure/${encodeURIComponent(adventureName)}`);
      const data = await response.json();
      
      apiResults.push({
        adventureName,
        status: response.status,
        success: data.success,
        characterCount: data.data?.characters?.length || 0,
        message: data.message
      });
      
      console.log(`${adventureName} API 결과:`, data);
    } catch (err) {
      apiResults.push({
        adventureName,
        status: 'ERROR',
        success: false,
        characterCount: 0,
        message: err.toString()
      });
      console.error(`${adventureName} API 오류:`, err as Error);
    }
  }
  
  // 4. 현재 allCharacters 상태 확인
  console.log('현재 allCharacters:', allCharacters.value);
  console.log('현재 allCharacters 길이:', allCharacters.value.length);
  
  // 5. 결과 요약
  const summary = {
    searchRecordCount: searchHistory.length,
    validAdventureCount: validAdventures.size,
    invalidAdventureCount: invalidAdventures.size,
    validAdventures: Array.from(validAdventures),
    invalidAdventures: Array.from(invalidAdventures),
    apiResults,
    currentCharacterCount: allCharacters.value.length
  };
  
  console.log('=== 디버깅 요약 ===', summary);
  
  // 사용자에게 알림으로도 표시
  alert(`파티 구성 디버깅 정보:\n\n` +
        `검색 기록: ${searchHistory.length}개\n` +
        `유효한 모험단: ${validAdventures.size}개\n` +
        `제외된 모험단: ${invalidAdventures.size}개\n` +
        `현재 캐릭터: ${allCharacters.value.length}개\n\n` +
        `유효한 모험단 목록:\n${Array.from(validAdventures).join('\n') || '없음'}\n\n` +
        `제외된 모험단 목록:\n${Array.from(invalidAdventures).join('\n') || '없음'}\n\n` +
        `자세한 내용은 브라우저 콘솔을 확인하세요.`);
};
</script>

<style scoped>
.party-formation {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.party-formation h2 {
  margin-bottom: 30px;
  text-align: center;
  color: #333;
  font-size: 28px;
  font-weight: 700;
}

.party-formation h3 {
  margin: 0 0 20px 0;
  padding: 15px 20px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 8px;
  border: 2px solid #dee2e6;
  color: #495057;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}

.top-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border-radius: 12px;
  border: 2px solid #90caf9;
  box-shadow: 0 4px 12px rgba(144, 202, 249, 0.3);
  flex-wrap: wrap;
  justify-content: space-between;
}

.first-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  width: 100%;
}

.second-row {
  width: 100%;
}

.search-section, .adventures-section {
  flex: 1;
  min-width: 0;
}

.selected-adventures-section {
  width: 100%;
}

.search-section .form-group {
  margin-bottom: 0;
}

.search-section label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #495057;
  font-size: 14px;
}

.party-controls {
  display: grid;
  grid-template-columns: 1fr 3fr; /* 좌우 1:3 비율 */
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border-radius: 12px;
  border: 2px solid #dee2e6;
}

/* 반응형: 화면이 작아지면 세로 배치 */
@media (max-width: 1200px) {
  .party-controls {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}

/* 좌측: 파티 구성 규칙 섹션 */
.party-rules-section {
  display: flex;
  flex-direction: column;
}

.party-info {
  flex: 1;
}

.party-info p {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.party-info ul {
  margin: 0;
  padding-left: 20px;
}

.party-info li {
  margin-bottom: 8px;
  font-size: 12px;
  color: #555;
  line-height: 1.4;
}

/* 우측 하단: 파티 구성 옵션 섹션 */
.party-options-section {
  flex: 3; /* 위아래 1:3 비율에서 3/3 차지 */
}

/* 파티 구성 옵션 스타일 */
.party-options-container {
  margin-bottom: 20px;
}

.party-options-box {
  padding: 20px;
  background: linear-gradient(135deg, #ffffff, #f8f9fa);
  border-radius: 12px;
  border: 2px solid #dee2e6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.party-options-box p {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.option-selector {
  display: flex;
  align-items: center;
  gap: 15px;
}

.option-selector label {
  font-weight: 600;
  color: #495057;
  font-size: 14px;
  white-space: nowrap;
}

.option-dropdown {
  padding: 10px 15px;
  border: 2px solid #ced4da;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 200px;
}

.option-dropdown:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.option-dropdown:hover {
  border-color: #007bff;
}

.option-description {
  margin-top: 10px;
  text-align: center;
}

/* Advanced 모드 설명 */
.mode-description {
  margin-top: 10px;
  text-align: center;
  color: #6c757d;
}

/* Advanced 옵션들 */
.advanced-options {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #dee2e6;
}

.advanced-section {
  margin-bottom: 20px;
}

.advanced-section h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #495057;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #495057;
  cursor: pointer;
  user-select: none;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.checkbox-label:hover {
  color: #007bff;
}

.option-description small {
  color: #6c757d;
  font-style: italic;
}

/* 우측: 컨트롤 및 옵션 영역 */
.party-controls-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 우측 상단: 컨트롤 버튼들 섹션 */
.control-buttons-section {
  flex: 1;
}

.control-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;
  overflow-x: auto;
  padding: 4px 0;
}

/* 반응형: 작은 화면에서 버튼들을 세로로 배치 */
@media (max-width: 768px) {
  .control-buttons {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
  
  .control-btn {
    min-width: auto;
    width: 100%;
  }
}

.control-btn {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 44px;
  min-width: 110px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.control-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.control-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 개별 버튼 스타일 */
.refresh-btn {
  background: linear-gradient(135deg, #17a2b8, #138496);
  color: white;
}

.refresh-btn:hover {
  background: linear-gradient(135deg, #138496, #117a8b);
}

.clear-btn {
  background: linear-gradient(135deg, #6c757d, #5a6268);
  color: white;
}

.clear-btn:hover {
  background: linear-gradient(135deg, #5a6268, #495057);
}

.optimize-btn {
  background: linear-gradient(135deg, #007bff, #0056b3);
  color: white;
}

.optimize-btn:hover {
  background: linear-gradient(135deg, #0056b3, #004085);
}

.copy-btn {
  background: linear-gradient(135deg, #28a745, #1e7e34);
  color: white;
}

.copy-btn:hover {
  background: linear-gradient(135deg, #1e7e34, #155724);
}

.auto-btn {
  background: linear-gradient(135deg, #fd7e14, #e55a00);
  color: white;
  font-weight: 700;
  min-width: 120px;
  box-shadow: 0 3px 6px rgba(253, 126, 20, 0.3);
}

.auto-btn:hover {
  background: linear-gradient(135deg, #e55a00, #cc4a00);
  box-shadow: 0 4px 8px rgba(253, 126, 20, 0.4);
}

/* 모험단 검색 섹션 스타일 */
.adventure-search-section {
  width: 100%;
  margin-bottom: 20px;
}

.adventure-search-section h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 18px;
}

.search-form {
  display: flex;
  gap: 15px;
  align-items: end;
  flex-wrap: wrap;
}

.search-form .form-group {
  margin-bottom: 0;
}

.search-form label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #495057;
  font-size: 14px;
}

.search-form select,
.search-form input {
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.search-btn {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  height: 36px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

.search-btn:hover:not(:disabled) {
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
  gap: 8px;
}

.recent-adventures label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.recent-adventure-select {
  padding: 8px 12px;
  border: 2px solid #90caf9;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 150px;
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

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: bold;
  color: #333;
}

.form-group select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  min-width: 200px;
}

.multi-select-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.selected-adventures {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
}

.placeholder {
  color: #999;
  font-style: italic;
}

.selected-adventure {
  display: flex;
  align-items: center;
  gap: 5px;
  background: linear-gradient(135deg, #1976d2, #1565c0);
  color: white;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 14px;
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.2);
}

.remove-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.party-controls {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.control-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.party-info {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
}

.party-info p {
  margin: 0 0 10px 0;
  font-weight: bold;
  color: #495057;
}

.party-info ul {
  margin: 0;
  padding-left: 20px;
}

.party-info li {
  margin-bottom: 5px;
  color: #6c757d;
  font-size: 13px;
}

.auto-btn {
  background: linear-gradient(135deg, #28a745, #20c997);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.3);
}

.auto-btn:hover {
  background: linear-gradient(135deg, #20c997, #28a745);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.clear-btn {
  background: linear-gradient(135deg, #6c757d, #5a6268);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

.clear-btn:hover {
  background: linear-gradient(135deg, #5a6268, #6c757d);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(108, 117, 125, 0.4);
}

.optimize-btn {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.3);
}

.optimize-btn:hover {
  background: linear-gradient(135deg, #1565c0, #1976d2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.4);
}

.main-content {
  display: flex;
  gap: 20px;
  min-height: 600px;
}

/* 반응형 레이아웃: 화면이 작아지면 세로 배치 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .left-panel,
  .right-panel {
    flex: none;
    width: 100%;
  }
  
  .right-panel {
    max-height: none;
  }
}

/* 더 작은 화면에서의 추가 최적화 */
@media (max-width: 768px) {
  .main-content {
    gap: 10px;
  }
  
  .left-panel,
  .right-panel {
    padding: 15px;
  }
  
  .party-tables {
    gap: 15px;
  }
  
  .party-table {
    padding: 10px;
  }
  
  .party-title-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }
}

.left-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.party-tables {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.party-table {
  border: 2px solid #e5e5e5;
  border-radius: 8px;
  padding: 15px;
  background: #f8f9fa;
}

.party-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 15px;
  padding: 12px;
  border-radius: 8px;
}

.party-title-stats {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: nowrap;
  justify-content: center;
  white-space: nowrap;
}

.party-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin-right: 10px;
}

.party-combat-power {
  font-size: 14px;
  font-weight: 600;
  color: #007bff;
  padding: 4px 8px;
}

.party-buff-power {
  font-size: 14px;
  font-weight: 600;
  color: #28a745;
  padding: 4px 8px;
}

.party-coefficient {
  font-size: 14px;
  font-weight: 700;
  color: #dc3545;
  padding: 4px 8px;
}

.party-separator {
  color: #6c757d;
  font-weight: 400;
  margin: 0 5px;
}

.character-counts {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #6c757d;
}

.dealer-count {
  color: #007bff;
  font-weight: 600;
}

.buffer-count {
  color: #28a745;
  font-weight: 600;
}

/* 캐릭터 섹션 스타일 */
.character-section {
  margin-bottom: 20px;
}

.section-header {
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 10px;
}

.section-header h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.dealer-header {
  background: linear-gradient(135deg, #007bff, #0056b3);
}

.buffer-header {
  background: linear-gradient(135deg, #28a745, #1e7e34);
}

.section-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
  margin-bottom: 15px;
}

/* 슬롯 텍스트 스타일 개선 */
.slot-number {
  font-size: 14px;
  font-weight: 600;
  color: #495057;
  display: block;
  margin-bottom: 2px;
  text-align: center;
}

.slot-text {
  font-size: 10px;
  color: #6c757d;
  text-align: center;
  line-height: 1.1;
  margin: 0;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2px;
  padding: 0;
}

.party-slot {
  min-height: 120px;
  border: 2px dashed #ccc;
  border-radius: 4px;
  position: relative;
  background: white;
  padding: 4px;
  margin: 0;
}

.party-slot.filled {
  border-style: solid;
  border-color: #28a745;
}

.character-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  border-radius: 6px;
  background: white;
  border: 1px solid #e5e5e5;
  min-height: 120px;
  height: auto;
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

/* 반응형 캐릭터 카드: 작은 화면에서 크기 조정 */
@media (max-width: 768px) {
  .character-card {
    min-height: 100px;
    padding: 6px;
  }
  
  .character-avatar {
    width: 35px;
    height: 35px;
  }
  
  .avatar-placeholder {
    font-size: 16px;
  }
  
  .character-name {
    font-size: 12px;
  }
  
  .adventure-name {
    font-size: 10px;
  }
  
  .stat {
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .character-card {
    min-height: 90px;
    padding: 5px;
  }
  
  .character-avatar {
    width: 30px;
    height: 30px;
  }
  
  .avatar-placeholder {
    font-size: 14px;
  }
  
  .character-name {
    font-size: 11px;
  }
  
  .adventure-name {
    font-size: 9px;
  }
  
  .stat {
    font-size: 9px;
  }
}

.character-card.draggable {
  cursor: move;
  transition: transform 0.2s ease;
}

.character-card.draggable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.character-card.in-use {
  opacity: 0.5;
  background: #f5f5f5;
}

.character-card.is-helper {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
  border: 2px solid #f39c12;
  box-shadow: 0 2px 8px rgba(243, 156, 18, 0.3);
}

.helper-badge {
  background: #f39c12;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  text-align: center;
  margin-top: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.character-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 5px;
}

.character-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 18px;
}

.character-info {
  text-align: center;
  width: 100%;
}

.character-name {
  font-weight: bold;
  font-size: 13px;
  margin-bottom: 3px;
  color: #333;
}

.adventure-name {
  font-size: 11px;
  color: #666;
  margin-bottom: 5px;
}

.character-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 5px;
}

.stat {
  font-size: 11px;
  color: #555;
}

.buffer-stat {
  color: #28a745;
  font-weight: bold;
}

.dealer-stat {
  color: #007bff;
  font-weight: bold;
}

.dungeon-status {
  margin-bottom: 3px;
}

.cleared {
  color: #28a745;
  font-weight: bold;
  font-size: 11px;
}

.not-cleared {
  color: #dc3545;
  font-weight: bold;
  font-size: 11px;
}

.character-fame {
  font-size: 11px;
  color: #666;
}

.empty-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.slot-placeholder {
  text-align: center;
}

.slot-number {
  display: block;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.slot-text {
  font-size: 13px;
}

.remove-from-party {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  cursor: pointer;
  font-size: 12px;
}

.add-party-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
}

.right-panel {
  flex: 1;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  /* max-height와 overflow-y 제거하여 스크롤바 없이 아래로 쭉 나오도록 수정 */
}

.adventure-panels {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.adventure-panel {
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  overflow: hidden;
}

/* 반응형 모험단 패널: 작은 화면에서 간격 조정 */
@media (max-width: 768px) {
  .adventure-panels {
    gap: 15px;
  }
  
  .adventure-header {
    padding: 8px 12px;
  }
  
  .adventure-header h4 {
    font-size: 16px;
  }
  
  .character-counts {
    font-size: 11px;
    gap: 10px;
  }
}

@media (max-width: 480px) {
  .adventure-panels {
    gap: 10px;
  }
  
  .adventure-header {
    padding: 6px 10px;
  }
  
  .adventure-header h4 {
    font-size: 14px;
  }
  
  .character-counts {
    font-size: 10px;
    gap: 8px;
  }
}

.adventure-header {
  background: #f8f9fa;
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.adventure-header h4 {
  margin: 0;
  color: #333;
}

.character-count {
  background: #007bff;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.character-list {
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  /* max-height와 overflow-y 제거하여 스크롤바 없이 아래로 쭉 나오도록 수정 */
}

/* 반응형 그리드: 화면 크기에 따라 열 수 조정 */
@media (max-width: 1400px) {
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
    gap: 8px;
  }
}

@media (max-width: 1200px) {
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
    gap: 6px;
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 5px;
    padding: 5px;
  }
}

.no-selection {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.search-link {
  display: inline-block;
  background: #007bff;
  color: white;
  text-decoration: none;
  padding: 12px 24px;
  border-radius: 6px;
  margin-top: 20px;
}

.loading, .error {
  text-align: center;
  padding: 20px;
  margin: 20px 0;
}

.error {
  background: #f8d7da;
  color: #721c24;
  border-radius: 4px;
}

.adventure-select-container {
  position: relative;
  width: 100%;
}

.adventure-select {
  width: 100%;
  padding: 8px 12px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 0;
}

.no-adventures-message {
  margin-top: 10px;
  padding: 10px;
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
  color: #856404;
  font-size: 0.9em;
}

.no-adventures-message p {
  margin: 5px 0;
}

.no-adventures-message a {
  color: #007bff;
  text-decoration: none;
}

.no-adventures-message a:hover {
  text-decoration: underline;
}

/* 파티에 들어간 캐릭터 스타일 */
.character-card.in-use {
  opacity: 0.6;
  background: #f8f9fa;
  border: 2px dashed #6c757d;
  cursor: not-allowed;
}

.character-card.in-use .character-name {
  color: #6c757d;
  text-decoration: line-through;
}

.character-card.in-use .character-stats {
  color: #6c757d;
}

.character-card.in-use .character-fame {
  color: #6c757d;
}

/* 드래그 가능한 캐릭터 스타일 */
.character-card.draggable {
  cursor: grab;
  transition: all 0.2s ease;
}

.character-card.draggable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.character-card.draggable:active {
  cursor: grabbing;
}

/* 파티 포함 배지 스타일 */
.in-party-badge {
  background: #6c757d;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: bold;
  text-align: center;
  margin-top: 4px;
}

/* 파티 포함 배지 - 카드 왼쪽 상단에 배치 */
.in-party-badge-left {
  position: absolute;
  top: 8px;
  left: 8px;
  color: #6c757d;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  z-index: 10;
}

.adventure-debug-info {
  margin-top: 5px;
  color: #6c757d;
  font-size: 0.8em;
}

.debug-btn {
  background: #007bff;
  color: white;
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  margin-top: 10px;
}

.debug-btn-small {
  background: none;
  border: none;
  color: #6c757d;
  cursor: pointer;
  font-size: 1.2em;
  padding: 0 5px;
}

.refresh-section {
  margin: 15px 0;
  padding: 15px;
  background: #e3f2fd;
  border: 1px solid #90caf9;
  border-radius: 8px;
  text-align: center;
}

.refresh-adventures-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: background 0.3s ease;
  margin-bottom: 8px;
}

.refresh-adventures-btn:hover:not(:disabled) {
  background: #218838;
}

.refresh-adventures-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.refresh-info {
  display: block;
  color: #1976d2;
  font-size: 12px;
  margin-top: 5px;
}

/* 새로운 UI 요소들을 위한 스타일 */
.dungeon-selection-container {
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  border-radius: 12px;
  border: 2px solid #90caf9;
  box-shadow: 0 4px 12px rgba(144, 202, 249, 0.3);
}

.dungeon-selection-row {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.dungeon-btn {
  padding: 15px 30px;
  font-size: 16px;
  font-weight: 600;
  border: 2px solid #90caf9;
  border-radius: 12px;
  background: linear-gradient(135deg, #e3f2fd, #f3e5f5);
  color: #1976d2;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 100px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(144, 202, 249, 0.2);
}

.dungeon-btn:hover {
  background: linear-gradient(135deg, #f3e5f5, #e3f2fd);
  border-color: #1976d2;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(144, 202, 249, 0.3);
}

.dungeon-btn.active {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  border-color: #1976d2;
  color: white;
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.4);
}

.dungeon-btn.nabel-normal-btn.active {
  background: #dc3545;
  border-color: #dc3545;
  box-shadow: 0 4px 8px rgba(220, 53, 69, 0.3);
}

.dungeon-btn.nabel-hard-btn.active {
  background: #dc3545;
  border-color: #dc3545;
  box-shadow: 0 4px 8px rgba(220, 53, 69, 0.3);
}

.dungeon-btn.venus-btn.active {
  background: #28a745;
  border-color: #28a745;
  box-shadow: 0 4px 8px rgba(40, 167, 69, 0.3);
}

.dungeon-btn.fog-btn.active {
  background: #17a2b8;
  border-color: #17a2b8;
  box-shadow: 0 4px 8px rgba(23, 162, 184, 0.3);
}

.dungeon-btn.twilight-btn.active {
  background: #6f42c1;
  border-color: #6f42c1;
  box-shadow: 0 4px 8px rgba(111, 66, 193, 0.3);
}

.search-row {
  display: flex;
  margin-bottom: 20px;
  align-items: end;
  justify-content: center;
}

.search-section {
  display: flex;
  width: 100%;
  align-items: end;
}

.search-input-container {
  display: flex;
  width: 100%;
  gap: 10px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  min-width: 0;
  max-width: calc(100% - 80px);
}

.full-width {
  width: 100%;
}

.search-section .form-group {
  margin-bottom: 0;
}

.add-section .form-group {
  margin-bottom: 0;
}

.add-btn {
  background: linear-gradient(135deg, #28a745, #20c997);
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.3);
}

.add-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #20c997, #28a745);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.add-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.adventures-list-row {
  margin-bottom: 20px;
}

.adventures-list-row .multi-select-container {
  border: 2px solid #dee2e6;
  border-radius: 8px;
  padding: 15px;
  background: #f8f9fa;
  min-height: 80px;
}

.selected-adventures-row {
  margin-bottom: 20px;
}

.selected-adventures-row .selected-adventures {
  border: 2px solid #dee2e6;
  border-radius: 8px;
  padding: 15px;
  background: #f8f9fa;
  min-height: 80px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

/* ========================================
   세밀한 반응형 디자인 - 디바이스별 최적화
   ======================================== */

/* 대형 데스크탑 (1920px 이상) */
@media screen and (min-width: 1920px) {
  .party-formation-container {
    max-width: 1600px;
    padding: 30px;
  }
  
  .party-slots {
    grid-template-columns: repeat(6, 1fr);
    gap: 25px;
  }
  
  .dungeon-btn {
    padding: 18px 30px;
    font-size: 18px;
    min-width: 120px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 20px;
  }
}

/* 데스크탑 (1600px ~ 1919px) */
@media screen and (min-width: 1600px) and (max-width: 1919px) {
  .party-formation-container {
    max-width: 1400px;
    padding: 25px;
  }
  
  .party-slots {
    grid-template-columns: repeat(5, 1fr);
    gap: 22px;
  }
}

/* 노트북 (1200px ~ 1599px) */
@media screen and (min-width: 1200px) and (max-width: 1599px) {
  .party-formation-container {
    max-width: 1200px;
    padding: 20px;
  }
  
  .party-slots {
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 18px;
  }
}

/* 태블릿 가로 (1024px ~ 1199px) */
@media screen and (min-width: 1024px) and (max-width: 1199px) {
  .party-formation-container {
    padding: 18px;
  }
  
  .main-content {
    flex-direction: column;
    gap: 25px;
  }
  
  .party-slots {
    grid-template-columns: repeat(3, 1fr);
    gap: 18px;
  }
  
  .dungeon-selection-row {
    flex-wrap: wrap;
    gap: 12px;
    justify-content: center;
  }
  
  .dungeon-btn {
    padding: 14px 24px;
    font-size: 15px;
    min-width: 100px;
  }
  
  .top-bar {
    flex-direction: row;
    gap: 25px;
  }
  
  .first-row {
    flex-direction: row;
    gap: 20px;
  }
  
  .search-section, .adventures-section {
    min-width: auto;
    width: 100%;
  }
  
  .selected-adventures-section {
    width: 100%;
  }
  
  .party-controls {
    flex-direction: row;
    gap: 25px;
  }
  
  .control-buttons {
    justify-content: flex-start;
  }
  
  .party-info {
    margin-right: 20px;
    margin-bottom: 0;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 16px;
  }
}

/* 태블릿 세로 (768px ~ 1023px) */
@media screen and (min-width: 768px) and (max-width: 1023px) {
  .party-formation-container {
    padding: 15px;
  }
  
  .main-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .party-slots {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .dungeon-selection-row {
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;
  }
  
  .dungeon-btn {
    padding: 12px 20px;
    font-size: 14px;
    min-width: 80px;
  }
  
  .top-bar {
    flex-direction: column;
    gap: 20px;
  }
  
  .first-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .search-section, .adventures-section {
    min-width: auto;
    width: 100%;
  }
  
  .selected-adventures-section {
    width: 100%;
  }
  
  .party-controls {
    flex-direction: column;
    gap: 20px;
  }
  
  .control-buttons {
    justify-content: center;
  }
  
  .party-info {
    margin-right: 0;
    margin-bottom: 20px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
    gap: 15px;
  }
}

/* 중형 모바일 (600px ~ 767px) */
@media screen and (min-width: 600px) and (max-width: 767px) {
  .party-formation-container {
    padding: 12px;
  }
  
  .party-slots {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  
  .dungeon-btn {
    padding: 11px 18px;
    font-size: 13px;
    min-width: 70px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;
  }
}

/* 소형 모바일 (480px ~ 599px) */
@media screen and (min-width: 480px) and (max-width: 599px) {
  .party-formation-container {
    padding: 10px;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .dungeon-btn {
    padding: 10px 16px;
    font-size: 12px;
    min-width: 60px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
    gap: 10px;
  }
}

/* 초소형 모바일 (320px ~ 479px) */
@media screen and (min-width: 320px) and (max-width: 479px) {
  .party-formation-container {
    padding: 8px;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .dungeon-btn {
    padding: 8px 12px;
    font-size: 11px;
    min-width: 50px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 8px;
  }
}

/* 매우 작은 화면 (400px 이하) 추가 최적화 */
@media screen and (max-width: 400px) {
  .party-formation-container {
    padding: 5px;
  }
  
  .party-slots {
    gap: 10px;
  }
  
  .dungeon-btn {
    padding: 6px 10px;
    font-size: 10px;
    min-width: 45px;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
    gap: 6px;
  }
  
  .main-content {
    gap: 15px;
  }
  
  .top-bar {
    gap: 15px;
  }
}

/* 터치 디바이스 최적화 */
@media (hover: none) and (pointer: coarse) {
  .dungeon-btn {
    min-height: 44px;
  }
  
  .character-item {
    min-height: 44px;
  }
  
  .control-button {
    min-height: 44px;
  }
}
</style> 