<template>
  <div class="character-list">
    <h1>캐릭터 리스트</h1>
    
    <!-- 모험단 선택 -->
    <div class="adventure-selection">
      <h2>모험단 선택</h2>
      <div class="adventure-grid">
        <div 
          v-for="adventure in availableAdventures" 
          :key="adventure"
          class="adventure-card"
          :class="{ selected: selectedAdventures.includes(adventure) }"
          @click="toggleAdventure(adventure)"
        >
          <h3>{{ adventure }}</h3>
          <p>{{ getAdventureCharacterCount(adventure) }}명의 캐릭터</p>
        </div>
      </div>
    </div>

    <!-- 선택된 모험단의 캐릭터 목록 -->
    <div v-if="selectedAdventures.length > 0" class="character-section">
      <div class="section-header">
        <h2>선택된 모험단 캐릭터</h2>
        <button @click="refreshAllCharacters" class="refresh-btn" :disabled="refreshing">
          {{ refreshing ? '새로고침 중...' : '전체 새로고침' }}
        </button>
      </div>
      
      <div class="character-table">
        <table>
          <thead>
            <tr>
              <th>캐릭터명</th>
              <th>서버</th>
              <th>레벨</th>
              <th>직업</th>
              <th>명성</th>
              <th>버프력</th>
              <th>총딜</th>
              <th>저장일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="character in filteredCharacters" :key="character.characterId">
              <td>{{ character.characterName }}</td>
              <td>{{ getServerName(character.serverId) }}</td>
              <td>{{ character.level || 'N/A' }}</td>
              <td>{{ character.jobName || 'N/A' }}</td>
              <td>{{ character.fame?.toLocaleString() || 'N/A' }}</td>
              <td>{{ formatNumber(character.buffPower) }}</td>
              <td>{{ formatNumber(character.totalDamage) }}</td>
              <td>{{ formatDate(character.savedAt) }}</td>
              <td>
                <div class="action-buttons">
                  <button @click="refreshCharacter(character)" class="refresh-char-btn" :disabled="refreshing">
                    🔄
                  </button>
                  <button @click="removeCharacter(character)" class="remove-btn">
                    ❌
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 저장된 캐릭터가 없는 경우 -->
    <div v-else-if="!loading" class="no-characters">
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
import { dfApiService, type Server } from '../services/dfApi';
import { dundamService } from '../services/dundamService';

// 반응형 데이터
const servers = ref<Server[]>([]);
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
const selectedAdventures = ref<string[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const error = ref('');

// 계산된 속성
const availableAdventures = computed(() => {
  const adventures = new Set(savedCharacters.value.map(c => c.adventureName));
  return Array.from(adventures).sort();
});

const filteredCharacters = computed(() => {
  if (selectedAdventures.value.length === 0) return [];
  return savedCharacters.value.filter(c => 
    selectedAdventures.value.includes(c.adventureName)
  );
});

// 메서드들
const loadServers = async () => {
  try {
    const serverList = await dfApiService.getServers();
    servers.value = serverList;
  } catch (err) {
    console.error('서버 목록 로드 실패:', err);
  }
};

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

const getServerName = (serverId: string): string => {
  const server = servers.value.find(s => s.serverId === serverId);
  return server?.serverName || serverId;
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

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('ko-KR');
};

const refreshCharacter = async (character: any) => {
  try {
    refreshing.value = true;
    error.value = '';
    
    // dundam.xyz에서 최신 정보 가져오기
    const dundamInfo = await dundamService.getCharacterInfo(
      character.serverId,
      character.characterId
    );
    
    if (dundamInfo) {
      // 캐릭터 정보 업데이트
      const updatedCharacter = {
        ...character,
        buffPower: dundamInfo.buffPower,
        totalDamage: dundamInfo.totalDamage,
        lastUpdated: dundamInfo.lastUpdated
      };
      
      // 로컬 스토리지 업데이트
      const index = savedCharacters.value.findIndex(c => c.characterId === character.characterId);
      if (index >= 0) {
        savedCharacters.value[index] = updatedCharacter;
        localStorage.setItem('df_characters', JSON.stringify(savedCharacters.value));
      }
    }
  } catch (err) {
    error.value = '캐릭터 정보 새로고침에 실패했습니다.';
    console.error(err);
  } finally {
    refreshing.value = false;
  }
};

const refreshAllCharacters = async () => {
  try {
    refreshing.value = true;
    error.value = '';
    
    for (const character of filteredCharacters.value) {
      await refreshCharacter(character);
    }
    
    alert('모든 캐릭터 정보가 새로고침되었습니다!');
  } catch (err) {
    error.value = '전체 새로고침에 실패했습니다.';
    console.error(err);
  } finally {
    refreshing.value = false;
  }
};

const removeCharacter = (character: any) => {
  if (confirm(`정말로 ${character.characterName} 캐릭터를 삭제하시겠습니까?`)) {
    const index = savedCharacters.value.findIndex(c => c.characterId === character.characterId);
    if (index >= 0) {
      savedCharacters.value.splice(index, 1);
      localStorage.setItem('df_characters', JSON.stringify(savedCharacters.value));
    }
  }
};

// 컴포넌트 마운트 시 데이터 로드
onMounted(async () => {
  loading.value = true;
  await loadServers();
  loadSavedCharacters();
  loading.value = false;
});
</script>

<style scoped>
.character-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.adventure-selection {
  margin-bottom: 40px;
}

.adventure-selection h2 {
  color: #333;
  margin-bottom: 20px;
}

.adventure-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.adventure-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.adventure-card:hover {
  border-color: #007bff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.adventure-card.selected {
  border-color: #28a745;
  background: #f8fff9;
}

.adventure-card h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.adventure-card p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.character-section {
  margin-top: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  color: #333;
}

.refresh-btn {
  background: #17a2b8;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.refresh-btn:hover:not(:disabled) {
  background: #138496;
}

.refresh-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.character-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  background: #f8f9fa;
  font-weight: bold;
  color: #333;
}

td {
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.refresh-char-btn, .remove-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  font-size: 16px;
  transition: background 0.3s ease;
}

.refresh-char-btn:hover {
  background: #e3f2fd;
}

.remove-btn:hover {
  background: #ffebee;
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
  .character-list {
    padding: 10px;
  }
  
  .adventure-grid {
    grid-template-columns: 1fr;
  }
  
  .section-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .character-table {
    overflow-x: auto;
  }
  
  table {
    min-width: 800px;
  }
}
</style> 