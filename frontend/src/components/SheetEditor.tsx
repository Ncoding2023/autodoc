import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { sheetApi } from '../api/sheet-api';
import type { SheetColumn, SheetRow } from '../api/types';
import { secondaryClassName } from './document-ui';
import { displayCell } from './sheet-data';

export function SheetEditor({ documentId }: { documentId: number }) {
  const [data, setData] = useState<{ columns: SheetColumn[]; rows: SheetRow[] }>();
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setData(undefined);
    setError(false);
    Promise.all([sheetApi.getColumns(documentId, controller.signal), sheetApi.getRows(documentId, controller.signal)])
      .then(([columns, rows]) => {
        if (!controller.signal.aborted) setData({ columns: [...columns].sort((a, b) => a.columnOrder - b.columnOrder), rows: [...rows].sort((a, b) => a.rowNo - b.rowNo) });
      }).catch(() => { if (!controller.signal.aborted) setError(true); });
    return () => controller.abort();
  }, [documentId, retry]);
  return <section className="mt-8" aria-labelledby="sheet-content-title">
    <div className="flex flex-wrap items-center justify-between gap-4"><h2 id="sheet-content-title" className="text-lg font-semibold">시트 내용</h2><Link to={`/documents/${documentId}/edit`} className={secondaryClassName}>시트 편집</Link></div>
    {error ? <div className="mt-4"><p role="alert" className="mb-3 text-sm text-red-700">시트 데이터를 불러오지 못했습니다.</p><button type="button" className={secondaryClassName} onClick={() => setRetry((value) => value + 1)}>다시 불러오기</button></div>
      : !data ? <p role="status" className="mt-4 text-sm text-slate-500">시트 데이터를 불러오는 중입니다.</p>
        : !data.columns.length ? <p className="mt-4 text-sm text-slate-500">등록된 열이 없습니다. 시트 편집에서 내용을 추가해 주세요.</p>
          : <div className="mt-4 overflow-x-auto rounded-xl border border-slate-200"><table className="w-full min-w-[640px] text-left text-sm"><caption className="sr-only">저장된 시트 내용</caption><thead className="bg-slate-50"><tr><th scope="col" className="px-4 py-4">번호</th>{data.columns.map((column) => <th key={column.id} scope="col" className="min-w-40 px-4 py-4">{column.columnName}</th>)}</tr></thead><tbody className="divide-y divide-slate-200">{data.rows.map((row) => <tr key={row.id}><th scope="row" className="px-4 py-4 font-normal text-slate-500">{row.rowNo}</th>{data.columns.map((column) => <td key={column.id} className="px-4 py-4 whitespace-pre-wrap [overflow-wrap:anywhere]">{displayCell(row.rowData[column.columnKey])}</td>)}</tr>)}{!data.rows.length && <tr><td colSpan={data.columns.length + 1} className="p-6 text-center text-slate-500">입력된 행이 없습니다.</td></tr>}</tbody></table></div>}
  </section>;
}
