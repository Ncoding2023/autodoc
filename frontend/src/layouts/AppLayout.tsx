import { useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLoaderData, useNavigate, useNavigation } from 'react-router-dom';
import { authApi } from '../api/auth-api';
import { getApiError } from '../api/client';
import type { User } from '../api/types';

const menus = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/documents', label: '문서 관리' },
  { to: '/templates', label: '템플릿 관리' },
  { to: '/teams', label: '팀 관리' },
];
const focusClassName = 'focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-300';

export function AppLayout() {
  const user = useLoaderData() as User;
  const navigate = useNavigate();
  const navigation = useNavigation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState('');
  const logoutPending = useRef(false);

  async function logout() {
    if (logoutPending.current) return;
    logoutPending.current = true;
    setLoggingOut(true);
    setError('');
    try {
      await authApi.logout();
      navigate('/login', { replace: true, state: { loggedOut: true } });
    } catch (failure: unknown) {
      setError(getApiError(failure)?.message ?? '로그아웃하지 못했습니다. 다시 시도해 주세요.');
    } finally {
      logoutPending.current = false;
      setLoggingOut(false);
    }
  }

  return (
    <div className="min-h-dvh bg-white">
      <a href="#main-content" className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-30 focus:rounded focus:bg-white focus:p-3 focus:text-blue-700">본문으로 이동</a>
      <header className="sticky top-0 z-20 border-b border-slate-700 bg-[#172b4d] text-white">
        <div className="mx-auto flex max-w-[1440px] flex-wrap items-center gap-x-6 gap-y-3 px-4 py-4 sm:px-8 lg:gap-x-10">
          <Link to="/dashboard" onClick={() => setMenuOpen(false)} className={`shrink-0 text-2xl font-bold tracking-tight ${focusClassName}`}>AutoDoc</Link>
          <div className="ml-auto flex min-w-0 items-center gap-3 lg:order-3 lg:gap-5">
            <NavLink to="/profile" aria-label={`내 정보: ${user.name}`} title="내 정보" onClick={() => setMenuOpen(false)} className={({ isActive }) => `hidden max-w-40 truncate rounded px-2 py-1 text-sm sm:block ${isActive ? 'bg-slate-700 text-white' : 'text-slate-200 hover:text-white'} ${focusClassName}`}>{user.name}님</NavLink>
            <button type="button" disabled={loggingOut} onClick={logout} className={`cursor-pointer whitespace-nowrap rounded border border-slate-500 px-3 py-2 text-sm hover:bg-slate-700 disabled:cursor-wait disabled:opacity-60 ${focusClassName}`}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button>
            <button type="button" onClick={() => setMenuOpen((open) => !open)} aria-expanded={menuOpen} aria-controls="app-menu" className={`cursor-pointer rounded px-2 py-2 text-sm hover:bg-slate-700 lg:hidden ${focusClassName}`}>{menuOpen ? '메뉴 닫기' : '메뉴'}</button>
          </div>
          <nav id="app-menu" aria-label="주 메뉴" className={`${menuOpen ? 'flex' : 'hidden'} w-full flex-col gap-1 border-t border-slate-600 pt-3 lg:order-2 lg:ml-auto lg:flex lg:w-auto lg:flex-row lg:border-0 lg:pt-0`}>
            {menus.map((menu) => <NavLink key={menu.to} to={menu.to} onClick={() => setMenuOpen(false)} className={({ isActive }) => `rounded-md px-3 py-2.5 text-sm font-medium whitespace-nowrap ${isActive ? 'bg-blue-600 text-white' : 'text-slate-200 hover:bg-slate-700 hover:text-white'} ${focusClassName}`}>{menu.label}</NavLink>)}
            <NavLink to="/profile" onClick={() => setMenuOpen(false)} className={({ isActive }) => `rounded-md px-3 py-2.5 text-sm lg:hidden ${isActive ? 'bg-blue-600 text-white' : 'text-slate-200 hover:bg-slate-700'} ${focusClassName}`}>내 정보</NavLink>
          </nav>
        </div>
      </header>
      <main id="main-content" tabIndex={-1} aria-busy={navigation.state !== 'idle'} className="mx-auto max-w-[1440px] px-4 py-8 sm:px-8 sm:py-10">
        {navigation.state === 'loading' && <p role="status" className="mb-6 rounded-lg bg-blue-50 p-3 text-sm text-blue-700">화면을 불러오는 중입니다.</p>}
        {error && <p role="alert" className="mb-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}
        <Outlet context={user} />
      </main>
    </div>
  );
}
