import { Link, isRouteErrorResponse, useRevalidator, useRouteError } from 'react-router-dom';

export function RouteErrorPage() {
  const revalidator = useRevalidator();
  const error = useRouteError();
  const message = isRouteErrorResponse(error) && typeof error.data === 'string'
    ? error.data : '서버 연결이나 로그인 상태를 확인하고 다시 시도해 주세요.';
  return (
    <main className="grid min-h-dvh place-items-center px-6">
      <section className="w-full max-w-lg rounded-xl border border-slate-200 bg-white p-8" aria-labelledby="route-error-title">
        <h1 id="route-error-title" className="text-xl font-bold">화면을 불러오지 못했습니다.</h1>
        <p role="alert" className="mt-3 text-sm leading-6 text-slate-500">{message}</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <button type="button" disabled={revalidator.state === 'loading'} onClick={() => revalidator.revalidate()} className="cursor-pointer rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-400 disabled:opacity-60">{revalidator.state === 'loading' ? '확인 중…' : '다시 시도'}</button>
          <Link to="/login" className="rounded-md border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-400">로그인으로 이동</Link>
          <Link to="/documents" className="rounded-md border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-400">문서 목록</Link>
        </div>
      </section>
    </main>
  );
}
