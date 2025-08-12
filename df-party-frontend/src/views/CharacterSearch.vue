<template>
  <div class="character-search">
    <h1>캐릭터 검색</h1>
    
    <!-- 검색 폼 -->
    <div class="search-form">
      <div class="form-group">
        <label for="server">서버 선택:</label>
        <select 
          id="server" 
          v-model="selectedServer" 
          @change="onServerChange"
          :disabled="loading"
        >
          <option value="">서버를 선택하세요</option>
          <option value="all">전체</option>
          <option 
            v-for="server in servers" 
            :key="server.serverId" 
            :value="server.serverId"
          >
            {{ server.serverName }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label for="characterName">캐릭터명:</label>
        <input 
          id="characterName" 
          v-model="characterName" 
          type="text" 
          placeholder="캐릭터명을 입력하세요"
          :disabled="loading"
        />
      </div>

      <button 
        @click="searchCharacters" 
        :disabled="!canSearch || loading"
        class="search-btn"
      >
        {{ loading ? '검색 중...' : '검색' }}
      </button>
    </div>

    <!-- 검색 결과 -->
    <div v-if="searchResults.length > 0" class="search-results">
      <h2>검색 결과</h2>
      <div class="results-grid">
        <div 
          v-for="character in searchResults" 
          :key="character.characterId"
          class="character-card"
          @click="selectCharacter(character)"
        >
          <h3>{{ character.characterName }}</h3>
          <p><strong>레벨:</strong> {{ character.level }}</p>
          <p><strong>직업:</strong> {{ character.jobName }}</p>
          <p><strong>전직:</strong> {{ character.jobGrowName }}</p>
          <p><strong>모험단:</strong> {{ character.adventureName }}</p>
          <p v-if="character.guildName"><strong>길드:</strong> {{ character.guildName }}</p>
        </div>
      </div>
    </div>

    <!-- 선택된 캐릭터 정보 -->
    <div v-if="selectedCharacterDetail" class="selected-character">
      <h2>선택된 캐릭터 정보</h2>
      <div class="character-detail">
        <h3>{{ selectedCharacterDetail.characterName }}</h3>
        <p><strong>레벨:</strong> {{ selectedCharacterDetail.level }}</p>
        <p><strong>직업:</strong> {{ selectedCharacterDetail.jobName }}</p>
        <p><strong>전직:</strong> {{ selectedCharacterDetail.jobGrowName }}</p>
        <p><strong>모험단:</strong> {{ selectedCharacterDetail.adventureName }}</p>
        <p><strong>명성:</strong> {{ selectedCharacterDetail.fame }}</p>
        
        <!-- dundam.xyz 정보 표시 -->
        <div v-if="dundamInfo" class="dundam-info">
          <h4>📊 던담 정보</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">버프력:</span>
              <span class="stat-value">{{ formatNumber(dundamInfo.buffPower) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">총딜:</span>
              <span class="stat-value">{{ formatNumber(dundamInfo.totalDamage) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">업데이트:</span>
              <span class="stat-value">{{ formatDate(dundamInfo.lastUpdated) }}</span>
            </div>
          </div>
        </div>
        
        <div class="action-buttons">
          <button @click="updateDundamInfo" class="update-btn" :disabled="loading">
            {{ loading ? '업데이트 중...' : '던담 정보 업데이트' }}
          </button>
          <button @click="saveCharacter" class="save-btn">캐릭터 저장</button>
        </div>
      </div>
    </div>

    <!-- 에러 메시지 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { dfApiService, type Server, type Character, type CharacterDetail } from '../services/dfApi';
import { dundamService, type DundamCharacterInfo } from '../services/dundamService';

// 반응형 데이터
const servers = ref<Server[]>([]);
const selectedServer = ref('');
const characterName = ref('');
const searchResults = ref<Character[]>([]);
const selectedCharacterDetail = ref<CharacterDetail | null>(null);
const dundamInfo = ref<DundamCharacterInfo | null>(null);
const loading = ref(false);
const error = ref('');

// 계산된 속성
const canSearch = computed(() => {
  return selectedServer.value && characterName.value.trim().length > 0;
});

// 메서드들
const loadServers = async () => {
  try {
    loading.value = true;
    error.value = '';
    const serverList = await dfApiService.getServers();
    servers.value = serverList;
  } catch (err) {
    error.value = '서버 목록을 불러오는데 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const onServerChange = () => {
  // 서버 변경 시 검색 결과 초기화
  searchResults.value = [];
  selectedCharacterDetail.value = null;
  dundamInfo.value = null;
};

const searchCharacters = async () => {
  if (!canSearch.value) return;

  try {
    loading.value = true;
    error.value = '';
    
    const results = await dfApiService.searchCharacters(
      selectedServer.value,
      characterName.value.trim(),
      { limit: 20 }
    );
    
    searchResults.value = results;
    
    if (results.length === 0) {
      error.value = '검색 결과가 없습니다.';
    }
  } catch (err) {
    error.value = '캐릭터 검색에 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const selectCharacter = async (character: Character) => {
  try {
    loading.value = true;
    error.value = '';
    
    // 던파 API에서 캐릭터 상세 정보 조회
    const detail = await dfApiService.getCharacterDetail(
      selectedServer.value,
      character.characterId
    );
    
    selectedCharacterDetail.value = detail;
    
    // dundam.xyz에서 추가 정보 조회
    await updateDundamInfo();
    
  } catch (err) {
    error.value = '캐릭터 상세 정보를 불러오는데 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const updateDundamInfo = async () => {
  if (!selectedCharacterDetail.value) return;
  
  try {
    loading.value = true;
    error.value = '';
    
    const info = await dundamService.getCharacterInfo(
      selectedServer.value,
      selectedCharacterDetail.value.characterId
    );
    
    if (info) {
      dundamInfo.value = info;
    } else {
      error.value = '던담 정보를 가져오는데 실패했습니다.';
    }
  } catch (err) {
    error.value = '던담 정보 업데이트에 실패했습니다.';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const saveCharacter = () => {
  if (!selectedCharacterDetail.value) return;
  
  // 캐릭터 정보와 dundam 정보를 함께 저장
  const characterData = {
    serverId: selectedServer.value,
    characterId: selectedCharacterDetail.value.characterId,
    characterName: selectedCharacterDetail.value.characterName,
    adventureName: selectedCharacterDetail.value.adventureName,
    fame: selectedCharacterDetail.value.fame,
    buffPower: dundamInfo.value?.buffPower,
    totalDamage: dundamInfo.value?.totalDamage,
    savedAt: new Date().toISOString()
  };
  
  // 기존 저장된 캐릭터들 가져오기
  const savedCharacters = JSON.parse(
    localStorage.getItem('df_characters') || '[]'
  ) as Array<{
    characterId: string;
    serverId: string;
    characterName: string;
    adventureName: string;
    fame: number;
    buffPower?: number;
    totalDamage?: number;
    savedAt: string;
  }>;
  
  // 중복 체크 및 추가
  const existingIndex = savedCharacters.findIndex(
    (c) => c.characterId === characterData.characterId
  );
  
  if (existingIndex >= 0) {
    savedCharacters[existingIndex] = characterData;
  } else {
    savedCharacters.push(characterData);
  }
  
  // 로컬 스토리지에 저장
  localStorage.setItem('df_characters', JSON.stringify(savedCharacters));
  
  alert('캐릭터가 저장되었습니다!');
};

// 유틸리티 메서드
const formatNumber = (num?: number): string => {
  if (!num) return 'N/A';
  if (num >= 100000000) {
    return (num / 100000000).toFixed(1) + '억';
  } else if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만';
  }
  return num.toLocaleString();
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString('ko-KR');
};

// 컴포넌트 마운트 시 서버 목록 로드
onMounted(() => {
  loadServers();
});
</script>

<style scoped>
.character-search {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.search-form {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
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
  background: #ccc;
  cursor: not-allowed;
}

.search-results {
  margin-bottom: 30px;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.character-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.character-card:hover {
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.character-card h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.character-card p {
  margin: 5px 0;
  color: #666;
}

.selected-character {
  background: #e8f5e8;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.character-detail h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.character-detail p {
  margin: 5px 0;
  color: #666;
}

.dundam-info {
  background: #f0f8ff;
  padding: 20px;
  border-radius: 8px;
  margin: 20px 0;
  border-left: 4px solid #007bff;
}

.dundam-info h4 {
  margin: 0 0 15px 0;
  color: #007bff;
}

.stats-grid {
  display: grid;
  gap: 10px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #e0e0e0;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  font-weight: bold;
  color: #555;
}

.stat-value {
  color: #007bff;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.update-btn {
  background: #17a2b8;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.update-btn:hover:not(:disabled) {
  background: #138496;
}

.update-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.save-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.save-btn:hover {
  background: #218838;
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
  .character-search {
    padding: 10px;
  }
  
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons button {
    width: 100%;
  }
}
</style> 