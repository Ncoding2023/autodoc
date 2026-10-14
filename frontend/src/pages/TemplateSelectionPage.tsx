import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { templateApi } from '../api/template-api';
import { getApiError } from '../api/client';
import type { DocumentTemplate, WritingFormat } from '../api/types';
import { WritingFormatChoice } from '../components/WritingFormatChoice';
import { findSelectedTemplate, getBasicTemplates, getTemplateSections, readWritingFormat } from './template-selection';

const primaryClassName = 'cursor-pointer rounded-md bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-400 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500';
const secondaryClassName = 'cursor-pointer rounded-md border border-slate-300 bg-white px-5 py-2.5 text-sm font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-400';

export function TemplateSelectionPage({ catalog = false }: { catalog?: boolean }) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const format = readWritingFormat(params.get('format')) ?? (catalog ? 'DOCS' : null);
  const [pickedFormat, setPickedFormat] = useState<WritingFormat | null>(null);
  const [templates, setTemplates] = useState<DocumentTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const visibleTemplates = format ? getBasicTemplates(templates, format) : [];
  const selected = findSelectedTemplate(visibleTemplates, params.get('templateId'));

  useEffect(() => {
    if (!format) return;
    const controller = new AbortController();
    setLoading(true);
    setError('');
    templateApi.getAll(controller.signal).then((result) => {
      if (!controller.signal.aborted) setTemplates(result);
    }).catch((failure: unknown) => {
      if (controller.signal.aborted) return;
      const apiError = getApiError(failure);
      if (apiError?.code === 'UNAUTHORIZED' || apiError?.code === 'INACTIVE_USER' || apiError?.code === 'USER_NOT_FOUND') {
        navigate(`/login?returnTo=${encodeURIComponent(window.location.pathname + window.location.search)}`, { replace: true });
      } else {
        setError(apiError?.message ?? '템플릿을 불러오지 못했습니다. 다시 시도해 주세요.');
      }
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [format, retry, navigate]);

  function chooseFormat(nextFormat: WritingFormat) {
    // 형식을 바꾸면 이전 형식의 템플릿 선택을 함께 해제한다.
    setParams({ format: nextFormat });
  }

  return (
    <section aria-labelledby="template-page-title" className="max-w-5xl">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 id="template-page-title" className="text-2xl font-bold sm:text-3xl">{catalog ? '기본 템플릿' : '새 문서 작성'}</h1>
          <p className="mt-3 text-sm leading-6 text-slate-500">{catalog ? '작성 형식별 기본 템플릿을 확인하고 문서에 사용할 템플릿을 선택하세요.' : '작성 형식을 고른 뒤 문서에 사용할 기본 템플릿을 선택하세요.'}</p>
        </div>
        {!catalog && <Link to="/templates" className={secondaryClassName}>기본 템플릿 둘러보기</Link>}
      </div>
      {!catalog && <ol aria-label="템플릿 선택 단계" className="mt-8 flex flex-wrap gap-3 text-sm">
        <li aria-current={!format ? 'step' : undefined} className={`rounded-full px-4 py-2 ${!format ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'}`}>1. 작성 형식</li>
        <li aria-current={format ? 'step' : undefined} className={`rounded-full px-4 py-2 ${format ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'}`}>2. 기본 템플릿</li>
        <li className="rounded-full bg-slate-100 px-4 py-2 text-slate-600">3. 문서 작성</li>
      </ol>}
      {!format ? (
        <form className="mt-8" onSubmit={(event) => { event.preventDefault(); if (pickedFormat) chooseFormat(pickedFormat); }}>
          <WritingFormatChoice value={pickedFormat} onChange={setPickedFormat} />
          <div className="mt-8 flex justify-end"><button type="submit" disabled={!pickedFormat} className={primaryClassName}>다음: 템플릿 선택</button></div>
        </form>
      ) : (
        <div className="mt-8">
          {catalog ? <WritingFormatChoice value={format} onChange={chooseFormat} /> : <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-medium">선택 형식: <span className={format === 'DOCS' ? 'text-blue-700' : 'text-teal-700'}>{format}형</span></p>
            <button type="button" onClick={() => { setPickedFormat(format); setParams({}); }} className={secondaryClassName}>형식 다시 선택</button>
          </div>}
          <h2 className="mt-8 text-lg font-semibold">기본 템플릿</h2>
          {loading ? <p role="status" className="mt-4 rounded-lg bg-slate-50 p-6 text-sm text-slate-500">템플릿을 불러오는 중입니다.</p> : error ? <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-6">
            <p role="alert" className="mb-4 text-sm text-red-700">{error}</p>
            <button type="button" onClick={() => setRetry((value) => value + 1)} className={secondaryClassName}>다시 불러오기</button>
          </div> : <>
            {params.has('templateId') && !selected && <p role="alert" className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">선택한 기본 템플릿을 찾을 수 없습니다. 목록에서 다시 선택해 주세요.</p>}
            {visibleTemplates.length === 0 ? <p role="status" className="mt-4 rounded-lg border border-slate-200 p-8 text-sm leading-6 text-slate-500">등록된 {format}형 기본 템플릿이 없습니다. 다른 작성 형식을 선택하거나 관리자에게 문의해 주세요.</p> : <fieldset className="mt-4">
              <legend className="sr-only">문서에 사용할 기본 템플릿 선택</legend>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {visibleTemplates.map((template) => {
                  const sections = getTemplateSections(template);
                  const isSelected = selected?.id === template.id;
                  return <label key={template.id} className="min-w-0 cursor-pointer">
                    <input type="radio" name="templateId" value={template.id} checked={isSelected} onChange={() => setParams({ format, templateId: String(template.id) })} className="peer sr-only" />
                    <span className={`flex h-full flex-col rounded-xl border p-5 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-4 peer-focus-visible:outline-blue-400 ${isSelected ? format === 'DOCS' ? 'border-blue-500 bg-blue-50' : 'border-teal-500 bg-teal-50' : 'border-slate-200 hover:border-slate-400'}`}>
                      <span className="flex flex-wrap items-center justify-between gap-2"><span className={`rounded px-2 py-1 text-xs font-medium ${format === 'DOCS' ? 'bg-blue-100 text-blue-800' : 'bg-teal-100 text-teal-800'}`}>{format}</span><span className="text-xs text-slate-500">{isSelected ? '✓ 선택됨' : '기본 템플릿'}</span></span>
                      <span className="mt-4 text-base font-semibold [overflow-wrap:anywhere]">{template.name}</span>
                      <span className="mt-3 text-sm leading-6 text-slate-500 [overflow-wrap:anywhere]">{sections.length ? sections.join(' · ') : '문서 구성은 작성 단계에서 확인할 수 있습니다.'}</span>
                      <span className="mt-5 text-xs text-slate-500">기본 디자인 적용</span>
                    </span>
                  </label>;
                })}
              </div>
            </fieldset>}
            {selected && <div className="mt-6 rounded-lg border border-slate-200 p-5" role="status">
              <p className="text-sm font-semibold">선택한 템플릿: {selected.name}</p>
              <p className="mt-2 text-sm text-slate-500">{selected.writingFormat}형 · {getTemplateSections(selected).join(' · ') || '기본 문서 구성'}</p>
            </div>}
            <div className="mt-8 flex justify-end">
              {catalog ? <Link to={selected ? `/documents/new/write?format=${format}&templateId=${selected.id}` : `/documents/new?format=${format}`} className={primaryClassName}>{selected ? '이 템플릿으로 시작' : '새 문서 템플릿 선택'}</Link> : <button type="button" disabled={!selected} onClick={() => { if (selected) navigate(`/documents/new/write?format=${format}&templateId=${selected.id}`); }} className={primaryClassName}>다음: 문서 작성</button>}
            </div>
          </>}
        </div>
      )}
    </section>
  );
}
