# Plataforma de Eventos e Ingressos

Solução completa para o desafio técnico: criação de eventos com mapa de assentos nomeados (estilo cinema/teatro), reserva com expiração automática, pagamento simulado, ingresso com QR code assinado e validação na portaria com leitura por câmera.

| Módulo | Stack | Docs |
|---|---|---|
| [`backend/`](backend/) | Java 17 · Spring Boot 4.1 · PostgreSQL · Flyway · JWT | [README do backend](backend/README.md) |
| [`frontend/`](frontend/) | React 19 · TypeScript · Vite | [README do frontend](frontend/README.md) |

## Rodando localmente

Pré-requisitos: JDK 17+, Node 20+, Docker Desktop.

```bash
# Terminal 1 — banco + API (porta 8080)
cd backend
docker compose up -d
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# Terminal 2 — front (porta 5173)
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`, crie uma conta de cada papel (Organizador, Cliente, Portaria) e siga o fluxo: organizador cria o evento → cliente reserva o assento e paga (simulado) → o ingresso com QR aparece em "Meus ingressos" → a portaria valida pela câmera ou digitação manual.

## Papéis e funcionalidades

- **Organizador**: cria eventos definindo fileiras e assentos, busca dados de filmes no TMDb (opcional), acompanha seus eventos.
- **Cliente**: navega e busca eventos, escolhe o assento no mapa, reserva (hold de 10 min), paga com simulação de aprovação ou recusa, vê seus ingressos com QR code.
- **Portaria**: seleciona o evento em operação e valida ingressos com retorno claro — **válido**, **inválido**, **já utilizado** ou **evento errado** — via câmera ou código manual.

## Deploy

- **Frontend (Vercel)**: aponte o projeto para o repositório com *Root Directory* = `frontend`, e defina `VITE_API_URL` com a URL pública do backend.
- **Backend**: qualquer host de containers/Java (Render, Railway, Fly.io) + PostgreSQL gerenciado. Defina `JWT_SECRET`, `QR_CODE_SECRET`, `CORS_ALLOWED_ORIGINS` (URL do front na Vercel) e, opcionalmente, `TMDB_API_KEY`.

## Destaques técnicos

- **Concorrência real testada**: lock pessimista na reserva, provado por teste de integração com 10 threads simultâneas disputando o mesmo assento (Testcontainers + PostgreSQL real).
- **QR não forjável**: HMAC-SHA256 sobre `ticketId:eventId:seatId`, verificação em tempo constante, lock contra scan duplo simultâneo.
- **Flyway como único dono do schema** (`ddl-auto: validate`), com unique index parcial garantindo no banco uma única reserva ativa por assento.

Detalhes das decisões no [README do backend](backend/README.md).
