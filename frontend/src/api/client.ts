const TOKEN_KEY = 'amos_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const res = await fetch(path, { ...options, headers })
  if (res.status === 401) {
    clearToken()
    if (!path.includes('/auth/login')) window.location.href = '/login'
  }
  const text = await res.text()
  const data: unknown = text && (text.startsWith('{') || text.startsWith('[')) ? JSON.parse(text) : text
  if (!res.ok) {
    const message =
      typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string'
        ? data.message
        : res.statusText || 'Request failed'
    throw new Error(message)
  }
  return data as T
}
