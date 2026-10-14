import { apiClient } from './client';
import type { DocumentTemplate, WritingFormat } from './types';

export type TemplateCreateRequest = {
  name: string;
  writingFormat: WritingFormat;
  documentType: string;
  structureData: string;
  templateStyleData: string;
};

export type TemplateUpdateRequest = Omit<TemplateCreateRequest, 'writingFormat'>;

export const templateApi = {
  getAll: async (signal?: AbortSignal) => (await apiClient.get<DocumentTemplate[]>('/templates', { signal })).data,
  getById: async (templateId: number, signal?: AbortSignal) => (await apiClient.get<DocumentTemplate>(`/templates/${templateId}`, { signal })).data,
  create: async (request: TemplateCreateRequest) => (await apiClient.post<DocumentTemplate>('/templates', request)).data,
  update: async (templateId: number, request: TemplateUpdateRequest) =>
    (await apiClient.patch<DocumentTemplate>(`/templates/${templateId}`, request)).data,
  remove: async (templateId: number) => apiClient.delete(`/templates/${templateId}`),
};
