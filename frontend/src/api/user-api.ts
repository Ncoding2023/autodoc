import { apiClient } from './client';
import type { User } from './types';

export type UserCreateRequest = {
  teamId?: number | null;
  email: string;
  password: string;
  name: string;
  team?: { name: string; description?: string };
};

export type UserUpdateRequest = {
  teamId?: number | null;
  name: string;
};

export const userApi = {
  getSignupTeams: async () => (await apiClient.get<{ id: number; name: string }[]>('/users/signup-teams')).data,
  signUp: async (request: UserCreateRequest) => (await apiClient.post<User>('/users', request)).data,
  getUser: async (userId: number) => (await apiClient.get<User>(`/users/${userId}`)).data,
  updateUser: async (userId: number, request: UserUpdateRequest) =>
    (await apiClient.patch<User>(`/users/${userId}`, request)).data,
};
