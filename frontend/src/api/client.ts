const baseUrl = '/api/v1'

export type ProblemDetail = { title?: string; detail?: string; status?: number }

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('content-type', 'application/json')
  const credentials = btoa(`${import.meta.env.VITE_API_USER ?? 'admin'}:${import.meta.env.VITE_API_PASSWORD ?? 'admin'}`)
  headers.set('authorization', `Basic ${credentials}`)
  const response = await fetch(`${baseUrl}${path}`, { ...options, headers })
  if (!response.ok) {
    const problem = await response.json() as ProblemDetail
    throw new Error(problem.detail ?? problem.title ?? `Request failed (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
