import { Link, useLoaderData, useSearchParams } from 'react-router-dom';
import type { DocumentListItem } from '../api/types';
import { FormatBadge, StatusBadge, primaryClassName, secondaryClassName } from '../components/document-ui';

export function DocumentListPage() {
  const documents = useLoaderData() as DocumentListItem[];
  const [params] = useSearchParams();
  const sorted = [...documents].sort((first, second) => second.id - first.id);
  return (
    <section aria-labelledby="documents-title">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div><h1 id="documents-title" className="text-2xl font-bold sm:text-3xl">문서 관리</h1><p className="mt-3 text-sm text-slate-500">내 문서 {documents.length}개</p></div>
        <Link to="/documents/new" className={primaryClassName}>＋ 새 문서 작성</Link>
      </div>
      {params.get('deleted') === '1' && <p role="status" className="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700">문서가 삭제되었습니다.</p>}
      {sorted.length === 0 ? <div className="mt-8 rounded-xl border border-slate-200 p-8 text-center"><h2 className="text-lg font-semibold">아직 작성한 문서가 없습니다.</h2><p className="mt-3 text-sm text-slate-500">기본 템플릿을 선택해 첫 문서를 작성해 보세요.</p><Link to="/documents/new" className={`mt-6 ${secondaryClassName}`}>새 문서 작성</Link></div> : (
        <div className="mt-8 overflow-x-auto rounded-xl border border-slate-200">
          <table className="w-full min-w-[640px] text-left text-sm">
            <caption className="sr-only">내 문서 목록</caption>
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-500"><tr><th scope="col" className="px-5 py-4">제목</th><th scope="col" className="px-5 py-4">작성 형식</th><th scope="col" className="px-5 py-4">문서 유형</th><th scope="col" className="px-5 py-4">상태</th><th scope="col" className="px-5 py-4">동작</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{sorted.map((document) => <tr key={document.id} className="hover:bg-slate-50">
              <td className="max-w-md px-5 py-4"><Link to={`/documents/${document.id}`} className="font-semibold break-words hover:text-blue-700 hover:underline focus-visible:outline-2 focus-visible:outline-blue-400">{document.title}</Link></td>
              <td className="px-5 py-4"><FormatBadge format={document.writingFormat} /></td><td className="max-w-48 px-5 py-4 break-words">{document.documentType}</td><td className="px-5 py-4"><StatusBadge status={document.status} /></td>
              <td className="px-5 py-4"><Link to={`/documents/${document.id}/edit`} aria-label={`${document.title} 수정`} className="rounded border border-slate-300 px-3 py-2 whitespace-nowrap hover:bg-white focus-visible:outline-2 focus-visible:outline-blue-400">수정</Link></td>
            </tr>)}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}
