import { apiClient } from './client';
import type { User } from './types';

export type UserCreateRequest = {
  teamId?: number | null;
  email: string;
  password: string;
  name: string;
};

export type UserUpdateRequest = {
  teamId?: number | null;
  name: string;
};

export const userApi = {
  signUp: async (request: UserCreateRequest) => (await apiClient.post<User>('/users', request)).data,
  getUser: async (userId: number) => (await apiClient.get<User>(`/users/${userId}`)).data,
  updateUser: async (userId: number, request: UserUpdateRequest) =>
    (await apiClient.patch<User>(`/users/${userId}`, request)).data,
};
