// API 설정
const getBaseUrl = (): string => {
  // 개발 환경에서는 로컬 백엔드 사용
  if (import.meta.env.DEV) {
    return 'http://localhost:8080/api'
  }
  // 프로덕션에서는 환경 변수 사용
  return import.meta.env.VITE_API_BASE_URL || '/api'
}

const getWsBaseUrl = (): string => {
  // 개발 환경에서는 로컬 백엔드 사용
  if (import.meta.env.DEV) {
    return 'http://localhost:8080'
  }
  // 프로덕션에서는 환경 변수 사용
  return import.meta.env.VITE_WS_BASE_URL || ''
}

// 런타임에 환경 변수를 읽는 함수
export const getCurrentApiConfig = () => ({
  BASE_URL: getBaseUrl(),
  WS_URL: getWsBaseUrl(),
  TIMEOUT: 10000,
});

// 기존 API_CONFIG는 하위 호환성을 위해 유지 (빌드 시점 값)
export const API_CONFIG = getCurrentApiConfig();

// API URL 헬퍼 함수
export const getApiUrl = (path: string): string => {
  const config = getCurrentApiConfig();
  if (!config.BASE_URL) {
    throw new Error('VITE_API_BASE_URL environment variable is not set');
  }
  return `${config.BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
};

// WebSocket URL 헬퍼 함수
export const getWsUrl = (path: string): string => {
  const config = getCurrentApiConfig();
  if (!config.WS_URL) {
    throw new Error('VITE_WS_BASE_URL environment variable is not set');
  }
  return `${config.WS_URL}${path.startsWith('/') ? path : `/${path}`}`;
};

// Fetch 헬퍼 함수
export const apiFetch = async (path: string, options: RequestInit = {}): Promise<Response> => {
  const url = getApiUrl(path);
  return fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });
};

// WebSocket 헬퍼 함수
export const createWebSocket = (path: string): WebSocket => {
  const url = getWsUrl(path);
  return new WebSocket(url);
};
