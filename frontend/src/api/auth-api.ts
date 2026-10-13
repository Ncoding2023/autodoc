import { apiClient } from './client';
import type { User } from './types';

export type LoginRequest = {
  email: string;
  password: string;
  rememberMe?: boolean;
};

export const authApi = {
  login: async (request: LoginRequest) => (await apiClient.post<User>('/auth/login', request)).data,
  logout: async () => apiClient.post('/auth/logout'),
  getCurrentUser: async () => (await apiClient.get<User>('/auth/me')).data,
};
