import { useRef, useState } from 'react';
import type { DocumentTemplate, SheetColumn, SheetColumnType } from '../api/types';
import type { SheetDraft } from '../api/document-api';
import { columnTypes, readRowInput, readSheetDraft } from './sheet-data';
import { inputClassName, secondaryClassName } from './document-ui';

function templateColumns(template: DocumentTemplate) {
  try {
    const structure = JSON.parse(template.structureData);
    const definitions = structure.columns ?? [];
    if (!Array.isArray(definitions)) return [];
    const result = readSheetDraft(JSON.stringify({ columns: definitions.map((column) => ({ columnKey: column.key, columnName: column.name, columnType: column.type })), rows: [] }));
    return (result.sheet?.columns ?? []).map((column, index) => ({ ...column, id: index + 1, columnOrder: index + 1 }));
  } catch { return []; }
}

type DraftRow = { id: number; values: Record<string, string> };

function serialize(columns: SheetColumn[], rows: DraftRow[]): { sheet?: SheetDraft; error?: string } {
  const data: SheetDraft = { columns: columns.map(({ columnKey, columnName, columnType }) => ({ columnKey, columnName, columnType })), rows: [] };
  for (const row of rows) {
    const form = new FormData();
    for (const column of columns) form.set(`cell:${column.columnKey}`, row.values[column.columnKey] ?? '');
    const result = readRowInput(form, columns);
    if (result.error) return { error: `${rows.indexOf(row) + 1}번 행: ${result.error}` };
    data.rows.push({ rowData: result.rowData! });
  }
  return readSheetDraft(JSON.stringify(data));
}

