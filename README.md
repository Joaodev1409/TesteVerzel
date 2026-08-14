# Plataforma de Eventos e Ingressos

API REST em Spring Boot para criação de eventos com mapa de assentos nomeados (estilo cinema/teatro), reserva com expiração automática, pagamento simulado, emissão de ingresso com QR code assinado e validação na portaria.

## Stack

- Java 17 · Spring Boot 4.1.0 · Maven
- Spring Web MVC, Spring Data JPA, Spring Security (JWT via OAuth2 Resource Server), Validation
- PostgreSQL 16 (Docker Compose) · Flyway para migrations
- Testcontainers para testes de integração
- Catálogo de referência: [TMDb API](https://developer.themoviedb.org/) (opcional)

## Atores e fluxo

| Papel | O que faz |
|---|---|
| `ORGANIZER` | Cria e gerencia eventos, consulta o catálogo TMDb |
| `CUSTOMER` | Navega, reserva assento, paga (simulado), recebe ingresso |
| `GATE` | Valida ingressos na entrada (escaneia o QR) |

Fluxo principal: organizador cria evento com mapa de assentos → cliente escolhe assento e reserva (hold de 10 min) → confirma pagamento simulado → recebe ingresso com QR code assinado → portaria valida o QR e marca o ingresso como usado.

## Como rodar

Pré-requisitos: JDK 17+ e Docker Desktop.

```bash
cd eventos-api

# 1. Sobe o PostgreSQL
docker compose up -d

# 2. Sobe a API (Flyway aplica as migrations automaticamente)
./mvnw spring-boot:run        # Linux/macOS
.\mvnw.cmd spring-boot:run    # Windows
```

A API sobe em `http://localhost:8080`.

### Variáveis de ambiente (opcionais em dev)

| Variável | Uso | Default |
|---|---|---|
| `JWT_SECRET` | Chave HS256 dos tokens (≥ 32 bytes) | valor de dev no `application.yaml` |
| `QR_CODE_SECRET` | Chave HMAC dos QR codes | valor de dev no `application.yaml` |
| `TMDB_API_KEY` | Chave da API do TMDb | vazio — endpoint de catálogo responde `503` |

> Os defaults existem só para facilitar a avaliação local. Em produção, os dois secrets viriam obrigatoriamente de variáveis de ambiente/secret manager.

## Endpoints

### Autenticação (públicos)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | `{ "email", "senha", "role": "ORGANIZER"\|"CUSTOMER"\|"GATE" }` → token JWT |
| POST | `/api/auth/login` | `{ "email", "senha" }` → token JWT |

Envie o token nas demais rotas: `Authorization: Bearer <token>`.

### Eventos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/events` | ORGANIZER | Cria evento; assentos gerados a partir de `fileiras: [{ "fileira": "A", "quantidade": 5 }]` |
| GET | `/api/events` | público | Lista eventos |
| GET | `/api/events/{id}` | público | Detalhe do evento |
| GET | `/api/events/{id}/seats` | público | Mapa de assentos com status (`AVAILABLE` / `HELD` / `SOLD`) |

### Reservas e ingressos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/reservations` | CUSTOMER | `{ "eventId", "seatId" }` → reserva `PENDING` com `expiresAt` (10 min) |
| POST | `/api/reservations/{id}/confirm` | CUSTOMER | `{ "paymentSuccessful": true\|false }` — `true` emite o ingresso com QR; `false` → `402` e a reserva segue `PENDING` para retry |
| POST | `/api/gate/validate` | GATE | `{ "qrCode" }` → dados do ingresso e marca `usedAt` |

### Catálogo (referência TMDb)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/catalog/movies?query=...` | ORGANIZER | Busca filmes para preencher título/sinopse/`tmdbId` do evento |

### Códigos de erro

| Situação | Status |
|---|---|
| Assento já reservado/vendido | `409` |
| Reserva expirada | `410` (assento volta a `AVAILABLE`) |
| Pagamento recusado | `402` |
| QR adulterado/forjado | `422` |
| Ingresso já utilizado (re-scan) | `409` |
| Reserva de outro usuário / recurso inexistente | `404` |
| Papel sem permissão na rota | `403` |
| Corpo inválido | `400` com mapa de erros por campo |

Erros seguem o formato [Problem Details (RFC 7807)](https://datatracker.ietf.org/doc/html/rfc7807).

## Decisões técnicas

**Concorrência na reserva** — lock pessimista (`@Lock(PESSIMISTIC_WRITE)`) na busca do assento, dentro de transação curta. A janela de contenção é pequena (clique do usuário), e a garantia forte é mais simples de testar e defender do que lock otimista com retry. Reforços independentes: `@Version` no assento e um unique index parcial no banco (`uq_reservations_active_seat`) que impede duas reservas ativas para o mesmo assento mesmo que a aplicação falhe.

**Expiração de reservas** — job `@Scheduled` varre reservas `PENDING` vencidas e libera os assentos (sem fila externa; fora do escopo). O confirm também expira lazily: reserva vencida que ainda não foi varrida é expirada na hora, nunca confirmada. Confirmação e expiração usam lock pessimista na reserva para não se atropelarem (lost update).

**QR code não forjável** — conteúdo do QR é `base64url(ticketId:eventId:seatId:hmac)`, com HMAC-SHA256 sobre os três ids usando secret da aplicação. A portaria recalcula e compara com `MessageDigest.isEqual` (constante no tempo, evita timing attack). Validação usa lock pessimista no ticket: dois scans simultâneos do mesmo QR não passam ambos. `usedAt` preenchido = ingresso queimado.

**Schema** — Flyway é o único dono do schema (`ddl-auto: validate`); migrations versionadas em `src/main/resources/db/migration`.

**Autenticação** — JWT HS256 emitido/validado pelo próprio Spring Security (OAuth2 Resource Server), sem biblioteca externa. Claim `role` vira authority; autorização por rota e `@PreAuthorize` por método.

**IDs** — UUIDs em todas as entidades: ids sequenciais seriam adivinháveis em recursos expostos (tickets, reservas).

## Testes

```bash
./mvnw test
```

Requer Docker rodando: `ReservationServiceConcurrencyTest` sobe um PostgreSQL efêmero via Testcontainers, aplica as migrations reais e dispara **10 threads simultâneas** (sincronizadas por `CyclicBarrier`) tentando reservar o mesmo assento — exatamente 1 deve conseguir e 9 devem falhar com `SeatNotAvailableException`. É a prova concreta de que o lock pessimista funciona, não só teoria.

## Estrutura

```
src/main/java/com/testeverzel/eventos_api/
├── client/       # TmdbClient (RestClient)
├── config/       # SecurityConfig, SchedulingConfig
├── controller/   # Auth, Event, Reservation, Gate, Catalog
├── domain/       # Entidades JPA + enums
├── dto/          # Records de request/response com Bean Validation
├── exception/    # Exceptions de negócio + GlobalExceptionHandler (RFC 7807)
├── repository/   # Spring Data JPA (locks pessimistas nas queries críticas)
├── security/     # JwtService, QrCodeSigner
└── service/      # AuthService, EventService, ReservationService, TicketService
```

## Roteiro de teste manual (Postman/curl)

1. `POST /api/auth/register` três vezes (ORGANIZER, CUSTOMER, GATE) — guarde os tokens.
2. Com o token ORGANIZER, `POST /api/events` com título, data futura, `precoBase` e `fileiras`.
3. Sem token, `GET /api/events/{id}/seats` — copie o id de um assento `AVAILABLE`.
4. Com o token CUSTOMER, `POST /api/reservations` com `eventId` + `seatId`.
5. `POST /api/reservations/{id}/confirm` com `{ "paymentSuccessful": true }` — receba o `qrCode`.
6. Com o token GATE, `POST /api/gate/validate` com o `qrCode` — retorna evento/assento e marca uso.
7. Repita o passo 6 → `409`. Altere um caractere do `qrCode` → `422`.
