const baseUrl = '/api/v1'

export type ProblemDetail = { title?: string; detail?: string; status?: number }
export type CurrentUser = { username: string }
type Csrf = { headerName: string; token: string }

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message)
  }
}

let csrf: Csrf | undefined

async function getCsrf(): Promise<Csrf> {
  const response = await fetch(`${baseUrl}/auth/csrf`, { credentials: 'same-origin' })
  if (!response.ok) throw new ApiError(`Request failed (${response.status})`, response.status)
  csrf = await response.json() as Csrf
  return csrf
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('content-type', 'application/json')
  const method = options.method?.toUpperCase() ?? 'GET'
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrf ?? await getCsrf()
    headers.set(token.headerName, token.token)
  }
  const response = await fetch(`${baseUrl}${path}`, { ...options, headers, credentials: 'same-origin' })
  if (!response.ok) {
    if (response.status === 401) window.dispatchEvent(new Event('fanel:unauthorized'))
    const problem = await response.json().catch(() => ({})) as ProblemDetail
    throw new ApiError(problem.detail ?? problem.title ?? `Request failed (${response.status})`, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function currentUser(): Promise<CurrentUser | null> {
  const response = await fetch(`${baseUrl}/auth/me`, { credentials: 'same-origin' })
  if (response.status === 401) return null
  if (!response.ok) throw new ApiError(`Request failed (${response.status})`, response.status)
  return response.json() as Promise<CurrentUser>
}

export async function login(username: string, password: string): Promise<boolean> {
  const token = await getCsrf()
  const body = new URLSearchParams({ username, password })
  const response = await fetch(`${baseUrl}/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded', [token.headerName]: token.token },
    body,
    credentials: 'same-origin',
  })
  csrf = undefined
  return response.ok
}

export async function logout(): Promise<void> {
  const token = csrf ?? await getCsrf()
  await fetch(`${baseUrl}/auth/logout`, {
    method: 'POST',
    headers: { [token.headerName]: token.token },
    credentials: 'same-origin',
  })
  csrf = undefined
}
