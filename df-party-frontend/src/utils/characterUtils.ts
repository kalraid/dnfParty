import type { Character } from '@/types'

/**
 * 캐릭터가 버퍼인지 확인하는 통합 함수
 * @param character 캐릭터 객체
 * @returns 버퍼 여부
 */
export const isBuffer = (character: Character): boolean => {
  if (!character || !character.jobGrowName) return false
  
  const jobGrowName = character.jobGrowName.toLowerCase()
  
  // 버퍼 직업 목록
  const bufferJobs = [
    '크루세이더',
    '뮤즈', 
    '패러메딕',
    '인챈트리스',
    '헤카테'
  ]
  
  return bufferJobs.some(job => jobGrowName.includes(job.toLowerCase()))
}

/**
 * 캐릭터가 딜러인지 확인하는 함수
 * @param character 캐릭터 객체
 * @returns 딜러 여부
 */
export const isDealer = (character: Character): boolean => {
  return !isBuffer(character)
}

/**
 * 캐릭터의 직업명을 정리하는 함수
 * @param jobName 직업명
 * @returns 정리된 직업명
 */
export const cleanJobName = (jobName: string): string => {
  if (!jobName) return ''
  return jobName.replaceAll('眞', '').trim()
}

/**
 * 캐릭터의 직업 타입을 반환하는 함수
 * @param character 캐릭터 객체
 * @returns 'buffer' | 'dealer'
 */
export const getCharacterType = (character: Character): 'buffer' | 'dealer' => {
  return isBuffer(character) ? 'buffer' : 'dealer'
}
