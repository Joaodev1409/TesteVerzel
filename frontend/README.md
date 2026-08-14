# Frontend — Eventos e Ingressos

SPA em React 19 + TypeScript + Vite. Consome a API do [`backend/`](../backend/).

## Rodando

```bash
npm install
npm run dev     # http://localhost:5173 (espera a API em http://localhost:8080)
```

Para apontar para outra API, crie um `.env` a partir do `.env.example` com `VITE_API_URL`.

## Páginas

| Rota | Acesso | Descrição |
|---|---|---|
| `/` | pública | Lista e busca de eventos (título/local) |
| `/eventos/:id` | pública (reserva exige login) | Mapa de assentos, reserva e pagamento simulado |
| `/login` · `/registro` | públicas | Autenticação; registro escolhe o papel |
| `/meus-ingressos` | CUSTOMER | Ingressos com QR code, código manual e botão de compartilhar |
| `/ingresso/:code` | pública | Ingresso aberto por link compartilhado, com QR e status de uso |
| `/organizador` | ORGANIZER | Criação e edição de eventos (fileiras dinâmicas, busca TMDb) e lista dos próprios eventos |
| `/portaria` | GATE | Validação por câmera (`html5-qrcode`) ou digitação manual, com evento selecionado |

## Decisões

- **Sem framework de SSR**: a aplicação é autenticada e dinâmica de ponta a ponta; uma SPA cobre os requisitos com menos complexidade.
- **Resultado da portaria por código de erro da API** (`INVALID_QR`, `TICKET_ALREADY_USED`, `WRONG_EVENT`): a tela distingue os quatro estados exigidos sem depender de texto de mensagem.
- **Checkout envia o cartão, não o resultado**: o formulário de pagamento submete o número do cartão e o backend decide. Botões de preenchimento rápido cobrem os cartões de teste — aprovado (`4242…`), recusado (`…0002`) e sem saldo (`…9995`) — e o motivo da recusa vem do `code` da resposta `402`.
- **`html5-qrcode` carregada sob demanda** (lazy route): a lib da câmera pesa ~370KB e só a portaria usa.
- **Estado global mínimo**: contexto de auth com persistência em `localStorage`; o resto é estado local por página.
- **Mapa de assentos por consulta periódica** (5s enquanto o usuário escolhe): se o lugar selecionado for ocupado por outra pessoa, a seleção é limpa com aviso, em vez de deixar o usuário clicar em vão. A corrida de verdade é resolvida no banco; isto é só para a tela não mentir.

## Build de produção

```bash
npm run build   # gera dist/
```

Deploy na Vercel: *Root Directory* = `frontend`, build padrão do Vite, env `VITE_API_URL` apontando para o backend público (que precisa listar a URL do front em `CORS_ALLOWED_ORIGINS`).
