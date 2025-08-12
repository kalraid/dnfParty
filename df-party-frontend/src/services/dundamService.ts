import axios from 'axios';

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
      
      // 임시로 더미 데이터 반환 (실제 구현 시 제거)
      return this.getMockCharacterInfo(serverId, characterId);
      
    } catch (error) {
      console.error('dundam.xyz에서 캐릭터 정보 조회 실패:', error);
      return null;
    }
  },

  // 더미 데이터 생성 (개발용, 실제 구현 시 제거)
  getMockCharacterInfo(serverId: string, characterId: string): DundamCharacterInfo {
    // 랜덤한 버프력과 총딜 생성 (실제로는 API에서 가져와야 함)
    const buffPower = Math.floor(Math.random() * 1000000) + 100000; // 10만 ~ 110만
    const totalDamage = Math.floor(Math.random() * 10000000000) + 1000000000; // 10억 ~ 110억
    
    return {
      characterName: '테스트캐릭터',
      serverId,
      characterId,
      buffPower,
      totalDamage,
      fame: Math.floor(Math.random() * 100000) + 10000,
      level: Math.floor(Math.random() * 20) + 100,
      jobName: '버퍼',
      lastUpdated: new Date().toISOString()
    };
  },

  // 버프력과 총딜 정보만 업데이트
  async updateCharacterStats(
    serverId: string,
    characterId: string
  ): Promise<{ buffPower?: number; totalDamage?: number } | null> {
    try {
      const characterInfo = await this.getCharacterInfo(serverId, characterId);
      if (characterInfo) {
        return {
          buffPower: characterInfo.buffPower,
          totalDamage: characterInfo.totalDamage
        };
      }
      return null;
    } catch (error) {
      console.error('캐릭터 스탯 업데이트 실패:', error);
      return null;
    }
  }
};

export default dundamService; 