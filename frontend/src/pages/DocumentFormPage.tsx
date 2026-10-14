import { useRef } from 'react';
import { Form, Link, useActionData, useLoaderData, useNavigation } from 'react-router-dom';
import type { Document, DocumentTemplate } from '../api/types';
import { FormatBadge, inputClassName, primaryClassName, secondaryClassName } from '../components/document-ui';
import { SheetDraftEditor } from '../components/SheetDraftEditor';
import type { SheetDraft } from '../api/document-api';
import { DocsPreviewButton, readDocsPreview } from '../components/DocsPreview';

export function DocumentFormPage({ creating = false }: { creating?: boolean }) {
  const loaded = useLoaderData() as DocumentTemplate | { document: Document; sheet?: SheetDraft };
  const source = 'document' in loaded ? loaded.document : loaded;
  const initialSheet = 'document' in loaded ? loaded.sheet : undefined;
  const document = 'scope' in source ? null : source;
  const action = useActionData() as { error?: string } | undefined;
  const navigation = useNavigation();
  const pending = navigation.state !== 'idle';
  const form = useRef<HTMLFormElement>(null);
  return (
    <section aria-labelledby="document-form-title" className="max-w-5xl">
      <h1 id="document-form-title" className="text-2xl font-bold sm:text-3xl">{creating ? '새 문서 작성' : '문서 수정'}</h1>
      <div className="mt-4 flex flex-wrap items-center gap-3 text-sm"><FormatBadge format={source.writingFormat} />{'scope' in source && <span className="text-slate-500">기본 템플릿: {source.name}</span>}</div>
      {action?.error && <p role="alert" className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{action.error}</p>}
      <Form ref={form} key={source.id} method="post" className="mt-8" aria-busy={pending}>
        <fieldset disabled={pending} className="space-y-6 disabled:opacity-65">
          <label htmlFor="document-title" className="block text-sm font-semibold">문서 제목<input id="document-title" name="title" required maxLength={255} defaultValue={document?.title ?? ''} placeholder="문서 제목을 입력해 주세요" className={inputClassName} /></label>
          <label htmlFor="document-type" className="block text-sm font-semibold">문서 유형<input id="document-type" name="documentType" required maxLength={50} defaultValue={source.documentType} readOnly={creating} className={`${inputClassName} read-only:bg-slate-50`} /></label>
          {source.writingFormat === 'DOCS' ? <>
            <label htmlFor="body-content" className="block text-sm font-semibold">본문<textarea id="body-content" name="bodyContent" rows={10} defaultValue={document?.bodyContent ?? ''} placeholder="업무 내용을 입력해 주세요" className={`${inputClassName} resize-y`} /></label>
            <div className="grid gap-6 md:grid-cols-2">
              <label htmlFor="important-notes" className="block text-sm font-semibold">중요 사항<textarea id="important-notes" name="importantNotes" rows={5} defaultValue={document?.importantNotes ?? ''} className={`${inputClassName} resize-y`} /></label>
              <label htmlFor="cautions" className="block text-sm font-semibold">주의 사항<textarea id="cautions" name="cautions" rows={5} defaultValue={document?.cautions ?? ''} className={`${inputClassName} resize-y`} /></label>
            </div>
          </> : <SheetDraftEditor key={source.id} template={'scope' in source ? source : undefined} initialSheet={initialSheet} />}
          <div className="flex flex-wrap justify-end gap-3">{source.writingFormat === 'DOCS' && <DocsPreviewButton draft readContent={() => readDocsPreview(new FormData(form.current!))} />}<Link to={document ? `/documents/${document.id}` : `/documents/new?format=${source.writingFormat}&templateId=${source.id}`} aria-disabled={pending} tabIndex={pending ? -1 : undefined} onClick={(event) => { if (pending) event.preventDefault(); }} className={`${secondaryClassName} ${pending ? 'opacity-60' : ''}`}>취소</Link><button type="submit" className={primaryClassName}>{pending ? '저장 중…' : '저장'}</button></div>
        </fieldset>
      </Form>
    </section>
  );
}
