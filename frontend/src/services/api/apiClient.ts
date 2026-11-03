import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ENV } from '@/config/env';
import { ApiErrorResponse } from '@/types';

// Storage key for token session
export const TOKEN_STORAGE_KEY = 'aethel_session_token';
export const USER_STORAGE_KEY = 'aethel_session_user';

export const isMockSession = (): boolean => {
  try {
    const userStr = localStorage.getItem(USER_STORAGE_KEY);
    if (!userStr) return false;
    const user = JSON.parse(userStr);
    return Boolean(user?.isMock || user?.token?.startsWith('demo-jwt-'));
  } catch {
    return false;
  }
};

export const apiClient = axios.create({
  baseURL: ENV.API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  timeout: 60000, // 60s timeout accommodates Render cold starts
});

// Centralized Request Interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    try {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (token && !token.startsWith('demo-jwt-') && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Ignore localStorage access failure in restricted environments
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Centralized Response Interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response) {
      // 401 Unauthorized handling
      if (error.response.status === 401) {
        try {
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(USER_STORAGE_KEY);
        } catch {}
        // Dispatch custom event so React AuthProvider can react without page refresh
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      }

      // Extract structured backend error message
      const errorData = error.response.data;
      const message =
        errorData?.message ||
        errorData?.error ||
        `Request failed with status ${error.response.status}`;

      return Promise.reject(new Error(message));
    } else if (error.request) {
      return Promise.reject(
        new Error('Network error: Unable to connect to banking backend server.')
      );
    }
    return Promise.reject(error);
  }
);

