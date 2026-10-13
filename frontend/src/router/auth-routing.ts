import { redirect, type LoaderFunctionArgs } from 'react-router-dom';
import { authApi } from '../api/auth-api';
import { getApiError } from '../api/client';

export function getLoginDestination(search: string) {
  const destination = new URLSearchParams(search).get('returnTo');
  return destination && /^\/(dashboard|documents|templates|teams|profile)(\/|\?|#|$)/.test(destination)
    ? destination : '/dashboard';
}

export async function requireUser({ request }: LoaderFunctionArgs) {
  const url = new URL(request.url);
  const loginUrl = `/login?returnTo=${encodeURIComponent(url.pathname + url.search)}`;
  try {
    const user = await authApi.getCurrentUser(request.signal);
    if (user.status !== 'ACTIVE') return redirect(loginUrl);
    return user;
  } catch (failure: unknown) {
    const code = getApiError(failure)?.code;
    if (code === 'UNAUTHORIZED' || code === 'INACTIVE_USER' || code === 'USER_NOT_FOUND') {
      return redirect(loginUrl);
    }
    throw failure;
  }
}
