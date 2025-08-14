import axios from 'axios';

// 백엔드 API 사용 (환경변수에서 가져오기)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const dfApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface Server {
  serverId: string;
  serverName: string;
}

export interface Character {
  serverId: string;
  characterId: string;
  characterName: string;
  jobId: string;
  jobGrowId: string;
  jobName: string;
  jobGrowName: string;
  level: number;
  adventureName: string;
  fame: number;
}

export interface CharacterDetail {
  serverId: string;
  characterId: string;
  characterName: string;
  jobId: string;
  jobGrowId: string;
  jobName: string;
  jobGrowName: string;
  level: number;
  adventureName: string;
  fame: number;
  status?: any;
  equipment?: any[];
  buffSkill?: any;
}

export interface CharacterSearchResponse {
  success: boolean;
  character: CharacterDetail;
  dundamInfo: {
    buffPower: number;
    totalDamage: number;
    source: string;
  };
  message: string;
}

export const dfApiService = {
  async getServers(): Promise<Server[]> {
    try {
      const response = await dfApiClient.get('/dfo/servers');
      return response.data;
    } catch (error) {
      console.error('서버 목록 조회 실패:', error);
      throw error;
    }
  },

  async searchCharacters(serverId: string, characterName: string, options: any = {}): Promise<Character[]> {
    try {
      const response = await dfApiClient.get('/dfo/characters/search', {
        params: {
          serverId,
          characterName,
          ...options
        }
      });
      return response.data;
    } catch (error) {
      console.error('캐릭터 검색 실패:', error);
      throw error;
    }
  },

  async getCharacterDetail(serverId: string, characterId: string): Promise<CharacterDetail> {
    try {
      const response = await dfApiClient.get(`/dfo/characters/${serverId}/${characterId}`);
      return response.data;
    } catch (error) {
      console.error('캐릭터 상세 정보 조회 실패:', error);
      throw error;
    }
  },

  async getCharacterTimeline(serverId: string, characterId: string, options: any = {}) {
    try {
      const response = await dfApiClient.get(`/dfo/characters/${serverId}/${characterId}/timeline`, {
        params: options
      });
      return response.data;
    } catch (error) {
      console.error('캐릭터 타임라인 조회 실패:', error);
      throw error;
    }
  },

  // 통합된 캐릭터 검색 (백엔드 API 사용)
  async searchCharacterComplete(serverId: string, characterName: string): Promise<CharacterSearchResponse> {
    try {
      const response = await dfApiClient.get('/characters/search', {
        params: {
          serverId,
          characterName
        }
      });
      return response.data;
    } catch (error) {
      console.error('통합 캐릭터 검색 실패:', error);
      throw error;
    }
  },

  // 캐릭터 스펙 업데이트
  async updateCharacterStats(serverId: string, characterId: string) {
    try {
      const response = await dfApiClient.post(`/characters/${serverId}/${characterId}/update-stats`);
      return response.data;
    } catch (error) {
      console.error('캐릭터 스펙 업데이트 실패:', error);
      throw error;
    }
  }
};

export default dfApiService; 