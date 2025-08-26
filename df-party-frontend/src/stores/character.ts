import { defineStore } from 'pinia'
import type { Character, Server } from '@/types'

interface SearchRecord {
  id: string;
  serverId: string;
  serverName: string;
  adventureName: string;
  characterName: string;
  characterId: string;
  timestamp: string;
}

export const useCharacterStore = defineStore('character', {
  state: () => ({
    characters: [] as Character[],
    servers: [] as Server[],
    selectedServer: '',
    searchQuery: '',
    loading: false,
    error: null as string | null,
    searchHistory: [] as SearchRecord[],
    adventures: new Map<string, Character[]>(), // 모험단별 캐릭터 그룹
    lockedCharacters: new Set<string>(), // 잠긴 캐릭터 ID 목록
  }),

  getters: {
    filteredCharacters: (state) => {
      if (!state.searchQuery) return state.characters
      
      return state.characters.filter(character =>
        character.characterName.toLowerCase().includes(state.searchQuery.toLowerCase()) ||
        character.jobName?.toLowerCase().includes(state.searchQuery.toLowerCase())
      )
    },

    favoriteCharacters: (state) => {
      return state.characters.filter(character => character.isFavorite)
    },

    charactersByServer: (state) => {
      return (serverId: string) => state.characters.filter(c => c.serverId === serverId)
    },

    charactersByJob: (state) => {
      return (jobName: string) => state.characters.filter(c => c.jobName === jobName)
    },

    highFameCharacters: (state) => {
      return (minFame: number) => state.characters.filter(c => (c.fame || 0) >= minFame)
    },

    // 잠금되지 않은 캐릭터만 반환 (자동 파티 구성용)
    unlockedCharacters: (state) => {
      return state.characters.filter(character => !state.lockedCharacters.has(character.characterId))
    },

    // 잠긴 캐릭터만 반환
    lockedCharactersList: (state) => {
      return state.characters.filter(character => state.lockedCharacters.has(character.characterId))
    },

    // 특정 캐릭터의 잠금 상태 확인
    isCharacterLocked: (state) => {
      return (characterId: string) => state.lockedCharacters.has(characterId)
    },

    // 모험단별 그룹화
    charactersByAdventure: (state) => {
      const grouped = new Map<string, Character[]>();
      state.characters.forEach(character => {
        const adventureName = character.adventureName || 'N/A';
        if (!grouped.has(adventureName)) {
          grouped.set(adventureName, []);
        }
        grouped.get(adventureName)!.push(character);
      });
      return grouped;
    },

    // 모험단 목록
    adventureNames: (state) => {
      const names = new Set<string>();
      state.characters.forEach(character => {
        if (character.adventureName && character.adventureName !== 'N/A') {
          names.add(character.adventureName);
        }
      });
      return Array.from(names).sort();
    },

    // 검색 기록에서 모험단 목록
    adventureNamesFromHistory: (state) => {
      const names = new Set<string>();
      state.searchHistory.forEach(record => {
        if (record.adventureName && record.adventureName !== 'N/A') {
          names.add(record.adventureName);
        }
      });
      return Array.from(names).sort();
    },
  },

  actions: {
    async loadServers() {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/dfo/servers')
        if (!response.ok) throw new Error('서버 목록을 불러올 수 없습니다.')
        
        const data = await response.json()
        this.servers = data
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('서버 목록 로드 실패:', error)
      } finally {
        this.loading = false
      }
    },

    async loadCharacters(serverId: string) {
      if (!serverId) return
      
      this.loading = true
      this.error = null
      this.selectedServer = serverId
      
      try {
        const response = await fetch(`/api/characters/server/${serverId}`)
        if (!response.ok) throw new Error('캐릭터 목록을 불러올 수 없습니다.')
        
        const data = await response.json()
        this.characters = data
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('캐릭터 목록 로드 실패:', error)
      } finally {
        this.loading = false
      }
    },

    async searchCharacter(serverId: string, characterName: string) {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch(`/api/dfo/characters/search?serverId=${serverId}&characterName=${encodeURIComponent(characterName)}`)
        if (!response.ok) throw new Error('캐릭터 검색에 실패했습니다.')
        
        const data = await response.json()
        return data
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('캐릭터 검색 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async toggleFavorite(character: Character) {
      try {
        const response = await fetch(`/api/characters/${character.serverId}/${character.characterId}/favorite`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            isFavorite: !character.isFavorite
          })
        })
        
        if (!response.ok) throw new Error('즐겨찾기 상태 변경에 실패했습니다.')
        
        // 로컬 상태 업데이트
        const index = this.characters.findIndex(c => c.characterId === character.characterId)
        if (index !== -1) {
          this.characters[index].isFavorite = !character.isFavorite
        }
        
        return true
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('즐겨찾기 토글 실패:', error)
        return false
      }
    },

    async excludeFromDungeon(character: Character, dungeonName: string) {
      try {
        const response = await fetch(`/api/characters/${character.serverId}/${character.characterId}/exclude`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            dungeonName
          })
        })
        
        if (!response.ok) throw new Error('던전 제외 설정에 실패했습니다.')
        
        // 로컬 상태 업데이트
        const index = this.characters.findIndex(c => c.characterId === character.characterId)
        if (index !== -1) {
          if (!this.characters[index].excludedDungeons) {
            this.characters[index].excludedDungeons = []
          }
          
          const dungeonIndex = this.characters[index].excludedDungeons!.indexOf(dungeonName)
          if (dungeonIndex === -1) {
            this.characters[index].excludedDungeons!.push(dungeonName)
          } else {
            this.characters[index].excludedDungeons!.splice(dungeonIndex, 1)
          }
        }
        
        return true
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('던전 제외 설정 실패:', error)
        return false
      }
    },

    setSearchQuery(query: string) {
      this.searchQuery = query
    },

    clearError() {
      this.error = null
    },

    clearCharacters() {
      this.characters = []
      this.selectedServer = ''
    },

    // 검색 기록 관리
    loadSearchHistory() {
      try {
        const saved = localStorage.getItem('df_search_history');
        if (saved) {
          this.searchHistory = JSON.parse(saved);
        }
      } catch (error) {
        console.error('검색 기록 로드 실패:', error);
        this.searchHistory = [];
      }
    },

    saveSearchHistory() {
      try {
        localStorage.setItem('df_search_history', JSON.stringify(this.searchHistory));
      } catch (error) {
        console.error('검색 기록 저장 실패:', error);
      }
    },

    addToSearchHistory(characters: Character | Character[]) {
      const charactersArray = Array.isArray(characters) ? characters : [characters];
      let addedCount = 0;

      charactersArray.forEach(character => {
        const newRecord: SearchRecord = {
          id: `${character.characterId}_${Date.now()}`,
          serverId: character.serverId,
          serverName: this.servers.find(s => s.serverId === character.serverId)?.serverName || character.serverId,
          adventureName: character.adventureName || 'N/A',
          characterName: character.characterName,
          characterId: character.characterId,
          timestamp: new Date().toISOString()
        };

        // 중복 제거 (같은 캐릭터 ID가 있으면 업데이트)
        const existingIndex = this.searchHistory.findIndex(r => r.characterId === character.characterId);
        if (existingIndex >= 0) {
          this.searchHistory[existingIndex] = newRecord;
        } else {
          this.searchHistory.unshift(newRecord); // 맨 앞에 추가
          addedCount++;
        }
      });

      // 최대 50개까지만 유지
      if (this.searchHistory.length > 50) {
        this.searchHistory = this.searchHistory.slice(0, 50);
      }

      this.saveSearchHistory();
      return addedCount;
    },

    removeFromSearchHistory(id: string) {
      const index = this.searchHistory.findIndex(r => r.id === id);
      if (index >= 0) {
        this.searchHistory.splice(index, 1);
        this.saveSearchHistory();
        return true;
      }
      return false;
    },

    clearSearchHistory() {
      this.searchHistory = [];
      this.saveSearchHistory();
    },

    // 캐릭터 잠금/해제 토글
    toggleCharacterLock(characterId: string) {
      if (this.lockedCharacters.has(characterId)) {
        this.lockedCharacters.delete(characterId);
      } else {
        this.lockedCharacters.add(characterId);
      }
      this.saveLockedCharacters();
    },

    // 특정 캐릭터 잠금
    lockCharacter(characterId: string) {
      this.lockedCharacters.add(characterId);
      this.saveLockedCharacters();
    },

    // 특정 캐릭터 잠금 해제
    unlockCharacter(characterId: string) {
      this.lockedCharacters.delete(characterId);
      this.saveLockedCharacters();
    },

    // 모든 캐릭터 잠금 해제
    unlockAllCharacters() {
      this.lockedCharacters.clear();
      this.saveLockedCharacters();
    },

    // 잠금 상태 로컬 스토리지에 저장
    saveLockedCharacters() {
      try {
        const lockedArray = Array.from(this.lockedCharacters);
        localStorage.setItem('df_locked_characters', JSON.stringify(lockedArray));
      } catch (error) {
        console.error('잠금 상태 저장 실패:', error);
      }
    },

    // 잠금 상태 로컬 스토리지에서 복원
    loadLockedCharacters() {
      try {
        const saved = localStorage.getItem('df_locked_characters');
        if (saved) {
          const lockedArray = JSON.parse(saved);
          this.lockedCharacters = new Set(lockedArray);
        }
      } catch (error) {
        console.error('잠금 상태 복원 실패:', error);
        this.lockedCharacters = new Set();
      }
    },
  },
})
