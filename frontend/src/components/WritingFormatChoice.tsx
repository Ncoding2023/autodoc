import type { WritingFormat } from '../api/types';

const formats = [
  { value: 'DOCS' as const, description: '인수인계서·업무보고서 등 서술형 업무 문서', output: 'DOCX' },
  { value: 'SHEETS' as const, description: '업무 목록·체크리스트 등 행·열 기반 업무 데이터', output: 'XLSX' },
];

export function WritingFormatChoice({ value, onChange }: { value: WritingFormat | null; onChange: (format: WritingFormat) => void }) {
  return (
    <fieldset>
      <legend className="mb-4 text-lg font-semibold">작성 형식</legend>
      <div className="grid gap-4 sm:grid-cols-2">
        {formats.map((format) => <label key={format.value} className="cursor-pointer">
          <input type="radio" name="writingFormat" value={format.value} checked={value === format.value} onChange={() => onChange(format.value)} className="peer sr-only" />
          <span className={`flex h-full flex-col rounded-xl border p-6 transition-colors peer-focus-visible:outline-2 peer-focus-visible:outline-offset-4 peer-focus-visible:outline-blue-400 ${value === format.value ? format.value === 'DOCS' ? 'border-blue-500 bg-blue-50' : 'border-teal-500 bg-teal-50' : 'border-slate-200 bg-white hover:border-slate-400'}`}>
            <span className="flex items-center justify-between gap-3">
              <span className="text-lg font-semibold">{format.value}형</span>
              {value === format.value && <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${format.value === 'DOCS' ? 'bg-blue-100 text-blue-800' : 'bg-teal-100 text-teal-800'}`}>선택됨</span>}
            </span>
            <span className="mt-3 text-sm leading-6 text-slate-500">{format.description}</span>
            <span className="mt-5 text-xs text-slate-500">출력 형식: {format.output}</span>
          </span>
        </label>)}
      </div>
    </fieldset>
  );
}
