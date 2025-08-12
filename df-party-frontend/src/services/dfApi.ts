import axios from 'axios';

// API 기본 설정
const API_BASE_URL = 'https://api.neople.co.kr/df';
const API_KEY = 'nJMykmaUtCURbOFEvieXPSzWgXmqePE2';

// API 클라이언트 생성
const dfApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API 응답 인터페이스
export interface Server {
  serverId: string;
  serverName: string;
}

export interface Character {
  characterId: string;
  characterName: string;
  level: number;
  jobId: string;
  jobGrowId: string;
  jobName: string;
  jobGrowName: string;
  adventureName: string;
  guildId?: string;
  guildName?: string;
}

export interface CharacterSearchResponse {
  rows: Character[];
}

export interface CharacterDetail {
  characterId: string;
  characterName: string;
  level: number;
  jobId: string;
  jobGrowId: string;
  jobName: string;
  jobGrowName: string;
  adventureName: string;
  guildId?: string;
  guildName?: string;
  fame: number;
  // 추가 필드들...
}

// API 메서드들
export const dfApiService = {
  // 서버 목록 조회
  async getServers(): Promise<Server[]> {
    try {
      const response = await dfApiClient.get(`/servers?apikey=${API_KEY}`);
      return response.data.rows || [];
    } catch (error) {
      console.error('서버 목록 조회 실패:', error);
      throw error;
    }
  },

  // 캐릭터 검색
  async searchCharacters(
    serverId: string,
    characterName: string,
    options: {
      jobId?: string;
      jobGrowId?: string;
      isAllJobGrow?: boolean;
      limit?: number;
      wordType?: 'match' | 'full';
    } = {}
  ): Promise<Character[]> {
    try {
      const params = new URLSearchParams({
        characterName: encodeURIComponent(characterName),
        apikey: API_KEY,
        ...options,
      });

      const response = await dfApiClient.get(
        `/servers/${serverId}/characters?${params.toString()}`
      );
      return response.data.rows || [];
    } catch (error) {
      console.error('캐릭터 검색 실패:', error);
      throw error;
    }
  },

  // 캐릭터 상세 정보 조회
  async getCharacterDetail(
    serverId: string,
    characterId: string
  ): Promise<CharacterDetail> {
    try {
      const response = await dfApiClient.get(
        `/servers/${serverId}/characters/${characterId}?apikey=${API_KEY}`
      );
      return response.data;
    } catch (error) {
      console.error('캐릭터 상세 정보 조회 실패:', error);
      throw error;
    }
  },

  // 캐릭터 타임라인 조회
  async getCharacterTimeline(
    serverId: string,
    characterId: string,
    options: {
      startDate?: string;
      endDate?: string;
      limit?: number;
      code?: string;
    } = {}
  ) {
    try {
      const params = new URLSearchParams({
        apikey: API_KEY,
        ...options,
      });

      const response = await dfApiClient.get(
        `/servers/${serverId}/characters/${characterId}/timeline?${params.toString()}`
      );
      return response.data;
    } catch (error) {
      console.error('캐릭터 타임라인 조회 실패:', error);
      throw error;
    }
  },
};

export default dfApiService; 