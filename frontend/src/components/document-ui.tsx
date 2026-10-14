import type { DocumentStatus, WritingFormat } from '../api/types';

export const primaryClassName = 'inline-block cursor-pointer rounded-md bg-blue-600 px-5 py-2.5 text-center text-sm font-semibold text-white hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-400 disabled:cursor-wait disabled:opacity-60';
export const secondaryClassName = 'inline-block cursor-pointer rounded-md border border-slate-300 bg-white px-5 py-2.5 text-center text-sm font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-400 disabled:cursor-wait disabled:opacity-60';
export const inputClassName = 'mt-2 w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 font-normal focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-400';

export function FormatBadge({ format }: { format: WritingFormat }) {
  return <span className={`inline-block rounded px-2 py-1 text-xs font-semibold ${format === 'DOCS' ? 'bg-blue-50 text-blue-700' : 'bg-teal-50 text-teal-700'}`}>{format}</span>;
}

export function StatusBadge({ status }: { status: DocumentStatus }) {
  return <span className={`inline-block rounded px-2 py-1 text-xs ${status === 'COMPLETED' ? 'bg-green-50 text-green-700' : 'bg-slate-100 text-slate-600'}`}>{status === 'COMPLETED' ? '완료' : '작성 중'}</span>;
}
