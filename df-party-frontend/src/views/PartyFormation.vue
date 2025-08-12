<template>
  <div class="party-formation">
    <h1>파티 짜기</h1>
    
    <!-- 던전 선택 -->
    <div class="dungeon-selection">
      <h2>던전 선택</h2>
      <div class="dungeon-options">
        <select v-model="selectedDungeon" class="dungeon-select">
          <option value="">던전을 선택하세요</option>
          <option value="navel">나벨</option>
          <option value="venus">베누스</option>
          <option value="fog">안개신</option>
        </select>
        
        <!-- 나벨 모드 선택 -->
        <div v-if="selectedDungeon === 'navel'" class="navel-mode">
          <label>
            <input type="radio" v-model="navelMode" value="normal" />
            일반 나벨
          </label>
          <label>
            <input type="radio" v-model="navelMode" value="hard" />
            하드 나벨
          </label>
        </div>
      </div>
      
      <!-- 던전별 상세 규칙 표시 -->
      <div v-if="selectedDungeon" class="dungeon-rules">
        <h3>던전 요구사항</h3>
        <div class="rules-content">
          <div v-if="selectedDungeon === 'navel'" class="rule-item">
            <h4>나벨 던전</h4>
            <div class="rule-details">
              <div class="requirement-section">
                <h5>명성 요구사항</h5>
                <div class="fame-requirement">
                  <span class="requirement-label">최소 명성:</span>
                  <span class="requirement-value fame-required">63,000</span>
                  <span class="requirement-note">(모든 모드 공통)</span>
                </div>
              </div>
              
              <div class="requirement-section">
                <h5>모드별 요구사항</h5>
                <div class="mode-requirements">
                  <div class="mode-requirement">
                    <span class="mode-label">일반 나벨:</span>
                    <span class="requirement-item">전투력 30억 이상</span>
                    <span class="requirement-item">버프력 400만 이상</span>
                  </div>
                  <div class="mode-requirement">
                    <span class="mode-label">하드 나벨:</span>
                    <span class="requirement-item">전투력 100억 이상</span>
                    <span class="requirement-item">버프력 500만 이상</span>
                  </div>
                </div>
              </div>
              
              <div class="requirement-section">
                <h5>파티 구성</h5>
                <div class="party-requirement">
                  <span class="requirement-item">최소 2인 이상</span>
                  <span class="requirement-item">버퍼 포함 권장</span>
                  <span class="requirement-item">딜러 1명 + 버퍼 1명 이상</span>
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="selectedDungeon === 'venus'" class="rule-item">
            <h4>베누스 던전</h4>
            <div class="rule-details">
              <div class="requirement-section">
                <h5>명성 요구사항</h5>
                <div class="fame-requirement">
                  <span class="requirement-label">최소 명성:</span>
                  <span class="requirement-value fame-required">41,929</span>
                </div>
              </div>
              
              <div class="requirement-section">
                <h5>파티 구성</h5>
                <div class="party-requirement">
                  <span class="requirement-item">최소 2인 이상</span>
                  <span class="requirement-item">버퍼 포함 권장</span>
                  <span class="requirement-item">자유로운 구성 가능</span>
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="selectedDungeon === 'fog'" class="rule-item">
            <h4>안개신 던전</h4>
            <div class="rule-details">
              <div class="requirement-section">
                <h5>명성 요구사항</h5>
                <div class="fame-requirement">
                  <span class="requirement-label">최소 명성:</span>
                  <span class="requirement-value fame-required">32,253</span>
                </div>
              </div>
              
              <div class="requirement-section">
                <h5>파티 구성</h5>
                <div class="party-requirement">
                  <span class="requirement-item">딜러 2명 이상</span>
                  <span class="requirement-item">버퍼 1명 이상</span>
                  <span class="requirement-item">딜러와 버퍼 밸런스 중요</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 모험단 선택 -->
    <div v-if="selectedDungeon" class="adventure-selection">
      <h2>모험단 선택</h2>
      
      <!-- 명성 기준 필터링 -->
      <div class="fame-filter">
        <h3>명성 기준 필터링</h3>
        <div class="filter-options">
          <label class="filter-option">
            <input 
              type="checkbox" 
              v-model="applyFameFilter" 
              @change="updateFilteredCharacters"
            />
            명성 기준 자동 필터링 적용
          </label>
          
          <div v-if="applyFameFilter" class="fame-thresholds">
            <div class="threshold-item">
              <span class="dungeon-name">나벨:</span>
              <span class="threshold-value">63,000</span>
              <span class="threshold-note">(모든 모드)</span>
            </div>
            <div class="threshold-item">
              <span class="dungeon-name">베누스:</span>
              <span class="threshold-value">41,929</span>
            </div>
            <div class="threshold-item">
              <span class="dungeon-name">안개신:</span>
              <span class="threshold-value">32,253</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="adventure-list">
        <div 
          v-for="adventure in availableAdventures" 
          :key="adventure"
          class="adventure-item"
          :class="{ selected: selectedAdventures.includes(adventure) }"
          @click="toggleAdventure(adventure)"
        >
          <span class="adventure-name">{{ adventure }}</span>
          <span class="character-count">
            {{ getAdventureCharacterCount(adventure) }}명
            <span v-if="applyFameFilter" class="filtered-count">
              (필터링: {{ getFilteredCharacterCount(adventure) }}명)
            </span>
          </span>
        </div>
      </div>
    </div>

    <!-- 파티 구성 모드 -->
    <div v-if="selectedAdventures.length > 0" class="party-mode">
      <h2>파티 구성 모드</h2>
      <div class="mode-selection">
        <label>
          <input type="radio" v-model="partyMode" value="auto" />
          자동 구성
        </label>
        <label>
          <input type="radio" v-model="partyMode" value="manual" />
          수동 구성
        </label>
      </div>
      
      <!-- 자동 구성 옵션 -->
      <div v-if="partyMode === 'auto'" class="auto-options">
        <div class="option-group">
          <label>최소 딜컷:</label>
          <select v-model="minDamageCut">
            <option value="30">30억</option>
            <option value="50">50억</option>
            <option value="100">100억</option>
            <option value="200">200억</option>
          </select>
        </div>
        
        <div class="option-group">
          <label>파티 인원:</label>
          <select v-model="partySize">
            <option value="2">2인</option>
            <option value="3">3인</option>
            <option value="4">4인</option>
            <option value="8">8인</option>
          </select>
        </div>
        
        <button @click="generateAutoParty" class="generate-btn" :disabled="generating">
          {{ generating ? '파티 생성 중...' : '자동 파티 생성' }}
        </button>
      </div>

      <!-- 수동 구성 옵션 -->
      <div v-if="partyMode === 'manual'" class="manual-options">
        <div class="option-group">
          <label>파티 인원:</label>
          <select v-model="partySize" @change="resetManualParty">
            <option value="2">2인</option>
            <option value="3">3인</option>
            <option value="4">4인</option>
            <option value="8">8인</option>
          </select>
        </div>
        
        <div class="manual-party-slots">
          <h3>파티 슬롯 구성</h3>
          <div class="party-slots">
            <div 
              v-for="index in partySize" 
              :key="index" 
              class="party-slot"
              :class="{ filled: manualPartyMembers[index - 1] }"
            >
              <div class="slot-header">
                <span class="slot-number">{{ index }}번 슬롯</span>
                <span class="slot-role">{{ getSlotRole(index) }}</span>
              </div>
              
              <div v-if="manualPartyMembers[index - 1]" class="selected-character">
                <div class="character-info">
                  <strong>{{ manualPartyMembers[index - 1]?.characterName }}</strong>
                  <span class="adventure-name">{{ manualPartyMembers[index - 1]?.adventureName }}</span>
                </div>
                <div class="character-stats">
                  <span class="damage">전투력: {{ formatNumber(manualPartyMembers[index - 1]?.totalDamage) }}</span>
                  <span class="buff">버프력: {{ formatNumber(manualPartyMembers[index - 1]?.buffPower) }}</span>
                </div>
                <button @click="removeFromManualParty(index - 1)" class="remove-slot-btn">❌</button>
              </div>
              
              <div v-else class="empty-slot">
                <select 
                  v-model="manualPartySelections[index - 1]" 
                  @change="addToManualParty(index - 1)"
                  class="character-select"
                >
                  <option value="">캐릭터 선택</option>
                  <option 
                    v-for="char in getAvailableCharactersForSlot(index)" 
                    :key="char.characterId"
                    :value="char.characterId"
                  >
                    {{ char.characterName }} ({{ char.adventureName }}) - {{ formatNumber(char.totalDamage) }}
                  </option>
                </select>
              </div>
            </div>
          </div>
          
          <div class="manual-party-actions">
            <button @click="generateManualParty" class="generate-btn" :disabled="!canGenerateManualParty">
              수동 파티 생성
            </button>
            <button @click="resetManualParty" class="reset-btn">
              초기화
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 파티 구성 결과 -->
    <div v-if="partyResult" class="party-result">
      <h2>파티 구성 결과</h2>
      <div class="party-info">
        <p><strong>던전:</strong> {{ getDungeonName(selectedDungeon) }}</p>
        <p><strong>파티 인원:</strong> {{ partyResult.memberCount }}명</p>
        <p><strong>총 전투력:</strong> {{ formatNumber(partyResult.totalDamage) }}</p>
        <p><strong>총 버프력:</strong> {{ formatNumber(partyResult.buffPower) }}</p>
        <p><strong>파티 전투력:</strong> {{ formatNumber(partyResult.partyCombatPower) }}</p>
      </div>
      
      <!-- 파티 효율성 분석 -->
      <div class="party-efficiency">
        <h3>파티 효율성 분석</h3>
        <div class="efficiency-metrics">
          <div class="metric-item">
            <span class="metric-label">딜러 비율:</span>
            <span class="metric-value">{{ getDealerRatio() }}%</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">버퍼 비율:</span>
            <span class="metric-value">{{ getBufferRatio() }}%</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">평균 전투력:</span>
            <span class="metric-value">{{ formatNumber(getAverageDamage()) }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">평균 버프력:</span>
            <span class="metric-value">{{ formatNumber(getAverageBuffPower()) }}</span>
          </div>
        </div>
      </div>
      
      <div class="party-members">
        <h3>파티 멤버</h3>
        <div class="member-table">
          <table>
            <thead>
              <tr>
                <th>모험단</th>
                <th>캐릭터명</th>
                <th>역할</th>
                <th>전투력</th>
                <th>버프력</th>
                <th>명성</th>
                <th>기여도</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in partyResult.members" :key="member.characterId">
                <td>{{ member.adventureName }}</td>
                <td>{{ member.characterName }}</td>
                <td>
                  <span :class="getRoleClass(member)">
                    {{ getRoleName(member) }}
                  </span>
                </td>
                <td>{{ formatNumber(member.totalDamage) }}</td>
                <td>{{ formatNumber(member.buffPower) }}</td>
                <td>{{ member.fame?.toLocaleString() || 'N/A' }}</td>
                <td>{{ getContributionPercentage(member) }}%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 저장된 캐릭터가 없는 경우 -->
    <div v-else-if="!loading && availableAdventures.length === 0" class="no-characters">
      <p>저장된 캐릭터가 없습니다.</p>
      <RouterLink to="/character-search" class="search-link">캐릭터 검색하러 가기</RouterLink>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading">
      <p>캐릭터 정보를 불러오는 중...</p>
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { RouterLink } from 'vue-router';

// 반응형 데이터
const selectedDungeon = ref('');
const navelMode = ref('normal');
const selectedAdventures = ref<string[]>([]);
const partyMode = ref('auto');
const minDamageCut = ref(100);
const partySize = ref(4);
const applyFameFilter = ref(false);
const filteredCharacters = ref<Array<{
  characterId: string;
  serverId: string;
  characterName: string;
  adventureName: string;
  fame: number;
  buffPower?: number;
  totalDamage?: number;
  level?: number;
  jobName?: string;
  savedAt: string;
}>>([]);
const partyResult = ref<{
  members: Array<{
    characterId: string;
    serverId: string;
    characterName: string;
    totalDamage: number;
    buffPower: number;
    fame: number;
    adventureName: string;
  }>;
  totalDamage: number;
  buffPower: number;
  partyCombatPower: number;
  memberCount: number;
} | null>(null);
const loading = ref(false);
const generating = ref(false);
const error = ref('');

// 수동 파티 구성 관련 데이터
const manualPartySelections = ref<string[]>([]);
const manualPartyMembers = ref<Array<{
  characterId: string;
  serverId: string;
  characterName: string;
  adventureName: string;
  fame: number;
  buffPower?: number;
  totalDamage?: number;
  level?: number;
  jobName?: string;
  savedAt: string;
} | undefined>>([]);

// 저장된 캐릭터 데이터 (실제로는 서비스에서 가져와야 함)
const savedCharacters = ref<Array<{
  characterId: string;
  serverId: string;
  characterName: string;
  adventureName: string;
  fame: number;
  buffPower?: number;
  totalDamage?: number;
  level?: number;
  jobName?: string;
  savedAt: string;
}>>([]);

// 계산된 속성
const availableAdventures = computed(() => {
  const adventures = new Set(savedCharacters.value.map(c => c.adventureName));
  return Array.from(adventures).sort();
});

const canGenerateManualParty = computed(() => {
  return manualPartySelections.value.length === partySize.value && manualPartySelections.value.every(id => id !== '');
});

// 메서드들
const loadSavedCharacters = () => {
  try {
    const characters = localStorage.getItem('df_characters');
    if (characters) {
      savedCharacters.value = JSON.parse(characters);
    }
  } catch (err) {
    console.error('저장된 캐릭터 로드 실패:', err);
    error.value = '저장된 캐릭터를 불러오는데 실패했습니다.';
  }
};

const toggleAdventure = (adventure: string) => {
  const index = selectedAdventures.value.indexOf(adventure);
  if (index >= 0) {
    selectedAdventures.value.splice(index, 1);
  } else {
    selectedAdventures.value.push(adventure);
  }
};

const getAdventureCharacterCount = (adventure: string): number => {
  return savedCharacters.value.filter(c => c.adventureName === adventure).length;
};

const getFilteredCharacterCount = (adventure: string): number => {
  if (!applyFameFilter.value) return getAdventureCharacterCount(adventure);
  return filteredCharacters.value.filter(c => c.adventureName === adventure).length;
};

const getDungeonFameThreshold = (): number => {
  switch (selectedDungeon.value) {
    case 'navel': return 63000;
    case 'venus': return 41929;
    case 'fog': return 32253;
    default: return 0;
  }
};

const updateFilteredCharacters = () => {
  if (!applyFameFilter.value) {
    filteredCharacters.value = savedCharacters.value;
    return;
  }
  
  const threshold = getDungeonFameThreshold();
  filteredCharacters.value = savedCharacters.value.filter(c => c.fame >= threshold);
};

const getAvailableCharactersForSlot = (slotIndex: number) => {
  const characters = applyFameFilter.value ? filteredCharacters.value : savedCharacters.value;
  const selectedAdventureChars = characters.filter(c => 
    selectedAdventures.value.includes(c.adventureName)
  );
  
  // 이미 선택된 캐릭터는 제외
  const usedCharacters = manualPartyMembers.value.filter(c => c !== undefined);
  return selectedAdventureChars.filter(c => 
    !usedCharacters.some(used => used?.characterId === c.characterId)
  );
};

const getDungeonName = (dungeonId: string): string => {
  const dungeonNames: { [key: string]: string } = {
    navel: '나벨',
    venus: '베누스',
    fog: '안개신'
  };
  return dungeonNames[dungeonId] || dungeonId;
};

const formatNumber = (num?: number): string => {
  if (!num) return 'N/A';
  if (num >= 100000000) {
    return (num / 100000000).toFixed(1) + '억';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만';
  }
  return num.toLocaleString();
};

const generateAutoParty = async () => {
  try {
    generating.value = true;
    error.value = '';
    
    // 선택된 모험단의 캐릭터들 필터링
    const availableCharacters = savedCharacters.value.filter(c => 
      selectedAdventures.value.includes(c.adventureName)
    );
    
    // 백엔드 API 호출을 위한 데이터 준비
    const requestData = {
      dungeonType: selectedDungeon.value,
      navelMode: navelMode.value,
      partySize: partySize.value,
      minDamageCut: minDamageCut.value * 100000000, // 억 단위를 실제 숫자로 변환
      characters: availableCharacters.map(c => ({
        characterId: c.characterId,
        serverId: c.serverId,
        characterName: c.characterName,
        totalDamage: c.totalDamage || 0,
        buffPower: c.buffPower || 0,
        fame: c.fame || 0,
        adventureName: c.adventureName
      }))
    };

    // 백엔드 API 호출
    const response = await fetch('http://localhost:8080/api/party/optimize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    });

    if (!response.ok) {
      throw new Error('파티 최적화 API 호출에 실패했습니다.');
    }

    const result = await response.json();
    
    if (result.success) {
      partyResult.value = {
        members: result.party,
        totalDamage: result.stats.totalDamage,
        buffPower: result.stats.totalBuffPower,
        partyCombatPower: result.stats.partyCombatPower,
        memberCount: result.stats.memberCount
      };
      error.value = '';
    } else {
      error.value = result.message || '파티 구성에 실패했습니다.';
    }
    
  } catch (err) {
    error.value = '파티 생성에 실패했습니다.';
    console.error(err);
  } finally {
    generating.value = false;
  }
};

const addToManualParty = (slotIndex: number) => {
  const selectedCharacterId = manualPartySelections.value[slotIndex];
  if (selectedCharacterId) {
    const character = savedCharacters.value.find(c => c.characterId === selectedCharacterId);
    if (character) {
      manualPartyMembers.value[slotIndex] = character;
      manualPartySelections.value[slotIndex] = ''; // 선택 초기화
    }
  }
};

const removeFromManualParty = (slotIndex: number) => {
  manualPartyMembers.value[slotIndex] = undefined;
  manualPartySelections.value[slotIndex] = '';
};

const resetManualParty = () => {
  manualPartyMembers.value = Array(partySize.value).fill(undefined);
  manualPartySelections.value = Array(partySize.value).fill('');
};

const generateManualParty = async () => {
  try {
    generating.value = true;
    error.value = '';

    // 수동으로 선택된 캐릭터들 필터링
    const selectedCharacters = manualPartyMembers.value.filter(c => c !== undefined) as Array<{
      characterId: string;
      serverId: string;
      characterName: string;
      adventureName: string;
      fame: number;
      buffPower?: number;
      totalDamage?: number;
      level?: number;
      jobName?: string;
      savedAt: string;
    }>;

    // 백엔드 API 호출을 위한 데이터 준비
    const requestData = {
      dungeonType: selectedDungeon.value,
      navelMode: navelMode.value,
      partySize: partySize.value,
      characters: selectedCharacters.map(c => ({
        characterId: c.characterId,
        serverId: c.serverId,
        characterName: c.characterName,
        totalDamage: c.totalDamage || 0,
        buffPower: c.buffPower || 0,
        fame: c.fame || 0,
        adventureName: c.adventureName
      }))
    };

    // 백엔드 API 호출
    const response = await fetch('http://localhost:8080/api/party/optimize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    });

    if (!response.ok) {
      throw new Error('파티 최적화 API 호출에 실패했습니다.');
    }

    const result = await response.json();
    
    if (result.success) {
      partyResult.value = {
        members: result.party,
        totalDamage: result.stats.totalDamage,
        buffPower: result.stats.totalBuffPower,
        partyCombatPower: result.stats.partyCombatPower,
        memberCount: result.stats.memberCount
      };
      error.value = '';
    } else {
      error.value = result.message || '파티 구성에 실패했습니다.';
    }
    
  } catch (err) {
    error.value = '파티 생성에 실패했습니다.';
    console.error(err);
  } finally {
    generating.value = false;
  }
};

const getAvailableCharactersForSlot = (slotIndex: number) => {
  const selectedCharacterId = manualPartySelections.value[slotIndex];
  if (selectedCharacterId) {
    return savedCharacters.value.filter(c => c.characterId !== selectedCharacterId);
  }
  return savedCharacters.value.filter(c => 
    selectedAdventures.value.includes(c.adventureName) && 
    !manualPartyMembers.value.some(m => m?.characterId === c.characterId)
  );
};

const getSlotRole = (slotIndex: number) => {
  const member = manualPartyMembers.value[slotIndex];
  if (!member) return '비어있음';
  return (member.buffPower || 0) > (member.totalDamage || 0) ? '버퍼' : '딜러';
};

// 역할 관련 헬퍼 함수들
const getRoleName = (member: { totalDamage?: number; buffPower?: number }): string => {
  const totalDamage = member.totalDamage || 0;
  const buffPower = member.buffPower || 0;
  return buffPower > totalDamage ? '버퍼' : '딜러';
};

const getRoleClass = (member: { totalDamage?: number; buffPower?: number }): string => {
  const totalDamage = member.totalDamage || 0;
  const buffPower = member.buffPower || 0;
  return buffPower > totalDamage ? 'role-buffer' : 'role-dealer';
};

// 파티 효율성 계산 함수들
const getDealerRatio = () => {
  if (!partyResult.value) return 0;
  const totalMembers = partyResult.value.memberCount;
  if (totalMembers === 0) return 0;
  const dealers = partyResult.value.members.filter(m => getRoleName(m) === '딜러').length;
  return ((dealers / totalMembers) * 100).toFixed(1);
};

const getBufferRatio = () => {
  if (!partyResult.value) return 0;
  const totalMembers = partyResult.value.memberCount;
  if (totalMembers === 0) return 0;
  const buffers = partyResult.value.members.filter(m => getRoleName(m) === '버퍼').length;
  return ((buffers / totalMembers) * 100).toFixed(1);
};

const getAverageDamage = () => {
  if (!partyResult.value) return 0;
  const totalDamage = partyResult.value.totalDamage;
  const memberCount = partyResult.value.memberCount;
  if (memberCount === 0) return 0;
  return totalDamage / memberCount;
};

const getAverageBuffPower = () => {
  if (!partyResult.value) return 0;
  const totalBuffPower = partyResult.value.buffPower;
  const memberCount = partyResult.value.memberCount;
  if (memberCount === 0) return 0;
  return totalBuffPower / memberCount;
};

const getContributionPercentage = (member: { totalDamage?: number; buffPower?: number }) => {
  if (!partyResult.value) return 0;
  const totalDamage = partyResult.value.totalDamage;
  const totalBuffPower = partyResult.value.buffPower;
  const memberDamage = member.totalDamage || 0;
  const memberBuffPower = member.buffPower || 0;

  if (totalDamage === 0 && totalBuffPower === 0) return 0;

  const damageContribution = (memberDamage / totalDamage) * 100;
  const buffPowerContribution = (memberBuffPower / totalBuffPower) * 100;

  return ((damageContribution + buffPowerContribution) / 2).toFixed(1);
};

// 컴포넌트 마운트 시 데이터 로드
onMounted(() => {
  loading.value = true;
  loadSavedCharacters();
  loading.value = false;
});

// 던전 변경 시 필터 업데이트
watch(selectedDungeon, () => {
  if (applyFameFilter.value) {
    updateFilteredCharacters();
  }
});

// 명성 필터 변경 시 필터 업데이트
watch(applyFameFilter, () => {
  updateFilteredCharacters();
});
</script>

<style scoped>
.party-formation {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.dungeon-selection,
.adventure-selection,
.party-mode {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.dungeon-selection h2,
.adventure-selection h2,
.party-mode h2 {
  color: #333;
  margin-bottom: 20px;
}

.dungeon-options {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.dungeon-select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  min-width: 200px;
}

.navel-mode {
  display: flex;
  gap: 20px;
}

.navel-mode label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.adventure-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.adventure-item {
  background: #f8f9fa;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.adventure-item:hover {
  border-color: #007bff;
  transform: translateY(-2px);
}

.adventure-item.selected {
  border-color: #28a745;
  background: #f8fff9;
}

.adventure-name {
  font-weight: bold;
  color: #333;
}

.character-count {
  background: #007bff;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
}

.mode-selection {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.mode-selection label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.auto-options,
.manual-options {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
}

.option-group {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.option-group label {
  font-weight: bold;
  min-width: 100px;
}

.option-group select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.generate-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 10px;
}

.generate-btn:hover:not(:disabled) {
  background: #218838;
}

.generate-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.manual-party-slots {
  margin-top: 20px;
}

.party-slots {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.party-slot {
  background: #f0f0f0;
  border: 1px solid #ccc;
  border-radius: 6px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.party-slot.filled {
  background: #e8f5e8;
  border-color: #28a745;
}

.slot-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.slot-number {
  font-weight: bold;
  color: #333;
}

.slot-role {
  background: #007bff;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
}

.selected-character {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.character-info {
  text-align: center;
}

.character-info strong {
  font-size: 1.1rem;
  color: #333;
}

.character-info .adventure-name {
  font-size: 0.9rem;
  color: #666;
}

.character-stats {
  display: flex;
  gap: 10px;
  font-size: 0.9rem;
  color: #555;
}

.remove-slot-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background 0.3s ease;
}

.remove-slot-btn:hover {
  background: #c82333;
}

.empty-slot {
  width: 100%;
}

.character-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background: white;
}

.manual-party-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.reset-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
}

.reset-btn:hover {
  background: #5a6268;
}

.party-result {
  background: #e8f5e8;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.party-info {
  background: white;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.party-info p {
  margin: 8px 0;
  color: #333;
}

.party-efficiency {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.party-efficiency h3 {
  color: #333;
  margin-bottom: 15px;
}

.efficiency-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 15px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 15px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.metric-label {
  font-weight: bold;
  color: #333;
}

.metric-value {
  font-size: 1.1rem;
  font-weight: bold;
  color: #28a745; /* 버프력 색상과 동일 */
}

.party-members h3 {
  color: #333;
  margin-bottom: 15px;
}

.member-table {
  margin-top: 20px;
}

.member-table table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.member-table th,
.member-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.member-table th {
  background: #f8f9fa;
  font-weight: bold;
  color: #333;
}

.member-table tr:hover {
  background: #f8f9fa;
}

.role-buffer {
  color: #28a745;
  font-weight: bold;
}

.role-dealer {
  color: #007bff;
  font-weight: bold;
}

.no-characters {
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
  transition: background 0.3s ease;
}

.search-link:hover {
  background: #0056b3;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 4px;
  margin-top: 20px;
  text-align: center;
}

.dungeon-rules {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.dungeon-rules h3 {
  color: #333;
  margin-bottom: 15px;
}

.rules-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.rule-item {
  background: white;
  padding: 15px;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.rule-item h4 {
  color: #333;
  margin-bottom: 10px;
}

.rule-item ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rule-item li {
  color: #555;
  font-size: 0.9rem;
  margin-bottom: 5px;
}

.rule-item li strong {
  color: #333;
}

/* 명성 필터 스타일 */
.fame-filter {
  background: #e3f2fd;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
  border-left: 4px solid #2196f3;
}

.fame-filter h3 {
  color: #1976d2;
  margin-bottom: 15px;
  font-size: 1.1rem;
}

.filter-options {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 500;
}

.filter-option input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #2196f3;
}

.fame-thresholds {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 10px;
  padding: 10px;
  background: white;
  border-radius: 4px;
}

.threshold-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 0.9rem;
}

.dungeon-name {
  font-weight: bold;
  color: #333;
}

.threshold-value {
  color: #e91e63;
  font-weight: bold;
  font-size: 1.1rem;
}

.threshold-note {
  color: #666;
  font-size: 0.8rem;
}

.filtered-count {
  color: #2196f3;
  font-size: 0.8rem;
  font-weight: 500;
}

/* 던전 규칙 상세 스타일 */
.rule-details {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.requirement-section {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 4px;
  border-left: 3px solid #28a745;
}

.requirement-section h5 {
  color: #28a745;
  margin-bottom: 10px;
  font-size: 1rem;
}

.fame-requirement {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.requirement-label {
  font-weight: bold;
  color: #333;
}

.fame-required {
  color: #e91e63;
  font-weight: bold;
  font-size: 1.2rem;
}

.requirement-note {
  color: #666;
  font-size: 0.8rem;
}

.mode-requirements {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mode-requirement {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 8px;
  background: white;
  border-radius: 4px;
}

.mode-label {
  font-weight: bold;
  color: #333;
  min-width: 80px;
}

.requirement-item {
  color: #555;
  font-size: 0.9rem;
}

.party-requirement {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

@media (max-width: 768px) {
  .party-formation {
    padding: 10px;
  }
  
  .dungeon-options {
    flex-direction: column;
    align-items: stretch;
  }
  
  .dungeon-select {
    min-width: auto;
  }
  
  .adventure-list {
    grid-template-columns: 1fr;
  }
  
  .member-list {
    grid-template-columns: 1fr;
  }
}
</style> 