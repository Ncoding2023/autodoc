import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth-api';
import { userApi } from '../api/user-api';
import { getApiError } from '../api/client';
import type { User } from '../api/types';

const focusClassName = 'focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-blue-300';
const fieldLabelClassName = 'flex flex-col gap-2 text-sm font-semibold';
const fieldClassName = `w-full min-h-11 rounded-md border border-slate-300 bg-white px-3 py-2.5 font-normal text-[#172b4d] placeholder:text-[#8591a3] ${focusClassName}`;
const checkboxClassName = `m-0 size-[18px] shrink-0 accent-blue-600 ${focusClassName}`;
const checkLabelClassName = 'flex cursor-pointer items-center gap-2.5 text-sm font-semibold';
const fieldsetClassName = 'm-0 flex min-w-0 flex-col gap-[18px] border-0 p-0 disabled:opacity-65';
const primaryClassName = `mt-2 min-h-11 w-full cursor-pointer rounded-md border border-blue-600 bg-blue-600 px-3 py-[11px] text-center text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-wait disabled:opacity-65 ${focusClassName}`;
const secondaryClassName = `block min-h-11 w-full cursor-pointer rounded-md border border-slate-300 px-3 py-[11px] text-center text-sm font-semibold text-[#172b4d] no-underline hover:bg-slate-100 ${focusClassName}`;
const descriptionClassName = 'mb-[26px] text-center text-sm leading-[1.6] text-slate-500';
const errorClassName = 'mb-5 rounded-md border border-red-200 bg-rose-50 p-3 text-sm leading-[1.6] text-red-700 [overflow-wrap:anywhere]';
const successClassName = 'mb-5 rounded-md border border-green-200 bg-green-50 p-3 text-sm leading-[1.6] text-green-700 [overflow-wrap:anywhere]';
const hintClassName = '-mt-2.5 text-xs leading-[1.6] text-slate-500';

