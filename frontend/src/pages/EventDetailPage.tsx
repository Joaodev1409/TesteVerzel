import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { EventResponse, ReservationResponse, SeatResponse, TicketResponse } from '../api/types'
import { formatDateTime, formatPrice } from '../format'

type Step =
  | { kind: 'browsing' }
  | { kind: 'paying'; reservation: ReservationResponse; seat: SeatResponse }
  | { kind: 'done'; ticket: TicketResponse; seat: SeatResponse }

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>()
  const { auth } = useAuth()
  const navigate = useNavigate()

  const [event, setEvent] = useState<EventResponse | null>(null)
  const [seats, setSeats] = useState<SeatResponse[] | null>(null)
  const [selected, setSelected] = useState<SeatResponse | null>(null)
  const [step, setStep] = useState<Step>({ kind: 'browsing' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const loadSeats = useCallback(() => {
    if (!eventId) return
    api<SeatResponse[]>(`/api/events/${eventId}/seats`)
      .then(setSeats)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Erro ao carregar assentos.'))
  }, [eventId])

  useEffect(() => {
    if (!eventId) return
    api<EventResponse>(`/api/events/${eventId}`)
      .then(setEvent)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Erro ao carregar evento.'))
    loadSeats()
  }, [eventId, loadSeats])

  const rows = useMemo(() => {
    if (!seats) return []
    const byRow = new Map<string, SeatResponse[]>()
    for (const seat of seats) {
      const row = byRow.get(seat.fileira) ?? []
      row.push(seat)
      byRow.set(seat.fileira, row)
    }
    return [...byRow.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([fileira, rowSeats]) => ({
        fileira,
        seats: [...rowSeats].sort((a, b) => a.numero - b.numero),
      }))
  }, [seats])

  async function handleReserve() {
    if (!auth) {
      navigate('/login', { state: { from: `/eventos/${eventId}` } })
      return
    }
    if (!selected || !eventId) return
    setError(null)
    setBusy(true)
    try {
      const reservation = await api<ReservationResponse>('/api/reservations', {
        method: 'POST',
        body: { eventId, seatId: selected.id },
        token: auth.token,
      })
      setStep({ kind: 'paying', reservation, seat: selected })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError('Esse assento acabou de ser reservado por outra pessoa. Escolha outro.')
        setSelected(null)
        loadSeats()
      } else {
        setError(err instanceof ApiError ? err.message : 'Erro ao reservar.')
      }
    } finally {
      setBusy(false)
    }
  }

  async function handlePayment(paymentSuccessful: boolean) {
    if (step.kind !== 'paying' || !auth) return
    setError(null)
    setBusy(true)
    try {
      const ticket = await api<TicketResponse>(`/api/reservations/${step.reservation.id}/confirm`, {
        method: 'POST',
        body: { paymentSuccessful },
        token: auth.token,
      })
      setStep({ kind: 'done', ticket, seat: step.seat })
      loadSeats()
    } catch (err) {
      if (err instanceof ApiError && err.status === 402) {
        setError('Pagamento recusado (simulado). A reserva continua válida — tente pagar de novo antes de expirar.')
      } else if (err instanceof ApiError && err.status === 410) {
        setError('A reserva expirou. Escolha um assento novamente.')
        setStep({ kind: 'browsing' })
        setSelected(null)
        loadSeats()
      } else {
        setError(err instanceof ApiError ? err.message : 'Erro ao confirmar pagamento.')
      }
    } finally {
      setBusy(false)
    }
  }

  if (!event) {
    return (
      <div className="page">
        {error ? <div className="alert alert-error">{error}</div> : <p className="muted">Carregando...</p>}
      </div>
    )
  }

  return (
    <div className="page">
      <Link to="/" className="back-link">
        ← Voltar aos eventos
      </Link>
      <h1>{event.titulo}</h1>
      <p className="muted">
        {formatDateTime(event.data)} · {event.local} · a partir de {formatPrice(event.precoBase)}
      </p>
      {event.sinopse && <p className="event-sinopse-full">{event.sinopse}</p>}

      {error && <div className="alert alert-error">{error}</div>}

      {step.kind === 'browsing' && (
        <>
          <div className="screen-indicator">TELA / PALCO</div>
          <div className="seat-map">
            {rows.map((row) => (
              <div key={row.fileira} className="seat-row">
                <span className="seat-row-label">{row.fileira}</span>
                {row.seats.map((seat) => (
                  <button
                    key={seat.id}
                    className={`seat seat-${seat.status.toLowerCase()} ${
                      selected?.id === seat.id ? 'seat-selected' : ''
                    }`}
                    disabled={seat.status !== 'AVAILABLE'}
                    onClick={() => setSelected(selected?.id === seat.id ? null : seat)}
                    title={`${seat.fileira}${seat.numero} — ${seat.status}`}
                  >
                    {seat.numero}
                  </button>
                ))}
              </div>
            ))}
          </div>
          <div className="seat-legend">
            <span>
              <i className="seat seat-available" /> Livre
            </span>
            <span>
              <i className="seat seat-held" /> Reservado
            </span>
            <span>
              <i className="seat seat-sold" /> Vendido
            </span>
            <span>
              <i className="seat seat-selected" /> Selecionado
            </span>
          </div>
          <div className="reserve-bar">
            {selected ? (
              <span>
                Assento <strong>{selected.fileira}{selected.numero}</strong> · {formatPrice(event.precoBase)}
              </span>
            ) : (
              <span className="muted">Selecione um assento livre</span>
            )}
            <button className="btn btn-primary" disabled={!selected || busy} onClick={handleReserve}>
              {busy ? 'Reservando...' : auth ? 'Reservar' : 'Entrar para reservar'}
            </button>
          </div>
        </>
      )}

      {step.kind === 'paying' && (
        <div className="card payment-card">
          <h2>Pagamento (simulado)</h2>
          <p>
            Assento <strong>{step.seat.fileira}{step.seat.numero}</strong> ·{' '}
            {formatPrice(event.precoBase)}
          </p>
          <p className="muted">
            Reserva válida até {formatDateTime(step.reservation.expiresAt)}. Se não pagar até lá, o
            assento volta a ficar livre.
          </p>
          <div className="payment-actions">
            <button className="btn btn-primary" disabled={busy} onClick={() => handlePayment(true)}>
              Aprovar pagamento
            </button>
            <button className="btn btn-danger" disabled={busy} onClick={() => handlePayment(false)}>
              Simular recusa
            </button>
          </div>
        </div>
      )}

      {step.kind === 'done' && (
        <div className="card success-card">
          <h2>🎉 Ingresso emitido!</h2>
          <p>
            Assento <strong>{step.seat.fileira}{step.seat.numero}</strong> confirmado para{' '}
            <strong>{event.titulo}</strong>.
          </p>
          <Link className="btn btn-primary" to="/meus-ingressos">
            Ver meus ingressos
          </Link>
        </div>
      )}
    </div>
  )
}
