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
    </div>

    <!-- 모험단 선택 -->
    <div v-if="selectedDungeon" class="adventure-selection">
      <h2>모험단 선택</h2>
      <div class="adventure-list">
        <div 
          v-for="adventure in availableAdventures" 
          :key="adventure"
          class="adventure-item"
          :class="{ selected: selectedAdventures.includes(adventure) }"
          @click="toggleAdventure(adventure)"
        >
          <span class="adventure-name">{{ adventure }}</span>
          <span class="character-count">{{ getAdventureCharacterCount(adventure) }}명</span>
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
          </select>
        </div>
        
        <button @click="generateAutoParty" class="generate-btn" :disabled="generating">
          {{ generating ? '파티 생성 중...' : '자동 파티 생성' }}
        </button>
      </div>
    </div>

    <!-- 파티 구성 결과 -->
    <div v-if="partyResult" class="party-result">
      <h2>파티 구성 결과</h2>
      <div class="party-info">
        <p><strong>던전:</strong> {{ getDungeonName(selectedDungeon) }}</p>
        <p><strong>파티 인원:</strong> {{ partyResult.members.length }}명</p>
        <p><strong>총 전투력:</strong> {{ formatNumber(partyResult.totalDamage) }}</p>
        <p><strong>버프력:</strong> {{ formatNumber(partyResult.buffPower) }}</p>
      </div>
      
      <div class="party-members">
        <h3>파티 멤버</h3>
        <div class="member-list">
          <div 
            v-for="(member, index) in partyResult.members" 
            :key="index"
            class="member-card"
          >
            <h4>{{ member.characterName }}</h4>
            <p><strong>모험단:</strong> {{ member.adventureName }}</p>
            <p><strong>전투력:</strong> {{ formatNumber(member.totalDamage) }}</p>
            <p><strong>버프력:</strong> {{ formatNumber(member.buffPower) }}</p>
            <p><strong>역할:</strong> {{ member.role }}</p>
          </div>
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
import { ref, computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';

// 반응형 데이터
const selectedDungeon = ref('');
const navelMode = ref('normal');
const selectedAdventures = ref<string[]>([]);
const partyMode = ref('auto');
const minDamageCut = ref(100);
const partySize = ref(4);
const partyResult = ref<any>(null);
const loading = ref(false);
const generating = ref(false);
const error = ref('');

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
    
    // 던전별 요구사항 체크
    const validCharacters = filterCharactersByDungeon(availableCharacters);
    
    if (validCharacters.length < partySize.value) {
      error.value = `파티 구성에 필요한 캐릭터가 부족합니다. (필요: ${partySize.value}명, 가능: ${validCharacters.length}명)`;
      return;
    }
    
    // 파티 구성 알고리즘 실행
    const party = createParty(validCharacters);
    
    if (party) {
      partyResult.value = party;
    } else {
      error.value = '파티 구성에 실패했습니다.';
    }
    
  } catch (err) {
    error.value = '파티 생성에 실패했습니다.';
    console.error(err);
  } finally {
    generating.value = false;
  }
};

const filterCharactersByDungeon = (characters: any[]): any[] => {
  const minDamage = minDamageCut.value * 100000000; // 억 단위를 실제 숫자로 변환
  
  return characters.filter(c => {
    // 기본 요구사항 체크
    if (!c.totalDamage || c.totalDamage < minDamage) return false;
    
    // 던전별 특별 요구사항 체크
    switch (selectedDungeon.value) {
      case 'navel':
        if (navelMode.value === 'hard') {
          // 하드 나벨: 전투력 100억, 버프력 500만 이상
          return c.totalDamage >= 10000000000 && (c.buffPower || 0) >= 5000000;
        } else {
          // 일반 나벨: 전투력 30억, 버프력 400만 이상
          return c.totalDamage >= 3000000000 && (c.buffPower || 0) >= 4000000;
        }
      case 'venus':
        // 베누스: 명성 41929 이상
        return c.fame >= 41929;
      case 'fog':
        // 안개신: 명성 32253 이상
        return c.fame >= 32253;
      default:
        return true;
    }
  });
};

const createParty = (characters: any[]): any => {
  try {
    // 버퍼와 딜러 분리
    const buffers = characters.filter(c => (c.buffPower || 0) > (c.totalDamage || 0));
    const dealers = characters.filter(c => (c.totalDamage || 0) >= (c.buffPower || 0));
    
    // 파티 구성 규칙에 따른 멤버 선택
    let selectedMembers: any[] = [];
    
    if (partySize.value === 4) {
      // 4인 파티: 버퍼 1명 + 딜러 3명
      if (buffers.length > 0) {
        selectedMembers.push(buffers[0]);
      }
      
      const neededDealers = partySize.value - selectedMembers.length;
      selectedMembers.push(...dealers.slice(0, neededDealers));
      
    } else if (partySize.value === 3) {
      // 3인 파티: 버퍼 1명 + 딜러 2명
      if (buffers.length > 0) {
        selectedMembers.push(buffers[0]);
      }
      
      const neededDealers = partySize.value - selectedMembers.length;
      selectedMembers.push(...dealers.slice(0, neededDealers));
      
    } else if (partySize.value === 2) {
      // 2인 파티: 버퍼 1명 + 딜러 1명
      if (buffers.length > 0) {
        selectedMembers.push(buffers[0]);
      }
      
      const neededDealers = partySize.value - selectedMembers.length;
      selectedMembers.push(...dealers.slice(0, neededDealers));
    }
    
    if (selectedMembers.length < partySize.value) {
      // 부족한 인원은 딜러로 채움
      const remainingDealers = dealers.filter(d => !selectedMembers.includes(d));
      selectedMembers.push(...remainingDealers.slice(0, partySize.value - selectedMembers.length));
    }
    
    // 파티 정보 계산
    const totalDamage = selectedMembers.reduce((sum, m) => sum + (m.totalDamage || 0), 0);
    const buffPower = selectedMembers.reduce((sum, m) => sum + (m.buffPower || 0), 0);
    
    // 멤버별 역할 설정
    const membersWithRoles = selectedMembers.map(member => ({
      ...member,
      role: (member.buffPower || 0) > (member.totalDamage || 0) ? '버퍼' : '딜러'
    }));
    
    return {
      members: membersWithRoles,
      totalDamage,
      buffPower,
      effectiveDamage: totalDamage * (1 + buffPower / 1000000) // 버프력 적용
    };
    
  } catch (err) {
    console.error('파티 생성 실패:', err);
    return null;
  }
};

// 컴포넌트 마운트 시 데이터 로드
onMounted(() => {
  loading.value = true;
  loadSavedCharacters();
  loading.value = false;
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

.auto-options {
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

.party-members h3 {
  color: #333;
  margin-bottom: 15px;
}

.member-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.member-card {
  background: white;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #007bff;
}

.member-card h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.member-card p {
  margin: 5px 0;
  color: #666;
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