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
cd backend

# 1. Sobe o PostgreSQL
docker compose up -d

# 2. Sobe a API (Flyway aplica as migrations automaticamente)
./mvnw spring-boot:run        # Linux/macOS
.\mvnw.cmd spring-boot:run    # Windows
```

Alternativa em um comando só, com a API também em container (mais lento na primeira vez, porque
compila a imagem):

```bash
docker compose --profile full up --build
```

A API sobe em `http://localhost:8080`.

### Variáveis de ambiente (opcionais em dev)

| Variável | Uso | Default |
|---|---|---|
| `JWT_SECRET` | Chave HS256 dos tokens (≥ 32 bytes) | valor de dev no `application.yaml` |
| `QR_CODE_SECRET` | Chave HMAC dos QR codes | valor de dev no `application.yaml` |
| `TMDB_API_KEY` | Chave da API do TMDb | vazio — endpoint de catálogo responde `503` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (frontend), separadas por vírgula | `http://localhost:5173` |

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
| PUT | `/api/events/{id}` | ORGANIZER (dono) | Edita título, sinopse, data, local e preço; o mapa de assentos não muda |
| GET | `/api/events` | público | Lista eventos |
| GET | `/api/events/{id}` | público | Detalhe do evento |
| GET | `/api/events/{id}/seats` | público | Mapa de assentos com status (`AVAILABLE` / `HELD` / `SOLD`) |

### Reservas e ingressos

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/reservations` | CUSTOMER | `{ "eventId", "seatId" }` → reserva `PENDING` com `expiresAt` (10 min) |
| POST | `/api/reservations/{id}/confirm` | CUSTOMER | `{ "cardNumber": "4242 4242 4242 4242" }` — o servidor simula o gateway e decide; aprovado emite o ingresso com QR, recusado → `402` e a reserva segue `PENDING` para retry |
| POST | `/api/reservations/{id}/cancel` | CUSTOMER | Desiste de uma reserva pendente e devolve o assento ao estoque na hora (`204`); reserva já confirmada → `409` |
| GET | `/api/me/tickets` | CUSTOMER | Ingressos do usuário logado, com QR code e status de uso |
| GET | `/api/tickets/shared/{qrCode}` | **público** | Abre um ingresso a partir do código do QR — é o que sustenta o compartilhamento por link |
| GET | `/api/me/events` | ORGANIZER | Eventos criados pelo organizador logado |
| POST | `/api/gate/validate` | GATE | `{ "qrCode", "expectedEventId"? }` → dados do ingresso e marca `usedAt`; com `expectedEventId`, ingresso de outro evento → `409 WRONG_EVENT` |

### Catálogo (referência TMDb)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/catalog/movies?query=...` | ORGANIZER | Busca filmes para preencher título/sinopse/`tmdbId` do evento |

### Códigos de erro

| Situação | Status |
|---|---|
| Assento já reservado/vendido | `409` |
| Reserva expirada | `410` (assento volta a `AVAILABLE`) |
| Pagamento recusado | `402` + `code: CARD_DECLINED` \| `INSUFFICIENT_FUNDS` \| `INVALID_CARD` |
| QR adulterado/forjado | `422` + `code: INVALID_QR` |
| Ingresso já utilizado (re-scan) | `409` + `code: TICKET_ALREADY_USED` |
| Ingresso de outro evento na portaria | `409` + `code: WRONG_EVENT` |
| Reserva de outro usuário / recurso inexistente | `404` |
| Papel sem permissão na rota | `403` |
| Corpo inválido | `400` com mapa de erros por campo |

Os `code` extras nos erros da portaria permitem à tela do gate distinguir os quatro resultados exigidos (válido / inválido / já utilizado / evento errado) sem parsear mensagens.

