import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/client'
import type { Role } from '../api/types'

function homeFor(role: Role): string {
  if (role === 'ORGANIZER') return '/organizador'
  if (role === 'GATE') return '/portaria'
  return '/'
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const state = await login(email, senha)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? homeFor(state.role))
    } catch (err) {
      setError(err instanceof ApiError ? 'Email ou senha inválidos.' : 'Erro inesperado.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Entrar</h1>
      <form className="card form" onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
        </label>
        <button className="btn btn-primary" disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
        <p className="form-footer">
          Não tem conta? <Link to="/registro">Criar conta</Link>
        </p>
      </form>
    </div>
  )
}
