<template>
  <div class="character-list">
    <h2>캐릭터 목록</h2>
    
    <!-- 검색 기록 관리 -->
    <div class="search-history-section">
      <h3>검색 기록</h3>
      <div class="search-history-list">
        <div v-for="record in searchHistory" :key="record.id" class="search-record">
          <span class="server-name">{{ record.serverName }}</span>
          <span class="adventure-name">{{ record.adventureName }}</span>
          <span class="character-name">{{ record.characterName }}</span>
          <button @click="loadCharacterFromHistory(record)" class="load-btn">불러오기</button>
          <button @click="removeSearchRecord(record.id)" class="remove-btn">삭제</button>
        </div>
      </div>
    </div>

    <!-- 모험단 선택 -->
    <div class="adventure-selection">
      <h3>모험단 선택</h3>
      <div class="adventure-filters">
        <label v-for="adventure in availableAdventures" :key="adventure" class="adventure-checkbox">
          <input 
            type="checkbox" 
            :value="adventure" 
            v-model="selectedAdventures"
            @change="loadCharactersFromDB"
          >
          {{ adventure }}
        </label>
      </div>
    </div>

    <!-- 캐릭터 목록 -->
    <div class="characters-section" v-if="charactersFromDB.length > 0">
      <h3>DB에서 조회된 캐릭터 ({{ charactersFromDB.length }}개)</h3>
      
      <!-- 우클릭 컨텍스트 메뉴 -->
      <div v-if="showContextMenu" class="context-menu" :style="contextMenuStyle" @click.stop>
        <div class="context-menu-item" @click="setFavoriteCharacter">
          <span v-if="selectedCharacter?.isFavorite">⭐ 업둥이 해제</span>
          <span v-else>☆ 업둥이 설정</span>
        </div>
        <div class="context-menu-item" @click="excludeFromDungeon('nabel')">
          <span v-if="isExcludedFromDungeon('nabel')">✅ 나벨 제외 해제</span>
          <span v-else>❌ 나벨에서 제외</span>
        </div>
        <div class="context-menu-item" @click="excludeFromDungeon('venus')">
          <span v-if="isExcludedFromDungeon('venus')">✅ 베누스 제외 해제</span>
          <span v-else>❌ 베누스에서 제외</span>
        </div>
        <div class="context-menu-item" @click="excludeFromDungeon('fog')">
          <span v-if="isExcludedFromDungeon('fog')">✅ 안개신 제외 해제</span>
          <span v-else>❌ 안개신에서 제외</span>
        </div>
      </div>
      
      <table class="characters-table">
        <thead>
          <tr>
            <th>모험단</th>
            <th>캐릭터명</th>
            <th>서버</th>
            <th>레벨</th>
            <th>명성</th>
            <th>버프력</th>
            <th>총딜</th>
            <th>던전 클리어 현황</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="character in charactersFromDB" :key="character.characterId" 
              @contextmenu.prevent="showContextMenuForCharacter($event, character)"
              @click="hideContextMenu">
            <td>{{ character.adventureName }}</td>
            <td>{{ character.characterName }}</td>
            <td>{{ character.serverId }}</td>
            <td>{{ character.level || 'N/A' }}</td>
            <td>{{ formatNumber(character.fame) }}</td>
            <td>{{ formatNumber(character.buffPower) }}</td>
            <td>{{ formatNumber(character.totalDamage) }}</td>
            <td>
              <div class="dungeon-clear-status">
                <div class="dungeon-item" :class="{ cleared: character.dungeonClearNabel }">
                  <span class="dungeon-name">나벨</span>
                  <span class="clear-status">{{ character.dungeonClearNabel ? '✅' : '❌' }}</span>
                </div>
                <div class="dungeon-item" :class="{ cleared: character.dungeonClearVenus }">
                  <span class="dungeon-name">베누스</span>
                  <span class="clear-status">{{ character.dungeonClearVenus ? '✅' : '❌' }}</span>
                </div>
                <div class="dungeon-item" :class="{ cleared: character.dungeonClearFog }">
                  <span class="dungeon-name">안개신</span>
                  <span class="clear-status">{{ character.dungeonClearFog ? '✅' : '❌' }}</span>
                </div>
              </div>
            </td>
            <td>
              <div class="action-buttons">
                <button @click="refreshCharacterFromDB(character)" class="refresh-char-btn" :disabled="refreshing">
                  🔄
                </button>
                <button @click="checkDungeonClear(character)" class="dungeon-check-btn" :disabled="checkingDungeon">
                  🏰
                </button>
                <button @click="toggleFavorite(character)" class="favorite-btn" :class="{ 'is-favorite': character.isFavorite }">
                  {{ character.isFavorite ? '⭐' : '☆' }}
                </button>
                <button @click="removeCharacterFromDB(character.characterId)" class="remove-btn">
                  ❌
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 검색 기록이 없을 때 -->
    <div v-else-if="searchHistory.length === 0" class="no-data">
      <p>검색 기록이 없습니다. 캐릭터 검색 페이지에서 캐릭터를 검색해보세요.</p>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading">
      <p>캐릭터 정보를 불러오는 중...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { dundamService } from '../services/dundamService';
