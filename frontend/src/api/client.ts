import axios from 'axios';

export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080';

const ACCESS_TOKEN_KEY = 'sqlgenie_access_token';
const REFRESH_TOKEN_KEY = 'sqlgenie_refresh_token';

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshInFlight: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }
  const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, { refreshToken });
  const { accessToken, refreshToken: newRefreshToken } = response.data;
  tokenStorage.setTokens(accessToken, newRefreshToken);
  return accessToken;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        refreshInFlight ??= refreshAccessToken().finally(() => {
          refreshInFlight = null;
        });
        const newAccessToken = await refreshInFlight;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch {
        tokenStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);
