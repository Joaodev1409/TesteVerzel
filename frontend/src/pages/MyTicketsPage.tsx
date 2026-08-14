import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { MyTicketResponse } from '../api/types'
import { formatDateTime } from '../format'

export function MyTicketsPage() {
  const { auth } = useAuth()
  const [tickets, setTickets] = useState<MyTicketResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!auth) return
    api<MyTicketResponse[]>('/api/me/tickets', { token: auth.token })
      .then(setTickets)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Erro ao carregar ingressos.'))
  }, [auth])

  return (
    <div className="page">
      <h1>Meus ingressos</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {!error && tickets === null && <p className="muted">Carregando...</p>}
      {tickets !== null && tickets.length === 0 && (
        <p className="muted">
          Você ainda não tem ingressos. <Link to="/">Ver eventos em cartaz</Link>
        </p>
      )}

      <div className="ticket-grid">
        {tickets?.map((ticket) => (
          <article key={ticket.id} className={`ticket-card ${ticket.usedAt ? 'ticket-used' : ''}`}>
            <div className="ticket-info">
              <h2>{ticket.eventTitulo}</h2>
              <p>{formatDateTime(ticket.eventData)}</p>
              <p>{ticket.eventLocal}</p>
              <p className="ticket-seat">
                Assento <strong>{ticket.fileira}{ticket.numero}</strong>
              </p>
              {ticket.usedAt ? (
                <span className="badge badge-used">Utilizado em {formatDateTime(ticket.usedAt)}</span>
              ) : (
                <span className="badge badge-valid">Válido</span>
              )}
            </div>
            <div className="ticket-qr">
              <QRCodeSVG value={ticket.qrCode} size={140} marginSize={2} />
              <details>
                <summary>Código manual</summary>
                <code className="qr-text">{ticket.qrCode}</code>
              </details>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
