import { apiClient } from './client';
import type { Team } from './types';

export type TeamRequest = {
  name: string;
  description?: string | null;
};

export const teamApi = {
  create: async (request: TeamRequest) => (await apiClient.post<Team>('/teams', request)).data,
  getAll: async () => (await apiClient.get<Team[]>('/teams')).data,
  getById: async (teamId: number) => (await apiClient.get<Team>(`/teams/${teamId}`)).data,
  update: async (teamId: number, request: TeamRequest) =>
    (await apiClient.patch<Team>(`/teams/${teamId}`, request)).data,
  remove: async (teamId: number) => apiClient.delete(`/teams/${teamId}`),
};
