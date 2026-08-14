import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { EventResponse } from '../api/types'
import { formatDateTime, formatPrice } from '../format'

export function EventsPage() {
  const { auth } = useAuth()
  const [events, setEvents] = useState<EventResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    api<EventResponse[]>('/api/events')
      .then(setEvents)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Erro ao carregar eventos.'))
  }, [])

  const filtered = useMemo(() => {
    if (!events) return null
    const term = search.trim().toLowerCase()
    if (!term) return events
    return events.filter(
      (e) => e.titulo.toLowerCase().includes(term) || e.local.toLowerCase().includes(term),
    )
  }, [events, search])

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <span className="eyebrow">Programação</span>
          <h1>Eventos em cartaz</h1>
        </div>
        <input
          className="search-input"
          type="search"
          placeholder="Buscar por título ou local..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {!auth && (
        <div className="guest-bar">
          <p>
            Você está navegando como visitante. Entre para escolher seu lugar e receber o ingresso.
          </p>
          <div className="guest-bar-actions">
            <Link className="btn btn-ghost" to="/login">
              Entrar
            </Link>
            <Link className="btn btn-primary" to="/registro">
              Criar conta
            </Link>
          </div>
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}
      {!error && filtered === null && <p className="muted">Carregando eventos...</p>}
      {filtered !== null && filtered.length === 0 && (
        <p className="muted">
          {search ? 'Nenhum evento encontrado para essa busca.' : 'Nenhum evento publicado ainda.'}
        </p>
      )}

      <div className="event-grid">
        {filtered?.map((event) => (
          <Link key={event.id} to={`/eventos/${event.id}`} className="event-card">
            <h2>{event.titulo}</h2>
            {event.sinopse && <p className="event-sinopse">{event.sinopse}</p>}
            <dl>
              <div>
                <dt>Data</dt>
                <dd>{formatDateTime(event.data)}</dd>
              </div>
              <div>
                <dt>Local</dt>
                <dd>{event.local}</dd>
              </div>
              <div>
                <dt>A partir de</dt>
                <dd className="event-price">{formatPrice(event.precoBase)}</dd>
              </div>
            </dl>
            <span className="btn btn-primary">Ver assentos</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
