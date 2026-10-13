import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth-api';
import { userApi } from '../api/user-api';
import { getApiError } from '../api/client';
import type { User } from '../api/types';

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
    <main className="auth-shell">
      <section className="auth-card" aria-labelledby="auth-title">
        <div className="auth-brand">AutoDoc</div>
        <h1 id="auth-title">{user ? '로그인 완료' : signup ? '회원가입' : '로그인'}</h1>
        <p className="auth-description">{user ? `${user.name}님, 환영합니다.` : signup ? 'AutoDoc 계정을 만들어 주세요.' : '계정으로 로그인해 주세요.'}</p>
        {error && <p className="auth-message auth-error" role="alert">{error}</p>}
        {notice && <p className="auth-message auth-success" role="status">{notice}</p>}
        {checking ? <p role="status">로그인 상태를 확인하고 있습니다.</p> : user ? (
          <div className="auth-form">
            <p className="auth-user-email">{user.email}</p>
            <p className="auth-description">문서 관리 화면은 추후 제공됩니다.</p>
            <button type="button" className="auth-primary" onClick={logout} disabled={pending}>{pending ? '로그아웃 중…' : '로그아웃'}</button>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit} aria-busy={pending}>
            <fieldset disabled={pending}>
              {signup && <label htmlFor="name">이름<input id="name" name="name" autoComplete="name" placeholder="이름" required maxLength={100} /></label>}
              <label htmlFor="email">이메일<input id="email" name="email" type="email" autoComplete="username" placeholder="이메일" required maxLength={255} /></label>
              <label htmlFor="password">비밀번호<input id="password" name="password" type="password" autoComplete={signup ? 'new-password' : 'current-password'} placeholder="비밀번호" required minLength={8} maxLength={72} aria-describedby={signup ? 'password-hint' : undefined} /></label>
              {signup && <>
                <p id="password-hint" className="auth-hint">8자 이상, UTF-8 기준 72바이트 이하로 입력해 주세요.</p>
                <label htmlFor="confirmPassword">비밀번호 확인<input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" placeholder="비밀번호 확인" required minLength={8} maxLength={72} /></label>
                <label className="auth-check" htmlFor="hasTeam"><input id="hasTeam" type="checkbox" checked={hasTeam} onChange={(event) => setHasTeam(event.target.checked)} aria-controls="team-fields" />팀이 있습니다</label>
                {hasTeam && <div id="team-fields" className="auth-team-fields">
                  <fieldset className="auth-team-choice">
                    <legend>팀 가입 방식</legend>
                    <label className="auth-check"><input type="radio" name="teamMode" value="existing" checked={teamMode === 'existing'} onChange={() => setTeamMode('existing')} />기존 팀 선택</label>
                    <label className="auth-check"><input type="radio" name="teamMode" value="new" checked={teamMode === 'new'} onChange={() => setTeamMode('new')} />새 팀 등록</label>
                  </fieldset>
                  {teamMode === 'existing' ? <>
                    {teamsLoading ? <p className="auth-hint" role="status">팀 목록을 불러오는 중입니다.</p> : teamsError ? <div><p className="auth-message auth-error" role="alert">{teamsError}</p><button type="button" className="auth-secondary" onClick={() => setTeamsRetry((value) => value + 1)}>다시 불러오기</button></div> : <>
                      <label htmlFor="teamId">소속 팀<select id="teamId" name="teamId" required defaultValue=""><option value="" disabled>팀을 선택해 주세요</option>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
                      {teams.length === 0 && <p className="auth-hint" role="status">등록된 팀이 없습니다. 새 팀 등록을 선택해 주세요.</p>}
                    </>}
                  </> : <>
                    <label htmlFor="teamName">팀 이름<input id="teamName" name="teamName" placeholder="팀 이름" required maxLength={100} /></label>
                    <label htmlFor="teamDescription">팀 설명 <span className="auth-hint">(선택)</span><textarea id="teamDescription" name="teamDescription" placeholder="팀을 간단히 소개해 주세요" maxLength={255} rows={3} /></label>
                  </>}
                </div>}
              </>}
              {!signup && <div className="auth-options">
                <label className="auth-check" htmlFor="rememberMe"><input id="rememberMe" name="rememberMe" type="checkbox" title="로그인 후 최대 7일간 유지" />로그인 유지</label>
                <button className="auth-text-button" type="button" onClick={() => recoveryDialog.current?.showModal()}>비밀번호 찾기</button>
              </div>}
              <button className="auth-primary" type="submit" disabled={signup && hasTeam && teamMode === 'existing' && (teamsLoading || !!teamsError || teams.length === 0)}>{pending ? signup ? '가입 중…' : '로그인 중…' : signup ? '회원가입' : '로그인'}</button>
            </fieldset>
            {!pending && <Link className="auth-secondary" to={signup ? '/login' : '/signup'} onClick={() => { setError(''); setNotice(''); }}>{signup ? '로그인으로 돌아가기' : '회원가입'}</Link>}
          </form>
        )}
      </section>
      <dialog ref={recoveryDialog} className="auth-dialog" aria-labelledby="recovery-title" aria-describedby="recovery-description">
        <h2 id="recovery-title">비밀번호 찾기</h2>
        <p id="recovery-description">비밀번호 재설정 기능은 준비 중입니다. 현재는 가입한 이메일로 비밀번호 재설정 링크를 발송할 수 없습니다.</p>
        <form method="dialog"><button className="auth-primary" autoFocus>로그인으로 돌아가기</button></form>
      </dialog>
    </main>
  );
}
