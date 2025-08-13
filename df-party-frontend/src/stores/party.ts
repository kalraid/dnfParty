import { defineStore } from 'pinia'
import type { Party, EightPersonParty, PartyAnalysis, PartyValidation, OptimizationResult, PartyRecommendationRequest, PartyRecommendationResult, DungeonSpecificRecommendations, PersonalizedRecommendationRequest, PersonalizedRecommendationResult, RecommendationConfig, RecommendationHistory, RecommendationStats } from '@/types'

export const usePartyStore = defineStore('party', {
  state: () => ({
    currentParty: null as EightPersonParty | null,
    partyHistory: [] as EightPersonParty[],
    loading: false,
    error: null as string | null,
    selectedDungeon: '',
    selectedCharacters: [] as string[],
    // 누락된 속성들 추가
    recommendationResult: null as any,
    dungeonSpecificRecommendations: null as any,
    personalizedRecommendation: null as any,
    recommendationConfig: null as any,
    recommendationHistory: null as any,
    recommendationStats: null as any,
  }),

  getters: {
    hasCurrentParty: (state) => state.currentParty !== null,
    
    currentPartyEfficiency: (state) => {
      if (!state.currentParty) return 0
      return state.currentParty.totalEfficiency
    },
    
    isPartyBalanced: (state) => {
      if (!state.currentParty?.analysis) return false
      return state.currentParty.analysis.isBalanced
    },
    
    partyStats: (state) => {
      if (!state.currentParty?.analysis) return null
      return state.currentParty.analysis
    },
    
    canCreateParty: (state) => {
      return state.selectedDungeon && state.selectedCharacters.length >= 8
    },
  },

  actions: {
    async createEightPersonParty(characterIds: string[], dungeonName: string) {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/eight-person-party/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            characterIds,
            dungeonName
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '8인 파티 구성에 실패했습니다.')
        }
        
        const result = await response.json()
        this.currentParty = result
        this.selectedDungeon = dungeonName
        this.selectedCharacters = characterIds
        
        // 히스토리에 추가
        this.partyHistory.unshift(result)
        if (this.partyHistory.length > 10) {
          this.partyHistory = this.partyHistory.slice(0, 10)
        }
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('8인 파티 구성 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async analyzeParty() {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/eight-person-party/analyze', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            party1: this.currentParty.party1,
            party2: this.currentParty.party2
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '파티 분석에 실패했습니다.')
        }
        
        const result = await response.json()
        
        // 현재 파티 업데이트
        if (this.currentParty) {
          this.currentParty.analysis = result
        }
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('파티 분석 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async validateParty() {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/eight-person-party/validate', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            party1: this.currentParty.party1,
            party2: this.currentParty.party2,
            dungeonName: this.selectedDungeon
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '파티 검증에 실패했습니다.')
        }
        
        const result = await response.json()
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('파티 검증 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async optimizeParty() {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/eight-person-party/optimize', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            characterIds: this.selectedCharacters,
            dungeonName: this.selectedDungeon,
            currentParty: this.currentParty
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '파티 최적화에 실패했습니다.')
        }
        
        const result = await response.json()
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('파티 최적화 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async swapCharacters(sourceSlot: number, targetSlot: number, partyKey: 'party1' | 'party2') {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-modification/swap', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            party: this.currentParty[partyKey],
            sourceSlot,
            targetSlot
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '캐릭터 교체에 실패했습니다.')
        }
        
        const result = await response.json()
        
        // 현재 파티 업데이트
        if (this.currentParty) {
          this.currentParty[partyKey] = result.party
        }
        
        // 파티 재분석
        await this.analyzeParty()
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('캐릭터 교체 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async addCharacter(characterId: string, slotNumber: number, partyKey: 'party1' | 'party2') {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-modification/add', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            party: this.currentParty[partyKey],
            characterId,
            slotNumber
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '캐릭터 추가에 실패했습니다.')
        }
        
        const result = await response.json()
        
        // 현재 파티 업데이트
        if (this.currentParty) {
          this.currentParty[partyKey] = result.party
        }
        
        // 파티 재분석
        await this.analyzeParty()
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('캐릭터 추가 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async removeCharacter(slotNumber: number, partyKey: 'party1' | 'party2') {
      if (!this.currentParty) return null
      
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-modification/remove', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            party: this.currentParty[partyKey],
            slotNumber
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.error || '캐릭터 제거에 실패했습니다.')
        }
        
        const result = await response.json()
        
        // 현재 파티 업데이트
        if (this.currentParty) {
          this.currentParty[partyKey] = result.party
        }
        
        // 파티 재분석
        await this.analyzeParty()
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('캐릭터 제거 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    setSelectedDungeon(dungeon: string) {
      this.selectedDungeon = dungeon
      // 던전 변경 시 파티 재구성
      if (this.currentParty) {
        this.currentParty = null
      }
    },

    setSelectedCharacters(characters: string[]) {
      this.selectedCharacters = characters
    },

    clearCurrentParty() {
      this.currentParty = null
    },

    clearError() {
      this.error = null
    },

    loadPartyFromHistory(party: EightPersonParty) {
      this.currentParty = party
      this.selectedDungeon = party.dungeonName
    },

    // 파티 추천 관련 액션들
    async generatePartyRecommendation(request: PartyRecommendationRequest) {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-recommendation/generate', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(request)
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '파티 구성 추천에 실패했습니다.')
        }
        
        const result = await response.json()
        this.recommendationResult = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('파티 구성 추천 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async generateDungeonSpecificRecommendations(serverId: string, characterIds: string[], dungeonName: string) {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-recommendation/dungeon-specific', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            serverId,
            characterIds,
            dungeonName
          })
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '던전별 추천 전략 생성에 실패했습니다.')
        }
        
        const result = await response.json()
        this.dungeonSpecificRecommendations = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('던전별 추천 전략 생성 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async generatePersonalizedRecommendation(request: PersonalizedRecommendationRequest) {
      this.loading = true
      this.error = null
      
      try {
        const response = await fetch('/api/party-recommendation/personalized', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(request)
        })
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '개인화된 파티 추천에 실패했습니다.')
        }
        
        const result = await response.json()
        this.personalizedRecommendation = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('개인화된 파티 추천 실패:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async getRecommendationConfig() {
      try {
        const response = await fetch('/api/party-recommendation/config')
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '추천 설정 조회에 실패했습니다.')
        }
        
        const result = await response.json()
        this.recommendationConfig = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('추천 설정 조회 실패:', error)
        throw error
      }
    },

    async getRecommendationHistory(params: {
      serverId?: string
      dungeonName?: string
      strategy?: string
      page?: number
      size?: number
    } = {}) {
      try {
        const searchParams = new URLSearchParams()
        if (params.serverId) searchParams.append('serverId', params.serverId)
        if (params.dungeonName) searchParams.append('dungeonName', params.dungeonName)
        if (params.strategy) searchParams.append('strategy', params.strategy)
        if (params.page !== undefined) searchParams.append('page', params.page.toString())
        if (params.size !== undefined) searchParams.append('size', params.size.toString())
        
        const response = await fetch(`/api/party-recommendation/history?${searchParams.toString()}`)
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '추천 히스토리 조회에 실패했습니다.')
        }
        
        const result = await response.json()
        this.recommendationHistory = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('추천 히스토리 조회 실패:', error)
        throw error
      }
    },

    async getRecommendationStats(serverId?: string, dungeonName?: string) {
      try {
        const searchParams = new URLSearchParams()
        if (serverId) searchParams.append('serverId', serverId)
        if (dungeonName) searchParams.append('dungeonName', dungeonName)
        
        const response = await fetch(`/api/party-recommendation/stats?${searchParams.toString()}`)
        
        if (!response.ok) {
          const errorData = await response.json()
          throw new Error(errorData.message || '추천 통계 조회에 실패했습니다.')
        }
        
        const result = await response.json()
        this.recommendationStats = result
        
        return result
      } catch (error) {
        this.error = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.'
        console.error('추천 통계 조회 실패:', error)
        throw error
      }
    },

    clearRecommendationData() {
      this.recommendationResult = null
      this.dungeonSpecificRecommendations = null
      this.personalizedRecommendation = null
    },
  },
})
