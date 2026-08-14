import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api } from '../api/client'
import type { AuthResponse, Role } from '../api/types'

interface AuthState {
  token: string
  email: string
  role: Role
}

interface AuthContextValue {
  auth: AuthState | null
  login: (email: string, senha: string) => Promise<AuthState>
  register: (email: string, senha: string, role: Role) => Promise<AuthState>
  logout: () => void
}

const STORAGE_KEY = 'eventos.auth'

const AuthContext = createContext<AuthContextValue | null>(null)

function loadStoredAuth(): AuthState | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthState
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(loadStoredAuth)

  const applyAuth = useCallback((response: AuthResponse): AuthState => {
    const state: AuthState = { token: response.token, email: response.email, role: response.role }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    setAuth(state)
    return state
  }, [])

  const login = useCallback(
    async (email: string, senha: string) => {
      const response = await api<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, senha } })
      return applyAuth(response)
    },
    [applyAuth],
  )

  const register = useCallback(
    async (email: string, senha: string, role: Role) => {
      const response = await api<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: { email, senha, role },
      })
      return applyAuth(response)
    },
    [applyAuth],
  )

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    setAuth(null)
  }, [])

  const value = useMemo(() => ({ auth, login, register, logout }), [auth, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
