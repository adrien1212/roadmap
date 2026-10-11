import { createIsomorphicFn } from '@tanstack/react-start'
import { getRequestHeader } from '@tanstack/react-start/server'

/**
 * The one HTTP client for the Spring API — every frontend call goes through it.
 *
 * - Browser: relative `/api/...` URLs, same origin; the proxy forwards them to Spring with the
 *   session cookie.
 * - SSR: no origin to be relative to, so Spring is called directly at `API_URL`, forwarding the
 *   incoming request's `Cookie` header so the Owner's session comes along.
 * - Mutating requests carry Spring's CSRF token (`XSRF-TOKEN` cookie → `X-XSRF-TOKEN` header).
 */
const target = createIsomorphicFn()
  .server(() => ({ baseUrl: process.env.API_URL ?? 'http://localhost:8045', cookie: getRequestHeader('cookie') }))
  .client(() => ({ baseUrl: '', cookie: undefined as string | undefined }))

const isBrowser = typeof document !== 'undefined'

/** The CSRF token Spring left in the `XSRF-TOKEN` cookie (readable by design), if any. */
function csrfToken(cookies: string | undefined): string | undefined {
  const match = (cookies ?? '').match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : undefined
}

/** A non-2xx answer; `detail` is the problem-JSON message from Spring when there is one. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string,
  ) {
    super(detail)
    this.name = 'ApiError'
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const { baseUrl, cookie } = target()
  const token = method === 'GET' ? undefined : csrfToken(isBrowser ? document.cookie : cookie)
  const response = await fetch(baseUrl + path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined && { 'Content-Type': 'application/json' }),
      ...(cookie && { Cookie: cookie }),
      ...(token && { 'X-XSRF-TOKEN': token }),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  // The session ended while the app was open: back to the Home page to sign in again
  // (not from Home itself, which asks /api/me to know whether someone is signed in).
  if (response.status === 401 && isBrowser && window.location.pathname !== '/' && !path.startsWith('/api/public/')) {
    window.location.assign('/')
  }
  if (!response.ok) {
    throw new ApiError(response.status, await errorDetail(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function errorDetail(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as { detail?: string; title?: string }
    return problem.detail ?? problem.title ?? response.statusText
  } catch {
    return response.statusText || `HTTP ${response.status}`
  }
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
  delete: (path: string) => request<void>('DELETE', path),
}

/** Ends the Spring session, then shows the Home page. */
export async function signOut(): Promise<void> {
  const token = csrfToken(document.cookie)
  await fetch('/logout', { method: 'POST', headers: token ? { 'X-XSRF-TOKEN': token } : {}, redirect: 'manual' })
  window.location.assign('/')
}
