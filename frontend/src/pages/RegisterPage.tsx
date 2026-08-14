import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/client'
import type { Role } from '../api/types'

const ROLES: { value: Role; label: string; hint: string }[] = [
  { value: 'CUSTOMER', label: 'Cliente', hint: 'Comprar ingressos' },
  { value: 'ORGANIZER', label: 'Organizador', hint: 'Criar e gerenciar eventos' },
  { value: 'GATE', label: 'Portaria', hint: 'Validar ingressos na entrada' },
]

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [role, setRole] = useState<Role>('CUSTOMER')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const state = await register(email, senha, role)
      navigate(state.role === 'ORGANIZER' ? '/organizador' : state.role === 'GATE' ? '/portaria' : '/')
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Este email já está em uso.')
      } else if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Erro inesperado.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page page-narrow">
      <span className="eyebrow">Cadastro</span>
      <h1>Criar conta</h1>
      <p className="muted">Leva menos de um minuto. O tipo de conta define o que você enxerga.</p>
      <form className="card form" onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha (mín. 6 caracteres)
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            minLength={6}
            required
          />
        </label>
        <fieldset className="role-picker">
          <legend>Tipo de conta</legend>
          {ROLES.map((r) => (
            <label key={r.value} className={`role-option ${role === r.value ? 'selected' : ''}`}>
              <input
                type="radio"
                name="role"
                value={r.value}
                checked={role === r.value}
                onChange={() => setRole(r.value)}
              />
              <span>
                <strong>{r.label}</strong>
                <small>{r.hint}</small>
              </span>
            </label>
          ))}
        </fieldset>
        <button className="btn btn-primary" disabled={loading}>
          {loading ? 'Criando...' : 'Criar conta'}
        </button>
        <p className="form-footer">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  )
}
