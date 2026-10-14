import { useId, useRef, useState } from 'react';
import type { Document } from '../api/types';
import { secondaryClassName } from './document-ui';

export type DocsPreviewContent = Pick<Document, 'title' | 'documentType' | 'bodyContent' | 'importantNotes' | 'cautions'>;

export function readDocsPreview(data: FormData): DocsPreviewContent {
  return {
    title: String(data.get('title') ?? ''),
    documentType: String(data.get('documentType') ?? ''),
    bodyContent: String(data.get('bodyContent') ?? ''),
    importantNotes: String(data.get('importantNotes') ?? ''),
    cautions: String(data.get('cautions') ?? ''),
  };
}

export function DocsPreview({ content }: { content: DocsPreviewContent }) {
  return <div className="rounded-xl border border-slate-200 bg-slate-100 p-3 sm:p-6">
    <article aria-label="문서 미리보기 내용" className="mx-auto min-h-[600px] w-full max-w-[794px] bg-white px-6 py-10 text-slate-900 shadow-sm sm:min-h-[1000px] sm:px-14 sm:py-16">
      <header className="border-b-2 border-slate-800 pb-7">
        <p className="text-sm text-slate-500 [overflow-wrap:anywhere]">{content.documentType.trim() || '문서 유형 미입력'}</p>
        <h3 className="mt-4 text-2xl leading-relaxed font-bold whitespace-pre-wrap [overflow-wrap:anywhere] sm:text-3xl">{content.title.trim() || '제목 없는 문서'}</h3>
      </header>
      <div className="mt-9 space-y-10">
        <PreviewSection title="본문" value={content.bodyContent} />
        <PreviewSection title="중요 사항" value={content.importantNotes} />
        <PreviewSection title="주의 사항" value={content.cautions} />
      </div>
    </article>
  </div>;
}

function PreviewSection({ title, value }: { title: string; value: string | null }) {
  return <section>
    <h4 className="mb-3 border-b border-slate-200 pb-3 text-base font-semibold">{title}</h4>
    <p className={`text-sm leading-8 whitespace-pre-wrap [overflow-wrap:anywhere] ${value?.trim() ? '' : 'text-slate-400'}`}>{value?.trim() ? value : '입력된 내용이 없습니다.'}</p>
  </section>;
}

export function DocsPreviewButton({ readContent, draft = false }: { readContent: () => DocsPreviewContent; draft?: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [content, setContent] = useState<DocsPreviewContent>();
  const titleId = useId();
  return <>
    <button type="button" className={secondaryClassName} onClick={() => {
      setContent(readContent());
      dialog.current?.showModal();
    }}>미리보기</button>
    <dialog ref={dialog} aria-labelledby={titleId} className="m-auto max-h-[92dvh] w-[calc(100%-24px)] max-w-5xl overflow-y-auto rounded-xl border border-slate-200 bg-white p-4 text-slate-900 backdrop:bg-[#172b4d99] sm:p-6">
      <div className="mb-5 flex items-start justify-between gap-4">
        <div><h2 id={titleId} className="text-xl font-bold">문서 미리보기</h2><p className="mt-2 text-sm leading-6 text-slate-500">{draft ? '현재 입력한 내용입니다. 저장하려면 미리보기를 닫고 저장 버튼을 눌러 주세요.' : '저장된 문서 내용입니다.'}</p></div>
        <button type="button" onClick={() => dialog.current?.close()} className={`${secondaryClassName} shrink-0`}>닫기</button>
      </div>
      {content && <DocsPreview content={content} />}
    </dialog>
  </>;
}
