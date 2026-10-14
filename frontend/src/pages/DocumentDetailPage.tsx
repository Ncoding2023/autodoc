import { useRef } from 'react';
import { Form, Link, useActionData, useLoaderData, useNavigation, useSearchParams } from 'react-router-dom';
import type { Document } from '../api/types';
import { FormatBadge, StatusBadge, primaryClassName, secondaryClassName } from '../components/document-ui';
import { SheetEditor } from '../components/SheetEditor';
import { DocsPreview, DocsPreviewButton } from '../components/DocsPreview';

export function DocumentDetailPage() {
  const document = useLoaderData() as Document;
  const action = useActionData() as { error?: string } | undefined;
  const [params] = useSearchParams();
  const navigation = useNavigation();
  const pending = navigation.state !== 'idle';
  const dialog = useRef<HTMLDialogElement>(null);
  return (
    <section aria-labelledby="document-detail-title">
      <div className="flex flex-wrap items-start justify-between gap-5">
        <div className="min-w-0"><h1 id="document-detail-title" className="text-2xl font-bold break-words sm:text-3xl">{document.title}</h1><div className="mt-4 flex flex-wrap items-center gap-3"><FormatBadge format={document.writingFormat} /><StatusBadge status={document.status} /><span className="text-sm text-slate-500">문서 유형: {document.documentType}</span></div></div>
        <div className="flex flex-wrap gap-3"><Link to="/documents" className={secondaryClassName}>목록</Link><Link to={`/documents/${document.id}/edit`} className={primaryClassName}>수정</Link><button type="button" onClick={() => dialog.current?.showModal()} className="cursor-pointer rounded-md border border-red-200 px-5 py-2.5 text-sm font-semibold text-red-700 hover:bg-red-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400">삭제</button></div>
      </div>
      {(params.get('created') === '1' || params.get('updated') === '1') && <p role="status" className="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">문서가 저장되었습니다.</p>}
      {action?.error && <p role="alert" className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{action.error}</p>}
      {document.writingFormat === 'DOCS' ? <div className="mt-8 space-y-4">
        <div className="flex items-center justify-between gap-4"><h2 className="text-lg font-semibold">문서 내용</h2><DocsPreviewButton readContent={() => document} /></div>
        <DocsPreview content={document} />
      </div> : <SheetEditor key={document.id} documentId={document.id} />}
      <dialog ref={dialog} aria-labelledby="delete-document-title" aria-describedby="delete-document-description" className="m-auto w-[calc(100%-32px)] max-w-lg rounded-xl border border-slate-200 bg-white p-7 backdrop:bg-[#172b4d66]">
        <h2 id="delete-document-title" className="text-xl font-bold">문서를 삭제하시겠습니까?</h2><p id="delete-document-description" className="mt-4 text-sm leading-6 text-slate-500">‘{document.title}’ 문서와 연결된 데이터가 삭제됩니다. 삭제한 문서는 복구할 수 없습니다.</p>
        {action?.error && <p role="alert" className="mt-4 text-sm text-red-700">{action.error}</p>}
        <Form method="post" className="mt-6 flex flex-wrap justify-end gap-3"><input type="hidden" name="intent" value="delete" /><button type="button" disabled={pending} onClick={() => dialog.current?.close()} className={secondaryClassName}>취소</button><button type="submit" disabled={pending} className="cursor-pointer rounded-md bg-red-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-red-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400 disabled:cursor-wait disabled:opacity-60">{pending ? '삭제 중…' : '삭제'}</button></Form>
      </dialog>
    </section>
  );
}
