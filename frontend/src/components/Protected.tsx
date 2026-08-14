import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'

export function Protected({ role, children }: { role: Role; children: ReactNode }) {
  const { auth } = useAuth()
  const location = useLocation()

  if (!auth) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  if (auth.role !== role) {
    return (
      <div className="page">
        <div className="alert alert-error">
          Acesso restrito ao papel {role}. Você está conectado como {auth.role}.
        </div>
      </div>
    )
  }
  return <>{children}</>
}
