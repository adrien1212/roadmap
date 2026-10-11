import { createIsomorphicFn } from '@tanstack/react-start'

/**
 * The one HTTP client for the Spring API — every frontend call goes through it.
 *
 * - Browser: relative `/api/...` URLs, same origin; the proxy forwards them to Spring.
 * - SSR: no origin to be relative to, so Spring is called directly at `API_URL`.
 *   (Task 21 forwards the incoming request's cookie here, in this one place.)
 */
const baseUrl = createIsomorphicFn()
  .server(() => process.env.API_URL ?? 'http://localhost:8045')
  .client(() => '')

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
  const response = await fetch(baseUrl() + path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined && { 'Content-Type': 'application/json' }),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
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
