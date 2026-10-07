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
  getAll: async () => (await apiClient.get<DocumentTemplate[]>('/templates')).data,
  getById: async (templateId: number) => (await apiClient.get<DocumentTemplate>(`/templates/${templateId}`)).data,
  create: async (request: TemplateCreateRequest) => (await apiClient.post<DocumentTemplate>('/templates', request)).data,
  update: async (templateId: number, request: TemplateUpdateRequest) =>
    (await apiClient.patch<DocumentTemplate>(`/templates/${templateId}`, request)).data,
  remove: async (templateId: number) => apiClient.delete(`/templates/${templateId}`),
};
