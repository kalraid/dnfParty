// API 설정
export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  WS_URL: import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080',
  TIMEOUT: 10000,
};

// API URL 헬퍼 함수
export const getApiUrl = (path: string): string => {
  return `${API_CONFIG.BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
};

// WebSocket URL 헬퍼 함수
export const getWsUrl = (path: string): string => {
  return `${API_CONFIG.WS_URL}${path.startsWith('/') ? path : `/${path}`}`;
};
