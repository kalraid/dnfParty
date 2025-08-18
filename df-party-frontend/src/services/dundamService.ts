import axios from 'axios';
import { getApiUrl } from '../config/api';

// dundam.xyz API 설정
const DUNDAM_BASE_URL = 'https://dundam.xyz';

// dundam.xyz 응답 인터페이스
export interface DundamCharacterInfo {
  characterName: string;
  serverId: string;
  characterId: string;
  buffPower?: number;  // 버프력
  totalDamage?: number; // 총딜
  fame?: number;       // 명성
  level?: number;      // 레벨
  jobName?: string;    // 직업명
  lastUpdated?: string; // 마지막 업데이트 시간
}

// dundam.xyz 서비스
export const dundamService = {
  // 캐릭터 정보 조회
  async getCharacterInfo(
    serverId: string,
    characterId: string
  ): Promise<DundamCharacterInfo | null> {
    try {
      // dundam.xyz 사이트에서 캐릭터 정보를 가져오는 로직
      // 실제로는 웹 스크래핑이나 API 호출이 필요할 수 있음
      
      const url = `${DUNDAM_BASE_URL}/character?server=${serverId}&key=${characterId}`;
      
      // CORS 문제를 피하기 위해 프록시 서버를 사용하거나
      // 백엔드에서 처리하는 것이 좋습니다
      console.log('dundam.xyz URL:', url);
      
      // 실제 API 호출 구현 필요
      throw new Error('dundam.xyz API 구현이 필요합니다');
      
    } catch (error) {
      console.error('dundam.xyz에서 캐릭터 정보 조회 실패:', error);
      return null;
    }
  },



  // 버프력과 총딜 정보만 업데이트
  async updateCharacterStats(serverId: string, characterId: string): Promise<{ buffPower?: number; totalDamage?: number } | null> {
    try {
      // 실제 구현 시 백엔드 API 호출
      const response = await axios.get(getApiUrl(`/characters/${serverId}/${characterId}/update-stats`));
      return response.data.dundamInfo;
    } catch (error) {
      console.error('캐릭터 스펙 업데이트 실패:', error);
      return null;
    }
  },

  // 던전 클리어 현황 조회
  async getDungeonClearInfo(serverId: string, characterId: string): Promise<{
    nabel: boolean;
    venus: boolean;
    fog: boolean;
  } | null> {
    try {
      // 백엔드 API 호출
      const response = await axios.get(getApiUrl(`/dungeon-clear/${serverId}/${characterId}`));
      if (response.data.success) {
        return response.data.clearStatus;
      }
      return null;
    } catch (error) {
      console.error('던전 클리어 정보 조회 실패:', error);
      return null;
    }
  },


};

export default dundamService; 