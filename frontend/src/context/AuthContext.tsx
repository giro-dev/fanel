import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { currentUser, login as requestLogin, logout as requestLogout, type CurrentUser } from '../api/client'

type AuthContextValue = {
  user: CurrentUser | null
  loading: boolean
  login: (username: string, password: string) => Promise<boolean>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    void currentUser().then(setUser).finally(() => setLoading(false))
    const unauthorized = () => setUser(null)
    window.addEventListener('fanel:unauthorized', unauthorized)
    return () => window.removeEventListener('fanel:unauthorized', unauthorized)
  }, [])

  const login = async (username: string, password: string) => {
    const authenticated = await requestLogin(username, password)
    if (authenticated) setUser(await currentUser())
    return authenticated
  }

  const logout = async () => {
    await requestLogout()
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
