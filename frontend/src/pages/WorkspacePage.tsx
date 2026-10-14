import { Link, useOutletContext } from 'react-router-dom';
import type { User } from '../api/types';

export function WorkspacePage({ title, description, dashboard = false }: { title: string; description: string; dashboard?: boolean }) {
  const user = useOutletContext<User>();
  return (
    <section aria-labelledby="workspace-title">
      <h1 id="workspace-title" className="text-2xl font-bold sm:text-3xl">{title}</h1>
      <p className="mt-3 text-sm leading-6 text-slate-500">{dashboard ? `${user.name}님, 환영합니다. AutoDoc에서 업무 문서를 관리해 보세요.` : description}</p>
      {(dashboard || title === '문서 관리') && <Link to="/documents/new" className="mt-6 inline-block rounded-md bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-400">새 문서 작성</Link>}
      <div className="mt-8 rounded-xl border border-slate-200 bg-slate-50 px-6 py-10 sm:px-8">
        <span className="inline-block rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-medium text-amber-800">준비 중</span>
        <h2 className="mt-4 text-lg font-semibold">{title} 기능을 준비하고 있습니다.</h2>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">{dashboard ? '문서 현황과 최근 문서는 문서 관리 기능을 연결한 후 제공됩니다. 상단 메뉴에서 각 작업 공간으로 이동할 수 있습니다.' : description}</p>
      </div>
    </section>
  );
}
