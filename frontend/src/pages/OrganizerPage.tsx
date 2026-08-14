import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { CreateEventInput, EventResponse, MovieSummary, SeatRowInput } from '../api/types'
import { formatDateTime, formatPrice } from '../format'

/** `datetime-local` só aceita 'YYYY-MM-DDTHH:mm' no fuso local, sem offset. */
function toDateTimeLocal(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const EMPTY_FORM = {
  titulo: '',
  sinopse: '',
  data: '',
  local: '',
  precoBase: '',
  tmdbId: null as number | null,
}

export function OrganizerPage() {
  const { auth } = useAuth()
  const [myEvents, setMyEvents] = useState<EventResponse[] | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [fileiras, setFileiras] = useState<SeatRowInput[]>([{ fileira: 'A', quantidade: 10 }])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function resetForm() {
    setForm(EMPTY_FORM)
    setFileiras([{ fileira: 'A', quantidade: 10 }])
    setEditingId(null)
  }

  function startEdit(event: EventResponse) {
    setForm({
      titulo: event.titulo,
      sinopse: event.sinopse ?? '',
      data: toDateTimeLocal(event.data),
      local: event.local,
      precoBase: String(event.precoBase),
      tmdbId: event.tmdbId,
    })
    setEditingId(event.id)
    setError(null)
    setSuccess(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const [tmdbQuery, setTmdbQuery] = useState('')
  const [tmdbResults, setTmdbResults] = useState<MovieSummary[] | null>(null)
  const [tmdbError, setTmdbError] = useState<string | null>(null)

  const loadMyEvents = useCallback(() => {
    if (!auth) return
    api<EventResponse[]>('/api/me/events', { token: auth.token })
      .then(setMyEvents)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Erro ao carregar eventos.'))
  }, [auth])

  useEffect(loadMyEvents, [loadMyEvents])

  async function searchTmdb() {
    if (!auth || !tmdbQuery.trim()) return
    setTmdbError(null)
    setTmdbResults(null)
    try {
      const movies = await api<MovieSummary[]>(
        `/api/catalog/movies?query=${encodeURIComponent(tmdbQuery.trim())}`,
        { token: auth.token },
      )
      setTmdbResults(movies)
    } catch (err) {
      if (err instanceof ApiError && err.status === 503) {
        setTmdbError('Integração TMDb não configurada no servidor (TMDB_API_KEY). Preencha manualmente.')
      } else {
        setTmdbError(err instanceof ApiError ? err.message : 'Erro na busca TMDb.')
      }
    }
  }

  function pickMovie(movie: MovieSummary) {
    setForm((f) => ({
      ...f,
      titulo: movie.titulo,
      sinopse: movie.sinopse ?? '',
      tmdbId: movie.id,
    }))
    setTmdbResults(null)
    setTmdbQuery('')
  }

  function updateRow(index: number, patch: Partial<SeatRowInput>) {
    setFileiras((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function addRow() {
    const nextLetter = String.fromCharCode(65 + fileiras.length) // A, B, C...
    setFileiras((rows) => [...rows, { fileira: nextLetter, quantidade: 10 }])
  }

  function removeRow(index: number) {
    setFileiras((rows) => rows.filter((_, i) => i !== index))
  }

  const totalSeats = fileiras.reduce((sum, row) => sum + (row.quantidade || 0), 0)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!auth) return
    setError(null)
    setSuccess(null)
    setBusy(true)
    try {
      const base = {
        titulo: form.titulo,
        sinopse: form.sinopse || null,
        data: new Date(form.data).toISOString(),
        local: form.local,
        precoBase: Number(form.precoBase),
        tmdbId: form.tmdbId,
      }

      if (editingId) {
        const updated = await api<EventResponse>(`/api/events/${editingId}`, {
          method: 'PUT',
          body: base,
          token: auth.token,
        })
        setSuccess(`Evento "${updated.titulo}" atualizado.`)
      } else {
        const body: CreateEventInput = { ...base, fileiras }
        const created = await api<EventResponse>('/api/events', {
          method: 'POST',
          body,
          token: auth.token,
        })
        setSuccess(`Evento "${created.titulo}" criado com ${created.capacidade} assentos.`)
      }

      resetForm()
      loadMyEvents()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erro ao salvar evento.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div>
        <span className="eyebrow">Bastidores</span>
        <h1>Painel do organizador</h1>
      </div>

      <section className="card">
        <h2>{editingId ? 'Editar evento' : 'Novo evento'}</h2>

        <div className="tmdb-search">
          <label>
            Buscar filme no TMDb (opcional)
            <div className="tmdb-search-row">
              <input
                type="text"
                placeholder="Ex.: Interestelar"
                value={tmdbQuery}
                onChange={(e) => setTmdbQuery(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    searchTmdb()
                  }
                }}
              />
              <button type="button" className="btn btn-ghost" onClick={searchTmdb}>
                Buscar
              </button>
            </div>
          </label>
          <p className="helper">Preenche título e sinopse automaticamente. Pode ignorar e digitar na mão.</p>
          {tmdbError && <div className="alert alert-warn">{tmdbError}</div>}
          {tmdbResults && (
            <ul className="tmdb-results">
              {tmdbResults.length === 0 && <li className="muted">Nada encontrado.</li>}
              {tmdbResults.slice(0, 6).map((movie) => (
                <li key={movie.id}>
                  <button type="button" onClick={() => pickMovie(movie)}>
                    <strong>{movie.titulo}</strong>
                    {movie.dataLancamento && <small> ({movie.dataLancamento.slice(0, 4)})</small>}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <form className="form" onSubmit={handleSubmit}>
          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <label>
            Título
            <input
              type="text"
              value={form.titulo}
              onChange={(e) => setForm((f) => ({ ...f, titulo: e.target.value }))}
              required
            />
          </label>
          <label>
            Sinopse
            <textarea
              rows={3}
              value={form.sinopse}
              onChange={(e) => setForm((f) => ({ ...f, sinopse: e.target.value }))}
            />
          </label>
          <div className="form-row">
            <label>
              Data e hora
              <input
                type="datetime-local"
                value={form.data}
                onChange={(e) => setForm((f) => ({ ...f, data: e.target.value }))}
                required
              />
            </label>
            <label>
              Local
              <input
                type="text"
                value={form.local}
                onChange={(e) => setForm((f) => ({ ...f, local: e.target.value }))}
                required
              />
            </label>
            <label>
              Preço base (R$)
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.precoBase}
                onChange={(e) => setForm((f) => ({ ...f, precoBase: e.target.value }))}
                required
              />
            </label>
          </div>

          {editingId ? (
            <p className="helper">
              O mapa de assentos não muda na edição: alterar fileiras depois que existem reservas
              mudaria o lugar de quem já comprou.
            </p>
          ) : (
          <fieldset className="rows-editor">
            <legend>Mapa de assentos ({totalSeats} assentos)</legend>
            {fileiras.map((row, index) => (
              <div key={index} className="row-editor-line">
                <label>
                  Fileira
                  <input
                    type="text"
                    maxLength={10}
                    value={row.fileira}
                    onChange={(e) => updateRow(index, { fileira: e.target.value.toUpperCase() })}
                    required
                  />
                </label>
                <label>
                  Assentos
                  <input
                    type="number"
                    min="1"
                    max="500"
                    value={row.quantidade}
                    onChange={(e) => updateRow(index, { quantidade: Number(e.target.value) })}
                    required
                  />
                </label>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => removeRow(index)}
                  disabled={fileiras.length === 1}
                >
                  Remover
                </button>
              </div>
            ))}
            <button type="button" className="btn btn-ghost" onClick={addRow}>
              + Adicionar fileira
            </button>
          </fieldset>
          )}

          <div className="form-actions">
            <button className="btn btn-primary" disabled={busy}>
              {busy ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Criar evento'}
            </button>
            {editingId && (
              <button type="button" className="btn btn-ghost" onClick={resetForm} disabled={busy}>
                Cancelar edição
              </button>
            )}
          </div>
        </form>
      </section>

      <section>
        <h2>Meus eventos</h2>
        {myEvents === null && <p className="muted">Carregando...</p>}
        {myEvents !== null && myEvents.length === 0 && (
          <p className="muted">Nenhum evento criado ainda.</p>
        )}
        <div className="my-events-list">
          {myEvents?.map((event) => (
            <div key={event.id} className="my-event-row">
              <div>
                <strong>{event.titulo}</strong>
                <p className="muted">
                  {formatDateTime(event.data)} · {event.local}
                </p>
              </div>
              <div className="my-event-meta">
                <span>{event.capacidade} assentos</span>
                <span>{formatPrice(event.precoBase)}</span>
                <button className="btn btn-ghost" onClick={() => startEdit(event)}>
                  Editar
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
