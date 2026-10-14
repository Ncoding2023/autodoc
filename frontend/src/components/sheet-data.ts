import type { SheetColumn, SheetColumnType } from '../api/types';
import type { SheetDraft } from '../api/document-api';

export const columnTypes: Record<SheetColumnType, string> = { TEXT: '문자', NUMBER: '숫자', DATE: '날짜', BOOLEAN: '예/아니오' };

export function readColumnInput(data: FormData) {
  const columnName = String(data.get('columnName') ?? '').trim();
  const columnType = String(data.get('columnType') ?? '') as SheetColumnType;
  if (!columnName || columnName.length > 100) return { error: '열 이름을 1~100자로 입력해 주세요.' };
  if (!Object.hasOwn(columnTypes, columnType)) return { error: '열의 데이터 형식을 선택해 주세요.' };
  return { input: { columnName, columnType } };
}

export function readRowInput(data: FormData, columns: SheetColumn[], original: Record<string, unknown> = {}) {
  if (!columns.length) return { error: '먼저 열을 추가해 주세요.' };
  const entries = new Map(Object.entries(original));
  for (const column of columns) {
    const text = String(data.get(`cell:${column.columnKey}`) ?? '');
    let value: unknown = text;
    if (column.columnType !== 'TEXT' && !text.trim()) value = null;
    else if (column.columnType === 'NUMBER') {
      value = Number(text);
      if (!Number.isFinite(value)) return { error: `${column.columnName}: 올바른 숫자를 입력해 주세요.` };
    } else if (column.columnType === 'DATE') {
      const date = new Date(`${text}T00:00:00Z`);
      if (!/^\d{4}-\d{2}-\d{2}$/.test(text) || Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== text)
        return { error: `${column.columnName}: 올바른 날짜를 입력해 주세요.` };
    } else if (column.columnType === 'BOOLEAN') {
      if (text !== 'true' && text !== 'false') return { error: `${column.columnName}: 예 또는 아니오를 선택해 주세요.` };
      value = text === 'true';
    }
    entries.set(column.columnKey, value);
  }
  return { rowData: Object.fromEntries(entries) };
}

export function displayCell(value: unknown) {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? '예' : '아니오';
  return typeof value === 'string' || typeof value === 'number' ? String(value) : '지원하지 않는 값';
}

export function moveColumn(columns: SheetColumn[], columnId: number, direction: -1 | 1) {
  const index = columns.findIndex((column) => column.id === columnId);
  const next = index + direction;
  if (index < 0 || next < 0 || next >= columns.length) return null;
  const ids = columns.map((column) => column.id);
  [ids[index], ids[next]] = [ids[next], ids[index]];
  return ids;
}

export function readSheetDraft(value: string): { sheet?: SheetDraft; error?: string } {
  try {
    const data = JSON.parse(value);
    if (!data || !Array.isArray(data.columns) || !Array.isArray(data.rows)) throw new Error();
    const columns = data.columns as SheetDraft['columns'];
    const keys = new Set<string>();
    for (const column of columns) {
      if (!column || typeof column.columnKey !== 'string' || !column.columnKey.trim() || column.columnKey.length > 100 || keys.has(column.columnKey)
        || typeof column.columnName !== 'string' || !column.columnName.trim() || column.columnName.length > 100 || !Object.hasOwn(columnTypes, column.columnType)) throw new Error();
      keys.add(column.columnKey);
    }
    if (data.rows.length && !columns.length) throw new Error();
    for (const row of data.rows) {
      if (!row?.rowData || typeof row.rowData !== 'object' || Array.isArray(row.rowData)) throw new Error();
      for (const [key, cell] of Object.entries(row.rowData)) {
        const column = columns.find((item) => item.columnKey === key);
        if (!column) throw new Error();
        if (cell === null) continue;
        if (column.columnType === 'TEXT' && typeof cell !== 'string') throw new Error();
        if (column.columnType === 'NUMBER' && (typeof cell !== 'number' || !Number.isFinite(cell))) throw new Error();
        if (column.columnType === 'BOOLEAN' && typeof cell !== 'boolean') throw new Error();
        if (column.columnType === 'DATE') {
          if (typeof cell !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(cell)) throw new Error();
          const date = new Date(`${cell}T00:00:00Z`);
          if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== cell) throw new Error();
        }
      }
    }
    return { sheet: { columns, rows: data.rows } };
  } catch { return { error: '시트의 열 이름, 형식과 입력값을 확인해 주세요.' }; }
}
