import { apiClient } from '../api/apiClient';
import { AuthResponse, LoginRequest, RegisterRequest } from '@/types';

export const authService = {
  /**
   * Authenticate customer or administrator.
   * POST /api/auth/login
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/api/auth/login', request);
    return response.data;
  },

  /**
   * Register a new user and generate an initial bank account.
   * POST /api/auth/register
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/api/auth/register', request);
    return response.data;
  },
};

