import { defineStore } from 'pinia'
import type { Character, Server } from '@/types'

export const useCharacterStore = defineStore('character', {
  state: () => ({
    characters: [] as Character[],
    servers: [] as Server[],
    selectedServer: '',
    searchQuery: '',
    loading: false,
    error: null as string | null,
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
  },
})
