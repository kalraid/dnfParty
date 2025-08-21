// 캐릭터 관련 타입
export interface Character {
  characterId: string
  characterName: string
  serverId: string
  adventureName?: string
  jobName?: string
  jobGrowName?: string
  job?: string
  fame?: number
  level?: number
  isFavorite?: boolean
  excludedDungeons?: string[]
  totalDamage?: number
  buffPower?: number
  combatPower?: number
  isHardNabelEligible?: boolean
  isNormalNabelEligible?: boolean
  isMatchingNabelEligible?: boolean
  isTwilightEligible?: boolean
  selectedNabelDifficulty?: 'hard' | 'normal' | 'matching'
  createdAt?: string
  updatedAt?: string
  // 던전 클리어 상태
  dungeonClearNabel?: boolean;
  dungeonClearVenus?: boolean;
  dungeonClearFog?: boolean;
  dungeonClearTwilight?: boolean;
  // 던전 제외 상태
  isExcludedNabel?: boolean
  isExcludedVenus?: boolean
  isExcludedFog?: boolean
  // 수동 입력 스탯
  manualTotalDamage?: number
  manualBuffPower?: number
  // 마지막 업데이트 시간
  lastDungeonCheck?: string
  lastStatsUpdate?: string
}

// 서버 관련 타입
export interface Server {
  serverId: string
  serverName: string
}

// 파티 슬롯 타입
export interface PartySlot {
  slotNumber: number
  characterId?: string
  characterName?: string
  role?: string
  fame?: number
  isFavorite?: boolean
}

// 파티 구성 타입
export interface Party {
  type: string
  size: number
  composition: string
  slots: PartySlot[]
  efficiency: number
}

// 8인 파티 타입
export interface EightPersonParty {
  type: string
  dungeonName: string
  createdAt: string
  party1: Party
  party2: Party
  totalEfficiency: number
  analysis: PartyAnalysis
}

// 파티 분석 타입
export interface PartyAnalysis {
  totalDealers: number
  totalBuffers: number
  totalUpdoongis: number
  totalFame: number
  averageFame: number
  party1Efficiency: number
  party2Efficiency: number
  efficiencyGap: number
  isBalanced: boolean
}

// 파티 검증 결과 타입
export interface PartyValidation {
  isValid: boolean
  errors: string[]
  warnings: string[]
  totalEmptySlots: number
  totalCharacters: number
}

// 최적화 제안 타입
export interface OptimizationSuggestion {
  suggestions: string[]
  improvement: 'BETTER' | 'WORSE' | 'SAME' | 'NEW'
  currentEfficiency?: number
  newEfficiency?: number
}

// 최적화 결과 타입
export interface OptimizationResult {
  newParty: EightPersonParty
  optimization: OptimizationSuggestion
}

// 던전 요구사항 타입
export interface DungeonRequirements {
  minFame: number
  minDealers: number
  minBuffers: number
}

// API 응답 타입
export interface ApiResponse<T> {
  data?: T
  error?: string
  message?: string
}

// 페이지네이션 타입
export interface Pagination {
  page: number
  size: number
  total: number
  totalPages: number
}

// 검색 필터 타입
export interface SearchFilter {
  serverId?: string
  jobName?: string
  minFame?: number
  maxFame?: number
  isFavorite?: boolean
  excludedDungeons?: string[]
}

// 고급 최적화 관련 타입
export interface AdvancedOptimizationRequest {
  serverId: string
  characterIds: string[]
  dungeonName: string
  partySize: number
  optimizationStrategy: string
}

export interface AdvancedOptimizationResult {
  optimizationType: string
  executionTime: number
  requestInfo: {
    serverId: string
    characterCount: number
    dungeonName: string
    partySize: number
    optimizationStrategy: string
  }
  // 4인 파티 결과
  party?: Party
  efficiency?: number
  // 8인 파티 결과
  party1?: Party
  party2?: Party
  totalEfficiency?: number
  // 밸런스 최적화 결과
  balanceScore?: number
  // 시너지 최적화 결과
  synergyScore?: number
  // 안전성 최적화 결과
  safetyScore?: number
  // 하이브리드 최적화 결과
  hybridScores?: {
    efficiency: number
    balance: number
    synergy: number
  }
}

export interface StrategyComparisonResult {
  bestStrategy: string
  bestScore: number
  recommendedStrategy: string
  scores: {
    [key: string]: {
      score: number
      rank: number
    }
  }
  detailedResults: {
    efficiency: AdvancedOptimizationResult
    balance: AdvancedOptimizationResult
    synergy: AdvancedOptimizationResult
    safety: AdvancedOptimizationResult
  }
  requestInfo: {
    serverId: string
    characterCount: number
    dungeonName: string
    partySize: number
  }
}

