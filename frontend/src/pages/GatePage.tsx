import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { EventResponse, ValidateTicketResponse } from '../api/types'
import { formatDateTime } from '../format'

type GateResult =
  | { kind: 'valid'; ticket: ValidateTicketResponse }
  | { kind: 'invalid' }
  | { kind: 'used'; detail: string }
  | { kind: 'wrong-event' }
  | { kind: 'error'; detail: string }

const SCANNER_REGION_ID = 'qr-scanner-region'

export function GatePage() {
  const { auth } = useAuth()
  const [events, setEvents] = useState<EventResponse[]>([])
  const [eventId, setEventId] = useState('')
  const [manualCode, setManualCode] = useState('')
  const [result, setResult] = useState<GateResult | null>(null)
  const [scanning, setScanning] = useState(false)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const scannerRef = useRef<Html5Qrcode | null>(null)
  const validatingRef = useRef(false)

  useEffect(() => {
    api<EventResponse[]>('/api/events')
      .then(setEvents)
      .catch(() => setEvents([]))
  }, [])

  // stop camera when leaving the page
  useEffect(() => {
    return () => {
      const scanner = scannerRef.current
      if (scanner && scanner.isScanning) {
        scanner.stop().catch(() => undefined)
      }
    }
  }, [])

  async function validate(qrCode: string) {
    if (!auth || validatingRef.current) return
    validatingRef.current = true
    setBusy(true)
    setResult(null)
    try {
      const ticket = await api<ValidateTicketResponse>('/api/gate/validate', {
        method: 'POST',
        body: { qrCode: qrCode.trim(), expectedEventId: eventId || null },
        token: auth.token,
      })
      setResult({ kind: 'valid', ticket })
    } catch (err) {
      if (err instanceof ApiError && err.code === 'TICKET_ALREADY_USED') {
        setResult({ kind: 'used', detail: err.message })
      } else if (err instanceof ApiError && err.code === 'WRONG_EVENT') {
        setResult({ kind: 'wrong-event' })
      } else if (err instanceof ApiError && err.code === 'INVALID_QR') {
        setResult({ kind: 'invalid' })
      } else {
        setResult({
          kind: 'error',
          detail: err instanceof ApiError ? err.message : 'Erro inesperado ao validar.',
        })
      }
    } finally {
      setBusy(false)
      validatingRef.current = false
    }
  }

  async function startCamera() {
    setCameraError(null)
    setResult(null)
    try {
      const scanner = new Html5Qrcode(SCANNER_REGION_ID)
      scannerRef.current = scanner
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 220, height: 220 } },
        (decodedText) => {
          stopCamera()
          validate(decodedText)
        },
        undefined,
      )
      setScanning(true)
    } catch {
      setCameraError(
        'Não foi possível acessar a câmera. Verifique a permissão do navegador ou use a digitação manual.',
      )
      setScanning(false)
    }
  }

  async function stopCamera() {
    const scanner = scannerRef.current
    if (scanner && scanner.isScanning) {
      await scanner.stop().catch(() => undefined)
    }
    setScanning(false)
  }

  function handleManualSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (manualCode.trim()) validate(manualCode)
  }

  return (
    <div className="page">
      <div>
        <span className="eyebrow">Entrada</span>
        <h1>Portaria</h1>
      </div>

      <div className="card gate-setup">
        <label>
          Evento desta portaria
          <select value={eventId} onChange={(e) => setEventId(e.target.value)}>
            <option value="">(qualquer evento — não recomendado)</option>
            {events.map((event) => (
              <option key={event.id} value={event.id}>
                {event.titulo} — {formatDateTime(event.data)}
              </option>
            ))}
          </select>
        </label>
        {!eventId && (
          <p className="muted">
            Selecione o evento para o resultado "evento errado" funcionar corretamente.
          </p>
        )}
      </div>

      <div className="gate-columns">
        <section className="card">
          <h2>Escanear pela câmera</h2>
          <div id={SCANNER_REGION_ID} className="qr-scanner-region" />
          {cameraError && <div className="alert alert-warn">{cameraError}</div>}
          {scanning ? (
            <button className="btn btn-danger" onClick={stopCamera}>
              Parar câmera
            </button>
          ) : (
            <button className="btn btn-primary" onClick={startCamera}>
              Iniciar câmera
            </button>
          )}
        </section>

        <section className="card">
          <h2>Digitação manual</h2>
          <form className="form" onSubmit={handleManualSubmit}>
            <label>
              Código do ingresso
              <textarea
                rows={4}
                placeholder="Cole ou digite o código do QR"
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
              />
            </label>
            <button className="btn btn-primary" disabled={busy || !manualCode.trim()}>
              {busy ? 'Validando...' : 'Validar'}
            </button>
          </form>
        </section>
      </div>

      {result && (
        <div className={`gate-result gate-result-${result.kind}`} role="status">
          {result.kind === 'valid' && (
            <>
              <span className="gate-result-title">✅ VÁLIDO</span>
              <p>
                <strong>{result.ticket.eventTitulo}</strong> · Assento{' '}
                <strong>
                  {result.ticket.fileira}
                  {result.ticket.numero}
                </strong>
              </p>
              <p className="muted">Entrada registrada em {formatDateTime(result.ticket.usedAt)}</p>
            </>
          )}
          {result.kind === 'invalid' && (
            <>
              <span className="gate-result-title">❌ INVÁLIDO</span>
              <p>QR code inválido ou adulterado. Não permita a entrada.</p>
            </>
          )}
          {result.kind === 'used' && (
            <>
              <span className="gate-result-title">⚠️ JÁ UTILIZADO</span>
              <p>{result.detail}</p>
            </>
          )}
          {result.kind === 'wrong-event' && (
            <>
              <span className="gate-result-title">🚫 EVENTO ERRADO</span>
              <p>Este ingresso pertence a outro evento.</p>
            </>
          )}
          {result.kind === 'error' && (
            <>
              <span className="gate-result-title">❌ ERRO</span>
              <p>{result.detail}</p>
            </>
          )}
          <button
            className="btn btn-ghost"
            onClick={() => {
              setResult(null)
              setManualCode('')
            }}
          >
            Validar próximo
          </button>
        </div>
      )}
    </div>
  )
}
