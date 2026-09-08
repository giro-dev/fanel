import { createContext, useContext, useSyncExternalStore, type ReactNode } from 'react'
import { getAuthHeader, getCredentials, setCredentials, subscribe } from '../auth/authStore'

type AuthContextValue = {
  username?: string
  login: (username: string, password: string) => Promise<boolean>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const credentials = useSyncExternalStore(subscribe, getCredentials)

  const value: AuthContextValue = {
    username: credentials?.username,
    login: async (username, password) => {
      setCredentials({ username, password })
      try {
        const response = await fetch('/api/v1/households', { headers: { authorization: getAuthHeader()! } })
        if (!response.ok) {
          setCredentials(null)
          return false
        }
        return true
      } catch {
        setCredentials(null)
        return false
      }
    },
    logout: () => setCredentials(null),
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
