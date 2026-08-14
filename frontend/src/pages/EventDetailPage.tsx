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

// The server decides the outcome from the card number; these mirror the usual sandbox test cards.
const TEST_CARDS = {
  aprovado: '4242 4242 4242 4242',
  recusado: '4000 0000 0000 0002',
  semSaldo: '4000 0000 0000 9995',
}

const DECLINE_MESSAGES: Record<string, string> = {
  CARD_DECLINED: 'Cartão recusado pela operadora.',
  INSUFFICIENT_FUNDS: 'Saldo insuficiente no cartão.',
  INVALID_CARD: 'Número de cartão inválido.',
}

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>()
  const { auth } = useAuth()
  const navigate = useNavigate()

  const [event, setEvent] = useState<EventResponse | null>(null)
  const [seats, setSeats] = useState<SeatResponse[] | null>(null)
  const [selected, setSelected] = useState<SeatResponse | null>(null)
  const [step, setStep] = useState<Step>({ kind: 'browsing' })
  const [cardNumber, setCardNumber] = useState(TEST_CARDS.aprovado)
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

  // Enquanto o usuário escolhe, outra pessoa pode ocupar um lugar: o mapa se atualiza sozinho.
  useEffect(() => {
    if (step.kind !== 'browsing') return
    const timer = window.setInterval(loadSeats, 5000)
    return () => window.clearInterval(timer)
  }, [step.kind, loadSeats])

  // Se o assento selecionado foi ocupado por outra pessoa, avisa em vez de deixar clicar em vão.
  useEffect(() => {
    if (!selected || !seats) return
    const atual = seats.find((seat) => seat.id === selected.id)
    if (atual && atual.status !== 'AVAILABLE') {
      setSelected(null)
      setError('O assento que você tinha selecionado acabou de ser ocupado. Escolha outro.')
    }
  }, [seats, selected])

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

  async function handleCancel() {
    if (step.kind !== 'paying' || !auth) return
    setError(null)
    setBusy(true)
    try {
      await api<void>(`/api/reservations/${step.reservation.id}/cancel`, {
        method: 'POST',
        token: auth.token,
      })
      setStep({ kind: 'browsing' })
      setSelected(null)
      loadSeats()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erro ao cancelar a reserva.')
    } finally {
      setBusy(false)
    }
  }

  async function handlePayment(e: React.FormEvent) {
    e.preventDefault()
    if (step.kind !== 'paying' || !auth) return
    setError(null)
    setBusy(true)
    try {
      const ticket = await api<TicketResponse>(`/api/reservations/${step.reservation.id}/confirm`, {
        method: 'POST',
        body: { cardNumber },
        token: auth.token,
      })
      setStep({ kind: 'done', ticket, seat: step.seat })
      loadSeats()
    } catch (err) {
      if (err instanceof ApiError && err.status === 402) {
        const motivo = DECLINE_MESSAGES[err.code ?? ''] ?? 'Pagamento recusado.'
        setError(`${motivo} A reserva continua válida — tente outro cartão antes de expirar.`)
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
      <span className="eyebrow">Escolha seu lugar</span>
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
        <form className="card payment-card form" onSubmit={handlePayment}>
          <h2>Pagamento (simulado)</h2>
          <p>
            Assento <strong>{step.seat.fileira}{step.seat.numero}</strong> ·{' '}
            {formatPrice(event.precoBase)}
          </p>
          <p className="muted">
            Reserva válida até {formatDateTime(step.reservation.expiresAt)}. Se não pagar até lá, o
            assento volta a ficar livre.
          </p>

          <label>
            Número do cartão
            <input
              type="text"
              inputMode="numeric"
              placeholder="0000 0000 0000 0000"
              value={cardNumber}
              onChange={(e) => setCardNumber(e.target.value)}
              required
            />
          </label>

          <p className="helper">
            Cobrança simulada — nenhum dado real é processado. O servidor decide o resultado a
            partir do cartão.
          </p>

          <div className="test-cards">
            <span>Cartões de teste:</span>
            {Object.entries(TEST_CARDS).map(([label, number]) => (
              <button
                key={label}
                type="button"
                className="btn btn-ghost"
                onClick={() => setCardNumber(number)}
              >
                {label === 'semSaldo' ? 'sem saldo' : label}
              </button>
            ))}
          </div>

          <button className="btn btn-primary btn-cta" disabled={busy}>
            {busy ? 'Processando...' : `Pagar ${formatPrice(event.precoBase)}`}
          </button>

          <button type="button" className="btn btn-ghost" disabled={busy} onClick={handleCancel}>
            Desistir e liberar o assento
          </button>
        </form>
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
