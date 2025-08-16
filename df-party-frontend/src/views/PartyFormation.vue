<template>
  <div class="party-formation">
    <h2>3. 파티 구성</h2>
    
    <!-- 성공/에러 메시지 -->
    <div v-if="successMessage" class="success-message">
      {{ successMessage }}
    </div>
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
    
    <!-- 상단 선택 바 -->
    <div class="top-bar">
    <!-- 던전 선택 -->
      <div class="form-group">
        <label for="dungeonSelect">던전 선택:</label>
        <select id="dungeonSelect" v-model="selectedDungeon" @change="onDungeonChange">
          <option value="">던전을 선택하세요</option>
          <option value="nabel">나벨</option>
          <option value="venus">베누스</option>
          <option value="fog">안개신</option>
          <option value="twilight">황혼전</option>
        </select>
      </div>
      
      <!-- 나벨 던전 난이도 선택 -->
      <div v-if="selectedDungeon === 'nabel'" class="form-group">
        <label for="nabelDifficulty">나벨 난이도:</label>
        <div class="difficulty-buttons">
          <button 
            @click="setNabelDifficulty('normal')" 
            :class="{ active: nabelDifficulty === 'normal' }"
            class="difficulty-btn normal-btn">
            일반
          </button>
          <button 
            @click="setNabelDifficulty('hard')" 
            :class="{ active: nabelDifficulty === 'hard' }"
            class="difficulty-btn hard-btn">
            하드
          </button>
                </div>
              </div>
              
      <!-- 모험단 멀티선택 -->
      <div class="form-group">
        <label for="adventureSelect">모험단 선택:</label>
        <div class="multi-select-container">
          <!-- 선택된 모험단들 표시 -->
          <div class="selected-adventures">
            <span v-if="selectedAdventures.length === 0" class="placeholder">모험단을 선택하세요</span>
            <div v-for="adventure in selectedAdventures" :key="adventure" class="selected-adventure">
              {{ adventure }}
              <button @click="removeAdventure(adventure)" class="remove-btn">×</button>
                </div>
              </div>
              
          <!-- 모험단 추가 드롭다운 -->
          <div class="adventure-select-container">
            <select @change="addAdventure" class="adventure-select">
              <option value="">모험단 추가...</option>
              <option v-for="adventure in availableAdventures.filter(a => !selectedAdventures.includes(a))" 
                      :key="adventure" :value="adventure">
                {{ adventure }}
              </option>
            </select>
            
            <!-- 모험단이 없을 때 안내 메시지 -->
            <div v-if="availableAdventures.length === 0" class="no-adventures-message">
              <p>⚠️ 아직 검색된 모험단이 없습니다.</p>
              <p>먼저 <router-link to="/character-search">캐릭터 검색</router-link>에서 캐릭터를 검색해주세요.</p>
              <button @click="debugLocalStorage" class="debug-btn">🔍 LocalStorage 디버그</button>
              </div>
              
            <!-- 모험단 목록 디버그 정보 -->
            <div v-if="availableAdventures.length > 0" class="adventure-debug-info">
              <small>사용 가능한 모험단: {{ availableAdventures.length }}개</small>
              <button @click="debugLocalStorage" class="debug-btn-small">🔍</button>
                </div>
              </div>
            </div>
          </div>
          
      <!-- 선택된 모험단 최신화 버튼 -->
      <div v-if="selectedAdventures.length > 0" class="refresh-section">
        <button @click="refreshSelectedAdventures" 
                :disabled="refreshingAdventures" 
                class="refresh-adventures-btn">
          {{ refreshingAdventures ? '최신화 중...' : '🔄 선택된 모험단 최신화' }}
        </button>
        <small class="refresh-info">선택된 {{ selectedAdventures.length }}개 모험단의 모든 캐릭터를 최신화합니다</small>
                </div>
              </div>
              
    <!-- 파티 구성 버튼들 -->
    <div v-if="selectedDungeon && selectedAdventures.length > 0" class="party-controls">
      <div class="party-info">
        <p><strong>파티 구성 규칙:</strong></p>
        <ul>
          <li>한 파티에 모험단 하나씩만 배치</li>
          <li>버퍼는 버프력, 딜러는 전투력으로 정렬</li>
          <li>각 모험단의 최고 스탯 캐릭터 1명 선택</li>
        </ul>
                </div>
      <button @click="autoGenerateParty" :disabled="loading" class="control-btn auto-btn">
        {{ loading ? '생성 중...' : '자동 파티 생성' }}
      </button>
      <button @click="clearParty" class="control-btn clear-btn">파티 초기화</button>
      <button @click="optimizeParty" :disabled="loading" class="control-btn optimize-btn">
        {{ loading ? '최적화 중...' : '파티 최적화' }}
      </button>
      <button @click="copyPartyToClipboard" class="control-btn copy-btn">📋 클립보드 복사</button>
              </div>

    <!-- 메인 컨텐츠 영역 -->
    <div v-if="selectedDungeon && selectedAdventures.length > 0" class="main-content">
      <!-- 좌측: 파티 테이블 -->
      <div class="left-panel">
        <h3>파티 구성</h3>
        <div class="party-tables">
          <div v-for="(party, index) in parties" :key="index" class="party-table">
            <div class="party-header">
              <h4>파티 {{ index + 1 }}</h4>
              <div class="party-info">
                <span class="party-stats">
                  총 전투력: {{ formatNumber(getPartyTotalDamage(party)) }} | 
                  총 버프력: {{ formatNumber(getPartyTotalBuffPower(party)) }}
                </span>
                <span v-if="selectedDungeon === 'nabel'" class="nabel-difficulty">
                  난이도: {{ nabelDifficulty === 'normal' ? '일반' : '하드' }}
                </span>
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
                      <div class="stat">전투력: {{ formatNumber(party[slotIndex - 1].totalDamage || 0) }}</div>
                      <div class="stat">버프력: {{ formatNumber(party[slotIndex - 1].buffPower || 0) }}</div>
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
                    <span class="slot-number">{{ slotIndex }}</span>
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
              <span class="character-count">{{ getFilteredCharacters(adventure).length }}명</span>
        </div>
            <div class="character-list">
              <!-- 디버깅: 필터링된 캐릭터 수 표시 -->
              <div v-if="getFilteredCharacters(adventure).length === 0" class="no-characters-debug">
                <small style="color: #dc3545;">
                  📋 디버깅: 필터링된 캐릭터 없음 
                  (전체: {{ allCharacters.value?.filter((c: any) => c.adventureName === adventure)?.length || 0 }}개, 
                  던전: {{ selectedDungeon || '미선택' }})
                </small>
        </div>
        
              <div 
                v-for="character in getFilteredCharacters(adventure)" 
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
                    <div v-if="isBuffer(character)" class="stat buffer-stat">
                        버프력: {{ formatNumber(character.buffPower || 0) }}
                </div>
                    <div v-else class="stat dealer-stat">
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
                  <!-- 파티 포함 표시 -->
                  <div v-if="isCharacterInParty(character.characterId)" class="in-party-badge">
                    🔒 파티 포함
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
import { ref, computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';

// 반응형 데이터
const selectedDungeon = ref('');
const selectedAdventures = ref<string[]>([]);
const parties = ref<Array<Array<any>>>([[]]);
const loading = ref(false);
const error = ref('');
const successMessage = ref('');
const allCharacters = ref<any[]>([]);
const refreshingAdventures = ref(false);
const nabelDifficulty = ref<'normal' | 'hard'>('normal');

// 컴포넌트 마운트
onMounted(() => {
  console.log('=== 파티 구성 페이지 마운트 ===');
  loadSearchHistory();
  loadCharactersFromAPI();
});

// 검색 기록에서 모험단 목록 가져오기 (LocalStorage 기반)
const availableAdventures = computed(() => {
  const searchHistory = JSON.parse(localStorage.getItem('df_search_history') || '[]');
  console.log('LocalStorage에서 로드된 검색 기록:', searchHistory);
  
  const adventures = new Set<string>();
  
  searchHistory.forEach((record: any) => {
    console.log('검색 기록 처리:', record);
    if (record.adventureName && 
        record.adventureName !== 'N/A' && 
        record.adventureName !== '모험단 정보 없음') {
      adventures.add(record.adventureName);
      console.log('모험단 추가됨:', record.adventureName);
    }
  });
  
  const result = Array.from(adventures).sort();
  console.log('최종 모험단 목록:', result);
  return result;
});

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
        console.log(`모험단 '${adventureName}' 캐릭터 로드 시작...`);
        const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(adventureName)}`);
        
        console.log(`모험단 '${adventureName}' API 응답 상태:`, response.status);
        
        if (response.ok) {
          const data = await response.json();
          console.log(`모험단 '${adventureName}' API 응답 데이터:`, data);
          
                    if (data.success && data.characters) {
            console.log(`모험단 '${adventureName}' 캐릭터 ${data.characters.length}개 로드됨`);
            return data.characters;
          } else {
            console.warn(`모험단 '${adventureName}' 캐릭터 데이터 없음:`, data);
            return [];
          }
        } else {
          console.error(`모험단 '${adventureName}' API 호출 실패:`, response.status, response.statusText);
          return [];
    }
  } catch (err) {
        console.error(`모험단 '${adventureName}' 로드 중 오류:`, err);
        return [];
      }
    });
    
    const results = await Promise.all(allCharacterPromises);
    const flatResults = results.flat();
    
    console.log('로드된 전체 캐릭터 결과:', flatResults);
    console.log('총 캐릭터 수:', flatResults.length);
    
    allCharacters.value = flatResults;
    
    if (flatResults.length === 0) {
      console.warn('로드된 캐릭터가 없습니다. LocalStorage 또는 백엔드 데이터를 확인하세요.');
      error.value = '사용 가능한 캐릭터가 없습니다. 먼저 캐릭터 검색에서 캐릭터를 검색해주세요.';
    }
    
  } catch (err) {
    console.error('캐릭터 데이터 로드 실패:', err);
    error.value = '캐릭터 데이터를 불러오는데 실패했습니다.';
  } finally {
    loading.value = false;
    console.log('=== 캐릭터 데이터 로드 완료 ===');
  }
};

// 검색 기록 로드
const loadSearchHistory = () => {
  // 컴포넌트 마운트 시 검색 기록을 로드하여 모험단 목록 갱신
};

// 던전 변경 시
const onDungeonChange = () => {
  // 황혼전 선택 시 개발중 메시지 표시
  if (selectedDungeon.value === 'twilight') {
    error.value = '⚠️ 황혼전은 아직 개발중인 던전입니다. 곧 업데이트 예정입니다!';
    // 황혼전은 아직 사용 불가하므로 선택 해제
    setTimeout(() => {
      selectedDungeon.value = '';
      error.value = '';
    }, 3000);
    return;
  }
  
  // 나벨 던전이 아닌 경우 난이도 초기화
  if (selectedDungeon.value !== 'nabel') {
    nabelDifficulty.value = 'normal';
  }
  
  // 파티 초기화
  parties.value = [[]];
  error.value = '';
};

// 나벨 던전 난이도 설정
const setNabelDifficulty = (difficulty: 'normal' | 'hard') => {
  nabelDifficulty.value = difficulty;
  // 난이도 변경 시 파티 초기화
  parties.value = [[]];
  error.value = '';
};

// DB의 isHardNabelEligible 속성을 사용하므로 별도 함수 불필요

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
      successMessage.value = '파티 구성이 클립보드에 복사되었습니다!';
      setTimeout(() => {
        successMessage.value = '';
      }, 3000);
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
  }
};

const removeAdventure = (adventure: string) => {
  selectedAdventures.value = selectedAdventures.value.filter(a => a !== adventure);
  // 해당 모험단 캐릭터들을 파티에서 제거
  parties.value.forEach(party => {
    for (let i = party.length - 1; i >= 0; i--) {
      if (party[i] && party[i].adventureName === adventure) {
        party.splice(i, 1);
      }
    }
  });
};

// 선택된 던전에 따라 조건에 맞는 캐릭터 필터링 (안감 제외, 업둥 포함)
const getFilteredCharacters = (adventureName: string) => {
  console.log(`getFilteredCharacters 호출: adventureName="${adventureName}"`);
  
  // allCharacters가 undefined이거나 null인 경우 빈 배열 반환
  if (!allCharacters.value || !Array.isArray(allCharacters.value)) {
    console.warn('allCharacters가 유효하지 않습니다:', allCharacters.value);
    return [];
  }
  
  console.log(`전체 캐릭터 수: ${allCharacters.value.length}`);
  
  // 1. 모험단별 캐릭터 필터링
  const adventureCharacters = allCharacters.value.filter(c => c.adventureName === adventureName);
  console.log(`모험단 "${adventureName}"의 캐릭터 수: ${adventureCharacters.length}`);
  
  if (adventureCharacters.length === 0) {
    console.warn(`모험단 "${adventureName}"에 캐릭터가 없습니다.`);
    return [];
  }
  
  // 2. 던전이 선택되지 않았다면 모든 캐릭터 반환 (안감만 제외)
  if (!selectedDungeon.value) {
    console.log(`던전 선택 없음, 안감만 제외하고 모든 캐릭터 반환`);
    return adventureCharacters; // 던전 선택 안했을 때는 모든 캐릭터 표시
  }
  
  // 3. 선택된 던전에 따라 필터링
  const filteredCharacters = adventureCharacters.filter(character => {
    let dungeonCondition = false;
    let isExcluded = false;
    
  switch (selectedDungeon.value) {
      case 'nabel':
        dungeonCondition = !character.dungeonClearNabel; // 클리어 안한 캐릭터
        isExcluded = character.isExcludedNabel; // 안감 여부
        
        // 나벨 난이도 조건 추가
        if (nabelDifficulty.value === 'hard') {
          // 하드: 하드 대상자만 포함 (100억 딜러, 500만 버퍼)
          dungeonCondition = dungeonCondition && character.isHardNabelEligible;
        } else {
          // 일반: 일반 대상자만 포함 (30억 딜러, 400만 버퍼) + 하드 대상자 제외
          dungeonCondition = dungeonCondition && character.isNormalNabelEligible && !character.isHardNabelEligible;
        }
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
        dungeonCondition = true; // 황혼전은 아직 클리어 데이터가 없으므로 모든 캐릭터
        isExcluded = false; // 황혼전은 아직 안감 기능 없음
        break;
      default:
        dungeonCondition = true;
        isExcluded = false;
    }
    
    // 안감인 경우 제외, 그 외에는 던전 조건에 맞는 캐릭터만 포함
    const shouldInclude = !isExcluded && dungeonCondition;
    
    console.log(`캐릭터 "${character.characterName}": 던전조건=${dungeonCondition}, 안감=${isExcluded}, 포함=${shouldInclude}`);
    
    return shouldInclude;
  });
  
  console.log(`던전 "${selectedDungeon.value}" 필터링 후 캐릭터 수: ${filteredCharacters.length}개`);
  return filteredCharacters;
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
        error.value = `파티 ${partyIndex + 1}에는 이미 다른 모험단의 캐릭터가 있습니다. 한 파티당 하나의 모험단만 허용됩니다.`;
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
  
  // 같은 모험단이거나 모험단이 없는 경우만 허용
  if (existingAdventures.length === 0) return true;
  if (existingAdventures.includes(character.adventureName)) return true;
  
  return false;
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

// 자동 파티 생성
const autoGenerateParty = async () => {
  try {
    loading.value = true;
    error.value = '';
    
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

// 파티 최적화
const optimizeParty = async () => {
  try {
    loading.value = true;
    error.value = '';

    const response = await fetch('http://localhost:8080/api/party/optimize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        dungeonType: selectedDungeon.value,
        characters: selectedAdventures.value.flatMap(adventure => getFilteredCharacters(adventure))
      })
    });

    if (response.ok) {
    const result = await response.json();
    if (result.success) {
        // 최적화된 파티 구성 적용
        parties.value = [result.party];
    } else {
        error.value = result.message || '파티 최적화에 실패했습니다.';
    }
    } else {
      error.value = '파티 최적화 요청에 실패했습니다.';
    }
  } catch (err) {
    error.value = '파티 최적화에 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

// 버퍼/딜러 구분 함수
const isBuffer = (character: any): boolean => {
  if (!character.jobName || character.jobName === 'N/A') return false;
  
  // 버퍼 직업 목록
  const bufferJobs = ['뮤즈', '패러메딕', '크루세이더', '인챈트리스'];
  
  return bufferJobs.some(job => 
    character.jobName.includes(job) || 
    (character.jobGrowName && character.jobGrowName.includes(job))
  );
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
      return false; // 황혼전은 아직 업둥 기능 없음
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

const getDungeonClearClass = (character: any): string => {
  const cleared = getDungeonClearStatus(character);
  return cleared ? 'cleared' : 'not-cleared';
};

const getDungeonClearText = (character: any): string => {
  const cleared = getDungeonClearStatus(character);
  return cleared ? '클리어' : '미클리어';
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
      return false; // 황혼전은 아직 클리어 데이터가 없음
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
        
        const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(adventureName)}/refresh`, {
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
    
    // 최종 결과 메시지
    const resultMessage = `모험단 최신화 완료!\n\n` +
      `총 성공: ${totalSuccess}개 캐릭터\n` +
      `총 실패: ${totalFail}개 캐릭터\n\n` +
      `상세 결과:\n${results.join('\n')}`;
    
    alert(resultMessage);
    
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
      const response = await fetch(`http://localhost:8080/api/characters/adventure/${encodeURIComponent(adventureName)}`);
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

.top-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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
  background: #007bff;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
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
  background: #28a745;
  color: white;
}

.clear-btn {
  background: #6c757d;
  color: white;
}

.optimize-btn {
  background: #007bff;
  color: white;
}

.main-content {
  display: flex;
  gap: 20px;
  min-height: 600px;
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.party-header h4 {
  margin: 0;
  color: #333;
}

.party-stats {
  font-size: 14px;
  color: #666;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.party-slot {
  min-height: 120px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  position: relative;
  background: white;
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
  height: 100%;
  position: relative;
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
  font-size: 12px;
  margin-bottom: 3px;
  color: #333;
}

.adventure-name {
  font-size: 10px;
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
  font-size: 10px;
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
  font-size: 10px;
}

.not-cleared {
  color: #dc3545;
  font-weight: bold;
  font-size: 10px;
}

.character-fame {
  font-size: 10px;
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
  font-size: 12px;
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
  max-height: 800px;
  overflow-y: auto;
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
  max-height: 400px;
  overflow-y: auto;
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

/* 반응형 디자인 */
@media (max-width: 1024px) {
  .main-content {
  flex-direction: column;
  }
  
  .party-slots {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .top-bar {
    flex-direction: column;
  }
  
  .party-slots {
    grid-template-columns: 1fr;
  }
  
  .character-list {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }
}
</style> 