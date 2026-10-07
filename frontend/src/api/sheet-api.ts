import { apiClient } from './client';
import type { SheetColumn, SheetColumnType, SheetRow } from './types';

export type SheetColumnCreateRequest = {
  columnKey: string;
  columnName: string;
  columnType: SheetColumnType;
};

export type SheetColumnUpdateRequest = Omit<SheetColumnCreateRequest, 'columnKey'>;

export type SheetRowRequest = {
  rowData: Record<string, unknown>;
};

const documentPath = (documentId: number) => `/documents/${documentId}`;

export const sheetApi = {
  addColumn: async (documentId: number, request: SheetColumnCreateRequest) =>
    (await apiClient.post<SheetColumn>(`${documentPath(documentId)}/columns`, request)).data,
  getColumns: async (documentId: number) =>
    (await apiClient.get<SheetColumn[]>(`${documentPath(documentId)}/columns`)).data,
  updateColumn: async (documentId: number, columnId: number, request: SheetColumnUpdateRequest) =>
    (await apiClient.patch<SheetColumn>(`${documentPath(documentId)}/columns/${columnId}`, request)).data,
  updateColumnOrder: async (documentId: number, columnIds: number[]) =>
    (await apiClient.patch<SheetColumn[]>(`${documentPath(documentId)}/columns/order`, { columnIds })).data,
  removeColumn: async (documentId: number, columnId: number) =>
    apiClient.delete(`${documentPath(documentId)}/columns/${columnId}`),
  addRow: async (documentId: number, request: SheetRowRequest) =>
    (await apiClient.post<SheetRow>(`${documentPath(documentId)}/rows`, request)).data,
  getRows: async (documentId: number) =>
    (await apiClient.get<SheetRow[]>(`${documentPath(documentId)}/rows`)).data,
  updateRow: async (documentId: number, rowId: number, request: SheetRowRequest) =>
    (await apiClient.patch<SheetRow>(`${documentPath(documentId)}/rows/${rowId}`, request)).data,
  removeRow: async (documentId: number, rowId: number) =>
    apiClient.delete(`${documentPath(documentId)}/rows/${rowId}`),
};
