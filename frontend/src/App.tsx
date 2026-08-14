import { Suspense, lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { Protected } from './components/Protected'
import { EventsPage } from './pages/EventsPage'
import { EventDetailPage } from './pages/EventDetailPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { MyTicketsPage } from './pages/MyTicketsPage'
import { OrganizerPage } from './pages/OrganizerPage'
import { SharedTicketPage } from './pages/SharedTicketPage'

// Loaded on demand: html5-qrcode is heavy and only the gate screen needs it
const GatePage = lazy(() => import('./pages/GatePage').then((m) => ({ default: m.GatePage })))

export function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<EventsPage />} />
          <Route path="/eventos/:eventId" element={<EventDetailPage />} />
          <Route path="/ingresso/:code" element={<SharedTicketPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />
          <Route
            path="/meus-ingressos"
            element={
              <Protected role="CUSTOMER">
                <MyTicketsPage />
              </Protected>
            }
          />
          <Route
            path="/organizador"
            element={
              <Protected role="ORGANIZER">
                <OrganizerPage />
              </Protected>
            }
          />
          <Route
            path="/portaria"
            element={
              <Protected role="GATE">
                <Suspense fallback={<p className="muted">Carregando portaria...</p>}>
                  <GatePage />
                </Suspense>
              </Protected>
            }
          />
        </Routes>
      </main>
    </>
  )
}
