import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Navbar() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">
        Eventos &amp; Ingressos
      </Link>
      <nav className="navbar-links">
        <NavLink to="/">Eventos</NavLink>
        {auth?.role === 'CUSTOMER' && <NavLink to="/meus-ingressos">Meus ingressos</NavLink>}
        {auth?.role === 'ORGANIZER' && <NavLink to="/organizador">Organizador</NavLink>}
        {auth?.role === 'GATE' && <NavLink to="/portaria">Portaria</NavLink>}
      </nav>
      <div className="navbar-auth">
        {auth ? (
          <>
            <span className="navbar-user">
              {auth.email} <span className="badge">{auth.role}</span>
            </span>
            <button className="btn btn-ghost" onClick={handleLogout}>
              Sair
            </button>
          </>
        ) : (
          <>
            <Link className="btn btn-ghost" to="/login">
              Entrar
            </Link>
            <Link className="btn btn-primary" to="/registro">
              Criar conta
            </Link>
          </>
        )}
      </div>
    </header>
  )
}