Erros seguem o formato [Problem Details (RFC 7807)](https://datatracker.ietf.org/doc/html/rfc7807).

## Decisões técnicas

**Concorrência na reserva** — lock pessimista (`@Lock(PESSIMISTIC_WRITE)`) na busca do assento, dentro de transação curta. A janela de contenção é pequena (clique do usuário), e a garantia forte é mais simples de testar e defender do que lock otimista com retry. Reforços independentes: `@Version` no assento e um unique index parcial no banco (`uq_reservations_active_seat`) que impede duas reservas ativas para o mesmo assento mesmo que a aplicação falhe.

**Expiração de reservas** — job `@Scheduled` varre reservas `PENDING` vencidas e libera os assentos (sem fila externa; fora do escopo). O confirm também expira lazily: reserva vencida que ainda não foi varrida é expirada na hora, nunca confirmada. Confirmação e expiração usam lock pessimista na reserva para não se atropelarem (lost update).

**Pagamento simulado, decidido no servidor** — o cliente envia o número do cartão, nunca o resultado. Um `PaymentGatewaySimulator` valida o cartão (comprimento + algoritmo de Luhn) e decide o desfecho a partir de cartões de teste, no estilo dos sandboxes de gateway reais: `4242 4242 4242 4242` aprova, `4000 0000 0000 0002` recusa, `4000 0000 0000 9995` retorna saldo insuficiente. Um booleano `paymentSuccessful` vindo do cliente seria mais simples, mas deixaria qualquer cliente autenticado emitir ingressos de graça — a decisão precisa nascer no servidor. Numa integração real, esta classe é substituída pelo gateway (ou pelo webhook de confirmação) sem mexer no resto do fluxo.

**QR code não forjável** — conteúdo do QR é `base64url(ticketId:eventId:seatId:hmac)`, com HMAC-SHA256 sobre os três ids usando secret da aplicação. A portaria recalcula e compara com `MessageDigest.isEqual` (constante no tempo, evita timing attack). Validação usa lock pessimista no ticket: dois scans simultâneos do mesmo QR não passam ambos. `usedAt` preenchido = ingresso queimado.

**Compartilhamento por link** — o link é `/ingresso/<código do QR>`, e o endpoint que o alimenta é público por definição: quem recebe não tem conta. A assinatura HMAC é verificada **antes** de qualquer consulta ao banco, então um link adulterado nunca chega a tocar em dados. Vale dizer com clareza qual é o trade-off: **o link é o ingresso** — quem tiver a URL consegue entrar, exatamente como acontece ao encaminhar um PDF de ingresso por e-mail. O que limita o estrago é a validação de uso único na portaria. Num sistema real, o passo seguinte seria transferência nominal (o link vincula o ingresso a outra conta em vez de valer por si).

**Cancelamento** — só reservas `PENDING` podem ser canceladas; o assento volta a `AVAILABLE` imediatamente, sem esperar o job de expiração. Reserva confirmada tem ingresso emitido, e desfazer isso exigiria estorno — fora do escopo, então responde `409`.

**Schema** — Flyway é o único dono do schema (`ddl-auto: validate`); migrations versionadas em `src/main/resources/db/migration`. A `V2` semeia os dados de demonstração (usuários, eventos e assentos, sem reservas) para que a aplicação suba pronta para avaliação.

**Autenticação** — JWT HS256 emitido/validado pelo próprio Spring Security (OAuth2 Resource Server), sem biblioteca externa. Claim `role` vira authority; autorização por rota e `@PreAuthorize` por método.

**IDs** — UUIDs em todas as entidades: ids sequenciais seriam adivinháveis em recursos expostos (tickets, reservas).

## Limitações conhecidas

Declaradas de propósito, para que ninguém descubra sozinho:

- **A integração com o TMDb nunca foi executada contra a API real**, por não haver chave à mão.
  O mapeamento da resposta está coberto por teste (`TmdbClientTest`) usando o JSON real de
  `/search/movie`, então erro de campo está descartado; o que permanece sem prova é o caminho de
  rede e autenticação. Sem `TMDB_API_KEY` — o estado padrão deste repositório — o endpoint responde
  `503` com mensagem explicativa. O restante do produto não depende dela: o organizador preenche
  título e sinopse manualmente e o `tmdbId` é opcional.
- **O organizador edita os dados do evento, mas não exclui um evento existente.** O mapa de
  assentos também não muda na edição, de propósito: alterar fileiras depois de vendas mudaria o
  lugar de quem já comprou.
- **O mapa de assentos se atualiza por consulta periódica (a cada 5s), não por push.** É suficiente
  para o uso real e mantém o servidor simples; a evolução natural seria SSE ou WebSocket para
  propagar o estado sem polling.
- **Só reservas pendentes podem ser canceladas.** Cancelar uma reserva já confirmada exigiria
  estorno e invalidação do ingresso — fora do escopo combinado.
- **O link de compartilhamento é o próprio ingresso**: quem tiver a URL consegue entrar. É o mesmo
  comportamento de encaminhar um PDF por e-mail, e o que limita o estrago é a validação de uso
  único na portaria.
- **Os segredos têm valores padrão de desenvolvimento** no `application.yaml` para facilitar a
  avaliação local. Em produção, `JWT_SECRET` e `QR_CODE_SECRET` precisam vir do ambiente.

## Testes

```bash
./mvnw test
```

- `ReservationServiceConcurrencyTest` sobe um PostgreSQL efêmero via Testcontainers (**requer Docker rodando**), aplica as migrations reais e dispara **10 threads simultâneas** (sincronizadas por `CyclicBarrier`) tentando reservar o mesmo assento — exatamente 1 deve conseguir e 9 devem falhar com `SeatNotAvailableException`. É a prova concreta de que o lock pessimista funciona, não só teoria.
- `TmdbClientTest` valida o contrato com a API externa sem rede nem chave, usando o JSON real de `/search/movie`: mapeamento dos campos, query e chave enviadas, erro claro quando a chave falta e propagação de falha do upstream.

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
5. `POST /api/reservations/{id}/confirm` com `{ "cardNumber": "4242 4242 4242 4242" }` — receba o `qrCode`. Para ver a recusa, use `4000 0000 0000 0002`.
6. Com o token GATE, `POST /api/gate/validate` com o `qrCode` — retorna evento/assento e marca uso.
7. Repita o passo 6 → `409`. Altere um caractere do `qrCode` → `422`.