import { apiFetch } from '../config/api';

// 검색 기록 인터페이스 (Local Storage에 저장)
interface SearchRecord {
  id: string;
  serverId: string;
  serverName: string;
  adventureName: string;
  characterName: string;
  characterId: string;
  timestamp: string;
}

// DB 캐릭터 인터페이스
interface CharacterFromDB {
  characterId: string;
  serverId: string;
  characterName: string;
  adventureName: string;
  fame: number;
  buffPower?: number;
  totalDamage?: number;
  level?: number;
  jobName?: string;
  dungeonClearNabel?: boolean;
  dungeonClearVenus?: boolean;
  dungeonClearFog?: boolean;
  isFavorite?: boolean;
  excludedDungeons?: string[];
}

// 반응형 데이터
const searchHistory = ref<SearchRecord[]>([]);
const charactersFromDB = ref<CharacterFromDB[]>([]);
const selectedAdventures = ref<string[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const checkingDungeon = ref(false);

// 컨텍스트 메뉴 관련
const showContextMenu = ref(false);
const contextMenuStyle = ref({ top: '0px', left: '0px' });
const selectedCharacter = ref<CharacterFromDB | null>(null);

// 사용 가능한 모험단 목록 (검색 기록에서 추출)
const availableAdventures = computed(() => {
  const adventures = new Set<string>();
  searchHistory.value.forEach(record => {
    if (record.adventureName) {
      adventures.add(record.adventureName);
    }
  });
  return Array.from(adventures).sort();
});

// 컴포넌트 마운트 시 검색 기록 로드
onMounted(() => {
  loadSearchHistory();
});

// 검색 기록 로드 (Local Storage에서)
const loadSearchHistory = () => {
  try {
    const saved = localStorage.getItem('df_search_history');
    if (saved) {
      searchHistory.value = JSON.parse(saved);
    }
  } catch (error) {
    console.error('검색 기록 로드 실패:', error);
    searchHistory.value = [];
  }
};

// 검색 기록 저장 (Local Storage에)
const saveSearchHistory = () => {
  try {
    localStorage.setItem('df_search_history', JSON.stringify(searchHistory.value));
  } catch (error) {
    console.error('검색 기록 저장 실패:', error);
  }
};

// 검색 기록에 추가
const addSearchRecord = (record: Omit<SearchRecord, 'id' | 'timestamp'>) => {
  const newRecord: SearchRecord = {
    ...record,
    id: Date.now().toString(),
    timestamp: new Date().toISOString()
  };
  
  // 중복 제거 (같은 캐릭터 ID가 있으면 업데이트)
  const existingIndex = searchHistory.value.findIndex(r => r.characterId === record.characterId);
  if (existingIndex >= 0) {
    searchHistory.value[existingIndex] = newRecord;
  } else {
    searchHistory.value.unshift(newRecord); // 맨 앞에 추가
  }
  
  saveSearchHistory();
};

// 검색 기록에서 캐릭터 불러오기
const loadCharacterFromHistory = async (record: SearchRecord) => {
  try {
    loading.value = true;
    
    // 백엔드 API에서 캐릭터 정보 조회
    const response = await apiFetch(`/characters/${record.serverId}/${record.characterId}`);
    if (response.ok) {
      const characterData = await response.json();
      // 캐릭터 정보를 DB 목록에 추가
      const existingIndex = charactersFromDB.value.findIndex(c => c.characterId === record.characterId);
      if (existingIndex >= 0) {
        charactersFromDB.value[existingIndex] = characterData;
      } else {
        charactersFromDB.value.push(characterData);
      }
    }
  } catch (error) {
    console.error('캐릭터 정보 로드 실패:', error);
  } finally {
    loading.value = false;
  }
};

// DB에서 선택된 모험단의 캐릭터들 로드
const loadCharactersFromDB = async () => {
  if (selectedAdventures.value.length === 0) {
    charactersFromDB.value = [];
    return;
  }
  
  try {
    loading.value = true;
    
    // 백엔드 API에서 모험단별 캐릭터 목록 조회
    const promises = selectedAdventures.value.map(async (adventure) => {
      const response = await apiFetch(`/characters/adventure/${adventure}`);
      if (response.ok) {
        return await response.json();
      }
      return [];
    });
    
    const results = await Promise.all(promises);
    charactersFromDB.value = results.flat();
    
  } catch (error) {
    console.error('캐릭터 목록 로드 실패:', error);
  } finally {
    loading.value = false;
  }
};

// 검색 기록에서 제거
const removeSearchRecord = (id: string) => {
  searchHistory.value = searchHistory.value.filter(record => record.id !== id);
  saveSearchHistory();
};

// DB에서 캐릭터 제거
const removeCharacterFromDB = async (characterId: string) => {
  try {
    const response = await apiFetch(`/characters/${characterId}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      charactersFromDB.value = charactersFromDB.value.filter(c => c.characterId !== characterId);
    }
  } catch (error) {
    console.error('캐릭터 제거 실패:', error);
  }
};

// DB에서 캐릭터 정보 새로고침
const refreshCharacterFromDB = async (character: CharacterFromDB) => {
  if (refreshing.value) return;
  
  try {
    refreshing.value = true;
    
    // 백엔드 API에서 캐릭터 정보 새로고침
    const response = await apiFetch(`/characters/${character.serverId}/${character.characterId}/refresh`);
    if (response.ok) {
      const updatedCharacter = await response.json();
      const index = charactersFromDB.value.findIndex(c => c.characterId === character.characterId);
      if (index >= 0) {
        charactersFromDB.value[index] = updatedCharacter;
      }
    }
  } catch (error) {
    console.error('캐릭터 정보 새로고침 실패:', error);
  } finally {
    refreshing.value = false;
  }
};

// 던전 클리어 현황 확인
const checkDungeonClear = async (character: CharacterFromDB) => {
  if (checkingDungeon.value) return;
  
  try {
    checkingDungeon.value = true;
    
    // 백엔드 API에서 던전 클리어 현황 확인
    const response = await apiFetch(`/dungeon-clear/${character.serverId}/${character.characterId}`);
    if (response.ok) {
      const dungeonInfo = await response.json();
      
      // DB 업데이트
      const index = charactersFromDB.value.findIndex(c => c.characterId === character.characterId);
      if (index >= 0) {
        charactersFromDB.value[index] = {
          ...charactersFromDB.value[index],
          dungeonClearNabel: dungeonInfo.nabel,
          dungeonClearVenus: dungeonInfo.venus,
          dungeonClearFog: dungeonInfo.fog
        };
      }
    }
  } catch (error) {
    console.error('던전 클리어 현황 확인 실패:', error);
  } finally {
    checkingDungeon.value = false;
  }
};

// 유틸리티 함수들
const formatNumber = (num?: number): string => {
  if (num === undefined || num === null) return 'N/A';
  return num.toLocaleString();
};

// 컨텍스트 메뉴 관련 함수들
const showContextMenuForCharacter = (event: MouseEvent, character: CharacterFromDB) => {
  event.preventDefault();
  selectedCharacter.value = character;
  showContextMenu.value = true;
  contextMenuStyle.value = {
    top: `${event.clientY}px`,
    left: `${event.clientX}px`
  };
};

const hideContextMenu = () => {
  showContextMenu.value = false;
  selectedCharacter.value = null;
};

const setFavoriteCharacter = async () => {
  if (!selectedCharacter.value) return;
  
  try {
    const response = await apiFetch(`/characters/${selectedCharacter.value.serverId}/${selectedCharacter.value.characterId}/favorite`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        isFavorite: !selectedCharacter.value.isFavorite
      })
    });
    
    if (response.ok) {
      const index = charactersFromDB.value.findIndex(c => c.characterId === selectedCharacter.value!.characterId);
      if (index >= 0) {
        charactersFromDB.value[index].isFavorite = !charactersFromDB.value[index].isFavorite;
      }
    }
  } catch (error) {
    console.error('업둥이 설정 실패:', error);
  } finally {
    hideContextMenu();
  }
};

const excludeFromDungeon = async (dungeonType: string) => {
  if (!selectedCharacter.value) return;
  
  try {
    const currentExcluded = selectedCharacter.value.excludedDungeons || [];
    const isCurrentlyExcluded = currentExcluded.includes(dungeonType);
    
    let newExcluded: string[];
    if (isCurrentlyExcluded) {
      newExcluded = currentExcluded.filter(d => d !== dungeonType);
    } else {
      newExcluded = [...currentExcluded, dungeonType];
    }
    
    const response = await apiFetch(`/characters/${selectedCharacter.value.serverId}/${selectedCharacter.value.characterId}/exclude`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        excludedDungeons: newExcluded
      })
    });
    
    if (response.ok) {
      const index = charactersFromDB.value.findIndex(c => c.characterId === selectedCharacter.value!.characterId);
      if (index >= 0) {
        charactersFromDB.value[index].excludedDungeons = newExcluded;
      }
    }
  } catch (error) {
    console.error('던전 제외 설정 실패:', error);
  } finally {
    hideContextMenu();
  }
};

const isExcludedFromDungeon = (dungeonType: string): boolean => {
  if (!selectedCharacter.value) return false;
  return (selectedCharacter.value.excludedDungeons || []).includes(dungeonType);
};

const toggleFavorite = async (character: CharacterFromDB) => {
  try {
    const response = await apiFetch(`/characters/${character.serverId}/${character.characterId}/favorite`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        isFavorite: !character.isFavorite
      })
    });
    
    if (response.ok) {
      character.isFavorite = !character.isFavorite;
    }
  } catch (error) {
    console.error('업둥이 설정 실패:', error);
  }
};

// 외부에서 호출할 수 있도록 함수 노출
defineExpose({
  addSearchRecord
});
</script>

<style scoped>
.character-list {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.search-history-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.search-history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.search-record {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: white;
  border-radius: 6px;
  border: 1px solid #dee2e6;
}

.server-name {
  font-weight: bold;
  color: #495057;
}

.adventure-name {
  color: #6c757d;
}

.character-name {
  color: #212529;
}

.load-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.load-btn:hover {
  background: #0056b3;
}

.adventure-selection {
  margin-bottom: 30px;
  padding: 20px;
  background: #e9ecef;
  border-radius: 8px;
}

.adventure-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 15px;
}

.adventure-checkbox {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

.characters-section {
  margin-bottom: 30px;
}

.characters-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
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
}

.dungeon-clear-status {
  display: flex;
  gap: 10px;
  font-size: 0.9rem;
}

.dungeon-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.dungeon-name {
  font-weight: bold;
  color: #333;
}

.clear-status {
  font-size: 1.1rem;
}

.dungeon-item.cleared .clear-status {
  color: #28a745;
}

.dungeon-item.cleared .dungeon-name {
  color: #28a745;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.refresh-char-btn,
.dungeon-check-btn,
.remove-btn {
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.refresh-char-btn {
  background: #17a2b8;
  color: white;
}

.refresh-char-btn:hover:not(:disabled) {
  background: #138496;
}

.dungeon-check-btn {
  background: #28a745;
  color: white;
}

.dungeon-check-btn:hover:not(:disabled) {
  background: #218838;
}

.remove-btn {
  background: #dc3545;
  color: white;
}

.remove-btn:hover {
  background: #c82333;
}

.refresh-char-btn:disabled,
.dungeon-check-btn:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #6c757d;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #007bff;
}

/* 컨텍스트 메뉴 스타일 */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 200px;
  padding: 8px 0;
}

.context-menu-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.context-menu-item:hover {
  background-color: #f8f9fa;
}

.context-menu-item:first-child {
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.context-menu-item:last-child {
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
}

/* 업둥이 버튼 스타일 */
.favorite-btn {
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  background: #ffc107;
  color: #212529;
}

.favorite-btn:hover {
  background: #e0a800;
}

.favorite-btn.is-favorite {
  background: #ffc107;
  color: #212529;
}

/* 제외된 캐릭터 스타일 */
.character-excluded {
  opacity: 0.6;
  background-color: #f8f9fa;
}

.character-excluded .character-name {
  text-decoration: line-through;
  color: #6c757d;
}
</style> 