export interface PerformanceTestResult {
  testType: string
  iterations: number
  totalExecutionTime: number
  averageExecutionTime: number
  minExecutionTime: number
  maxExecutionTime: number
  executionTimes: number[]
}

export interface OptimizationStrategy {
  id: string
  name: string
  description: string
}

export interface OptimizationConfig {
  availableStrategies: OptimizationStrategy[]
  dungeonWeights: {
    [key: string]: {
      efficiency: number
      balance: number
      synergy: number
    }
  }
  optimizationSettings: {
    maxIterations: number
    convergenceThreshold: number
    enableParallelProcessing: boolean
    cacheResults: boolean
  }
}

// 파티 추천 관련 타입
export interface PartyRecommendationRequest {
  serverId: string
  characterIds: string[]
  dungeonName: string
  partySize: number
  preferences?: {
    strategy?: string
    preferUpdoongis?: boolean
    preferHighFame?: boolean
    preferJobSynergy?: boolean
  }
}

export interface PartyRecommendationResult {
  recommendation: Party | EightPersonParty
  analysis: {
    totalScore: number
    dungeonCompatibility: number
    overallRating: number
    roleBalance?: {
      dealers: number
      buffers: number
      updoongis: number
      isBalanced: boolean
    }
    fameBalance?: {
      average: number
      variance: number
      isBalanced: boolean
    }
    balance?: number
  }
  alternatives: (Party | EightPersonParty)[]
  strategy: string
  dungeonRequirements: {
    dungeonName: string
    minFame: number
    requiredRoles: string[]
    preferredJobs: string[]
    difficulty: string
  }
  characterAnalysis: {
    totalCharacters: number
    dealers: number
    buffers: number
    updoongis: number
    others: number
    averageFame: number
  }
  generatedAt: string
}

export interface DungeonSpecificRecommendations {
  party4: PartyRecommendationResult
  party8: PartyRecommendationResult
  optimalStrategy: string
  specializedParty: Party | EightPersonParty
  dungeonRequirements: {
    dungeonName: string
    minFame: number
    requiredRoles: string[]
    preferredJobs: string[]
    difficulty: string
  }
  characterPool: {
    totalCharacters: number
    dealers: number
    buffers: number
    updoongis: number
    others: number
    averageFame: number
  }
}

export interface PersonalizedRecommendationRequest {
  serverId: string
  characterIds: string[]
  dungeonName: string
  partySize: number
  userPreferences?: {
    strategy?: string
    preferUpdoongis?: boolean
    preferHighFame?: boolean
    preferJobSynergy?: boolean
  }
  playHistory?: {
    frequentDungeons?: string[]
    preferredPartySize?: number
    successRate?: number
    favoriteJobs?: string[]
  }
}

export interface PersonalizedRecommendationResult extends PartyRecommendationResult {
  personalizationFactors: {
    strategy: string
    preferUpdoongis: boolean
    preferHighFame: boolean
    preferJobSynergy: boolean
    favoriteJobs?: string[]
  }
  playPatterns: {
    frequentDungeons?: string[]
    preferredPartySize?: number
    successRate?: number
  }
  userPreferences: {
    strategy?: string
    preferUpdoongis?: boolean
    preferHighFame?: boolean
    preferJobSynergy?: boolean
  }
}

export interface RecommendationStrategy {
  id: string
  name: string
  description: string
}

export interface RecommendationConfig {
  availableStrategies: RecommendationStrategy[]
  dungeonWeights: {
    [key: string]: {
      efficiency: number
      balance: number
      safety: number
    }
  }
  recommendationSettings: {
    maxIterations: number
    convergenceThreshold: number
    enableParallelProcessing: boolean
    cacheResults: boolean
    maxCacheSize: number
    cacheExpirationHours: number
  }
}

export interface RecommendationHistory {
  page: number
  size: number
  totalElements: number
  content: Array<{
    id: string
    serverId: string
    dungeonName: string
    strategy: string
    partySize: number
    createdAt: string
    successRate?: number
  }>
}

export interface RecommendationStats {
  totalRecommendations: number
  popularDungeons: Array<{
    dungeonName: string
    count: number
    successRate: number
  }>
  popularStrategies: Array<{
    strategy: string
    count: number
    successRate: number
  }>
  averageSuccessRate: number
}
