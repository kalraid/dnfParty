// API 설정
export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL,
  WS_URL: import.meta.env.VITE_WS_BASE_URL,
  TIMEOUT: 10000,
};

// API URL 헬퍼 함수
export const getApiUrl = (path: string): string => {
  if (!API_CONFIG.BASE_URL) {
    throw new Error('VITE_API_BASE_URL environment variable is not set');
  }
  return `${API_CONFIG.BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
};

// WebSocket URL 헬퍼 함수
export const getWsUrl = (path: string): string => {
  if (!API_CONFIG.WS_URL) {
    throw new Error('VITE_WS_BASE_URL environment variable is not set');
  }
  return `${API_CONFIG.WS_URL}${path.startsWith('/') ? path : `/${path}`}`;
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
