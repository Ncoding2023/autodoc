import { apiClient } from './client';
import type { Document, DocumentListItem, WritingFormat } from './types';

export type DocumentCreateRequest = {
  templateId: number;
  title: string;
  writingFormat: WritingFormat;
  documentType: string;
  bodyContent?: string | null;
  importantNotes?: string | null;
  cautions?: string | null;
};

export type DocumentUpdateRequest = Omit<DocumentCreateRequest, 'templateId' | 'writingFormat'>;

export const documentApi = {
  create: async (request: DocumentCreateRequest) => (await apiClient.post<Document>('/documents', request)).data,
  getMyDocuments: async () => (await apiClient.get<DocumentListItem[]>('/documents')).data,
  getById: async (documentId: number) => (await apiClient.get<Document>(`/documents/${documentId}`)).data,
  update: async (documentId: number, request: DocumentUpdateRequest) =>
    (await apiClient.patch<Document>(`/documents/${documentId}`, request)).data,
  remove: async (documentId: number) => apiClient.delete(`/documents/${documentId}`),
};