export function AuthPage({ signup = false }: { signup?: boolean }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState<User | null>(null);
  const [checking, setChecking] = useState(true);
  const [pending, setPending] = useState(false);
  const [hasTeam, setHasTeam] = useState(false);
  const [teamMode, setTeamMode] = useState<'existing' | 'new'>('existing');
  const [teams, setTeams] = useState<{ id: number; name: string }[]>([]);
  const [teamsLoading, setTeamsLoading] = useState(false);
  const [teamsError, setTeamsError] = useState('');
  const [teamsRetry, setTeamsRetry] = useState(0);
  const recoveryDialog = useRef<HTMLDialogElement>(null);
  const submitting = useRef(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(location.state?.signupComplete ? '회원가입이 완료되었습니다. 이메일과 비밀번호로 로그인해 주세요.' : '');

  useEffect(() => {
    let active = true;
    authApi.getCurrentUser().then((currentUser) => {
      if (active) setUser(currentUser);
    }).catch((failure: unknown) => {
      if (active && getApiError(failure)?.code !== 'UNAUTHORIZED') {
        setError('로그인 상태를 확인하지 못했습니다. 서버 연결을 확인해 주세요.');
      }
    }).finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!signup || !hasTeam || teamMode !== 'existing') return;
    let active = true;
    setTeamsLoading(true);
    setTeamsError('');
    userApi.getSignupTeams().then((result) => {
      if (active) setTeams(result);
    }).catch(() => {
      if (active) setTeamsError('팀 목록을 불러오지 못했습니다. 다시 시도해 주세요.');
    }).finally(() => { if (active) setTeamsLoading(false); });
    return () => { active = false; };
  }, [signup, hasTeam, teamMode, teamsRetry]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    const email = String(data.get('email')).trim();
    const password = String(data.get('password'));
    const name = String(data.get('name') ?? '').trim();
    setError('');
    setNotice('');
    if (signup && !name) {
      setError('이름을 입력해 주세요.');
      return;
    }
    if (signup && password !== data.get('confirmPassword')) {
      setError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      form.elements.namedItem('confirmPassword') instanceof HTMLInputElement &&
        (form.elements.namedItem('confirmPassword') as HTMLInputElement).focus();
      return;
    }
    if (signup && !/[\p{P}\p{S}]/u.test(password)) {
      setError('비밀번호는 특수문자를 하나 이상 포함해 주세요.');
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      setError('비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요.');
      return;
    }
    const teamId = signup && hasTeam && teamMode === 'existing' ? Number(data.get('teamId')) : undefined;
    const teamName = String(data.get('teamName') ?? '').trim();
    if (signup && hasTeam && teamMode === 'existing' && (teamsLoading || teamsError || !teams.some((team) => team.id === teamId))) {
      setError('가입할 기존 팀을 선택해 주세요.');
      return;
    }
    if (signup && hasTeam && teamMode === 'new' && !teamName) {
      setError('새 팀 이름을 입력해 주세요.');
      return;
    }
    submitting.current = true;
    setPending(true);
    try {
      if (signup) {
        await userApi.signUp({ name, email, password, teamId,
          team: hasTeam && teamMode === 'new' ? { name: teamName, description: String(data.get('teamDescription') ?? '').trim() } : undefined });
        form.reset();
        navigate('/login', { replace: true, state: { signupComplete: true } });
      } else {
        setUser(await authApi.login({ email, password, rememberMe: data.get('rememberMe') === 'on' }));
        form.reset();
      }
    } catch (failure: unknown) {
      setError(getApiError(failure)?.message ?? '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  async function logout() {
    if (submitting.current) return;
    submitting.current = true;
    setPending(true);
    setError('');
    try {
      await authApi.logout();
      setUser(null);
      setNotice('로그아웃되었습니다.');
      navigate('/login', { replace: true });
    } catch (failure: unknown) {
      setError(getApiError(failure)?.message ?? '로그아웃하지 못했습니다. 다시 시도해 주세요.');
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <main className="grid min-h-dvh place-items-center px-5 py-10 max-[440px]:px-4 max-[440px]:py-6">
      <section className="w-full max-w-[400px] rounded-xl border border-[#e0e5ec] bg-white px-11 py-10 shadow-[0_8px_30px_#172b4d08] max-[440px]:px-6 max-[440px]:py-8" aria-labelledby="auth-title">
        <div className="mb-[26px] text-center text-[30px] font-[750] tracking-[-1px]">AutoDoc</div>
        <h1 className="mb-2.5 text-center text-xl font-bold" id="auth-title">{user ? '로그인 완료' : signup ? '회원가입' : '로그인'}</h1>
        <p className={descriptionClassName}>{user ? `${user.name}님, 환영합니다.` : signup ? 'AutoDoc 계정을 만들어 주세요.' : '계정으로 로그인해 주세요.'}</p>
        {error && <p className={errorClassName} role="alert">{error}</p>}
        {notice && <p className={successClassName} role="status">{notice}</p>}
        {checking ? <p role="status">로그인 상태를 확인하고 있습니다.</p> : user ? (
          <div className="flex flex-col gap-[18px]">
            <p className="text-center [overflow-wrap:anywhere]">{user.email}</p>
            <p className={descriptionClassName}>문서 관리 화면은 추후 제공됩니다.</p>
            <button type="button" className={primaryClassName} onClick={logout} disabled={pending}>{pending ? '로그아웃 중…' : '로그아웃'}</button>
          </div>
        ) : (
          <form className="flex flex-col gap-[18px]" onSubmit={submit} aria-busy={pending}>
            <fieldset className={fieldsetClassName} disabled={pending}>
              {signup && <label className={fieldLabelClassName} htmlFor="name">이름<input className={fieldClassName} id="name" name="name" autoComplete="name" placeholder="이름" required maxLength={100} /></label>}
              <label className={fieldLabelClassName} htmlFor="email">이메일<input className={fieldClassName} id="email" name="email" type="email" autoComplete="username" placeholder="이메일" required maxLength={255} /></label>
              <label className={fieldLabelClassName} htmlFor="password">비밀번호<input className={fieldClassName} id="password" name="password" type="password" autoComplete={signup ? 'new-password' : 'current-password'} placeholder="비밀번호" required minLength={8} maxLength={72} aria-describedby={signup ? 'password-hint' : undefined} /></label>
              {signup && <>
                <p id="password-hint" className={hintClassName}>8자 이상, 특수 문자를 섞어서 입력해 주세요.</p>
                <label className={fieldLabelClassName} htmlFor="confirmPassword">비밀번호 확인<input className={fieldClassName} id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" placeholder="비밀번호 확인" required minLength={8} maxLength={72} /></label>
                <label className={checkLabelClassName} htmlFor="hasTeam"><input className={checkboxClassName} id="hasTeam" type="checkbox" checked={hasTeam} onChange={(event) => setHasTeam(event.target.checked)} aria-controls="team-fields" />팀이 있습니다</label>
                {hasTeam && <div id="team-fields" className="flex flex-col gap-[18px] rounded-lg border border-[#e0e5ec] bg-slate-50 px-3.5 py-[18px]">
                  <fieldset className={fieldsetClassName}>
                    <legend className="mb-3 text-sm font-semibold">팀 가입 방식</legend>
                    <label className={checkLabelClassName}><input className={checkboxClassName} type="radio" name="teamMode" value="existing" checked={teamMode === 'existing'} onChange={() => setTeamMode('existing')} />기존 팀 선택</label>
                    <label className={checkLabelClassName}><input className={checkboxClassName} type="radio" name="teamMode" value="new" checked={teamMode === 'new'} onChange={() => setTeamMode('new')} />새 팀 등록</label>
                  </fieldset>
                  {teamMode === 'existing' ? <>
                    {teamsLoading ? <p className={hintClassName} role="status">팀 목록을 불러오는 중입니다.</p> : teamsError ? <div><p className={errorClassName} role="alert">{teamsError}</p><button type="button" className={secondaryClassName} onClick={() => setTeamsRetry((value) => value + 1)}>다시 불러오기</button></div> : <>
                      <label className={fieldLabelClassName} htmlFor="teamId">소속 팀<select className={fieldClassName} id="teamId" name="teamId" required defaultValue=""><option value="" disabled>팀을 선택해 주세요</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
                      {teams.length === 0 && <p className={hintClassName} role="status">등록된 팀이 없습니다. 새 팀 등록을 선택해 주세요.</p>}
                    </>}
                  </> : <>
                    <label className={fieldLabelClassName} htmlFor="teamName">팀 이름<input className={fieldClassName} id="teamName" name="teamName" placeholder="팀 이름" required maxLength={100} /></label>
                    <label className={fieldLabelClassName} htmlFor="teamDescription">팀 설명 <span className="text-xs font-normal text-slate-500">(선택)</span><textarea className={`resize-y ${fieldClassName}`} id="teamDescription" name="teamDescription" placeholder="팀을 간단히 소개해 주세요" maxLength={255} rows={3} /></label>
                  </>}
                </div>}
              </>}
              {!signup && <div className="flex items-center justify-between gap-3">
                <label className="flex cursor-pointer items-center gap-2.5 text-[13px] font-normal whitespace-nowrap" htmlFor="rememberMe"><input className={checkboxClassName} id="rememberMe" name="rememberMe" type="checkbox" title="로그인 후 최대 7일간 유지" />로그인 유지</label>
                <button className={`cursor-pointer self-center border-0 bg-transparent py-1 text-[13px] whitespace-nowrap text-slate-600 hover:text-blue-600 ${focusClassName}`} type="button" onClick={() => recoveryDialog.current?.showModal()}>비밀번호 찾기</button>
              </div>}
              <button className={primaryClassName} type="submit" disabled={signup && hasTeam && teamMode === 'existing' && (teamsLoading || !!teamsError || teams.length === 0)}>{pending ? signup ? '가입 중…' : '로그인 중…' : signup ? '회원가입' : '로그인'}</button>
            </fieldset>
            {!pending && <Link className={secondaryClassName} to={signup ? '/login' : '/signup'} onClick={() => { setError(''); setNotice(''); }}>{signup ? '로그인으로 돌아가기' : '회원가입'}</Link>}
          </form>
        )}
      </section>
      <dialog ref={recoveryDialog} className="m-auto w-[calc(100%-32px)] max-w-[400px] rounded-xl border border-[#e0e5ec] bg-white p-7 text-[#172b4d] backdrop:bg-[#172b4d66]" aria-labelledby="recovery-title" aria-describedby="recovery-description">
        <h2 className="mb-4 text-xl font-bold" id="recovery-title">비밀번호 찾기</h2>
        <p className="mb-5 text-sm leading-[1.7] text-slate-500" id="recovery-description">비밀번호 재설정 기능은 준비 중입니다. 현재는 가입한 이메일로 비밀번호 재설정 링크를 발송할 수 없습니다.</p>
        <form method="dialog"><button className={primaryClassName} autoFocus>로그인으로 돌아가기</button></form>
      </dialog>
    </main>
  );
}