export function SheetDraftEditor({ template, initialSheet }: { template?: DocumentTemplate; initialSheet?: SheetDraft }) {
  const [columns, setColumns] = useState<SheetColumn[]>(() => initialSheet
    ? initialSheet.columns.map((column, index) => ({ ...column, id: index + 1, columnOrder: index + 1 }))
    : template ? templateColumns(template) : []);
  const [rows, setRows] = useState<DraftRow[]>(() => (initialSheet?.rows ?? []).map((row, index) => ({
    id: index + 1, values: Object.fromEntries(Object.entries(row.rowData).map(([key, value]) => [key, value == null ? '' : String(value)])),
  })));
  const [notice, setNotice] = useState('');
  const [undo, setUndo] = useState<{ columns: SheetColumn[]; rows: DraftRow[] } | null>(null);
  const nextColumnId = useRef(columns.length + 1);
  const nextRowId = useRef(rows.length + 1);
  const draft = serialize(columns, rows);
  const toolClassName = 'cursor-pointer rounded border border-slate-300 bg-white px-2 py-1 text-xs hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-blue-400';

  function updateCell(rowId: number, key: string, value: string) {
    setUndo(null);
    setRows((current) => current.map((row) => row.id === rowId ? { ...row, values: { ...row.values, [key]: value } } : row));
    setNotice('');
  }

  function changeType(id: number, type: SheetColumnType) {
    const next = columns.map((column) => column.id === id ? { ...column, columnType: type } : column);
    const result = serialize(next, rows);
    if (result.error) { setNotice('입력한 값과 호환되는 형식을 선택하거나 해당 열의 값을 먼저 비워 주세요.'); return; }
    setUndo(null);
    setColumns(next);
    setNotice('');
  }

  function removeColumn(column: SheetColumn) {
    setUndo({ columns, rows });
    setColumns((current) => current.filter((item) => item.id !== column.id));
    setRows((current) => current.map((row) => ({ ...row, values: Object.fromEntries(Object.entries(row.values).filter(([key]) => key !== column.columnKey)) })));
    setNotice('');
  }

  function move(index: number, direction: -1 | 1) {
    setUndo(null);
    const next = [...columns];
    [next[index], next[index + direction]] = [next[index + direction], next[index]];
    setColumns(next);
  }

  return (
    <section aria-labelledby="sheet-draft-title">
      <input type="hidden" name="sheetDraft" value={JSON.stringify(draft.sheet ?? {})} />
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><h2 id="sheet-draft-title" className="text-lg font-semibold">시트 내용</h2><p className="mt-2 text-sm font-normal leading-6 text-slate-500">제목과 열·행을 작성한 뒤 아래 저장 버튼을 누르면 한 번에 저장됩니다.</p></div>
        <div className="flex flex-wrap gap-3">
          <button type="button" className={secondaryClassName} onClick={() => { setUndo(null); const id = nextColumnId.current++; setColumns((current) => [...current, { id, columnKey: `col_${crypto.randomUUID().replaceAll('-', '')}`, columnName: '새 열', columnType: 'TEXT', columnOrder: current.length + 1 }]); }}>＋ 열 추가</button>
          <button type="button" disabled={!columns.length} className={secondaryClassName} onClick={() => { setUndo(null); const id = nextRowId.current++; setRows((current) => [...current, { id, values: {} }]); }}>＋ 행 추가</button>
          {undo && <button type="button" className={secondaryClassName} onClick={() => { setColumns(undo.columns); setRows(undo.rows); setUndo(null); setNotice('삭제를 취소했습니다.'); }}>삭제 취소</button>}
        </div>
      </div>
      {(notice || draft.error) && <p role="alert" className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm font-normal text-amber-800">{notice || draft.error}</p>}
      {!columns.length ? <p className="mt-4 rounded-lg border border-slate-200 p-6 text-sm font-normal text-slate-500">열을 추가해 시트 구성을 만들어 주세요.</p> : <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200">
        <table className="w-full min-w-[640px] text-left text-sm"><caption className="sr-only">시트 내용 편집</caption>
          <thead className="bg-slate-50"><tr><th scope="col" className="px-3 py-4">번호</th>{columns.map((column, index) => <th key={column.id} scope="col" className="min-w-[220px] px-3 py-4">
            <input aria-label={`${index + 1}번 열 이름`} name={`draftColumnName:${column.id}`} required maxLength={100} value={column.columnName} onChange={(event) => { setUndo(null); setColumns((current) => current.map((item) => item.id === column.id ? { ...item, columnName: event.target.value } : item)); }} className={inputClassName} />
            <select aria-label={`${column.columnName} 열 형식`} value={column.columnType} onChange={(event) => changeType(column.id, event.target.value as SheetColumnType)} className={inputClassName}>{Object.entries(columnTypes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
            <div className="mt-3 flex gap-2"><button type="button" disabled={index === 0} aria-label={`${column.columnName} 열 왼쪽 이동`} className={toolClassName} onClick={() => move(index, -1)}>←</button><button type="button" disabled={index === columns.length - 1} aria-label={`${column.columnName} 열 오른쪽 이동`} className={toolClassName} onClick={() => move(index, 1)}>→</button><button type="button" aria-label={`${column.columnName} 열 삭제`} className={`${toolClassName} text-red-700`} onClick={() => removeColumn(column)}>열 삭제</button></div>
          </th>)}<th scope="col" className="px-3 py-4">동작</th></tr></thead>
          <tbody className="divide-y divide-slate-200">{rows.map((row, index) => <tr key={row.id}><th scope="row" className="px-3 py-4 font-normal text-slate-500">{index + 1}</th>{columns.map((column) => <td key={column.id} className="px-3 py-3">
            {column.columnType === 'BOOLEAN' ? <select aria-label={`${index + 1}번 행 ${column.columnName}`} value={row.values[column.columnKey] ?? ''} onChange={(event) => updateCell(row.id, column.columnKey, event.target.value)} className={inputClassName}><option value="">미입력</option><option value="true">예</option><option value="false">아니오</option></select>
              : column.columnType === 'TEXT' ? <textarea aria-label={`${index + 1}번 행 ${column.columnName}`} rows={2} value={row.values[column.columnKey] ?? ''} onChange={(event) => updateCell(row.id, column.columnKey, event.target.value)} className={`${inputClassName} resize-y`} />
                : <input aria-label={`${index + 1}번 행 ${column.columnName}`} type={column.columnType === 'NUMBER' ? 'number' : 'date'} step={column.columnType === 'NUMBER' ? 'any' : undefined} value={row.values[column.columnKey] ?? ''} onChange={(event) => updateCell(row.id, column.columnKey, event.target.value)} className={inputClassName} />}
          </td>)}<td className="px-3 py-3"><button type="button" aria-label={`${index + 1}번 행 삭제`} className={`${toolClassName} whitespace-nowrap text-red-700`} onClick={() => { setUndo({ columns, rows }); setRows((current) => current.filter((item) => item.id !== row.id)); }}>행 삭제</button></td></tr>)}
            {!rows.length && <tr><td colSpan={columns.length + 2} className="px-6 py-8 text-center font-normal text-slate-500">행 추가 버튼으로 내용을 입력해 주세요.</td></tr>}
          </tbody>
        </table>
      </div>}
    </section>
  );
}
