# Uso de IA neste projeto

## Ferramenta utilizada

Durante o desenvolvimento, utilizei o **Claude Code** (Anthropic), integrado ao VS Code, principalmente como ferramenta de apoio à implementação, revisão e verificação do projeto.

## Decisões de projeto

A arquitetura e as principais decisões técnicas foram definidas antes da implementação. Entre elas:

* Modelagem das entidades `User`, `Event`, `Seat`, `Reservation` e `Ticket`, incluindo os estados dos assentos e das reservas.
* Uso de **lock pessimista** para tratar concorrência na reserva de assentos, priorizando uma garantia forte dentro de uma transação curta.
* Expiração automática das reservas por meio de um job `@Scheduled`, evitando adicionar componentes externos desnecessários ao escopo, como Redis ou RabbitMQ.
* Assinatura dos dados do QR Code utilizando **HMAC-SHA256**, com validação segura da assinatura.
* Uso do **Flyway** como responsável pelo versionamento e criação do schema, mantendo o Hibernate apenas com `ddl-auto: validate`.
* Autenticação baseada em **JWT**, incluindo informações de papel do usuário e autorização das operações de acordo com cada perfil.

As justificativas dessas escolhas estão documentadas no [README do backend](backend/README.md#decisões-técnicas).

## Como a ferramenta foi utilizada

Depois que a estrutura e as decisões principais estavam definidas, utilizei a ferramenta principalmente para auxiliar em tarefas mais mecânicas de implementação, como criação de estruturas repetitivas, sugestões de código, ajustes de CSS e execução de testes.

Todo código utilizado no projeto passou por revisão antes de ser incorporado. Em diferentes momentos, sugestões geradas precisaram ser modificadas ou descartadas por não atenderem corretamente aos requisitos ou às decisões arquiteturais adotadas.

## Revisões e correções realizadas

Um exemplo importante ocorreu no fluxo de pagamento. Em uma implementação inicial, o endpoint de confirmação aceitava um valor indicando diretamente se o pagamento havia sido aprovado. Durante minha revisão percebi que isso permitiria que o próprio cliente controlasse o resultado da operação, criando uma falha grave no fluxo.

A solução foi alterada para que o cliente envie apenas os dados necessários para a tentativa de pagamento, enquanto um `PaymentGatewaySimulator` realiza a decisão no servidor, incluindo validação de Luhn e cartões específicos para os cenários de teste.

Durante essa alteração também identifiquei um problema relacionado à expiração de reservas. O método liberava o assento e, logo depois, lançava uma exceção. Como toda a operação estava dentro de uma transação, o Spring executava rollback e desfazia a própria liberação. O comportamento era parcialmente mascarado pelo job periódico de expiração. A correção foi realizada utilizando `@Transactional(noRollbackFor = ...)`.

Também fiz revisões específicas do projeto em relação ao enunciado. Essas revisões identificaram itens que ainda precisavam ser implementados ou documentados, como compartilhamento por link, dados iniciais para demonstração e a documentação sobre o uso de IA.

Outra revisão identificou a necessidade de declarar explicitamente funcionalidades que não haviam sido validadas em ambiente real. A integração com o TMDb, por exemplo, estava implementada, mas não havia sido executada diretamente contra a API externa. Essa informação passou a constar na seção de [Limitações conhecidas](backend/README.md#limitações-conhecidas), e o mapeamento da resposta foi posteriormente validado por teste.

Também optei por manter o catálogo de eventos disponível para usuários não autenticados, em vez de redirecionar automaticamente todo visitante para a tela de login, pois isso estava mais alinhado ao requisito de navegação pública.

No front-end, a primeira proposta visual também foi revisada. A identidade inicial foi substituída por uma implementação baseada na linguagem visual da página do Elite Dev, utilizando uma paleta, tipografia, espaçamentos, raios e estados de interação próprios definidos em CSS.

## Atividades realizadas diretamente

Além das decisões de arquitetura e das revisões de código, realizei manualmente:

* a validação dos fluxos da aplicação utilizando Postman;
* os testes dos diferentes papéis de usuário e cenários de erro;
* a definição do escopo e das funcionalidades que fariam parte da solução;
* a análise dos requisitos do desafio;
* a revisão do código antes de sua inclusão no projeto;
* a identificação e correção de problemas que surgiram durante os testes.

## Considerações finais

A IA foi utilizada como **ferramenta de apoio ao desenvolvimento**, principalmente para agilizar tarefas de implementação e auxiliar em verificações. A definição da solução, as escolhas arquiteturais, o controle de escopo, a validação dos requisitos e a decisão sobre quais sugestões seriam ou não utilizadas permaneceram sob minha responsabilidade.

O processo também mostrou a importância da revisão humana: algumas soluções inicialmente propostas apresentavam problemas que só foram identificados durante a análise e os testes do projeto.
