<template>
  <div class="character-search">
    <h2>캐릭터 검색</h2>
    
    <!-- 검색 폼 -->
    <div class="search-form">
      <div class="form-group">
        <label for="server">서버 선택:</label>
        <select id="server" v-model="selectedServer" required>
          <option value="">서버를 선택하세요</option>
          <option value="all">전체 서버</option>
          <option v-for="server in servers" :key="server.serverId" :value="server.serverId">
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
          required
        >
      </div>
      
      <button @click="searchCharacters" :disabled="searching" class="search-btn">
        {{ searching ? '검색 중...' : '검색' }}
      </button>
    </div>

    <!-- 검색 결과 -->
    <div v-if="searchResults.length > 0" class="search-results">
      <h3>검색 결과 ({{ searchResults.length }}개)</h3>
      <div class="results-grid">
        <div 
          v-for="character in searchResults" 
          :key="character.characterId" 
          class="character-card"
        >
          <div class="character-info">
            <h4>{{ character.characterName }}</h4>
            <p><strong>서버:</strong> {{ getServerName(character.serverId) }}</p>
            <p><strong>모험단:</strong> {{ character.adventureName || 'N/A' }}</p>
            <p><strong>레벨:</strong> {{ character.level || 'N/A' }}</p>
            <p><strong>직업:</strong> {{ character.jobName || 'N/A' }}</p>
            <p><strong>명성:</strong> {{ formatNumber(character.fame) }}</p>
          </div>
          
          <div class="character-stats" v-if="character.buffPower || character.totalDamage">
            <h5>스펙 정보</h5>
            <p><strong>버프력:</strong> {{ formatNumber(character.buffPower) }}</p>
            <p><strong>총딜:</strong> {{ formatNumber(character.totalDamage) }}</p>
          </div>
          
          <div class="character-actions">
            <button @click="saveCharacterToDB(character)" class="save-btn">
              DB에 저장
            </button>
            <button @click="addToSearchHistory(character)" class="history-btn">
              검색 기록에 추가
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 검색 기록 -->
    <div v-if="searchHistory.length > 0" class="search-history">
      <h3>최근 검색 기록</h3>
      <div class="history-list">
        <div v-for="record in searchHistory" :key="record.id" class="history-item">
          <span class="server-name">{{ record.serverName }}</span>
          <span class="adventure-name">{{ record.adventureName }}</span>
          <span class="character-name">{{ record.characterName }}</span>
          <span class="timestamp">{{ formatDate(record.timestamp) }}</span>
          <button @click="loadCharacterFromHistory(record)" class="load-btn">불러오기</button>
          <button @click="removeFromHistory(record.id)" class="remove-btn">삭제</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { dfApiService, type Server } from '../services/dfApi';

// 검색 기록 인터페이스
interface SearchRecord {
  id: string;
  serverId: string;
  serverName: string;
  adventureName: string;
  characterName: string;
  characterId: string;
  timestamp: string;
}

// 반응형 데이터
const selectedServer = ref('');
const characterName = ref('');
const servers = ref<Server[]>([]);
const searchResults = ref<any[]>([]);
const searchHistory = ref<SearchRecord[]>([]);
const searching = ref(false);
const error = ref('');
const successMessage = ref('');

// 컴포넌트 마운트 시 서버 목록과 검색 기록 로드
onMounted(async () => {
  await loadServers();
  loadSearchHistory();
});

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

// 캐릭터 검색
const searchCharacters = async () => {
  if (!characterName.value.trim()) {
    error.value = '캐릭터명을 입력해주세요.';
    return;
  }

  try {
    searching.value = true;
    error.value = '';
    successMessage.value = '';

    // 백엔드 API를 통한 통합 캐릭터 검색
    const response = await fetch(`http://localhost:8080/api/characters/search?serverId=${selectedServer.value || 'all'}&characterName=${encodeURIComponent(characterName.value)}`);
    
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        searchResults.value = Array.isArray(data.characters) ? data.characters : [data.character];
        successMessage.value = `${searchResults.value.length}개의 캐릭터를 찾았습니다.`;
      } else {
        error.value = data.message || '캐릭터 검색에 실패했습니다.';
      }
    } else {
      error.value = '캐릭터 검색 중 오류가 발생했습니다.';
    }

  } catch (err) {
    console.error('캐릭터 검색 실패:', err);
    error.value = '캐릭터 검색 중 오류가 발생했습니다.';
  } finally {
    searching.value = false;
  }
};

// 캐릭터를 DB에 저장
const saveCharacterToDB = async (character: any) => {
  try {
    const response = await fetch('http://localhost:8080/api/characters', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(character)
    });

    if (response.ok) {
      successMessage.value = `${character.characterName} 캐릭터가 DB에 저장되었습니다.`;
      // 검색 기록에도 자동 추가
      addToSearchHistory(character);
    } else {
      error.value = '캐릭터 저장에 실패했습니다.';
    }
  } catch (err) {
    console.error('캐릭터 저장 실패:', err);
    error.value = '캐릭터 저장 중 오류가 발생했습니다.';
  }
};

// 검색 기록에 추가
const addToSearchHistory = (character: any) => {
  const newRecord: SearchRecord = {
    id: Date.now().toString(),
    serverId: character.serverId,
    serverName: getServerName(character.serverId),
    adventureName: character.adventureName || 'N/A',
    characterName: character.characterName,
    characterId: character.characterId,
    timestamp: new Date().toISOString()
  };

  // 중복 제거 (같은 캐릭터 ID가 있으면 업데이트)
  const existingIndex = searchHistory.value.findIndex(r => r.characterId === character.characterId);
  if (existingIndex >= 0) {
    searchHistory.value[existingIndex] = newRecord;
  } else {
    searchHistory.value.unshift(newRecord); // 맨 앞에 추가
  }

  // 최대 50개까지만 유지
  if (searchHistory.value.length > 50) {
    searchHistory.value = searchHistory.value.slice(0, 50);
  }

  saveSearchHistory();
  successMessage.value = '검색 기록에 추가되었습니다.';
};

// 검색 기록에서 캐릭터 불러오기
const loadCharacterFromHistory = async (record: SearchRecord) => {
  try {
    // 백엔드 API에서 캐릭터 정보 조회
    const response = await fetch(`http://localhost:8080/api/characters/${record.serverId}/${record.characterId}`);
    if (response.ok) {
      const characterData = await response.json();
      // 검색 결과에 표시
      searchResults.value = [characterData];
      successMessage.value = '검색 기록에서 캐릭터를 불러왔습니다.';
    }
  } catch (error) {
    console.error('캐릭터 정보 로드 실패:', error);
    this.error = '캐릭터 정보를 불러오는데 실패했습니다.';
  }
};

// 검색 기록에서 제거
const removeFromHistory = (id: string) => {
  searchHistory.value = searchHistory.value.filter(record => record.id !== id);
  saveSearchHistory();
  successMessage.value = '검색 기록에서 제거되었습니다.';
};

// 유틸리티 함수들
const getServerName = (serverId: string): string => {
  const server = servers.value.find(s => s.serverId === serverId);
  return server?.serverName || serverId;
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

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('ko-KR');
};
</script>

<style scoped>
.character-search {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-form {
  background: #f8f9fa;
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
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 15px;
}

.character-card {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 4px;
  margin-top: 20px;
  text-align: center;
}

.success-message {
  background: #d4edda;
  color: #155724;
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
  
  .history-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .character-actions {
    flex-direction: column;
  }
}
</style> 