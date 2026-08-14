import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { api, ApiError } from '../api/client'
import type { MyTicketResponse } from '../api/types'
import { formatDateTime } from '../format'

export function SharedTicketPage() {
  const { code } = useParams<{ code: string }>()
  const [ticket, setTicket] = useState<MyTicketResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!code) return
    api<MyTicketResponse>(`/api/tickets/shared/${encodeURIComponent(code)}`)
      .then(setTicket)
      .catch((err) => {
        if (err instanceof ApiError && err.code === 'INVALID_QR') {
          setError('Este link de ingresso é inválido ou foi adulterado.')
        } else {
          setError(err instanceof ApiError ? err.message : 'Não foi possível carregar o ingresso.')
        }
      })
  }, [code])

  return (
    <div className="page page-narrow">
      <span className="eyebrow">Ingresso compartilhado</span>

      {error && <div className="alert alert-error">{error}</div>}
      {!error && !ticket && <p className="muted">Carregando ingresso...</p>}

      {ticket && (
        <article className={`card shared-ticket ${ticket.usedAt ? 'ticket-used' : ''}`}>
          <h1>{ticket.eventTitulo}</h1>
          <p className="muted">
            {formatDateTime(ticket.eventData)} · {ticket.eventLocal}
          </p>
          <p className="shared-ticket-seat">
            Assento{' '}
            <strong>
              {ticket.fileira}
              {ticket.numero}
            </strong>
          </p>

          {ticket.usedAt ? (
            <span className="badge badge-used">Utilizado em {formatDateTime(ticket.usedAt)}</span>
          ) : (
            <span className="badge badge-valid">Válido</span>
          )}

          <div className="ticket-qr shared-ticket-qr">
            <QRCodeSVG value={ticket.qrCode} size={180} marginSize={2} />
          </div>

          <p className="helper">
            Apresente este código na entrada do evento. Quem tiver este link consegue usar o
            ingresso, então compartilhe apenas com quem vai ocupar o lugar.
          </p>
        </article>
      )}

      <Link className="back-link" to="/">
        ← Ver outros eventos
      </Link>
    </div>
  )
}
