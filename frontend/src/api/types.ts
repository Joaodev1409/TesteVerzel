export type Role = 'ORGANIZER' | 'CUSTOMER' | 'GATE'
export type SeatStatus = 'AVAILABLE' | 'HELD' | 'SOLD'
export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'

export interface AuthResponse {
  token: string
  email: string
  role: Role
}

export interface EventResponse {
  id: string
  titulo: string
  sinopse: string | null
  data: string
  local: string
  capacidade: number
  precoBase: number
  tmdbId: number | null
}

export interface SeatResponse {
  id: string
  fileira: string
  numero: number
  status: SeatStatus
}

export interface ReservationResponse {
  id: string
  eventId: string
  seatId: string
  status: ReservationStatus
  expiresAt: string
}

export interface TicketResponse {
  id: string
  reservationId: string
  eventId: string
  seatId: string
  qrCode: string
}

export interface MyTicketResponse {
  id: string
  eventId: string
  eventTitulo: string
  eventData: string
  eventLocal: string
  fileira: string
  numero: number
  qrCode: string
  usedAt: string | null
}

export interface ValidateTicketResponse {
  ticketId: string
  eventId: string
  eventTitulo: string
  fileira: string
  numero: number
  usedAt: string
}

export interface MovieSummary {
  id: number
  titulo: string
  sinopse: string | null
  dataLancamento: string | null
  posterPath: string | null
}

export interface SeatRowInput {
  fileira: string
  quantidade: number
}

export interface CreateEventInput {
  titulo: string
  sinopse: string | null
  data: string
  local: string
  precoBase: number
  tmdbId: number | null
  fileiras: SeatRowInput[]
}
