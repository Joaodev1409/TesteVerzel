# Uso de IA neste projeto

> Seção pedida no enunciado. Escrevo aqui com franqueza como a ferramenta entrou no processo,
> o que decidi antes dela, onde ela acelerou e onde eu tive que corrigir o rumo.

## Ferramenta

**Claude Code** (Anthropic), rodando dentro do VS Code, ao longo de todo o projeto — back-end,
front-end e documentação.

## O que eu defini antes de escrever qualquer linha

O desenho da solução veio primeiro, e a ferramenta trabalhou dentro dele. Antes de gerar código
eu já tinha fechado:

- **A modelagem**: `User`, `Event`, `Seat`, `Reservation`, `Ticket`, com os estados de assento
  (`AVAILABLE`/`HELD`/`SOLD`) e de reserva (`PENDING`/`CONFIRMED`/`EXPIRED`/`CANCELLED`).
- **A estratégia de concorrência**: lock pessimista na busca do assento, dentro de transação curta.
  Escolhi isso em vez de lock otimista com retry porque a janela de contenção é o clique do
  usuário — curta — e a garantia forte é mais fácil de testar e defender.
- **A expiração das reservas**: job `@Scheduled`, sem fila externa (Redis/RabbitMQ seria peso
  desnecessário para o escopo).
- **O QR não forjável**: payload assinado com HMAC-SHA256 e comparação em tempo constante.
- **A divisão Flyway/Hibernate**: Flyway como única fonte de verdade do schema, `ddl-auto: validate`.
- **A autenticação**: JWT com claim de papel e autorização por rota/método.

Essas decisões estão justificadas no [README do backend](backend/README.md#decisões-técnicas).

## Onde a IA acelerou

- Entidades JPA, DTOs, repositories e controllers a partir da modelagem que eu já tinha.
- A migration inicial do Flyway e o seed de demonstração.
- O CSS do front e a estrutura das telas.
- Rodar a suíte, subir a aplicação e executar roteiros de verificação ponta a ponta.

## Onde eu tive que corrigir o rumo

**O caso mais importante foi o pagamento.** A primeira versão do endpoint de confirmação recebia
`{ "paymentSuccessful": true }` — ou seja, o próprio cliente declarava que o pagamento tinha dado
certo. Funcionava para demonstrar aprovação e recusa, mas qualquer pessoa com um token de cliente
emitiria ingresso de graça com um `curl`. Eu questionei isso ("um customer pode aprovar
pagamento?") e refizemos: hoje o cliente envia **dados do cartão** e um `PaymentGatewaySimulator`
decide no servidor, com validação de Luhn e cartões de teste no estilo dos sandboxes reais.
A decisão nunca sai do backend.

Ao refazer esse trecho apareceu um **bug de verdade**: a expiração preguiçosa dentro do
`confirmReservation` liberava o assento e em seguida lançava uma exceção — que fazia o Spring dar
rollback justamente na liberação recém-feita. O código parecia certo e não fazia nada (o job
agendado limpava depois, mascarando o efeito). Corrigido com `@Transactional(noRollbackFor = ...)`.

**Outras intervenções minhas:**

- Pedi uma revisão do projeto contra o enunciado e ela apontou requisitos que faltavam
  (compartilhamento por link, dados semeados, esta própria seção) — implementados depois. Numa
  segunda passagem apareceu algo que a primeira tinha deixado escapar: o enunciado exige declarar
  no README o que não está funcionando, e a integração TMDb — implementada, mas nunca executada
  contra a API real — não estava declarada em lugar nenhum. Virou a seção
  [Limitações conhecidas](backend/README.md#limitações-conhecidas), e o risco de mapeamento foi
  fechado com um teste que usa a resposta real do TMDb sem precisar de chave.
- Rejeitei a proposta de mandar todo visitante deslogado para o login: o enunciado pede navegação
  pública pelos eventos, então o catálogo continua aberto e o convite ao login virou uma faixa.
- A identidade visual foi refeita depois que eu apontei que o resultado inicial tinha "cara de
  projeto gerado" — fundo escuro com roxo padrão. Extraímos a linguagem visual da própria página
  do Elite Dev (paleta, escala tipográfica, raios, estados de foco) e reconstruímos os padrões em
  CSS próprio, com tokens nomeados por função.

## O que fiz sem IA

- Validação manual de todo o fluxo no Postman, papel por papel, incluindo os casos de erro.
- As decisões de escopo: o que entrava, o que ficava de fora e o que era exagero para o desafio.
- Leitura e revisão do que foi gerado antes de aceitar — foi assim que o problema do pagamento
  apareceu.

## Em resumo

Usei a ferramenta como um par que digita rápido, não como quem decide. As decisões de arquitetura,
os limites de escopo e as correções de rumo foram minhas; a ferramenta acelerou a escrita e me
ajudou a verificar o resultado. Onde eu não revisei com atenção, apareceu problema — e é por isso
que revisar continua sendo o trabalho.
