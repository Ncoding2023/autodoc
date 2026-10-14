import { apiClient } from './client';
import type { Document, DocumentListItem, SheetColumnType, WritingFormat } from './types';

export type SheetDraft = {
  columns: { columnKey: string; columnName: string; columnType: SheetColumnType }[];
  rows: { rowData: Record<string, unknown> }[];
};

export type DocumentCreateRequest = {
  templateId: number;
  title: string;
  writingFormat: WritingFormat;
  documentType: string;
  bodyContent?: string | null;
  importantNotes?: string | null;
  cautions?: string | null;
  sheet?: SheetDraft;
};

export type DocumentUpdateRequest = Omit<DocumentCreateRequest, 'templateId' | 'writingFormat'>;

export const documentApi = {
  create: async (request: DocumentCreateRequest) => (await apiClient.post<Document>('/documents', request)).data,
  getMyDocuments: async (signal?: AbortSignal) => (await apiClient.get<DocumentListItem[]>('/documents', { signal })).data,
  getById: async (documentId: number, signal?: AbortSignal) => (await apiClient.get<Document>(`/documents/${documentId}`, { signal })).data,
  update: async (documentId: number, request: DocumentUpdateRequest) =>
    (await apiClient.patch<Document>(`/documents/${documentId}`, request)).data,
  remove: async (documentId: number) => apiClient.delete(`/documents/${documentId}`),
};
