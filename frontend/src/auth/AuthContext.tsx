import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearToken, getToken, setToken } from '../api/client'
import type { AuthResponse, UserView } from '../types'

interface AuthContextValue {
  user: UserView | null
  loading: boolean
  can: (code: string) => boolean
  login: (username: string, password: string) => Promise<UserView>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserView | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function boot() {
      if (!getToken()) {
        setLoading(false)
        return
      }
      try {
        setUser(await api<UserView>('/api/me'))
      } catch {
        clearToken()
      } finally {
        setLoading(false)
      }
    }
    void boot()
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    can: (code) => Boolean(user?.permissions.includes(code) || user?.roles.includes('ADMIN')),
    async login(username, password) {
      const res = await api<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      setToken(res.token)
      setUser(res.user)
      return res.user
    },
    logout() {
      clearToken()
      setUser(null)
    },
  }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
