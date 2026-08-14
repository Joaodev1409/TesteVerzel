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

Abra `http://localhost:5173`. O banco já sobe **com dados de demonstração**, então dá para percorrer o fluxo inteiro sem cadastrar nada.

### Contas de teste (senha `senha123` para todas)

| E-mail | Papel | Serve para |
|---|---|---|
| `organizador@demo.com` | ORGANIZER | Criar e acompanhar eventos |
| `cliente1@demo.com` | CUSTOMER | Reservar, pagar e receber ingresso |
| `cliente2@demo.com` | CUSTOMER | Disputar o mesmo assento que o cliente 1 |
| `portaria@demo.com` | GATE | Validar ingressos na entrada |

Já existem três eventos publicados com assentos livres (30, 24 e 24 lugares). O fluxo completo:
organizador cria o evento → cliente reserva o assento e paga (simulado) → o ingresso com QR aparece
em "Meus ingressos", com opção de **compartilhar por link** → a portaria valida pela câmera ou
digitação manual.

> Os dados de demonstração vêm da migration `V2__seed_demo_data.sql`. Para subir sem eles, basta
> apagar esse arquivo antes da primeira execução.

## Papéis e funcionalidades

- **Organizador**: cria eventos definindo fileiras e assentos, edita os dados de um evento existente, busca dados de filmes no TMDb (opcional), acompanha seus eventos.
- **Cliente**: navega e busca eventos, escolhe o assento no mapa, reserva (hold de 10 min), paga informando um cartão de teste (o servidor simula o gateway e decide aprovar ou recusar), pode desistir antes de pagar devolvendo o lugar ao estoque, vê seus ingressos com QR code e compartilha um ingresso por link.
- **Portaria**: seleciona o evento em operação e valida ingressos com retorno claro — **válido**, **inválido**, **já utilizado** ou **evento errado** — via câmera ou código manual.

## Deploy

O repositório já tem o que cada plataforma precisa: [`backend/Dockerfile`](backend/Dockerfile)
(build em duas etapas, imagem final só com JRE e usuário sem privilégios) e
[`frontend/vercel.json`](frontend/vercel.json) (reescrita de SPA — sem ela, abrir um link de
ingresso compartilhado direto na URL resultaria em 404).

**1. Banco** — crie um PostgreSQL gerenciado (Render, Neon, Supabase, Railway) e guarde a URL JDBC.

**2. Backend** — em qualquer host que rode container (Render, Railway, Fly.io), apontando para a
pasta `backend`. A porta vem de `PORT`, que essas plataformas injetam sozinhas. Variáveis:

| Variável | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host:5432/banco` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | credenciais do banco |
| `JWT_SECRET` | segredo com 32+ caracteres |
| `QR_CODE_SECRET` | outro segredo, diferente do anterior |
| `CORS_ALLOWED_ORIGINS` | a URL da Vercel (ex.: `https://seu-projeto.vercel.app`) |
| `TMDB_API_KEY` | opcional |

As migrations rodam sozinhas na subida, inclusive o seed de demonstração.

**3. Frontend (Vercel)** — importe o repositório com **Root Directory = `frontend`**; o Vite é
detectado automaticamente. Defina `VITE_API_URL` com a URL pública do backend, **sem barra no
final**.

**4. Feche o círculo** — depois que a Vercel gerar o domínio, volte no backend e coloque essa URL
em `CORS_ALLOWED_ORIGINS`, senão o navegador bloqueia as chamadas.

> A leitura de QR pela câmera exige HTTPS, que a Vercel fornece por padrão — em produção a portaria
> funciona pela câmera sem configuração extra.

## Destaques técnicos

- **Concorrência real testada**: lock pessimista na reserva, provado por teste de integração com 10 threads simultâneas disputando o mesmo assento (Testcontainers + PostgreSQL real).
- **QR não forjável**: HMAC-SHA256 sobre `ticketId:eventId:seatId`, verificação em tempo constante, lock contra scan duplo simultâneo.
- **Pagamento decidido no servidor**: o cliente envia dados do cartão, não o resultado — um cliente autenticado não consegue declarar o próprio pagamento como aprovado.
- **Flyway como único dono do schema** (`ddl-auto: validate`), com unique index parcial garantindo no banco uma única reserva ativa por assento.

Detalhes das decisões no [README do backend](backend/README.md).

## Limitações conhecidas

O que não está pronto ou não foi verificado está listado em
[Limitações conhecidas](backend/README.md#limitações-conhecidas) — com destaque para a integração
TMDb, implementada mas nunca executada contra a API real por falta de chave.

## Uso de IA

Como a ferramenta entrou no processo, o que foi decidido antes dela e onde precisei corrigir o
rumo: [USO-DE-IA.md](USO-DE-IA.md).
